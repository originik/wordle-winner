import com.slack.api.model.Message
import com.slack.api.model.User
import java.util.*

abstract class ScoreCalculator(
    val channel: String,
    val gameName: String
) {
    val contestDays = 7
    val userData: UserData = SlackHelper.getChannelUserData(channel)

    protected val usersToScoreDataMap = mutableMapOf<String, UserScoreData>()

    abstract fun calculateScoresFromMessagesForUsersInTimeZone(messages: List<Message>, timeZone: String)
    abstract fun calculateFinalScores(): SortedMap<Int, MutableList<UserScoreData>>
    abstract fun formatScore(score: Int): String

    fun userFromId(userId: String) = userData.userIdToUserMap[userId]

    fun isUserInTimeZone(user: User, timeZone: String): Boolean {
        return userData.usersInTimezones[timeZone]?.firstOrNull { it.id == user.id } != null
    }
}