package com.proyecto.taskflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.proyecto.taskflow.data.entity.Task
import kotlinx.coroutines.flow.Flow

/**
 * Puerta de entrada a la tabla tasks
 * Aquí defino operaciones:
 * - insert para crear
 * - observeAllOrdered para escuchar cambios en tiempo real con Flow
 * - getAllOrdered para obtener un snapshot puntual
 * - getById para cargar una tarea al editar
 * - setDone para alternar el estado de forma ligera
 * - update y delete para CRUD completo
 *
 * Criterio de orden estable
 * 1) Tareas con fecha primero y sin fecha después
 * 2) Dentro de las que tienen fecha, las más próximas primero
 * 3) Empates resueltos por createdAt para que la lista no “salte”
 */
@Dao
interface TaskDao {

    // Inserto y, si ya existe un id en conflicto, reemplazo
    // Devuelvo el id generado para enlazarlo si hace falta
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    // Observación reactiva de todas las tareas
    // Flow emite listas cada vez que hay cambios en la tabla
    @Query("""
        SELECT * FROM tasks
        ORDER BY 
          CASE WHEN dueAtMillis IS NULL THEN 1 ELSE 0 END,
          dueAtMillis ASC,
          createdAt ASC
    """)
    fun observeAllOrdered(): Flow<List<Task>>

    // Lectura puntual con el mismo criterio de orden
    // Útil para recomponer filtros bajo demanda
    @Query("""
        SELECT * FROM tasks
        ORDER BY 
          CASE WHEN dueAtMillis IS NULL THEN 1 ELSE 0 END,
          dueAtMillis ASC,
          createdAt ASC
    """)
    suspend fun getAllOrdered(): List<Task>

    // Cargo una tarea concreta para edición
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Task?

    // Cambio solo el campo done, más eficiente que actualizar toda la fila
    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    // Actualizo todos los campos según el id de la entity
    @Update
    suspend fun update(task: Task)

    // Elimino una tarea concreta
    @Delete
    suspend fun delete(task: Task)
}
