package com.proyectofinal

import com.proyectofinal.model.*
import com.proyectofinal.viewmodel.DispositivosViewModel
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetalleDispositivoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_NOMBRE_DISPOSITIVO = "nombre_dispositivo"
        const val EXTRA_CATEGORIA = "categoria"
        const val EXTRA_MARCA = "marca"
        const val EXTRA_MODELO = "modelo"
        const val EXTRA_FOTO = "foto"
    }

    private lateinit var viewModel: DispositivosViewModel
    private lateinit var campoNombre: EditText
    private lateinit var campoMarca: EditText
    private lateinit var campoModelo: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var botonGuardar: Button
    private lateinit var botonEliminar: Button
    private lateinit var botonEditarCalendario: TextView
    private lateinit var contenedorCalendario: LinearLayout
    private lateinit var textoResumenIA: TextView
    private lateinit var imagenDispositivo: ImageView

    private var dispositivoId: Long = 0
    private var fotoDispositivo: String = ""
    private val aiService by lazy {
        MaintenanceAiService(
            GoogleGenAiClient(BuildConfig.GOOGLE_GENAI_API_KEY),
            BraveSearchClient(BuildConfig.BRAVE_SEARCH_API_KEY)
        )
    }

    private val resultadoEditarCalendario = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (dispositivoId != 0L) {
            cargarCalendarioDispositivo()
            cargarResumenIA()
        }
        if (resultado.resultCode == RESULT_OK) {
            setResult(RESULT_OK)
        }
    }

    private data class ItemCalendarioDispositivo(
        val tipo: String,
        val nombre: String,
        val descripcion: String,
        val repetirCada: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_dispositivo)

        viewModel = ViewModelProvider(this)[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, barrasSistema.bottom)
            insets
        }

        campoNombre = findViewById(R.id.campo_nombre)
        campoMarca = findViewById(R.id.campo_marca)
        campoModelo = findViewById(R.id.campo_modelo)
        spinnerCategoria = findViewById(R.id.spinner_categoria)
        botonGuardar = findViewById(R.id.boton_guardar)
        botonEliminar = findViewById(R.id.boton_eliminar_dispositivo)
        botonEditarCalendario = findViewById(R.id.boton_editar_calendario)
        contenedorCalendario = findViewById(R.id.contenedor_calendario_dispositivo)
        textoResumenIA = findViewById(R.id.texto_resumen_ia)
        imagenDispositivo = findViewById(R.id.foto_dispositivo)

        dispositivoId = intent.getLongExtra(EXTRA_ID, 0)
        fotoDispositivo = intent.getStringExtra(EXTRA_FOTO) ?: ""
        campoNombre.setText(intent.getStringExtra(EXTRA_NOMBRE_DISPOSITIVO) ?: "")
        campoMarca.setText(intent.getStringExtra(EXTRA_MARCA) ?: "")
        campoModelo.setText(intent.getStringExtra(EXTRA_MODELO) ?: "")
        mostrarFotoDispositivo()

        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: ""
        val categorias = resources.getStringArray(R.array.opciones_categoria)
        val indiceCategoria = categorias.indexOf(categoria)
        if (indiceCategoria >= 0) {
            spinnerCategoria.setSelection(indiceCategoria)
        }

        cargarCalendarioDispositivo()
        cargarResumenIA()

        botonGuardar.setOnClickListener {
            val nombre = campoNombre.text.toString().trim()
            val marca = campoMarca.text.toString().trim()
            val modelo = campoModelo.text.toString().trim()
            val categoriaSel = spinnerCategoria.selectedItem.toString()

            if (nombre.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
                Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dispositivo = Dispositivo(
                id = dispositivoId,
                nombre = nombre,
                categoria = categoriaSel,
                marca = marca,
                modelo = modelo,
                foto = fotoDispositivo
            )

            lifecycleScope.launch {
                viewModel.actualizarDispositivo(dispositivo)
                cargarResumenIA()
                Toast.makeText(this@DetalleDispositivoActivity, "Dispositivo actualizado", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        botonEditarCalendario.setOnClickListener {
            val intent = Intent(this, AgregarTareaActivity::class.java).apply {
                putExtra(AgregarTareaActivity.EXTRA_DISPOSITIVO_ID, dispositivoId)
            }
            resultadoEditarCalendario.launch(intent)
        }

        botonEliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar dispositivo")
                .setMessage("Se eliminara este dispositivo junto con su calendario, mantenimientos e inspecciones. Esta accion no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    viewModel.eliminarDispositivo(dispositivoId)
                    Toast.makeText(this, "Dispositivo eliminado", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarFotoDispositivo() {
        if (fotoDispositivo.isBlank()) return
        val uri = if (fotoDispositivo.startsWith("content:")) Uri.parse(fotoDispositivo) else Uri.fromFile(File(fotoDispositivo))
        imagenDispositivo.setImageURI(uri)
        findViewById<TextView>(R.id.texto_foto_placeholder)?.visibility = android.view.View.GONE
    }

    private fun cargarCalendarioDispositivo() {
        contenedorCalendario.removeAllViews()
        val tareas = viewModel.obtenerTodasTareasPorDispositivo(dispositivoId).map {
            ItemCalendarioDispositivo(
                tipo = "Mantenimiento",
                nombre = it.nombre,
                descripcion = it.descripcion,
                repetirCada = it.repetirCada
            )
        }
        val inspecciones = viewModel.obtenerTodasInspeccionesPorDispositivo(dispositivoId).map {
            ItemCalendarioDispositivo(
                tipo = "Inspeccion",
                nombre = it.nombre,
                descripcion = it.descripcion,
                repetirCada = it.repetirCada
            )
        }
        val items = (tareas + inspecciones)
            .distinctBy { "${it.tipo}|${it.nombre}|${it.descripcion}|${it.repetirCada}" }
            .sortedWith(compareBy<ItemCalendarioDispositivo> { it.tipo }.thenBy { it.nombre })

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "No hay mantenimientos ni inspecciones registradas"
                textSize = 14f
                setTextColor(resources.getColor(R.color.text_secondary, theme))
                setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
            }
            contenedorCalendario.addView(vacio)
            return
        }

        items.forEach { item ->
            contenedorCalendario.addView(crearTarjetaCalendario(item))
        }
    }

    private fun cargarResumenIA() {
        textoResumenIA.text = "Analizando inspecciones..."
        lifecycleScope.launch {
            val dispositivo = dispositivoActual()
            val detalles = withContext(Dispatchers.IO) {
                viewModel.obtenerTodasTareasPorDispositivo(dispositivoId)
                    .flatMap { tarea -> viewModel.cargarDetallesPorTarea(tarea.id) }
            }
            val inspeccionesConEstado = detalles.filter {
                it.tipo == "inspeccion" && (it.condicion.isNotBlank() || it.notas.isNotBlank())
            }

            if (inspeccionesConEstado.isEmpty()) {
                textoResumenIA.text = MaintenanceAiService.generarFeedbackLocal(detalles)
                return@launch
            }

            val feedback = runCatching {
                withContext(Dispatchers.IO) {
                    aiService.generateInspectionFeedback(dispositivo, detalles)
                }
            }.getOrElse {
                MaintenanceAiService.generarFeedbackLocal(detalles)
            }
            textoResumenIA.text = feedback
        }
    }

    private fun dispositivoActual(): Dispositivo {
        return Dispositivo(
            id = dispositivoId,
            nombre = campoNombre.text.toString().trim(),
            categoria = spinnerCategoria.selectedItem?.toString() ?: "",
            marca = campoMarca.text.toString().trim(),
            modelo = campoModelo.text.toString().trim(),
            foto = fotoDispositivo
        )
    }

    private fun crearTarjetaCalendario(item: ItemCalendarioDispositivo): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.fondo_tarjeta)
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4.dp(), 0, 4.dp(), 8.dp())
            }

            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = item.tipo
                textSize = 12f
                setTextColor(resources.getColor(R.color.purple_500, theme))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = item.nombre
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 4.dp(), 0, 2.dp())
            })
            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = item.descripcion.ifBlank { "Sin descripcion" }
                textSize = 13f
                setTextColor(resources.getColor(R.color.text_secondary, theme))
            })
            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = "Repite: ${item.repetirCada}"
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_secondary, theme))
                setPadding(0, 6.dp(), 0, 0)
            })
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
