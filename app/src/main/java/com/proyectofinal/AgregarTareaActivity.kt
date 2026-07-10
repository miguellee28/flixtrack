package com.proyectofinal

import com.proyectofinal.model.*
import com.proyectofinal.viewmodel.DispositivosViewModel
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgregarTareaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DISPOSITIVO_ID = "extra_dispositivo_id"
    }

    private lateinit var viewModel: DispositivosViewModel
    private lateinit var contenedorTarjetas: LinearLayout
    private lateinit var spinnerDispositivo: Spinner
    private lateinit var botonAgregarTarea: Button
    private lateinit var botonAgregarInspeccion: Button
    private lateinit var botonGuardar: Button

    private var contadorTareas = 0
    private var contadorInspecciones = 0
    private var dispositivosDisponibles: List<Dispositivo> = emptyList()
    private var dispositivoInicialId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_tarea)

        viewModel = ViewModelProvider(this)[DispositivosViewModel::class.java]
        dispositivoInicialId = intent.getLongExtra(EXTRA_DISPOSITIVO_ID, 0)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        contenedorTarjetas = findViewById(R.id.contenedor_tarjetas)
        spinnerDispositivo = findViewById(R.id.spinner_dispositivo)
        botonAgregarTarea = findViewById(R.id.boton_agregar_tarea)
        botonAgregarInspeccion = findViewById(R.id.boton_agregar_inspeccion)
        botonGuardar = findViewById(R.id.boton_guardar)

        configurarSpinner()
        configurarBotones()

        agregarTarjetaTarea()
        agregarTarjetaInspeccion()
        configurarSeleccionDispositivo()
        seleccionarDispositivoInicial()
    }

    private fun configurarSpinner() {
        dispositivosDisponibles = viewModel.obtenerTodosDispositivos()

        if (dispositivosDisponibles.isEmpty()) {
            val opciones = listOf("Primero agrega un dispositivo")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opciones)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDispositivo.adapter = adapter
            spinnerDispositivo.isEnabled = false
        } else {
            val nombres = mutableListOf("Ninguno (sin dispositivo)")
            nombres.addAll(dispositivosDisponibles.map { it.nombre })
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDispositivo.adapter = adapter
        }
    }

    private fun configurarSeleccionDispositivo() {
        if (!spinnerDispositivo.isEnabled) return

        spinnerDispositivo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    mostrarTarjetasVacias()
                    return
                }

                val dispositivo = dispositivosDisponibles.getOrNull(position - 1) ?: return
                cargarTarjetasDelDispositivo(dispositivo.id)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun seleccionarDispositivoInicial() {
        if (!spinnerDispositivo.isEnabled || dispositivoInicialId == 0L) return
        val posicion = dispositivosDisponibles.indexOfFirst { it.id == dispositivoInicialId }
        if (posicion >= 0) {
            spinnerDispositivo.setSelection(posicion + 1)
        }
    }

    private fun configurarBotones() {
        botonAgregarTarea.setOnClickListener {
            if (!puedeAgregarTarjeta(R.id.campo_nombre_tarea, "Completa la tarea actual antes de agregar otra")) {
                return@setOnClickListener
            }
            agregarTarjetaTarea()
        }

        botonAgregarInspeccion.setOnClickListener {
            if (!puedeAgregarTarjeta(R.id.campo_nombre_inspeccion, "Completa la inspeccion actual antes de agregar otra")) {
                return@setOnClickListener
            }
            agregarTarjetaInspeccion()
        }

        botonGuardar.setOnClickListener {
            guardarTareasEInspecciones()
        }
    }

    private fun puedeAgregarTarjeta(campoNombreId: Int, mensaje: String): Boolean {
        for (i in 0 until contenedorTarjetas.childCount) {
            val campo = contenedorTarjetas.getChildAt(i).findViewById<EditText>(campoNombreId)
            if (campo != null && campo.text.toString().trim().isEmpty()) {
                campo.requestFocus()
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun agregarTarjetaTarea() {
        val vista = LayoutInflater.from(this).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
        configurarCalendarioTarea(vista)
        contadorTareas++
        contenedorTarjetas.addView(vista)
    }

    private fun agregarTarjetaTarea(tarea: Tarea) {
        val vista = LayoutInflater.from(this).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
        configurarCalendarioTarea(vista, tarea.fecha)
        vista.findViewById<EditText>(R.id.campo_nombre_tarea)?.setText(tarea.nombre)
        vista.findViewById<EditText>(R.id.campo_descripcion)?.setText(tarea.descripcion)
        seleccionarSpinnerPorTexto(vista.findViewById(R.id.spinner_repetir), tarea.repetirCada)
        vista.tag = tarea
        contadorTareas++
        contenedorTarjetas.addView(vista)
    }

    private fun agregarTarjetaInspeccion() {
        val vista = LayoutInflater.from(this).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
        configurarCalendarioInspeccion(vista)
        contadorInspecciones++
        contenedorTarjetas.addView(vista)
    }

    private fun agregarTarjetaInspeccion(inspeccion: Inspeccion) {
        val vista = LayoutInflater.from(this).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
        configurarCalendarioInspeccion(vista, inspeccion.fecha)
        vista.findViewById<EditText>(R.id.campo_nombre_inspeccion)?.setText(inspeccion.nombre)
        vista.findViewById<EditText>(R.id.campo_descripcion_inspeccion)?.setText(inspeccion.descripcion)
        seleccionarSpinnerPorTexto(vista.findViewById(R.id.spinner_repetir_inspeccion), inspeccion.repetirCada)
        vista.tag = inspeccion
        contadorInspecciones++
        contenedorTarjetas.addView(vista)
    }

    private fun mostrarTarjetasVacias() {
        contenedorTarjetas.removeAllViews()
        contadorTareas = 0
        contadorInspecciones = 0
        agregarTarjetaTarea()
        agregarTarjetaInspeccion()
    }

    private fun cargarTarjetasDelDispositivo(dispositivoId: Long) {
        val tareas = viewModel.obtenerTareasPorDispositivo(dispositivoId)
        val inspecciones = viewModel.obtenerInspeccionesPorDispositivo(dispositivoId)

        contenedorTarjetas.removeAllViews()
        contadorTareas = 0
        contadorInspecciones = 0

        if (tareas.isEmpty() && inspecciones.isEmpty()) {
            agregarTarjetaTarea()
            agregarTarjetaInspeccion()
            return
        }

        tareas.forEach { agregarTarjetaTarea(it) }
        inspecciones.forEach { agregarTarjetaInspeccion(it) }
    }


    private fun configurarCalendarioPlegable(
        vista: View,
        textoFechaId: Int,
        fechaInicial: String? = null
    ) {
        val textoFecha = vista.findViewById<TextView>(textoFechaId) ?: return
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaSeleccionada = Calendar.getInstance().apply {
            val fechaParseada = fechaInicial?.let { runCatching { formato.parse(it) }.getOrNull() }
            if (fechaParseada != null) {
                time = fechaParseada
            }
        }

        textoFecha.text = formato.format(fechaSeleccionada.time)

        textoFecha.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    fechaSeleccionada.set(year, month, dayOfMonth)
                    textoFecha.text = formato.format(fechaSeleccionada.time)
                },
                fechaSeleccionada.get(Calendar.YEAR),
                fechaSeleccionada.get(Calendar.MONTH),
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun configurarCalendarioTarea(vista: View) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_tarea
        )
    }

    private fun configurarCalendarioTarea(vista: View, fechaInicial: String?) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_tarea,
            fechaInicial
        )
    }

    private fun configurarCalendarioInspeccion(vista: View) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_inspeccion
        )
    }

    private fun configurarCalendarioInspeccion(vista: View, fechaInicial: String?) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_inspeccion,
            fechaInicial
        )
    }

    private fun seleccionarSpinnerPorTexto(spinner: Spinner?, texto: String) {
        if (spinner == null) return
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == texto) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun guardarTareasEInspecciones() {
        val dispositivos = viewModel.obtenerTodosDispositivos()
        val dispositivoId = if (spinnerDispositivo.isEnabled && spinnerDispositivo.selectedItemPosition > 0) {
            dispositivos.getOrNull(spinnerDispositivo.selectedItemPosition - 1)?.id
        } else {
            null
        }

        val tareas = mutableListOf<Tarea>()
        val inspecciones = mutableListOf<Inspeccion>()

        for (i in 0 until contenedorTarjetas.childCount) {
            val vista = contenedorTarjetas.getChildAt(i)

            val nombreCampo = vista.findViewById<EditText>(R.id.campo_nombre_tarea)
            if (nombreCampo != null) {
                val nombre = nombreCampo.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val existente = vista.tag as? Tarea
                    val desc = vista.findViewById<EditText>(R.id.campo_descripcion)?.text.toString().trim()
                    val repetir = vista.findViewById<Spinner>(R.id.spinner_repetir)?.selectedItem?.toString() ?: "Una vez"
                    val fecha = vista.findViewById<TextView>(R.id.texto_fecha_tarea)?.text.toString()
                    tareas.add(
                        Tarea(
                            id = existente?.id ?: 0,
                            nombre = nombre,
                            descripcion = desc,
                            fecha = fecha,
                            repetirCada = repetir,
                            completada = existente?.completada ?: false
                        )
                    )
                }
            }

            val nombreInspeccion = vista.findViewById<EditText>(R.id.campo_nombre_inspeccion)
            if (nombreInspeccion != null) {
                val nombre = nombreInspeccion.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val existente = vista.tag as? Inspeccion
                    val desc = vista.findViewById<EditText>(R.id.campo_descripcion_inspeccion)?.text.toString().trim()
                    val repetir = vista.findViewById<Spinner>(R.id.spinner_repetir_inspeccion)?.selectedItem?.toString() ?: "Una vez"
                    val fecha = vista.findViewById<TextView>(R.id.texto_fecha_inspeccion)?.text.toString()
                    inspecciones.add(
                        Inspeccion(
                            id = existente?.id ?: 0,
                            nombre = nombre,
                            descripcion = desc,
                            fecha = fecha,
                            repetirCada = repetir,
                            completada = existente?.completada ?: false
                        )
                    )
                }
            }
        }

        if (tareas.isEmpty() && inspecciones.isEmpty()) {
            Toast.makeText(this, "Agrega al menos una tarea o inspeccion", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val hayItemsExistentes = tareas.any { it.id > 0 } || inspecciones.any { it.id > 0 }
            if (dispositivoId != null && hayItemsExistentes) {
                viewModel.guardarEdicionCalendario(dispositivoId, tareas, inspecciones)
            } else {
                viewModel.guardarTareasEInspecciones(dispositivoId, tareas, inspecciones)
            }
            Toast.makeText(this@AgregarTareaActivity, "Guardado correctamente", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
