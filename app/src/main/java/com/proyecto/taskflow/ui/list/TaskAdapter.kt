package com.proyecto.taskflow.ui.list

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.proyecto.taskflow.R
import com.proyecto.taskflow.data.entity.Task
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador de la lista de tareas
 * Aquí convierto cada Task en una fila visual usando item_task.xml
 * Uso ListAdapter para que DiffUtil calcule qué ha cambiado y la lista se actualice de forma eficiente
 * Expongo dos callbacks, clic corto para alternar hecho y clic largo para editar
 */
class TaskAdapter(
    private val onItemClicked: (Task) -> Unit,
    private val onItemLongClicked: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskVH>(Diff) {

    // Formateadores de fecha y hora, los creo una vez y los reutilizo
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskVH {
        // Inflo la vista de cada fila a partir del layout item_task
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        // Entrego al ViewHolder los formateadores y los callbacks para no acoplarlo al adaptador
        return TaskVH(v, onItemClicked, onItemLongClicked, dateFormat, timeFormat)
    }

    override fun onBindViewHolder(holder: TaskVH, position: Int) =
        // Pido al ViewHolder que pinte la tarea en la posición dada
        holder.bind(getItem(position))

    /**
     * ViewHolder, aquí guardo las referencias a las vistas y explico cómo pinto una Task
     * Mantengo la lógica de presentación contenida, así el adaptador queda limpio
     */
    class TaskVH(
        view: View,
        private val onItemClicked: (Task) -> Unit,
        private val onItemLongClicked: (Task) -> Unit,
        private val dateFormat: SimpleDateFormat,
        private val timeFormat: SimpleDateFormat
    ) : RecyclerView.ViewHolder(view) {

        private val vDot: View = view.findViewById(R.id.vDot)
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        private val tvNotes: TextView = view.findViewById(R.id.tvNotes)

        fun bind(task: Task) {
            // Título, lo coloco tal cual viene de la entidad
            tvTitle.text = task.title

            // Meta, compongo fecha y hora si existen
            // Prefiero construir una lista de partes y unirlas, así es fácil añadir más datos en el futuro
            val parts = mutableListOf<String>()
            task.dueAtMillis?.let { millis ->
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                parts.add("${dateFormat.format(cal.time)} ${timeFormat.format(cal.time)}")
            }
            tvMeta.text = parts.joinToString(" • ")

            // Notas, enseño solo la primera línea para no saturar la fila
            val firstLine = task.notes?.lineSequence()?.firstOrNull()?.trim().orEmpty()
            tvNotes.visibility = if (firstLine.isEmpty()) View.GONE else View.VISIBLE
            tvNotes.text = firstLine

            // Estilo visual si la tarea está hecha, tachado en el título y ligera atenuación del item
            tvTitle.paintFlags = if (task.done)
                tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            itemView.alpha = if (task.done) 0.6f else 1f

            // Indicador de estado con un punto de color
            // Hecha o sin fecha, gris
            // Vencida, rojo
            // Para hoy, naranja
            // Futuro, verde
            val ctx = itemView.context
            val colorRes = when {
                task.done -> android.R.color.darker_gray
                task.dueAtMillis == null -> android.R.color.darker_gray
                isOverdue(task.dueAtMillis!!) -> android.R.color.holo_red_light
                isToday(task.dueAtMillis!!) -> android.R.color.holo_orange_light
                else -> android.R.color.holo_green_light
            }
            vDot.backgroundTintList = ContextCompat.getColorStateList(ctx, colorRes)

            // Eventos de interacción
            // Clic corto alterna el estado de la tarea
            itemView.setOnClickListener { onItemClicked(task) }
            // Clic largo abre el editor, devuelvo true para consumir el evento
            itemView.setOnLongClickListener { onItemLongClicked(task); true }
        }

        // Utilidad para saber si está vencida
        private fun isOverdue(millis: Long): Boolean = millis < System.currentTimeMillis()

        // Utilidad para saber si vence hoy comparando año y día del año
        private fun isToday(millis: Long): Boolean {
            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        }
    }

    /**
     * DiffUtil define cuándo dos items son el mismo y cuándo su contenido cambió
     * Así evito refrescar toda la lista y consigo animaciones y rendimiento mejores
     */
    object Diff : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
