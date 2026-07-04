package com.proyectofinal

import android.content.ContentValues
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DispositivoRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // ==================== DISPOSITIVOS ====================

    fun insertar(dispositivo: Dispositivo): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
            put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
            put(DatabaseHelper.COL_MARCA, dispositivo.marca)
            put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
        }
        val id = db.insert(DatabaseHelper.TABLE_DISPOSITIVOS, null, values)
        return id
    }

    fun obtenerTodos(): List<Dispositivo> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Dispositivo>()
        val cursor = db.query(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_NOMBRE} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Dispositivo(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_NOMBRE)),
                        categoria = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORIA)),
                        marca = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MARCA)),
                        modelo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_MODELO))
                    )
                )
            }
        }
        return lista
    }

    fun actualizar(dispositivo: Dispositivo): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NOMBRE, dispositivo.nombre)
            put(DatabaseHelper.COL_CATEGORIA, dispositivo.categoria)
            put(DatabaseHelper.COL_MARCA, dispositivo.marca)
            put(DatabaseHelper.COL_MODELO, dispositivo.modelo)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            values,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(dispositivo.id.toString())
        )
        return filas
    }

    fun eliminar(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_DISPOSITIVOS,
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(id.toString())
        )
        return filas
    }

    // ==================== TAREAS ====================

    fun insertarTarea(tarea: Tarea): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TAREA_NOMBRE, tarea.nombre)
            put(DatabaseHelper.COL_TAREA_DESCRIPCION, tarea.descripcion)
            put(DatabaseHelper.COL_TAREA_FECHA, tarea.fecha)
            put(DatabaseHelper.COL_TAREA_REPETIR, tarea.repetirCada)
            put(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID, tarea.dispositivoId)
            put(DatabaseHelper.COL_TAREA_COMPLETADA, if (tarea.completada) 1 else 0)
        }
        val id = db.insert(DatabaseHelper.TABLE_TAREAS, null, values)
        if (id > 0) {
            insertarDetalleBaseDeTarea(id, tarea.nombre, tarea.descripcion)
        }
        return id
    }

    private fun insertarDetalleBaseDeTarea(tareaId: Long, nombre: String, descripcion: String): Long {
        return insertarTareaDetalleSiNoExiste(
            TareaDetalle(
                tareaId = tareaId,
                tipo = "mantenimiento",
                nombre = nombre,
                descripcion = descripcion
            )
        )
    }

    private fun insertarDetalleInspeccionParaTarea(tareaId: Long, inspeccion: Inspeccion): Long {
        return insertarTareaDetalleSiNoExiste(
            TareaDetalle(
                tareaId = tareaId,
                tipo = "inspeccion",
                nombre = inspeccion.nombre,
                descripcion = inspeccion.descripcion
            )
        )
    }

    private fun insertarTareaDetalleSiNoExiste(detalle: TareaDetalle): Long {
        val db = dbHelper.writableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            arrayOf(DatabaseHelper.COL_DETALLE_ID),
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ? AND ${DatabaseHelper.COL_DETALLE_TIPO} = ? AND ${DatabaseHelper.COL_DETALLE_NOMBRE} = ? AND ${DatabaseHelper.COL_DETALLE_DESCRIPCION} = ?",
            arrayOf(detalle.tareaId.toString(), detalle.tipo, detalle.nombre, detalle.descripcion),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_ID))
            }
        }

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_DETALLE_TAREA_ID, detalle.tareaId)
            put(DatabaseHelper.COL_DETALLE_TIPO, detalle.tipo)
            put(DatabaseHelper.COL_DETALLE_NOMBRE, detalle.nombre)
            put(DatabaseHelper.COL_DETALLE_DESCRIPCION, detalle.descripcion)
            put(DatabaseHelper.COL_DETALLE_CONDICION, detalle.condicion)
            put(DatabaseHelper.COL_DETALLE_NOTAS, detalle.notas)
            put(DatabaseHelper.COL_DETALLE_FOTOS, detalle.fotos.joinToString(","))
            put(DatabaseHelper.COL_DETALLE_COMPLETADA, if (detalle.completada) 1 else 0)
            put(DatabaseHelper.COL_DETALLE_FECHA_COMPLETADA, detalle.fechaCompletada)
        }
        return db.insert(DatabaseHelper.TABLE_TAREA_DETALLES, null, values)
    }

    fun vincularInspeccionesATarea(tareaId: Long, inspecciones: List<Inspeccion>) {
        for (inspeccion in inspecciones) {
            insertarDetalleInspeccionParaTarea(tareaId, inspeccion)
        }
    }

    fun obtenerTareas(): List<Tarea> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Tarea>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_TAREA_NOMBRE} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Tarea(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun obtenerTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Tarea>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ? AND ${DatabaseHelper.COL_TAREA_COMPLETADA} = 0",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_TAREA_FECHA} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Tarea(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun obtenerTodasTareasPorDispositivo(dispositivoId: Long): List<Tarea> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Tarea>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = ?",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_TAREA_FECHA} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Tarea(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun eliminarTarea(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_TAREAS,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(id.toString())
        )
        return filas
    }

    fun marcarTareaCompletada(id: Long) {
        val db = dbHelper.writableDatabase
        val tarea = obtenerTareaPorId(id)
        val inspeccionesRelacionadas = tarea?.let { obtenerInspeccionesRelacionadas(it) }.orEmpty()
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TAREA_COMPLETADA, 1)
        }
        db.update(DatabaseHelper.TABLE_TAREAS, values, "${DatabaseHelper.COL_TAREA_ID} = ?", arrayOf(id.toString()))

        if (tarea != null) {
            val valoresInspeccion = ContentValues().apply {
                put(DatabaseHelper.COL_INSPECCION_COMPLETADA, 1)
            }
            db.update(
                DatabaseHelper.TABLE_INSPECCIONES,
                valoresInspeccion,
                "${DatabaseHelper.COL_INSPECCION_FECHA} = ? AND ${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?",
                arrayOf(tarea.fecha, tarea.dispositivoId.toString())
            )
            crearSiguienteRepeticion(tarea, inspeccionesRelacionadas)
        }
    }

    private fun crearSiguienteRepeticion(tarea: Tarea, inspecciones: List<Inspeccion>) {
        val siguienteFechaTarea = calcularSiguienteFecha(tarea.fecha, tarea.repetirCada)
        val nuevaTareaId = if (siguienteFechaTarea != null) {
            insertarTarea(
                tarea.copy(
                    id = 0,
                    fecha = siguienteFechaTarea,
                    completada = false
                )
            )
        } else {
            0L
        }

        val nuevasInspecciones = inspecciones
            .mapNotNull { inspeccion ->
                val siguienteFechaInspeccion = calcularSiguienteFecha(inspeccion.fecha, inspeccion.repetirCada)
                    ?: return@mapNotNull null
                inspeccion.copy(
                    id = 0,
                    fecha = siguienteFechaInspeccion,
                    completada = false
                )
            }

        for (inspeccion in nuevasInspecciones) {
            insertarInspeccion(inspeccion)
        }

        if (nuevaTareaId > 0 && siguienteFechaTarea != null) {
            vincularInspeccionesATarea(
                nuevaTareaId,
                nuevasInspecciones.filter { it.fecha == siguienteFechaTarea }
            )
        }
    }

    private fun calcularSiguienteFecha(fecha: String, repetirCada: String): String? {
        val repeticion = repetirCada.lowercase(Locale.getDefault())
        if (repeticion.contains("una vez")) return null

        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            isLenient = false
        }
        val fechaBase = runCatching { formato.parse(fecha) }.getOrNull() ?: return null
        val calendario = Calendar.getInstance().apply {
            time = fechaBase
        }

        when {
            repeticion.contains("semana") -> calendario.add(Calendar.DAY_OF_YEAR, 7)
            repeticion.contains("6") && repeticion.contains("mes") -> calendario.add(Calendar.MONTH, 6)
            repeticion.contains("mes") -> calendario.add(Calendar.MONTH, 1)
            repeticion.contains("1") && (repeticion.contains("ano") || repeticion.contains("a")) -> calendario.add(Calendar.YEAR, 1)
            else -> return null
        }
        return formato.format(calendario.time)
    }

    // ==================== INSPECCIONES ====================

    fun insertarInspeccion(inspeccion: Inspeccion): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_INSPECCION_NOMBRE, inspeccion.nombre)
            put(DatabaseHelper.COL_INSPECCION_DESCRIPCION, inspeccion.descripcion)
            put(DatabaseHelper.COL_INSPECCION_FECHA, inspeccion.fecha)
            put(DatabaseHelper.COL_INSPECCION_REPETIR, inspeccion.repetirCada)
            put(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID, inspeccion.dispositivoId)
            put(DatabaseHelper.COL_INSPECCION_COMPLETADA, if (inspeccion.completada) 1 else 0)
        }
        val id = db.insert(DatabaseHelper.TABLE_INSPECCIONES, null, values)
        return id
    }

    fun obtenerInspecciones(): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null, null, null, null, null,
            "${DatabaseHelper.COL_INSPECCION_NOMBRE} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Inspeccion(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun obtenerInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ? AND ${DatabaseHelper.COL_INSPECCION_COMPLETADA} = 0",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_INSPECCION_FECHA} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Inspeccion(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun obtenerTodasInspeccionesPorDispositivo(dispositivoId: Long): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?",
            arrayOf(dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_INSPECCION_FECHA} ASC"
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Inspeccion(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun eliminarInspeccion(id: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_INSPECCIONES,
            "${DatabaseHelper.COL_INSPECCION_ID} = ?",
            arrayOf(id.toString())
        )
        return filas
    }

    // ==================== TAREAS CON DISPOSITIVO (JOIN) ====================

    fun obtenerTareasConDispositivo(): List<TareaConDispositivo> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaConDispositivo>()
        val query = """
            SELECT t.*, COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
            ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} ASC
        """
        val cursor = db.rawQuery(query, null)

        cursor.use { c ->
            while (c.moveToNext()) {
                val tarea = Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
                val nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo"))
                lista.add(TareaConDispositivo(tarea, nombreDispositivo))
            }
        }
        return lista
    }

    fun obtenerTareasPasadas(): List<TareaConDispositivo> {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaConDispositivo>()
        val query = """
            SELECT t.*, COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
            WHERE t.${DatabaseHelper.COL_TAREA_FECHA} < ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0
            ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} ASC
        """
        val cursor = db.rawQuery(query, arrayOf(hoy))

        cursor.use { c ->
            while (c.moveToNext()) {
                val tarea = Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
                val nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo"))
                lista.add(TareaConDispositivo(tarea, nombreDispositivo))
            }
        }
        return lista
    }

    fun obtenerTareasProximas(): List<TareaConDispositivo> {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
        val en30 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaConDispositivo>()
        val query = """
            SELECT t.*, COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
            WHERE t.${DatabaseHelper.COL_TAREA_FECHA} >= ? AND t.${DatabaseHelper.COL_TAREA_FECHA} <= ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0
            ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} ASC
        """
        val cursor = db.rawQuery(query, arrayOf(hoy, en30))

        cursor.use { c ->
            while (c.moveToNext()) {
                val tarea = Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
                val nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo"))
                lista.add(TareaConDispositivo(tarea, nombreDispositivo))
            }
        }
        return lista
    }

    fun obtenerTareasLejanas(): List<TareaConDispositivo> {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 31)
        val en31 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaConDispositivo>()
        val query = """
            SELECT t.*, COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
            WHERE t.${DatabaseHelper.COL_TAREA_FECHA} > ? AND t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 0
            ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} ASC
        """
        val cursor = db.rawQuery(query, arrayOf(en31))

        cursor.use { c ->
            while (c.moveToNext()) {
                val tarea = Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
                val nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo"))
                lista.add(TareaConDispositivo(tarea, nombreDispositivo))
            }
        }
        return lista
    }

    fun obtenerTareasCompletadas(): List<TareaConDispositivo> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaConDispositivo>()
        val query = """
            SELECT t.*, COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
            WHERE t.${DatabaseHelper.COL_TAREA_COMPLETADA} = 1
            ORDER BY t.${DatabaseHelper.COL_TAREA_FECHA} DESC
        """
        val cursor = db.rawQuery(query, null)

        cursor.use { c ->
            while (c.moveToNext()) {
                val tarea = Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
                val nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo"))
                lista.add(TareaConDispositivo(tarea, nombreDispositivo))
            }
        }
        return lista
    }

    // ==================== TAREA DETALLES ====================

    fun insertarTareaDetalle(detalle: TareaDetalle): Long {
        return insertarTareaDetalleSiNoExiste(detalle)
    }

    fun obtenerDetallesPorTarea(tareaId: Long): List<TareaDetalle> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<TareaDetalle>()
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            null,
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ?",
            arrayOf(tareaId.toString()),
            null, null, null
        )

        cursor.use { c ->
            while (c.moveToNext()) {
                val fotosStr = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_FOTOS)) ?: ""
                val fotos = if (fotosStr.isNotEmpty()) fotosStr.split(",") else emptyList()
                lista.add(
                    TareaDetalle(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_ID)),
                        tareaId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_TAREA_ID)),
                        tipo = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_TIPO)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_DESCRIPCION)),
                        condicion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_CONDICION)),
                        notas = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_NOTAS)),
                        fotos = fotos,
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_COMPLETADA)) == 1,
                        fechaCompletada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_DETALLE_FECHA_COMPLETADA))
                    )
                )
            }
        }

        val tareaParaDetalle = obtenerTareaPorId(tareaId)
        if (lista.none { it.tipo == "mantenimiento" } && tareaParaDetalle != null) {
            val detalleId = insertarDetalleBaseDeTarea(tareaId, tareaParaDetalle.nombre, tareaParaDetalle.descripcion)
            if (detalleId > 0) {
                lista.add(
                    TareaDetalle(
                        id = detalleId,
                        tareaId = tareaId,
                        tipo = "mantenimiento",
                        nombre = tareaParaDetalle.nombre,
                        descripcion = tareaParaDetalle.descripcion
                    )
                )
            }
        }

        if (tareaParaDetalle != null) {
            val inspeccionesRelacionadas = obtenerInspeccionesRelacionadas(tareaParaDetalle)
            for (inspeccion in inspeccionesRelacionadas) {
                val yaExiste = lista.any {
                    it.tipo == "inspeccion" &&
                        it.nombre == inspeccion.nombre &&
                        it.descripcion == inspeccion.descripcion
                }
                if (!yaExiste) {
                    val detalleId = insertarDetalleInspeccionParaTarea(tareaId, inspeccion)
                    if (detalleId > 0) {
                        lista.add(
                            TareaDetalle(
                                id = detalleId,
                                tareaId = tareaId,
                                tipo = "inspeccion",
                                nombre = inspeccion.nombre,
                                descripcion = inspeccion.descripcion
                            )
                        )
                    }
                }
            }
        }
        return lista
    }

    private fun obtenerTareaPorId(tareaId: Long): Tarea? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAREAS,
            null,
            "${DatabaseHelper.COL_TAREA_ID} = ?",
            arrayOf(tareaId.toString()),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return Tarea(
                    id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_ID)),
                    nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_NOMBRE)),
                    descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DESCRIPCION)),
                    fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_FECHA)),
                    repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_REPETIR)),
                    dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_DISPOSITIVO_ID)),
                    completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_TAREA_COMPLETADA)) == 1
                )
            }
        }
        return null
    }

    private fun obtenerInspeccionesRelacionadas(tarea: Tarea): List<Inspeccion> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Inspeccion>()
        val cursor = db.query(
            DatabaseHelper.TABLE_INSPECCIONES,
            null,
            "${DatabaseHelper.COL_INSPECCION_FECHA} = ? AND ${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = ?",
            arrayOf(tarea.fecha, tarea.dispositivoId.toString()),
            null, null,
            "${DatabaseHelper.COL_INSPECCION_NOMBRE} ASC"
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Inspeccion(
                        id = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_ID)),
                        nombre = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_NOMBRE)),
                        descripcion = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DESCRIPCION)),
                        fecha = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_FECHA)),
                        repetirCada = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_REPETIR)),
                        dispositivoId = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID)),
                        completada = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_INSPECCION_COMPLETADA)) == 1
                    )
                )
            }
        }
        return lista
    }

    fun actualizarTareaDetalle(detalle: TareaDetalle): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_DETALLE_CONDICION, detalle.condicion)
            put(DatabaseHelper.COL_DETALLE_NOTAS, detalle.notas)
            put(DatabaseHelper.COL_DETALLE_FOTOS, detalle.fotos.joinToString(","))
            put(DatabaseHelper.COL_DETALLE_COMPLETADA, if (detalle.completada) 1 else 0)
            put(DatabaseHelper.COL_DETALLE_FECHA_COMPLETADA, detalle.fechaCompletada)
        }
        val filas = db.update(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            values,
            "${DatabaseHelper.COL_DETALLE_ID} = ?",
            arrayOf(detalle.id.toString())
        )
        return filas
    }

    fun eliminarDetallesPorTarea(tareaId: Long): Int {
        val db = dbHelper.writableDatabase
        val filas = db.delete(
            DatabaseHelper.TABLE_TAREA_DETALLES,
            "${DatabaseHelper.COL_DETALLE_TAREA_ID} = ?",
            arrayOf(tareaId.toString())
        )
        return filas
    }

    // ==================== COMBINADO (TAREAS + INSPECCIONES) ====================

    private fun ejecutarUnionQuery(whereClause: String, whereArgs: Array<String>?): List<ItemProgramado> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<ItemProgramado>()
        val baseTareas = """
            SELECT t.${DatabaseHelper.COL_TAREA_ID} as id,
                   t.${DatabaseHelper.COL_TAREA_NOMBRE} as nombre,
                   t.${DatabaseHelper.COL_TAREA_DESCRIPCION} as descripcion,
                   t.${DatabaseHelper.COL_TAREA_FECHA} as fecha,
                   COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo,
                   'tarea' as tipo
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d
                ON t.${DatabaseHelper.COL_TAREA_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
        """
        val baseInspecciones = """
            SELECT i.${DatabaseHelper.COL_INSPECCION_ID} as id,
                   i.${DatabaseHelper.COL_INSPECCION_NOMBRE} as nombre,
                   i.${DatabaseHelper.COL_INSPECCION_DESCRIPCION} as descripcion,
                   i.${DatabaseHelper.COL_INSPECCION_FECHA} as fecha,
                   COALESCE(d.${DatabaseHelper.COL_NOMBRE}, '') as nombre_dispositivo,
                   'inspeccion' as tipo
            FROM ${DatabaseHelper.TABLE_INSPECCIONES} i
            LEFT JOIN ${DatabaseHelper.TABLE_DISPOSITIVOS} d
                ON i.${DatabaseHelper.COL_INSPECCION_DISPOSITIVO_ID} = d.${DatabaseHelper.COL_ID}
        """
        val query = """
            $baseTareas
            WHERE $whereClause
            UNION ALL
            $baseInspecciones
            WHERE $whereClause
            ORDER BY fecha ASC
        """
        val argsDuplicados = whereArgs?.let { it + it }
        val cursor = db.rawQuery(query, argsDuplicados)
        cursor.use { c ->
            while (c.moveToNext()) {
                lista.add(
                    ItemProgramado(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        nombre = c.getString(c.getColumnIndexOrThrow("nombre")),
                        descripcion = c.getString(c.getColumnIndexOrThrow("descripcion")),
                        fecha = c.getString(c.getColumnIndexOrThrow("fecha")),
                        nombreDispositivo = c.getString(c.getColumnIndexOrThrow("nombre_dispositivo")),
                        tipo = c.getString(c.getColumnIndexOrThrow("tipo"))
                    )
                )
            }
        }
        return lista
    }

    fun obtenerItemsPasadas(): List<ItemProgramado> {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return ejecutarUnionQuery(
            "fecha < ? AND completada = 0",
            arrayOf(hoy)
        )
    }

    fun obtenerItemsProximas(): List<ItemProgramado> {
        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
        val en30 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
        return ejecutarUnionQuery(
            "fecha >= ? AND fecha <= ? AND completada = 0",
            arrayOf(hoy, en30)
        )
    }

    fun obtenerItemsLejanas(): List<ItemProgramado> {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 31)
        val en31 = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
        return ejecutarUnionQuery(
            "fecha > ? AND completada = 0",
            arrayOf(en31)
        )
    }

    fun obtenerItemsCompletadas(): List<ItemProgramado> {
        return ejecutarUnionQuery(
            "completada = 1",
            null
        )
    }
}
