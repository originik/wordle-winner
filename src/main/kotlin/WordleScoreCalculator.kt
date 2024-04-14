import com.slack.api.model.Message
import com.slack.api.model.User
import java.util.SortedMap

class WordleScoreCalculator(
    private val usersInTimeZones: MutableMap<String, MutableList<User>>,
    private val userIdToUserMap: MutableMap<String, User>
) {
    private val usersToScoreDataMap = mutableMapOf<String, UserScoreData>()

    fun calculateScoresFromMessagesForUsersInTimeZone(messages: List<Message>, timeZone: String) {
        for (message in messages) {
            val user = userFromId(message.user) ?: continue
            if (!isUserInTimeZone(user, timeZone)) {
                continue
            }



            val guesses =
                guessesFromMessage(message.text) ?: continue // Probably a message not containing a Wordle score
            val scoreData = usersToScoreDataMap[user.id] ?: UserScoreData(user, 0, 0)
            scoreData.days += 1
            scoreData.guesses += guesses
            usersToScoreDataMap[user.id] = scoreData
        }
    }

    fun calculateFinalScores(): SortedMap<Int, MutableList<UserScoreData>> {
        val finalScoreMap = sortedMapOf<Int, MutableList<UserScoreData>>()
        usersToScoreDataMap.values.forEach { userScoreData ->
            if (userScoreData.days > CONTEST_DAYS) {
                // TODO: See if there is a way to de-dup the extra days, or maybe there is an
                //  error with the start and end days that is including extra Wordle posts
                println("User ${userScoreData.user.realName} has ${userScoreData.guesses} guesses. Figure out what went wrong.")
            } else if (userScoreData.days < CONTEST_DAYS) {
                val missedDays = CONTEST_DAYS - userScoreData.days
                userScoreData.guesses += missedDays * FAIL_GUESSES
                if (!finalScoreMap.containsKey(userScoreData.guesses)) {
                    finalScoreMap[userScoreData.guesses] = mutableListOf()
                }
                finalScoreMap[userScoreData.guesses]?.add(userScoreData)
            } else {
                if (!finalScoreMap.containsKey(userScoreData.guesses)) {
                    finalScoreMap[userScoreData.guesses] = mutableListOf()
                }
                finalScoreMap[userScoreData.guesses]?.add(userScoreData)
            }
        }
        return finalScoreMap.toSortedMap()
    }

    private fun userFromId(userId: String) = userIdToUserMap[userId]

    private fun isUserInTimeZone(user: User, timeZone: String): Boolean {
        return usersInTimeZones[timeZone]?.firstOrNull { it.id == user.id } != null
    }

    private fun guessesFromMessage(message: String): Int? {
        val matchResult = pointsRegex.find(message) ?: return null
        return if (matchResult.groupValues.count() == 3) {
            val score = matchResult.groupValues[2]
            if (score == "X") {
                FAIL_GUESSES
            } else {
                try {
                    score.toInt()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        } else {
            null
        }

    }

    companion object {
        private const val CONTEST_DAYS = 7
        private const val FAIL_GUESSES = 7
        private val pointsRegex = Regex("Wordle [0-9]{1,3}(,[0-9]{3})* ([\\dX])/\\d")
    }
}