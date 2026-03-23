package com.proyecto.taskflow.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Etiquetas de clasificación
 * Con las labels agrupo tareas por temas transversales como Urgente o Personal
 * Mantengo solo el nombre para empezar sencillo y rápido
 *
 * Decisión importante
 * - Creo un índice único sobre name para evitar duplicados
 *   Así puedo hacer “buscar o crear” sin miedo a tener dos etiquetas iguales
 */
@Entity(
    tableName = "labels",
    indices = [Index(value = ["name"], unique = true)]
)
data class Label(
    // Id autogenerado por Room, me olvido de gestionarlo manualmente
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Nombre visible de la etiqueta
    val name: String
)
