import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

// TODO: Create a cronjob that runs this program each Sunday morning
fun main() {
    val token = System.getenv("SLACK_BOT_TOKEN")
    val wordleChannel = System.getenv("WORDLE_CHANNEL")
    val channelToPostTo = System.getenv("POST_CHANNEL")
    val slack = Slack.getInstance()
    val methods = slack.methods(token)

    if (token == null) {
        println("SLACK_BOT_TOKEN not provided")
        return
    }

    if (wordleChannel == null) {
        println("WORDLE_CHANNEL not provided")
        return
    }

    if (channelToPostTo == null) {
        println("POST_CHANNEL not provided")
        return
    }

    // Get all the userIds of the members of the Wordle channel so that we can figure out which timezones they are
    // in for determining the applicable days of the contest
    val userIds = methods.conversationsMembers { builder ->
        builder.channel(wordleChannel)
    }.members

    if (userIds.isNullOrEmpty()) {
        println("userIds are null or empty")
        return
    }

    // Map of users keyed by time zone
    val usersInTimeZones = mutableMapOf<String, MutableList<User>>()
    val userIdToUserMap = mutableMapOf<String, User>()

    // Now get the more details user objects from those userIds
    userIds.forEach { userId ->
        val userResult = methods.usersInfo {
                builder -> builder.user(userId)
        }
        userResult.user?.let { user ->
            val users = usersInTimeZones[user.tz] ?: mutableListOf()
            users.add(user)
            usersInTimeZones[user.tz] = users

            userIdToUserMap[userId] = user
        }
    }

    // This is assuming the first day of the week is Sunday and the last day of the week is Saturday.
    // If you are running this for a country where that's not the case, adjust this.
    val firstDayOfWeek = DayOfWeek.SUNDAY
    val lastDayOfWeek = DayOfWeek.SATURDAY

    val wordleScoreCalculator = WordleScoreCalculator(usersInTimeZones, userIdToUserMap)

    var today = LocalDate.now()
    while (today.dayOfWeek != DayOfWeek.SUNDAY) {
       today = today.minusDays(1L)
    }
    val start = today.with(TemporalAdjusters.previous(firstDayOfWeek))
    val end = today.with(TemporalAdjusters.previous(lastDayOfWeek))

    for (timezone in usersInTimeZones.keys) {
        val zoneId = ZoneId.of(timezone)
        val zoneOffset = zoneId.rules.getOffset(LocalDateTime.now())
        val oldestTimestamp = start.atStartOfDay().toEpochSecond(zoneOffset).toString()
        val latestTimestamp = end.atTime(11, 59, 59, 999).toEpochSecond(zoneOffset).toString()

        val messages = methods.conversationsHistory { builder ->
            builder.channel(wordleChannel)
                .oldest(oldestTimestamp)
                .latest(latestTimestamp)
                .inclusive(true)
        }.messages ?: continue

        wordleScoreCalculator.calculateScoresFromMessagesForUsersInTimeZone(messages, timezone)
    }

    val finalScores = wordleScoreCalculator.calculateFinalScores()
    if (finalScores.isNotEmpty()) {
        val formatter = DateTimeFormatter.ofPattern("MM/dd")
        val topScore = finalScores.firstKey()
        val finalScoresStringBuilder = StringBuilder()
        val winners = finalScores[topScore]!!
        if (winners.count() > 1) {
            finalScoresStringBuilder.append("*Congratulations to this week's Wordle Winners (${start.format(formatter)} - ${end.format(formatter)}), ${winners.joinToString(", ") { it.user.profile.realName }} :clap:*\n\n")
        } else {
            finalScoresStringBuilder.append("*Congratulations to this week's Wordle Winner (${start.format(formatter)} - ${end.format(formatter)}), ${winners.first().user.profile.realName} :clap:*\n\n")
        }

        finalScoresStringBuilder.append("*Scores*:\n")
        for ((index, score) in finalScores.keys.withIndex()) {
            val medal = when (index) {
                0 -> ":first_place_medal:"
                1 -> ":second_place_medal:"
                2 -> ":third_place_medal:"
                else -> ""
            }
            finalScoresStringBuilder.append("• $medal ${finalScores[score]!!.joinToString(", ") { it.user.realName }} : ${finalScores[score]!!.first().guesses}\n\n")
        }

        val request = ChatPostMessageRequest.builder()
            .channel(channelToPostTo)
            .text(finalScoresStringBuilder.toString())
            .build()

        val response = methods.chatPostMessage(request)
        if (response.isOk) {
            println("Successfully calculated and reported Wordle Winner for this week.")
        } else {
            println("Failed to post score message to #wordle channel this week")
            println(response.errors)
        }
    }
}
