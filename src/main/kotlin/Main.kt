import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

fun main() {
    val token = System.getenv("SLACK_BOT_TOKEN")
    val wordleChannel = System.getenv("WORDLE_CHANNEL")
    val queensChannel = System.getenv("QUEENS_CHANNEL")
    val slack = Slack.getInstance()
    val methods = slack.methods(token)

    if (token == null) {
        println("SLACK_BOT_TOKEN not provided")
        return
    }

    val scoreCalculators = mutableListOf<ScoreCalculator>()

    if (wordleChannel != null) {
        scoreCalculators.add(WordleScoreCalculator(wordleChannel))
    }

    if (queensChannel != null) {
        scoreCalculators.add(QueensScoreCalculator(queensChannel))
    }

    // This is assuming the first day of the week is Sunday and the last day of the week is Saturday.
    // If you are running this for a country where that's not the case, adjust this.
    val firstDayOfWeek = DayOfWeek.SUNDAY
    val lastDayOfWeek = DayOfWeek.SATURDAY

    var today = LocalDate.now()
    while (today.dayOfWeek != DayOfWeek.SUNDAY) {
        today = today.minusDays(1L)
    }
    val start = today.with(TemporalAdjusters.previous(firstDayOfWeek))
    val end = today.with(TemporalAdjusters.previous(lastDayOfWeek))
    println("Contest start: $start")
    println("Contest end: $end")

    for (scoreCalculator in scoreCalculators) {
        val userData = scoreCalculator.userData
        for (timezone in userData.usersInTimezones.keys) {
            val zoneId = ZoneId.of(timezone)
            val zoneOffset = zoneId.rules.getOffset(LocalDateTime.now())
            val startTime = start.atStartOfDay()
            val oldestTimestamp = startTime.toEpochSecond(zoneOffset).toString()
            val endTime = end.atTime(23, 59, 59, 999999999)
            val latestTimestamp = endTime.toEpochSecond(zoneOffset).toString()

            val messages = methods.conversationsHistory { builder ->
                builder.channel(scoreCalculator.channel)
                    .oldest(oldestTimestamp)
                    .latest(latestTimestamp)
                    .inclusive(true)
            }.messages ?: continue

            scoreCalculator.calculateScoresFromMessagesForUsersInTimeZone(messages, timezone)
        }

        val finalScores = scoreCalculator.calculateFinalScores()
        if (finalScores.isNotEmpty()) {
            val formatter = DateTimeFormatter.ofPattern("MM/dd")
            val topScore = finalScores.firstKey()
            val finalScoresStringBuilder = StringBuilder()
            val winners = finalScores[topScore]!!
            if (winners.count() > 1) {
                finalScoresStringBuilder.append(
                    "*Congratulations to this week's ${scoreCalculator.gameName} Winners (${
                        start.format(
                            formatter
                        )
                    } - ${end.format(formatter)}), ${winners.joinToString(", ") { it.user.profile.realName }} :clap:*\n\n"
                )
            } else {
                finalScoresStringBuilder.append(
                    "*Congratulations to this week's ${scoreCalculator.gameName} Winner (${
                        start.format(
                            formatter
                        )
                    } - ${end.format(formatter)}), ${winners.first().user.profile.realName} :clap:*\n\n"
                )
            }

            finalScoresStringBuilder.append("*Scores*:\n")
            for ((index, score) in finalScores.keys.withIndex()) {
                val medal = when (index) {
                    0 -> ":first_place_medal:"
                    1 -> ":second_place_medal:"
                    2 -> ":third_place_medal:"
                    else -> ""
                }
                finalScoresStringBuilder.append(
                    "• $medal ${finalScores[score]!!.joinToString(", ") { it.user.realName }}: ${
                        scoreCalculator.formatScore(
                            finalScores[score]!!.first().score
                        )
                    }\n\n"
                )
            }

            val request = ChatPostMessageRequest.builder()
                .channel(scoreCalculator.channel)
                .text(finalScoresStringBuilder.toString())
                .build()

            val response = methods.chatPostMessage(request)
            if (response.isOk) {
                println("Successfully calculated and reported ${scoreCalculator.gameName} Winner for this week.")
            } else {
                println("Failed to post score message to #${scoreCalculator.channel} channel this week")
                println(response.errors)
            }
        }
    }
}
