import com.slack.api.model.Message
import java.util.*
import kotlin.math.pow

class QueensScoreCalculator(channel: String) : ScoreCalculator(channel, "Queens") {

    private val solveTimeRegex = Regex("Queens #([\\d,]+) \\| ([\\d:]+)")
    override fun calculateScoresFromMessagesForUsersInTimeZone(messages: List<Message>, timeZone: String) {
        for (message in messages) {
            val user = userFromId(message.user) ?: continue
            if (!isUserInTimeZone(user, timeZone)) {
                continue
            }

            val (puzzleKey, seconds) = secondsToSolvePuzzleFromMessage(message.text)
                ?: continue // Probably a message not containing a Queens result
            println("${user.realName} solved puzzle #$puzzleKey in $seconds seconds")
            val scoreData = usersToScoreDataMap[user.id] ?: UserScoreData(user, 0, 0)
            scoreData.updateScore(puzzleKey, seconds)
            usersToScoreDataMap[user.id] = scoreData
        }
    }

    override fun calculateFinalScores(): SortedMap<Int, MutableList<UserScoreData>> {
        val finalScoreMap = sortedMapOf<Int, MutableList<UserScoreData>>()
        usersToScoreDataMap.values.forEach { userScoreData ->
            if (userScoreData.days > contestDays) {
                println("User ${userScoreData.user.realName} has shared too many Queens results. Figure out what went wrong.")
            } else if (userScoreData.days < contestDays) {
                println("User ${userScoreData.user.realName} missed ${contestDays - userScoreData.days} days and therefore cannot win.")
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
        var runningScore = score
        var str = ""
        val divisors = listOf(3600, 60, 1)
        for (divisor in divisors) {
            val quotient = runningScore / divisor
            if (quotient > 0) {
                if (str.isNotEmpty() && quotient < 10) {
                    str += "0"
                }
                str += "$quotient:"
                runningScore -= quotient * divisor
            } else if (divisor == 60 && str.isEmpty()) {
                str += "0:"
            }
        }

        return str.removeSuffix(":")
    }

    private fun secondsToSolvePuzzleFromMessage(message: String): Pair<String, Int>? {
        val matchResult = solveTimeRegex.find(message) ?: return null
        return if (matchResult.groupValues.count() == 3) {
            val key = matchResult.groupValues[1]
            val timeToSolve = matchResult.groupValues[2]
            key to secondsToSolve(timeToSolve)
        } else {
            null
        }
    }

    /**
     * Convert a string of the format 1:02:32 where the 1 is hours, the 02 is minutes, and the 32 is seconds
     * to the total number of seconds. For this example, it would be 3752 seconds.
     *
     * This will also handle strings like "02:32" and strings like "32" (only seconds)
     */
    private fun secondsToSolve(timeToSolve: String): Int {
        val parts = timeToSolve.split(":")
        var seconds = 0
        val partCount = parts.count()
        for (i in 0..<partCount) {
            val exponent = (partCount - i - 1).toDouble()
            val multiplier = (60.0.pow(exponent)).toInt()
            seconds += parts[i].toInt() * multiplier
        }
        return seconds
    }
}