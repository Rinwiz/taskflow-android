package com.proyecto.taskflow.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.proyecto.taskflow.data.dao.LabelDao
import com.proyecto.taskflow.data.dao.TaskDao
import com.proyecto.taskflow.data.dao.TaskListDao
import com.proyecto.taskflow.data.entity.Label
import com.proyecto.taskflow.data.entity.Task
import com.proyecto.taskflow.data.entity.TaskList

/**
 * Configuración central de Room
 * Aquí declaro qué entidades forman parte de la base de datos y expongo sus DAOs
 * Sigo el patrón singleton para que toda la app use la misma instancia
 *
 * Decisiones
 * - version = 1 porque es el primer esquema estable
 * - exportSchema = false para simplificar el proyecto en esta fase
 *   En proyectos grandes, exportaría el esquema a una carpeta para controlar migraciones
 */
@Database(
    entities = [Task::class, TaskList::class, Label::class],
    version = 1,
    exportSchema = false
)
abstract class TaskFlowDatabase : RoomDatabase() {

    // Puntos de acceso a cada tabla
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun labelDao(): LabelDao

    companion object {
        // La marca Volatile garantiza visibilidad entre hilos
        @Volatile private var INSTANCE: TaskFlowDatabase? = null

        /**
         * Devuelvo siempre la misma instancia de base de datos
         * Uso applicationContext para evitar pérdidas de memoria asociadas a Activities
         */
        fun getInstance(context: Context): TaskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                // Si aún no existe, la creo de forma segura entre hilos
                Room.databaseBuilder(
                    context.applicationContext,
                    TaskFlowDatabase::class.java,
                    "taskflow.db"
                )
                    // Arranco sin fallback destructivo para proteger datos
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
