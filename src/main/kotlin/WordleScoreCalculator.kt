import com.slack.api.model.Message
import java.util.*

class WordleScoreCalculator(channel: String) : ScoreCalculator(channel, "Wordle") {

    override fun calculateScoresFromMessagesForUsersInTimeZone(messages: List<Message>, timeZone: String) {
        for (message in messages) {
            val user = userFromId(message.user) ?: continue
            if (!isUserInTimeZone(user, timeZone)) {
                continue
            }

            val (key, guessCount) = guessCountFromMessage(message.text)
                ?: continue // Probably a message not containing a Wordle score
            val scoreData = usersToScoreDataMap[user.id] ?: UserScoreData(user, 0, 0)
            scoreData.updateScore(key, guessCount)
            usersToScoreDataMap[user.id] = scoreData
        }
    }

    override fun calculateFinalScores(): SortedMap<Int, MutableList<UserScoreData>> {
        val finalScoreMap = sortedMapOf<Int, MutableList<UserScoreData>>()
        usersToScoreDataMap.values.forEach { userScoreData ->
            if (userScoreData.days > contestDays) {
                println("User ${userScoreData.user.realName} has ${userScoreData.score} guesses. Figure out what went wrong.")
            } else if (userScoreData.days < contestDays) {
                val missedDays = contestDays - userScoreData.days
                userScoreData.score += missedDays * FAIL_GUESSES
                if (!finalScoreMap.containsKey(userScoreData.score)) {
                    finalScoreMap[userScoreData.score] = mutableListOf()
                }
                finalScoreMap[userScoreData.score]?.add(userScoreData)
            } else {
                if (!finalScoreMap.containsKey(userScoreData.score)) {
                    finalScoreMap[userScoreData.score] = mutableListOf()
                }
                finalScoreMap[userScoreData.score]?.add(userScoreData)
            }
        }
        return finalScoreMap
    }

    override fun formatScore(score: Int): String {
        return score.toString()
    }

    private fun guessCountFromMessage(message: String): Pair<String, Int>? {
        val matchResult = pointsRegex.find(message) ?: return null
        return if (matchResult.groupValues.count() == 4) {
            val score = matchResult.groupValues[3]
            val key = matchResult.groupValues[1]
            if (score == "X") {
                Pair(key, FAIL_GUESSES)
            } else {
                try {
                    Pair(key, score.toInt())
                } catch (e: NumberFormatException) {
                    null
                }
            }
        } else {
            null
        }
    }

    companion object {
        private const val FAIL_GUESSES = 7
        private val pointsRegex = Regex("Wordle ([0-9]{1,3}(,[0-9]{3}))* ([\\dX])/\\d")
    }
}