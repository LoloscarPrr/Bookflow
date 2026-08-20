package app.bookflow.reader.core

enum class Pillar(
    val title: String,
    val subtitle: String,
) {
    READING(
        title = "Lectura",
        subtitle = "Importa, organiza y lee PDF, DOCX y texto con una experiencia limpia."
    ),
    NARRATION(
        title = "Narración",
        subtitle = "Voz offline o neuronal, reproducción continua y paisaje sonoro contextual."
    ),
    AI_COMPREHENSION(
        title = "Comprensión IA",
        subtitle = "Un cerebro del libro que entiende tu progreso sin adelantarte spoilers."
    )
}
