package com.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DetalleDispositivoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_NOMBRE_DISPOSITIVO = "nombre_dispositivo"
        const val EXTRA_CATEGORIA = "categoria"
        const val EXTRA_MARCA = "marca"
        const val EXTRA_MODELO = "modelo"
    }

    private lateinit var viewModel: DispositivosViewModel
    private lateinit var campoNombre: EditText
    private lateinit var campoMarca: EditText
    private lateinit var campoModelo: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var botonGuardar: Button
    private lateinit var botonCompartir: Button
    private lateinit var botonEditarCalendario: TextView
    private lateinit var contenedorCalendario: LinearLayout

    private var dispositivoId: Long = 0

    private val resultadoEditarCalendario = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (dispositivoId != 0L) {
            cargarCalendarioDispositivo()
        }
    }

    private data class ItemCalendarioDispositivo(
        val fecha: String,
        val tipo: String,
        val nombre: String,
        val descripcion: String,
        val repetirCada: String,
        val completada: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_dispositivo)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

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
        botonCompartir = findViewById(R.id.boton_compartir)
        botonEditarCalendario = findViewById(R.id.boton_editar_calendario)
        contenedorCalendario = findViewById(R.id.contenedor_calendario_dispositivo)

        dispositivoId = intent.getLongExtra(EXTRA_ID, 0)
        campoNombre.setText(intent.getStringExtra(EXTRA_NOMBRE_DISPOSITIVO) ?: "")
        campoMarca.setText(intent.getStringExtra(EXTRA_MARCA) ?: "")
        campoModelo.setText(intent.getStringExtra(EXTRA_MODELO) ?: "")

        val categoria = intent.getStringExtra(EXTRA_CATEGORIA) ?: ""
        val categorias = resources.getStringArray(R.array.opciones_categoria)
        val indiceCategoria = categorias.indexOf(categoria)
        if (indiceCategoria >= 0) {
            spinnerCategoria.setSelection(indiceCategoria)
        }

        cargarCalendarioDispositivo()

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
                modelo = modelo
            )

            lifecycleScope.launch {
                viewModel.actualizarDispositivo(dispositivo)
                Toast.makeText(this@DetalleDispositivoActivity, "Dispositivo actualizado", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        }

        botonCompartir.setOnClickListener {
            val texto = "Dispositivo: ${campoNombre.text}\nMarca: ${campoMarca.text}\nModelo: ${campoModelo.text}\nCategoría: ${spinnerCategoria.selectedItem}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(intent, "Compartir dispositivo"))
            } else {
                Toast.makeText(this, "No hay apps disponibles para compartir", Toast.LENGTH_SHORT).show()
            }
        }

        botonEditarCalendario.setOnClickListener {
            val intent = Intent(this, AgregarTareaActivity::class.java).apply {
                putExtra(AgregarTareaActivity.EXTRA_DISPOSITIVO_ID, dispositivoId)
            }
            resultadoEditarCalendario.launch(intent)
        }
    }

    private fun cargarCalendarioDispositivo() {
        contenedorCalendario.removeAllViews()
        val tareas = viewModel.obtenerTodasTareasPorDispositivo(dispositivoId).map {
            ItemCalendarioDispositivo(
                fecha = it.fecha,
                tipo = "Mantenimiento",
                nombre = it.nombre,
                descripcion = it.descripcion,
                repetirCada = it.repetirCada,
                completada = it.completada
            )
        }
        val inspecciones = viewModel.obtenerTodasInspeccionesPorDispositivo(dispositivoId).map {
            ItemCalendarioDispositivo(
                fecha = it.fecha,
                tipo = "Inspeccion",
                nombre = it.nombre,
                descripcion = it.descripcion,
                repetirCada = it.repetirCada,
                completada = it.completada
            )
        }
        val items = (tareas + inspecciones).sortedBy { it.fecha }

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "No hay mantenimientos ni inspecciones registradas"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                setPadding(16, 12, 16, 12)
            }
            contenedorCalendario.addView(vacio)
            return
        }

        items.forEach { item ->
            contenedorCalendario.addView(crearTarjetaCalendario(item))
        }
    }

    private fun crearTarjetaCalendario(item: ItemCalendarioDispositivo): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.fondo_tarjeta)
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(4, 0, 4, 8)
            }

            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = "${item.fecha} - ${item.tipo}"
                textSize = 12f
                setTextColor(resources.getColor(R.color.purple_500, theme))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = item.nombre
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, 4, 0, 2)
            })
            addView(TextView(this@DetalleDispositivoActivity).apply {
                text = item.descripcion.ifBlank { "Sin descripcion" }
                textSize = 13f
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            })
            addView(TextView(this@DetalleDispositivoActivity).apply {
                val estado = if (item.completada) "Completado" else "Pendiente"
                text = "Repite: ${item.repetirCada} - $estado"
                textSize = 12f
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                setPadding(0, 6, 0, 0)
            })
        }
    }
}
