import com.slack.api.model.User

data class UserScoreData(val user: User, var guesses: Int, var days: Int)