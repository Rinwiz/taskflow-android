package com.proyecto.taskflow.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.proyecto.taskflow.data.entity.Label

/**
 * DAO de etiquetas
 * Aquí resuelvo el patrón "buscar o crear" apoyándome en el índice único por name
 * Si intento insertar un nombre ya existente, IGNORE evita el duplicado y me devuelve 0
 * Con getByName puedo recuperar después la etiqueta existente y su id
 */
@Dao
interface LabelDao {

    // Inserto una etiqueta y, si el nombre ya existe, no hago nada y recibo 0 como id
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(label: Label): Long

    // Busco por nombre para completar el "buscar o crear"
    @Query("SELECT * FROM labels WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Label?

    // Busco etiqueta por id (para precargarla al editar)
    @Query("SELECT * FROM labels WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Label?

    // Listado ordenado alfabéticamente
    @Query("SELECT * FROM labels ORDER BY name")
    suspend fun getAll(): List<Label>
}
