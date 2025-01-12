import com.slack.api.model.User

data class UserData(
    val usersInTimezones: Map<String, MutableList<User>>,
    val userIdToUserMap: Map<String, User>
)
