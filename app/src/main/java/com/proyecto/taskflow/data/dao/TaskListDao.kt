package com.proyecto.taskflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.proyecto.taskflow.data.entity.TaskList

/**
 * DAO de listas
 * Mantengo operaciones mínimas y claras porque, hoy, solo necesito garantizar una lista por defecto
 * Decisiones
 * - insert con IGNORE para no duplicar por nombre cuando fuerce la creación de "General"
 * - getByName para resolver rápidamente el id de una lista concreta
 * - getAll por si más adelante quiero un selector de listas en la UI
 */
@Dao
interface TaskListDao {

    // Intento insertar y, si ya existe una fila con el mismo nombre, Room devuelve 0
    // Ese 0 me sirve para saber que no se insertó por conflicto y recuperar la existente
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(list: TaskList): Long

    // Busco por nombre, útil para garantizar la lista "General"
    @Query("SELECT * FROM task_lists WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TaskList?

    // Listado alfabético, me viene bien para futuros combos o ajustes
    @Query("SELECT * FROM task_lists ORDER BY name")
    suspend fun getAll(): List<TaskList>
}
