package com.proyecto.taskflow.ui.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.proyecto.taskflow.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.proyecto.taskflow.data.db.TaskFlowDatabase
import com.proyecto.taskflow.data.entity.*

/**
 * Editor de tareas
 * Esta pantalla sirve tanto para crear como para editar
 * La decisión se toma con el argumento 'taskId'
 * - taskId > 0   edito una existente
 * - taskId == -1 creo una nueva
 *
 * Diseño de la interacción
 * - Los campos de fecha y hora no son editables a mano, se abren con pickers de Material
 * - Valido título obligatorio de forma amable, muestro error y aviso con Toast
 * - Guardo en Room en un hilo de IO y vuelvo al hilo principal para navegar y enseñar Snackbar
 */
class EditTaskFragment : Fragment(R.layout.fragment_edit_task) {

    // Estado temporal del vencimiento, lo compongo con estos tres valores
    private var selectedDateMillis: Long? = null
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null

    // Formateadores para pintar al usuario lo que elige
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Si taskId > 0 estoy editando, si es -1 estoy creando
    private val taskId: Long by lazy { arguments?.getLong("taskId", -1L) ?: -1L }
    private var loadedTask: Task? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias de UI, mantengo nombres claros según el XML
        val tilTitle = view.findViewById<TextInputLayout>(R.id.tilTitle)
        val etTitle  = view.findViewById<TextInputEditText>(R.id.etTitle)
        val etNotes  = view.findViewById<TextInputEditText>(R.id.etNotes)

        val tilDueDate = view.findViewById<TextInputLayout>(R.id.tilDueDate)
        val etDueDate  = view.findViewById<TextInputEditText>(R.id.etDueDate)
        val tilDueTime = view.findViewById<TextInputLayout>(R.id.tilDueTime)
        val etDueTime  = view.findViewById<TextInputEditText>(R.id.etDueTime)

        val tilLabel   = view.findViewById<TextInputLayout>(R.id.tilLabel)
        val actvLabel  = view.findViewById<AutoCompleteTextView>(R.id.actvLabel)

        val btnCancel  = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave    = view.findViewById<MaterialButton>(R.id.btnSave)

