import com.slack.api.model.User

data class UserScoreData(val user: User, var guesses: Int, var days: Int) {
    private var guessKeys = mutableSetOf<String>()
    fun updateGuesses(key: String, guessCount: Int) {
        if (guessKeys.contains(key)) return

        // We haven't processed this guess yet, so update guesses and days
        guesses += guessCount
        days += 1
        guessKeys.add(key)
    }
}