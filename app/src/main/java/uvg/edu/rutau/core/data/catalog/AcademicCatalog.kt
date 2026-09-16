package uvg.edu.rutau.core.data.catalog

/** Academic institutions and campuses available during the frontend MVP. */
object AcademicCatalog {
    private val campusesByUniversity = linkedMapOf(
        "Universidad de San Carlos de Guatemala" to listOf(
            "Campus Central",
            "Centro Universitario Metropolitano (CUM)",
            "Campus Central - Zona 12",
        ),
        "Universidad del Valle de Guatemala" to listOf(
            "Campus Central",
            "Campus Sur",
        ),
        "Universidad Rafael Landívar" to listOf(
            "Campus Central",
        ),
        "Universidad Francisco Marroquín" to listOf(
            "Campus Central",
        ),
    )

    val universities: List<String> = campusesByUniversity.keys.toList()

    fun campusesFor(university: String): List<String> =
        campusesByUniversity[university].orEmpty()
}
