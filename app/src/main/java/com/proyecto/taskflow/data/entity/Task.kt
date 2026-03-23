package com.proyecto.taskflow.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Esta es la entidad central de la app, una tarea en la base de datos de Room
 * La mantengo deliberadamente simple para no complicar el CRUD ni la UI
 * Aquí solo hay datos, sin lógica de presentación
 *
 * Decisiones clave
 * - Guardo el vencimiento en epoch millis para comparar y ordenar rápido
 * - Uso dos claves foráneas, una a la lista y otra a la etiqueta
 * - Si se borra la lista, arrastro sus tareas con CASCADE
 * - Si se borra una etiqueta, no quiero borrar tareas, solo les quito la etiqueta con SET_NULL
 * - Creo índices sobre listId y labelId para acelerar filtros y ordenaciones
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE // si elimino una lista, elimino sus tareas
        ),
        ForeignKey(
            entity = Label::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onDelete = ForeignKey.SET_NULL // si elimino una etiqueta, las tareas quedan sin etiqueta
        )
    ],
    indices = [
        Index("listId"),  // voy a filtrar por lista con frecuencia
        Index("labelId")  // y también por etiqueta, así evito scans completos
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Título obligatorio, la validación la hago en la capa de UI antes de insertar
    val title: String,

    // Notas opcionales, si el usuario no escribe nada lo dejo en null para ahorrar espacio
    val notes: String? = null,

    // Fecha de vencimiento en millis, null significa que no hay fecha
    val dueAtMillis: Long? = null,

    // Estado de la tarea, arranco en pendiente y lo alterno desde la lista
    val done: Boolean = false,

    // Referencia a la lista propietaria, por defecto uso una lista "General"
    val listId: Long,

    // Etiqueta opcional, si no hay la dejo nula
    val labelId: Long? = null,

    // Marca temporal de creación, me permite ordenar de forma estable cuando no hay due date
    val createdAt: Long = System.currentTimeMillis()
)
