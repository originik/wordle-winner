import com.slack.api.model.User

data class UserScoreData(val user: User, var score: Int, var days: Int) {
    private var puzzleKeys = mutableSetOf<String>()
    fun updateScore(key: String, score: Int) {
        if (puzzleKeys.contains(key)) return

        // We haven't processed this score yet, so update score and days
        this.score += score
        days += 1
        puzzleKeys.add(key)
    }
}