package com.example.data.database

data class ExportedRoutine(
    val name: String,
    val description: String,
    val exercises: List<ExportedPlanExercise>
)

data class ExportedPlanExercise(
    val exerciseName: String,
    val targetSets: Int,
    val orderIndex: Int
)

data class ExportData(
    val version: Int = 1,
    val routines: List<ExportedRoutine>
)
