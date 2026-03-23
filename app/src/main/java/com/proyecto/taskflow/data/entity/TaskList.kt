package com.proyecto.taskflow.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Listas de tareas
 * Esta entidad existe para agrupar tareas por contextos sencillos
 * Mantengo el modelo mínimo, solo nombre, para no complicar la primera versión
 * Si más adelante quiero color, icono u orden manual, ampliaré campos y crearé una migración
 */
@Entity(tableName = "task_lists")
data class TaskList(
    // Id autogenerado, así no me preocupo de gestionarlo en la app
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Nombre visible de la lista, por ejemplo General, Trabajo o Personal
    val name: String
)