        // Etiquetas de ejemplo, suficientes para probar el desplegable
        // En el paso de persistencia ya dejamos listo cargar y crear etiquetas reales
        val demoLabels = listOf("Trabajo", "Personal", "Urgente", "Ideas")
        actvLabel.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, demoLabels)
        )

        // Si vengo en modo edición, cargo la tarea desde Room y precargo los campos
        if (taskId > 0) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val db = TaskFlowDatabase.getInstance(requireContext())
                val t = db.taskDao().getById(taskId)
                loadedTask = t

                // Si la tarea tiene labelId, busco el nombre de la etiqueta
                val loadedLabelName: String? = t?.labelId?.let { labelId ->
                    db.labelDao().getById(labelId)?.name
                }

                withContext(Dispatchers.Main) {
                    t?.let { task ->
                        // Título y notas se colocan tal cual, las notas pueden ser nulas
                        etTitle.setText(task.title)
                        etNotes.setText(task.notes ?: "")

                        // Si hay vencimiento, guardo milis y pinto fecha y hora formateadas
                        task.dueAtMillis?.let { millis ->
                            selectedDateMillis = millis
                            val cal = Calendar.getInstance().apply { timeInMillis = millis }
                            etDueDate.setText(dateFormat.format(cal.time))
                            selectedHour = cal.get(Calendar.HOUR_OF_DAY)
                            selectedMinute = cal.get(Calendar.MINUTE)
                            etDueTime.setText(timeFormat.format(cal.time))
                        }

                        // 🔹 Si la tarea tenía etiqueta, la muestro en el desplegable
                        if (!loadedLabelName.isNullOrBlank()) {
                            // El 'false' evita que salte el desplegable al poner el texto
                            actvLabel.setText(loadedLabelName, false)
                        }

                        btnSave.text = "Guardar cambios"
                    }
                }
            }
        }


        // Selector de fecha, uso MaterialDatePicker en modo calendar
        etDueDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha de vencimiento")
                // Si ya había una fecha elegida, la reutilizo, si no, hoy
                .setSelection(selectedDateMillis ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            // Al confirmar, guardo milis y pinto el texto formateado
            picker.addOnPositiveButtonClickListener { utcMillis ->
                selectedDateMillis = utcMillis
                etDueDate.setText(dateFormat.format(utcMillis))
            }
            picker.show(parentFragmentManager, "datePicker")
        }
        // Icono de limpiar fecha, borro estado y texto
        tilDueDate.setEndIconOnClickListener {
            selectedDateMillis = null
            etDueDate.setText("")
        }

        // Selector de hora, 24h por claridad
        etDueTime.setOnClickListener {
            val hour = selectedHour ?: 12
            val minute = selectedMinute ?: 0
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Selecciona hora")
                .build()

            // Al confirmar, guardo hora y minuto y pinto formateado
            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, selectedHour!!)
                cal.set(Calendar.MINUTE, selectedMinute!!)
                etDueTime.setText(timeFormat.format(cal.time))
            }
            timePicker.show(parentFragmentManager, "timePicker")
        }
        // Icono de limpiar hora
        tilDueTime.setEndIconOnClickListener {
            selectedHour = null
            selectedMinute = null
            etDueTime.setText("")
        }

        // Cancelar vuelve atrás sin tocar la base de datos
        btnCancel.setOnClickListener { findNavController().popBackStack() }

        // Guardar, valido título y compongo el timestamp si hay fecha u hora
        btnSave.setOnClickListener {
            val title = etTitle.text?.toString()?.trim().orEmpty()
            val notes = etNotes.text?.toString()?.trim().orEmpty()
            val labelName = actvLabel.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                tilTitle.error = "El título es obligatorio"
                Toast.makeText(requireContext(), "Añade un título", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                tilTitle.error = null
            }

            val dueAtMillis = combineDateTime(selectedDateMillis, selectedHour, selectedMinute)

            // Persistencia en Room fuera del hilo principal
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val db = TaskFlowDatabase.getInstance(requireContext())
                // Me aseguro de tener una lista por defecto y, si hay nombre de etiqueta, lo resuelvo o creo
                val listId = ensureDefaultList(db, "General")
                val labelId: Long? = if (labelName.isNotEmpty()) findOrCreateLabel(db, labelName) else null

                if (taskId > 0 && loadedTask != null) {
                    // Editar, mantengo id y createdAt
                    val updated = loadedTask!!.copy(
                        title = title,
                        notes = notes.ifEmpty { null },
                        dueAtMillis = dueAtMillis,
                        listId = listId,      // si quisiera respetar la lista original, usaría loadedTask!!.listId
                        labelId = labelId
                    )
                    db.taskDao().update(updated)
                } else {
                    // Crear nueva
                    val task = Task(
                        title = title,
                        notes = notes.ifEmpty { null },
                        dueAtMillis = dueAtMillis,
                        listId = listId,
                        labelId = labelId
                    )
                    db.taskDao().insert(task)
                }

                // De vuelta al hilo principal, aviso y cierro el editor
                withContext(Dispatchers.Main) {
                    Snackbar.make(view, "Cambios guardados", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    /**
     * Combino fecha y hora en un único instante en milis
     * Si no hay fecha ni hora, devuelvo null para representar “sin vencimiento”
     */
    private fun combineDateTime(dateUtcMillis: Long?, hour: Int?, minute: Int?): Long? {
        if (dateUtcMillis == null && hour == null && minute == null) return null
        val cal = Calendar.getInstance()
        if (dateUtcMillis != null) cal.timeInMillis = dateUtcMillis else cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, hour ?: 0)
        cal.set(Calendar.MINUTE,      minute ?: 0)
        cal.set(Calendar.SECOND,      0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Garantizo que existe una lista por defecto
     * Si no existe, la creo y devuelvo su id
     */
    private suspend fun ensureDefaultList(db: TaskFlowDatabase, name: String): Long {
        val dao = db.taskListDao()
        val existing = dao.getByName(name)
        if (existing != null) return existing.id
        val insertedId = dao.insert(TaskList(name = name))
        return if (insertedId != 0L) insertedId else dao.getByName(name)!!.id
    }

    /**
     * Resuelvo o creo una etiqueta por nombre
     * Devuelvo el id para guardar la referencia en la tarea
     */
    private suspend fun findOrCreateLabel(db: TaskFlowDatabase, name: String): Long {
        val dao = db.labelDao()
        val existing = dao.getByName(name)
        if (existing != null) return existing.id
        val insertedId = dao.insert(Label(name = name))
        return if (insertedId != 0L) insertedId else dao.getByName(name)!!.id
    }
}
