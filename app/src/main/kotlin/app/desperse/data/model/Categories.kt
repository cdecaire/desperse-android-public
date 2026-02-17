package app.desperse.data.model

/**
 * Preset categories matching web app.
 * Max 3 per post.
 */
object Categories {
    val PRESETS = listOf(
        "Comics",
        "Illustration",
        "Digital Art",
        "Photography",
        "3D / CG",
        "Animation / Motion",
        "Design",
        "Video",
        "Music",
        "Writing",
        "Education",
        "Memes"
    )

    const val MAX_CATEGORIES = 3
}
