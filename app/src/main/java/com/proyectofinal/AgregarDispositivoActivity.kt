package com.proyectofinal

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgregarDispositivoActivity : AppCompatActivity() {

    private lateinit var viewModel: DispositivosViewModel

    private lateinit var botonManual: Button
    private lateinit var botonIA: Button
    private lateinit var contenedorTarjetas: LinearLayout
    private lateinit var botonAceptar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_dispositivo)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        botonManual = findViewById(R.id.opcion_manual)
        botonIA = findViewById(R.id.opcion_ia)
        contenedorTarjetas = findViewById(R.id.contenedor_tarjetas)
        botonAceptar = findViewById(R.id.boton_aceptar)

        configurarToggle()
        configurarBotones()
        observarViewModel()
        mostrarTarjetas()
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.mensaje.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@AgregarDispositivoActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.limpiarMensaje()
                    setResult(RESULT_OK)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (!isFinishing) {
                            finish()
                        }
                    }
                }
            }
        }
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
            ocultarTarjetas()
        }
    }

    private fun configurarBotones() {
        botonAceptar.setOnClickListener {
            try {
                val nombre = contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre)?.text.toString().trim()
                val marca = contenedorTarjetas.findViewById<EditText>(R.id.campo_marca)?.text.toString().trim()
                val modelo = contenedorTarjetas.findViewById<EditText>(R.id.campo_modelo)?.text.toString().trim()
                val categoria = contenedorTarjetas.findViewById<Spinner>(R.id.spinner_categoria)?.selectedItem?.toString() ?: ""

                if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
                    Toast.makeText(this, "Complete nombre, marca y modelo del dispositivo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val dispositivo = Dispositivo(nombre = nombre, categoria = categoria, marca = marca, modelo = modelo)

                val tarea = construirTarea()
                val inspeccion = construirInspeccion()

                viewModel.guardarDispositivoConTareaEInspeccion(dispositivo, tarea, inspeccion)
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun construirTarea(): Tarea? {
        val nombre = contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre_tarea)?.text.toString().trim()
        if (nombre.isEmpty()) return null

        val desc = contenedorTarjetas.findViewById<EditText>(R.id.campo_descripcion)?.text.toString().trim()
        val repetir = contenedorTarjetas.findViewById<Spinner>(R.id.spinner_repetir)?.selectedItem?.toString() ?: "Una vez"
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(contenedorTarjetas.findViewById<android.widget.CalendarView>(R.id.calendario_tarea)?.date ?: System.currentTimeMillis())
        )

        return Tarea(nombre = nombre, descripcion = desc, fecha = fecha, repetirCada = repetir)
    }

    private fun construirInspeccion(): Inspeccion? {
        val nombre = contenedorTarjetas.findViewById<EditText>(R.id.campo_nombre_inspeccion)?.text.toString().trim()
        if (nombre.isEmpty()) return null

        val desc = contenedorTarjetas.findViewById<EditText>(R.id.campo_descripcion_inspeccion)?.text.toString().trim()
        val repetir = contenedorTarjetas.findViewById<Spinner>(R.id.spinner_repetir_inspeccion)?.selectedItem?.toString() ?: "Una vez"
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(contenedorTarjetas.findViewById<android.widget.CalendarView>(R.id.calendario_inspeccion)?.date ?: System.currentTimeMillis())
        )

        return Inspeccion(nombre = nombre, descripcion = desc, fecha = fecha, repetirCada = repetir)
    }

    private fun mostrarTarjetas() {
        contenedorTarjetas.removeAllViews()
        val layouts = listOf(
            R.layout.layout_detalle_dispositivo,
            R.layout.layout_tarea,
            R.layout.layout_inspeccion
        )
        for (layout in layouts) {
            LayoutInflater.from(this).inflate(layout, contenedorTarjetas, true)
        }
    }

    private fun ocultarTarjetas() {
        contenedorTarjetas.removeAllViews()
    }
}
