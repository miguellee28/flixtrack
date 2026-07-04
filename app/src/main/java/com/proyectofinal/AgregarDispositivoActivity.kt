package com.proyectofinal

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

class AgregarDispositivoActivity : AppCompatActivity() {

    private lateinit var viewModel: DispositivosViewModel

    private lateinit var botonManual: Button
    private lateinit var botonIA: Button
    private lateinit var contenedorTarjetas: LinearLayout
    private lateinit var contenedorBotonesAgregar: LinearLayout
    private lateinit var botonAceptar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_dispositivo)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        botonManual = findViewById(R.id.opcion_manual)
        botonIA = findViewById(R.id.opcion_ia)
        contenedorTarjetas = findViewById(R.id.contenedor_tarjetas)
        contenedorBotonesAgregar = findViewById(R.id.contenedor_botones_agregar)
        botonAceptar = findViewById(R.id.boton_aceptar)

        configurarToggle()
        configurarBotones()
        mostrarTarjetas()
    }

    private fun configurarToggle() {
        botonManual.setOnClickListener {
            botonManual.setBackgroundResource(R.drawable.fondo_toggle_seleccionado)
            botonManual.setTextColor(resources.getColor(R.color.white, theme))
            botonIA.setBackgroundResource(R.drawable.fondo_toggle_no_seleccionado)
            botonIA.setTextColor(resources.getColor(R.color.black, theme))
            mostrarTarjetas()
        }

        botonIA.setOnClickListener {
            botonIA.setBackgroundResource(R.drawable.fondo_toggle_seleccionado)
            botonIA.setTextColor(resources.getColor(R.color.white, theme))
            botonManual.setBackgroundResource(R.drawable.fondo_toggle_no_seleccionado)
            botonManual.setTextColor(resources.getColor(R.color.black, theme))
            mostrarDetalleDispositivoParaIA()
        }
    }

    private fun configurarBotones() {
        botonAceptar.setOnClickListener {
            val nombre = contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre)?.text.toString().trim()
            val marca = contenedorTarjetas.findViewById<EditText>(R.id.campo_marca)?.text.toString().trim()
            val modelo = contenedorTarjetas.findViewById<EditText>(R.id.campo_modelo)?.text.toString().trim()
            val categoria = contenedorTarjetas.findViewById<Spinner>(R.id.spinner_categoria)?.selectedItem?.toString() ?: ""

            if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
                Toast.makeText(this, "Complete nombre, marca y modelo del dispositivo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dispositivo = Dispositivo(nombre = nombre, categoria = categoria, marca = marca, modelo = modelo)
            val tareas = construirTareas()
            val inspecciones = construirInspecciones()

            lifecycleScope.launch {
                val dispositivoId = viewModel.guardarDispositivoConTareaEInspeccion(dispositivo, null, null)
                viewModel.guardarTareasEInspecciones(dispositivoId, tareas, inspecciones)
                Toast.makeText(this@AgregarDispositivoActivity, "Dispositivo guardado", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun construirTareas(): List<Tarea> {
        val tareas = mutableListOf<Tarea>()

        for (i in 0 until contenedorTarjetas.childCount) {
            val vista = contenedorTarjetas.getChildAt(i)
            val nombreCampo = vista.findViewById<EditText>(R.id.campo_nombre_tarea)
            if (nombreCampo != null) {
                val nombre = nombreCampo.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val desc = vista.findViewById<EditText>(R.id.campo_descripcion)?.text.toString().trim()
                    val repetir = vista.findViewById<Spinner>(R.id.spinner_repetir)?.selectedItem?.toString() ?: "Una vez"
                    val fecha = vista.findViewById<TextView>(R.id.texto_fecha_tarea)?.text.toString()
                    tareas.add(Tarea(nombre = nombre, descripcion = desc, fecha = fecha, repetirCada = repetir))
                }
            }
        }

        return tareas
    }

    private fun construirInspecciones(): List<Inspeccion> {
        val inspecciones = mutableListOf<Inspeccion>()

        for (i in 0 until contenedorTarjetas.childCount) {
            val vista = contenedorTarjetas.getChildAt(i)
            val nombreCampo = vista.findViewById<EditText>(R.id.campo_nombre_inspeccion)
            if (nombreCampo != null) {
                val nombre = nombreCampo.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    val desc = vista.findViewById<EditText>(R.id.campo_descripcion_inspeccion)?.text.toString().trim()
                    val repetir = vista.findViewById<Spinner>(R.id.spinner_repetir_inspeccion)?.selectedItem?.toString() ?: "Una vez"
                    val fecha = vista.findViewById<TextView>(R.id.texto_fecha_inspeccion)?.text.toString()
                    inspecciones.add(Inspeccion(nombre = nombre, descripcion = desc, fecha = fecha, repetirCada = repetir))
                }
            }
        }

        return inspecciones
    }


    private fun configurarCalendarioPlegable(
        vista: View,
        textoFechaId: Int
    ) {
        val textoFecha = vista.findViewById<TextView>(textoFechaId) ?: return
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaSeleccionada = Calendar.getInstance()

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

    private fun configurarCalendarioInspeccion(vista: View) {
        configurarCalendarioPlegable(
            vista,
            R.id.texto_fecha_inspeccion
        )
    }

    private fun mostrarTarjetas() {
        contenedorTarjetas.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.layout_detalle_dispositivo, contenedorTarjetas, true)

        val tarea = LayoutInflater.from(this).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
        configurarCalendarioTarea(tarea)
        contenedorTarjetas.addView(tarea)

        val inspeccion = LayoutInflater.from(this).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
        configurarCalendarioInspeccion(inspeccion)
        contenedorTarjetas.addView(inspeccion)

        agregarBotonesDinamicos()
    }

    private fun mostrarDetalleDispositivoParaIA() {
        contenedorTarjetas.removeAllViews()
        contenedorBotonesAgregar.removeAllViews()
        LayoutInflater.from(this).inflate(R.layout.layout_detalle_dispositivo, contenedorTarjetas, true)
    }

    private fun agregarBotonesDinamicos() {
        contenedorBotonesAgregar.removeAllViews()

        val botonTarea = Button(this).apply {
            text = "+ Agregar Mantenimiento"
            setAllCaps(false)
            textSize = 13f
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundColor(resources.getColor(R.color.purple_500, theme))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                44.dp()
            )
            params.topMargin = 8
            params.bottomMargin = 6
            layoutParams = params
            minHeight = 0
        }

        botonTarea.setOnClickListener {
            if (!puedeAgregarTarjeta(R.id.campo_nombre_tarea, "Completa la tarea actual antes de agregar otra")) {
                return@setOnClickListener
            }
            val tarea = LayoutInflater.from(this@AgregarDispositivoActivity).inflate(R.layout.layout_tarea, contenedorTarjetas, false)
            configurarCalendarioTarea(tarea)
            contenedorTarjetas.addView(tarea)
        }

        val botonInspeccion = Button(this).apply {
            text = "+ Agregar Inspeccion"
            setAllCaps(false)
            textSize = 13f
            setTextColor(resources.getColor(R.color.white, theme))
            setBackgroundColor(resources.getColor(R.color.purple_500, theme))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                44.dp()
            )
            params.topMargin = 0
            params.bottomMargin = 6
            layoutParams = params
            minHeight = 0
        }

        botonInspeccion.setOnClickListener {
            if (!puedeAgregarTarjeta(R.id.campo_nombre_inspeccion, "Completa la inspeccion actual antes de agregar otra")) {
                return@setOnClickListener
            }
            val inspeccion = LayoutInflater.from(this@AgregarDispositivoActivity).inflate(R.layout.layout_inspeccion, contenedorTarjetas, false)
            configurarCalendarioInspeccion(inspeccion)
            contenedorTarjetas.addView(inspeccion)
        }

        contenedorBotonesAgregar.addView(botonTarea)
        contenedorBotonesAgregar.addView(botonInspeccion)
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

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun ocultarTarjetas() {
        contenedorTarjetas.removeAllViews()
        contenedorBotonesAgregar.removeAllViews()
    }
}
