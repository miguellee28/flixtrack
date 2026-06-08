package com.proyectofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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

    private var dispositivoId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_dispositivo)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        campoNombre = findViewById(R.id.campo_nombre)
        campoMarca = findViewById(R.id.campo_marca)
        campoModelo = findViewById(R.id.campo_modelo)
        spinnerCategoria = findViewById(R.id.spinner_categoria)
        botonGuardar = findViewById(R.id.boton_guardar)
        botonCompartir = findViewById(R.id.boton_compartir)

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

        observarViewModel()

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

            viewModel.actualizarDispositivo(dispositivo)
        }

        botonCompartir.setOnClickListener {
            compartirDispositivo()
        }
    }

    private fun compartirDispositivo() {
        val nombre = campoNombre.text.toString().trim()
        val marca = campoMarca.text.toString().trim()
        val modelo = campoModelo.text.toString().trim()
        val categoria = spinnerCategoria.selectedItem.toString()

        val texto = "Dispositivo: $nombre\nCategoría: $categoria\nMarca: $marca\nModelo: $modelo"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Compartir dispositivo"))
        } else {
            Toast.makeText(this, "No hay aplicaciones para compartir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observarViewModel() {
        lifecycleScope.launch {
            viewModel.mensaje.collect { msg ->
                if (msg != null) {
                    Toast.makeText(this@DetalleDispositivoActivity, msg, Toast.LENGTH_SHORT).show()
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
}
