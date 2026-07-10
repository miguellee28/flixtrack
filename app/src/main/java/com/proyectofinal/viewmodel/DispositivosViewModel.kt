package com.proyectofinal.viewmodel

// Estado y operaciones que consumen las pantallas.
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.proyectofinal.MaintenanceNotificationScheduler
import com.proyectofinal.data.DispositivoRepository
import com.proyectofinal.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DispositivosViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = DispositivoRepository(app)

    private val _dispositivos = MutableStateFlow<List<Dispositivo>>(emptyList())
    val dispositivos: StateFlow<List<Dispositivo>> = _dispositivos.asStateFlow()

    private val _tareas = MutableStateFlow<List<Tarea>>(emptyList())
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    private val _inspecciones = MutableStateFlow<List<Inspeccion>>(emptyList())
    val inspecciones: StateFlow<List<Inspeccion>> = _inspecciones.asStateFlow()

    private val _tareasPasadas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasPasadas: StateFlow<List<ItemProgramado>> = _tareasPasadas.asStateFlow()

    private val _tareasProximas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasProximas: StateFlow<List<ItemProgramado>> = _tareasProximas.asStateFlow()

    private val _tareasLejanas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasLejanas: StateFlow<List<ItemProgramado>> = _tareasLejanas.asStateFlow()

    private val _calendarioProximas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val calendarioProximas: StateFlow<List<ItemProgramado>> = _calendarioProximas.asStateFlow()

    private val _tareasCompletadas = MutableStateFlow<List<ItemProgramado>>(emptyList())
    val tareasCompletadas: StateFlow<List<ItemProgramado>> = _tareasCompletadas.asStateFlow()

    private val _tareaSeleccionada = MutableStateFlow("proximo")
    val tareaSeleccionada: StateFlow<String> = _tareaSeleccionada.asStateFlow()

    private suspend fun reprogramarNotificaciones() {
        withContext(Dispatchers.IO) {
            MaintenanceNotificationScheduler.scheduleAll(getApplication())
        }
    }

    fun cargarDispositivos() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerTodos()
            }
            _dispositivos.value = datos
        }
    }

    fun insertarDispositivo(dispositivo: Dispositivo) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertar(dispositivo)
            }
            cargarDispositivos()
        }
    }

    suspend fun actualizarDispositivo(dispositivo: Dispositivo) {
        withContext(Dispatchers.IO) {
            repository.actualizar(dispositivo)
        }
        cargarDispositivos()
    }

    fun eliminarDispositivo(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminar(id)
            }
            reprogramarNotificaciones()
            cargarDispositivos()
            cargarHomeData()
            cargarCalendarioData()
        }
    }

    // ==================== TAREAS ====================

    fun cargarTareas() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerTareas()
            }
            _tareas.value = datos
        }
    }

    suspend fun insertarTarea(tarea: Tarea) {
        withContext(Dispatchers.IO) {
            repository.insertarTarea(tarea)
        }
        reprogramarNotificaciones()
        cargarTareas()
    }

    fun eliminarTarea(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminarTarea(id)
            }
            reprogramarNotificaciones()
            cargarTareas()
        }
    }

    suspend fun marcarTareaCompletada(id: Long) {
        withContext(Dispatchers.IO) {
            repository.marcarTareaCompletada(id)
        }
        reprogramarNotificaciones()
        cargarHomeData()
        cargarCalendarioData()
    }

    // ==================== INSPECCIONES ====================

    fun cargarInspecciones() {
        viewModelScope.launch {
            val datos = withContext(Dispatchers.IO) {
                repository.obtenerInspecciones()
            }
            _inspecciones.value = datos
        }
    }

    suspend fun insertarInspeccion(inspeccion: Inspeccion) {
        withContext(Dispatchers.IO) {
            repository.insertarInspeccion(inspeccion)
        }
        reprogramarNotificaciones()
        cargarInspecciones()
    }

    fun eliminarInspeccion(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.eliminarInspeccion(id)
            }
            reprogramarNotificaciones()
            cargarInspecciones()
        }
    }

    suspend fun marcarInspeccionCompletada(id: Long) {
        withContext(Dispatchers.IO) {
            repository.marcarInspeccionCompletada(id)
        }
        reprogramarNotificaciones()
        cargarInspecciones()
        cargarHomeData()
        cargarCalendarioData()
    }

    // ==================== HOME DATA ====================

    fun cargarHomeData() {
        viewModelScope.launch {
            val pasadas = withContext(Dispatchers.IO) { repository.obtenerItemsPasadas() }
            val proximas = withContext(Dispatchers.IO) { repository.obtenerItemsProximas() }
            val lejanas = withContext(Dispatchers.IO) { repository.obtenerItemsLejanas() }
            _tareasPasadas.value = pasadas
            _tareasProximas.value = proximas
            _tareasLejanas.value = lejanas
        }
    }

    // ==================== CALENDARIO DATA ====================

    fun seleccionarTabCalendario(tab: String) {
        _tareaSeleccionada.value = tab
        cargarCalendarioData()
    }

    fun cargarCalendarioData() {
        viewModelScope.launch {
            when (_tareaSeleccionada.value) {
                "proximo" -> {
                    val datos = withContext(Dispatchers.IO) {
                        (repository.obtenerItemsProximas() + repository.obtenerItemsLejanas())
                            .distinctBy { "${it.tipo}:${it.id}" }
                            .sortedBy { it.fecha }
                    }
                    _calendarioProximas.value = datos
                }
                "atrasado" -> {
                    val datos = withContext(Dispatchers.IO) { repository.obtenerItemsPasadas() }
                    _tareasPasadas.value = datos
                }
                "completado" -> {
                    val datos = withContext(Dispatchers.IO) { repository.obtenerItemsCompletadas() }
                    _tareasCompletadas.value = datos
                }
            }
        }
    }

    // ==================== TAREA DETALLES ====================

    fun insertarTareaDetalle(detalle: TareaDetalle) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.insertarTareaDetalle(detalle)
            }
        }
    }

    fun cargarDetallesPorTarea(tareaId: Long): List<TareaDetalle> {
        return repository.obtenerDetallesPorTarea(tareaId)
    }

    suspend fun actualizarTareaDetalle(detalle: TareaDetalle) {
        withContext(Dispatchers.IO) {
            repository.actualizarTareaDetalle(detalle)
        }
    }

    // ==================== GUARDAR TODO ====================

    suspend fun guardarDispositivoConTareaEInspeccion(
        dispositivo: Dispositivo,
        tarea: Tarea?,
        inspeccion: Inspeccion?
    ): Long {
        val id = withContext(Dispatchers.IO) {
            repository.insertar(dispositivo)
        }
        if (tarea != null) {
            withContext(Dispatchers.IO) {
                repository.insertarTarea(tarea.copy(dispositivoId = id))
            }
        }
        if (inspeccion != null) {
            withContext(Dispatchers.IO) {
                repository.insertarInspeccion(inspeccion.copy(dispositivoId = id))
            }
        }
        reprogramarNotificaciones()
        cargarDispositivos()
        return id
    }

    suspend fun guardarTareasEInspecciones(
        dispositivoId: Long?,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ) {
        val idDispositivo = dispositivoId ?: 0
        val inspeccionesGuardadas = inspecciones.map { it.copy(dispositivoId = idDispositivo) }

        withContext(Dispatchers.IO) {
            for (inspeccion in inspeccionesGuardadas) {
                repository.insertarInspeccion(inspeccion)
            }

            for (tarea in tareas) {
                val tareaGuardada = tarea.copy(dispositivoId = idDispositivo)
                val tareaId = repository.insertarTarea(tareaGuardada)
                val inspeccionesParaTarea = inspeccionesGuardadas.filter { it.fecha == tareaGuardada.fecha }
                    .ifEmpty { if (tareas.size == 1) inspeccionesGuardadas else emptyList() }
                repository.vincularInspeccionesATarea(tareaId, inspeccionesParaTarea)
            }
        }
        reprogramarNotificaciones()
        cargarTareas()
        cargarInspecciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
    }

    suspend fun guardarEdicionCalendario(
        dispositivoId: Long,
        tareas: List<Tarea>,
        inspecciones: List<Inspeccion>
    ) {
        val tareasGuardadas = tareas.map { it.copy(dispositivoId = dispositivoId) }
        val inspeccionesGuardadas = inspecciones.map { it.copy(dispositivoId = dispositivoId) }

        withContext(Dispatchers.IO) {
            for (inspeccion in inspeccionesGuardadas) {
                if (inspeccion.id > 0) {
                    repository.actualizarInspeccion(inspeccion)
                } else {
                    repository.insertarInspeccion(inspeccion)
                }
            }

            for (tarea in tareasGuardadas) {
                val tareaGuardada = if (tarea.id > 0) {
                    repository.actualizarTarea(tarea)
                    tarea
                } else {
                    tarea.copy(id = repository.insertarTarea(tarea))
                }
                val inspeccionesParaTarea = inspeccionesGuardadas.filter { it.fecha == tareaGuardada.fecha }
                    .ifEmpty { if (tareasGuardadas.size == 1) inspeccionesGuardadas else emptyList() }
                repository.sincronizarDetallesDeTarea(tareaGuardada, inspeccionesParaTarea)
            }
        }
        reprogramarNotificaciones()
        cargarTareas()
        cargarInspecciones()
        cargarHomeData()
        cargarCalendarioData()
        cargarDispositivos()
    }

    fun obtenerTodosDispositivos(): List<Dispositivo> = repository.obtenerTodos()

    fun obtenerTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        return repository.obtenerTareasPorDispositivo(dispositivoId)
    }

    fun obtenerInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        return repository.obtenerInspeccionesPorDispositivo(dispositivoId)
    }

    fun obtenerTodasTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        return repository.obtenerTodasTareasPorDispositivo(dispositivoId)
    }

    fun obtenerTodasInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        return repository.obtenerTodasInspeccionesPorDispositivo(dispositivoId)
    }
}
