import com.slack.api.Slack
import com.slack.api.model.User

object SlackHelper {

    private val token = System.getenv("SLACK_BOT_TOKEN")
    private val slack = Slack.getInstance()
    private val methods = slack.methods(token)

    fun getChannelUserData(channel: String): UserData {
        val userIds = methods.conversationsMembers { builder ->
            builder.channel(channel)
        }.members

        if (userIds.isEmpty()) {
            println("userIds is empty")
            throw IllegalStateException("userData is null for channel $channel!")
        }

        return getUserData(userIds)
    }

    private fun getUserData(userIds: List<String>): UserData {
        // Map of users keyed by time zone
        val usersInTimeZones = mutableMapOf<String, MutableList<User>>()
        val userIdToUserMap = mutableMapOf<String, User>()

        // Now get the more details user objects from those userIds
        userIds.forEach { userId ->
            val userResult = methods.usersInfo { builder ->
                builder.user(userId)
            }
            userResult.user?.let { user ->
                val users = usersInTimeZones[user.tz] ?: mutableListOf()
                users.add(user)
                usersInTimeZones[user.tz] = users

                userIdToUserMap[userId] = user
            }
        }

        val userData = UserData(
            usersInTimeZones,
            userIdToUserMap
        )

        printUserDebugInfo(userData)

        return userData
    }

    private fun printUserDebugInfo(userData: UserData) {
        for ((timezone, users) in userData.usersInTimezones) {
            println("Users in timezone $timezone: ${users.joinToString(", ") { it.realName }}")
        }
    }
}