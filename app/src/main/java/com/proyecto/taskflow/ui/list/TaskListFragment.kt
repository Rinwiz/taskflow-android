package com.proyecto.taskflow.ui.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.proyecto.taskflow.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.proyecto.taskflow.data.db.TaskFlowDatabase
import com.proyecto.taskflow.data.entity.Task
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.os.bundleOf
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.collectLatest

/**
 * Pantalla de lista de tareas
 * Aquí conecto la interfaz con los datos de Room y aplico los filtros
 * La idea es clara, escucho los cambios de la base de datos, filtro en memoria y pinto en el RecyclerView
 */
class TaskListFragment : Fragment(R.layout.fragment_task_list) {

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var db: TaskFlowDatabase

    private lateinit var actvFilterLabel: MaterialAutoCompleteTextView
    private lateinit var tbgState: MaterialButtonToggleGroup

    // Filtro por estado, arranco mostrando todo
    private var stateFilter: StateFilter = StateFilter.ALL
    // Necesito traducir el nombre que el usuario elige a su id real de etiqueta
    private var labelNameToId: Map<String, Long> = emptyMap()
    // Si es null, significa que no filtro por etiqueta y muestro todas
    private var selectedLabelId: Long? = null

    // Adaptador de la lista, delego acciones sencillas
    // Clic corto alterna hecho o pendiente, clic largo abre el editor para esa tarea
    private val adapter = TaskAdapter(
        onItemClicked = { task -> toggleDone(task) },
        onItemLongClicked = { task -> openEditor(task) }
    )

    enum class StateFilter { ALL, PENDING, DONE }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pido una instancia única de la base de datos
        db = TaskFlowDatabase.getInstance(requireContext())

        // Enlazo vistas del layout
        rv = view.findViewById(R.id.rvTasks)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        rv.adapter = adapter

        actvFilterLabel = view.findViewById(R.id.actvFilterLabel)
        tbgState = view.findViewById(R.id.tbgState)

        // Cargo etiquetas desde Room en un hilo de IO y preparo el desplegable
// Aquí construyo el mapa nombre → id para poder filtrar por labelId con precisión
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val labels = db.labelDao().getAll().sortedBy { it.name }
            labelNameToId = labels.associate { it.name to it.id }
            val items = listOf("Todas las etiquetas") + labels.map { it.name }

            // Configuro el AutoComplete en el hilo principal, que es el de la UI
            withContext(Dispatchers.Main) {
                actvFilterLabel.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
                )

                // Cuando el usuario elige una etiqueta, traduzco a id y llamo a refresh
                actvFilterLabel.setOnItemClickListener { _, _, position, _ ->
                    selectedLabelId = if (position == 0) {
                        null
                    } else {
                        val chosenName = items[position]
                        labelNameToId[chosenName]
                    }
                    refreshFilter()
                }

                // Al volver de otra pantalla, mantengo visible la etiqueta actualmente filtrada
                val currentLabelName = selectedLabelId?.let { currentId ->
                    // Busco el nombre cuyo id coincide con el filtro actual
                    labelNameToId.entries.firstOrNull { it.value == currentId }?.key
                }

                if (!currentLabelName.isNullOrBlank()) {
                    // Hay filtro de etiqueta activo: muestro ese nombre
                    actvFilterLabel.setText(currentLabelName, false)
                } else {
                    // Si no hay filtro de etiqueta activo, muestro "Todas las etiquetas"
                    actvFilterLabel.setText("Todas las etiquetas", false)
                }
            }
        }

        // Manejo del filtro de estado con el grupo de botones
        tbgState.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            stateFilter = when (checkedId) {
                R.id.btnPending -> StateFilter.PENDING
                R.id.btnDone    -> StateFilter.DONE
                else            -> StateFilter.ALL
            }
            refreshFilter()
        }

        // El botón flotante me lleva al editor en modo crear
        view.findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            findNavController().navigate(R.id.action_taskListFragment_to_editTaskFragment)
        }

        // Gesto de deslizar para borrar con opción de deshacer
        ItemTouchHelper(object : SimpleCallback(0, LEFT or RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val task = adapter.currentList.getOrNull(vh.bindingAdapterPosition) ?: return
                deleteWithUndo(task)
            }
        }).attachToRecyclerView(rv)

        // Suscripción a los cambios de la base de datos usando Flow
        // Mientras la vista esté STARTED, recibo la lista ordenada y aplico filtros antes de pintar
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                db.taskDao().observeAllOrdered().collectLatest { allTasks ->
                    applyFiltersAndSubmit(allTasks)
                }
            }
        }
    }

    // Aplico ambos filtros, primero por etiqueta y luego por estado, y envío el resultado al adaptador
    private fun applyFiltersAndSubmit(all: List<Task>) {
        val byLabel = selectedLabelId?.let { wantedId ->
            all.filter { it.labelId == wantedId }
        } ?: all

        val byState = when (stateFilter) {
            StateFilter.ALL     -> byLabel
            StateFilter.PENDING -> byLabel.filter { !it.done }
            StateFilter.DONE    -> byLabel.filter { it.done }
        }

        adapter.submitList(byState)
        tvEmpty.visibility = if (byState.isEmpty()) View.VISIBLE else View.GONE
    }

    // Forzar un refresco bajo demanda, vuelvo a pedir la lista actual y reaplico filtros
    private fun refreshFilter() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val all = db.taskDao().getAllOrdered()
            withContext(Dispatchers.Main) { applyFiltersAndSubmit(all) }
        }
    }

    // Alterno el estado hecho o pendiente, lo hago en IO y delego el repintado a la suscripción de arriba
    private fun toggleDone(task: Task) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.taskDao().setDone(task.id, !task.done)
        }
    }

    // Abro el editor con el id de la tarea para que cargue en modo edición
    private fun openEditor(task: Task) {
        val args = bundleOf("taskId" to task.id)
        findNavController().navigate(R.id.action_taskListFragment_to_editTaskFragment, args)
    }

    // Borro con opción de deshacer, si el usuario pulsa deshacer, reinsertar la tarea
    private fun deleteWithUndo(task: Task) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.taskDao().delete(task)
            withContext(Dispatchers.Main) {
                Snackbar.make(requireView(), "Tarea eliminada", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer") {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { db.taskDao().insert(task) }
                    }.show()
            }
        }
    }
}
