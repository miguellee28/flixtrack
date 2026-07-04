package com.proyectofinal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var barraNavegacion: BottomNavigationView
    private lateinit var contenedorPantallas: FrameLayout
    private lateinit var viewModel: DispositivosViewModel

    companion object {
        private const val ID_INICIO = 1
        private const val ID_CALENDARIO = 2
        private const val ID_DISPOSITIVOS = 3
        private const val ID_AJUSTES = 4
        const val PREFS_NAME = "mis_preferencias"
        const val PREFS_TEMA_OSCURO = "tema_oscuro"
    }

    private val resultadoAgregarDispositivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.cargarDispositivos()
        viewModel.cargarHomeData()
        viewModel.cargarCalendarioData()
    }

    private val resultadoDetalleDispositivo = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.cargarDispositivos()
    }

    private val resultadoAgregarTarea = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.cargarHomeData()
        viewModel.cargarCalendarioData()
    }

    private val resultadoTareaDetalle = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.cargarHomeData()
        viewModel.cargarCalendarioData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        barraNavegacion = findViewById(R.id.barra_navegacion)
        contenedorPantallas = findViewById(R.id.contenedor_pantallas)

        viewModel = ViewModelProvider(this, DispositivosViewModelFactory(application))[DispositivosViewModel::class.java]

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { vista, insets ->
            val barrasSistema = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barrasSistema.left, barrasSistema.top, barrasSistema.right, 0)
            insets
        }

        cargarPreferencias()
        configurarBarra()
        observarDispositivos()
        observarHomeData()
        observarCalendarioData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarDispositivos()
        viewModel.cargarHomeData()
        viewModel.cargarCalendarioData()
    }

    private fun observarDispositivos() {
        lifecycleScope.launch {
            viewModel.dispositivos.collect { lista ->
                val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
                if (listView != null) {
                    listView.adapter = AdaptadorDispositivos(this@MainActivity, lista)
                }
            }
        }
    }

    private fun observarHomeData() {
        lifecycleScope.launch {
            viewModel.tareasPasadas.collect { lista ->
                actualizarSeccionAtrasadas(lista)
            }
        }
        lifecycleScope.launch {
            viewModel.tareasProximas.collect { lista ->
                actualizarSeccionPorHacer(lista)
            }
        }
        lifecycleScope.launch {
            viewModel.tareasLejanas.collect { lista ->
                actualizarSeccionProximo(lista)
            }
        }
    }

    private fun observarCalendarioData() {
        lifecycleScope.launch {
            viewModel.tareaSeleccionada.collect { tab ->
                actualizarCalendarioSegunTab(tab)
            }
        }
        lifecycleScope.launch {
            viewModel.tareasPasadas.collect { lista ->
                if (viewModel.tareaSeleccionada.value == "atrasado") {
                    actualizarListaCalendario(lista)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.tareasProximas.collect { lista ->
                if (viewModel.tareaSeleccionada.value == "proximo") {
                    actualizarListaCalendario(lista)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.tareasCompletadas.collect { lista ->
                if (viewModel.tareaSeleccionada.value == "completado") {
                    actualizarListaCalendario(lista)
                }
            }
        }
    }

    private fun actualizarSeccionAtrasadas(items: List<ItemProgramado>) {
        val seccion = contenedorPantallas.findViewById<LinearLayout>(R.id.seccion_atrasadas) ?: return
        seccion.removeAllViews()

        val header = LayoutInflater.from(this).inflate(R.layout.section_header, seccion, false)
        header.findViewById<TextView>(R.id.texto_titulo_seccion).text = "Atrasado"
        header.findViewById<TextView>(R.id.texto_contador).text = "${items.size} items"
        seccion.addView(header)

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "No hay tareas atrasadas"
                textSize = 14f
                setPadding(32, 16, 32, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            }
            seccion.addView(vacio)
        } else {
            for (grupo in agruparPorFechaYDispositivo(items)) {
                agregarCardGrupoInicio(seccion, grupo)
            }
        }
    }

    private fun actualizarSeccionPorHacer(items: List<ItemProgramado>) {
        val seccion = contenedorPantallas.findViewById<LinearLayout>(R.id.seccion_por_hacer) ?: return
        seccion.removeAllViews()

        val header = LayoutInflater.from(this).inflate(R.layout.section_header, seccion, false)
        header.findViewById<TextView>(R.id.texto_titulo_seccion).text = "Por Hacer"
        header.findViewById<TextView>(R.id.texto_contador).text = "${items.size} items"
        seccion.addView(header)

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "No hay tareas pendientes"
                textSize = 14f
                setPadding(32, 16, 32, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            }
            seccion.addView(vacio)
        } else {
            for (grupo in agruparPorFechaYDispositivo(items)) {
                agregarCardGrupoInicio(seccion, grupo)
            }
        }
    }

    private fun actualizarSeccionProximo(items: List<ItemProgramado>) {
        val seccion = contenedorPantallas.findViewById<LinearLayout>(R.id.seccion_proximo) ?: return
        seccion.removeAllViews()

        val header = LayoutInflater.from(this).inflate(R.layout.section_header, seccion, false)
        header.findViewById<TextView>(R.id.texto_titulo_seccion).text = "Proximo"
        header.findViewById<TextView>(R.id.texto_contador).text = "${items.size} items"
        seccion.addView(header)

        if (items.isEmpty()) {
            val vacio = TextView(this).apply {
                text = "No hay tareas proximas"
                textSize = 14f
                setPadding(32, 16, 32, 16)
                setTextColor(resources.getColor(android.R.color.darker_gray, theme))
            }
            seccion.addView(vacio)
        } else {
            for (grupo in agruparPorFechaYDispositivo(items)) {
                agregarCardGrupoProximo(seccion, grupo)
            }
        }
    }

    private fun agruparPorFechaYDispositivo(items: List<ItemProgramado>): List<List<ItemProgramado>> {
        return items.groupBy { Pair(it.fecha, it.nombreDispositivo.ifBlank { "Sin dispositivo" }) }.values.toList()
    }

    private fun agregarCardGrupoInicio(seccion: LinearLayout, grupo: List<ItemProgramado>) {
        val primero = grupo.firstOrNull() ?: return
        val dispositivo = primero.nombreDispositivo.ifBlank { "Sin dispositivo" }
        val vista = LayoutInflater.from(this).inflate(R.layout.item_tarea_inicio, seccion, false)
        vista.findViewById<TextView>(R.id.texto_nombre_tarea).text = dispositivo
        vista.findViewById<TextView>(R.id.texto_descripcion_tarea).text = resumenGrupo(grupo)
        vista.findViewById<TextView>(R.id.texto_nombre_dispositivo).text = "${primero.fecha} - ${textoCantidadGrupo(grupo)}"
        vista.findViewById<Button>(R.id.boton_ver).apply {
            visibility = View.VISIBLE
            setOnClickListener { abrirDetalleGrupo(grupo) }
        }
        seccion.addView(vista)
    }

    private fun agregarCardGrupoProximo(seccion: LinearLayout, grupo: List<ItemProgramado>) {
        val primero = grupo.firstOrNull() ?: return
        val dispositivo = primero.nombreDispositivo.ifBlank { "Sin dispositivo" }
        val vista = LayoutInflater.from(this).inflate(R.layout.item_tarea_proximo, seccion, false)
        vista.findViewById<TextView>(R.id.texto_fecha).text = primero.fecha
        vista.findViewById<TextView>(R.id.texto_nombre_tarea).text = dispositivo
        vista.findViewById<TextView>(R.id.texto_nombre_dispositivo).text = resumenGrupo(grupo)
        vista.findViewById<Button>(R.id.boton_ver).apply {
            visibility = View.VISIBLE
            setOnClickListener { abrirDetalleGrupo(grupo) }
        }
        seccion.addView(vista)
    }

    private fun resumenGrupo(grupo: List<ItemProgramado>): String {
        return grupo.joinToString(separator = "\n") { item ->
            val tipo = if (item.tipo == "tarea") "Mantenimiento" else "Inspeccion"
            "$tipo: ${item.nombre}"
        }
    }

    private fun textoCantidadGrupo(grupo: List<ItemProgramado>): String {
        return if (grupo.size == 1) "1 item" else "${grupo.size} items"
    }

    private fun abrirDetalleGrupo(grupo: List<ItemProgramado>) {
        val tarea = grupo.firstOrNull { it.tipo == "tarea" }
        abrirDetalleItem(tarea ?: grupo.first())
    }

    private fun cargarPreferencias() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val temaOscuro = prefs.getBoolean(PREFS_TEMA_OSCURO, false)

        if (temaOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun configurarBarra() {
        barraNavegacion.isItemActiveIndicatorEnabled = false

        val menuBarra = barraNavegacion.menu
        agregarOpcion(menuBarra, ID_INICIO, R.drawable.ic_inicio, "Inicio")
        agregarOpcion(menuBarra, ID_CALENDARIO, R.drawable.ic_calendario, "Calendario")
        agregarOpcion(menuBarra, ID_DISPOSITIVOS, R.drawable.ic_dispositivos, "Dispositivos")
        agregarOpcion(menuBarra, ID_AJUSTES, R.drawable.ic_ajustes, "Ajustes")
        barraNavegacion.setOnItemSelectedListener { elemento ->
            procesarSeleccion(elemento)
        }

        barraNavegacion.selectedItemId = ID_INICIO
    }

    private fun agregarOpcion(menu: Menu, id: Int, icono: Int, titulo: String) {
        menu.add(Menu.NONE, id, Menu.NONE, titulo).setIcon(icono)
    }

    private fun procesarSeleccion(elemento: MenuItem): Boolean {
        return when (elemento.itemId) {
            ID_INICIO -> {
                mostrarInicio()
                viewModel.cargarHomeData()
                true
            }
            ID_DISPOSITIVOS -> {
                mostrarPantalla(R.layout.layout_dispositivos)
                configurarBotonAgregar()
                configurarClickLista()
                viewModel.cargarDispositivos()
                asignarListaDispositivos()
                true
            }
            ID_CALENDARIO -> {
                mostrarPantalla(R.layout.layout_calendario)
                configurarCalendario()
                viewModel.cargarCalendarioData()
                true
            }
            ID_AJUSTES -> {
                mostrarPantalla(R.layout.layout_ajustes)
                configurarAjustes()
                true
            }
            else -> false
        }
    }

    private fun mostrarInicio() {
        mostrarPantalla(R.layout.layout_inicio)
        actualizarSeccionAtrasadas(viewModel.tareasPasadas.value)
        actualizarSeccionPorHacer(viewModel.tareasProximas.value)
        actualizarSeccionProximo(viewModel.tareasLejanas.value)
    }

    private fun asignarListaDispositivos() {
        val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
        if (listView != null) {
            listView.adapter = AdaptadorDispositivos(this, viewModel.dispositivos.value)
        }
    }

    private fun configurarClickLista() {
        val listView = contenedorPantallas.findViewById<ListView>(R.id.lista_dispositivos)
        listView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, posicion, _ ->
            val dispositivo = viewModel.dispositivos.value.getOrNull(posicion) ?: return@OnItemClickListener
            val intent = Intent(this, DetalleDispositivoActivity::class.java).apply {
                putExtra(DetalleDispositivoActivity.EXTRA_ID, dispositivo.id)
                putExtra(DetalleDispositivoActivity.EXTRA_NOMBRE_DISPOSITIVO, dispositivo.nombre)
                putExtra(DetalleDispositivoActivity.EXTRA_CATEGORIA, dispositivo.categoria)
                putExtra(DetalleDispositivoActivity.EXTRA_MARCA, dispositivo.marca)
                putExtra(DetalleDispositivoActivity.EXTRA_MODELO, dispositivo.modelo)
            }
            resultadoDetalleDispositivo.launch(intent)
        }
    }

    private fun configurarCalendario() {
        val botonProximo = contenedorPantallas.findViewById<Button>(R.id.boton_proximo)
        val botonAtrasado = contenedorPantallas.findViewById<Button>(R.id.boton_atrasado)
        val botonCompletado = contenedorPantallas.findViewById<Button>(R.id.boton_completado)
        val botonAgregar = contenedorPantallas.findViewById<Button>(R.id.boton_agregar_tarea_calendario)
        val listaCalendario = contenedorPantallas.findViewById<ListView>(R.id.lista_calendario)

        val actualizarSeleccion = { seleccion: String ->
            val botones = listOf(botonProximo, botonAtrasado, botonCompletado)
            for (btn in botones) {
                btn?.alpha = 0.5f
            }
            when (seleccion) {
                "proximo" -> botonProximo?.alpha = 1.0f
                "atrasado" -> botonAtrasado?.alpha = 1.0f
                "completado" -> botonCompletado?.alpha = 1.0f
            }
            viewModel.seleccionarTabCalendario(seleccion)
        }

        botonProximo?.setOnClickListener { actualizarSeleccion("proximo") }
        botonAtrasado?.setOnClickListener { actualizarSeleccion("atrasado") }
        botonCompletado?.setOnClickListener { actualizarSeleccion("completado") }

        botonAgregar?.setOnClickListener {
            val intent = Intent(this, AgregarTareaActivity::class.java)
            resultadoAgregarTarea.launch(intent)
        }

        actualizarSeleccion(viewModel.tareaSeleccionada.value)
        actualizarCalendarioSegunTab(viewModel.tareaSeleccionada.value)
    }

    private fun actualizarCalendarioSegunTab(tab: String) {
        val listaCalendario = contenedorPantallas.findViewById<ListView>(R.id.lista_calendario) ?: return
        when (tab) {
            "proximo" -> listaCalendario.adapter = AdaptadorTareasCalendario(this, viewModel.tareasProximas.value, ::abrirDetalleGrupo)
            "atrasado" -> listaCalendario.adapter = AdaptadorTareasCalendario(this, viewModel.tareasPasadas.value, ::abrirDetalleGrupo)
            "completado" -> listaCalendario.adapter = AdaptadorTareasCalendario(this, viewModel.tareasCompletadas.value, ::abrirDetalleGrupo)
        }
    }

    private fun actualizarListaCalendario(items: List<ItemProgramado>) {
        val listaCalendario = contenedorPantallas.findViewById<ListView>(R.id.lista_calendario) ?: return
        listaCalendario.adapter = AdaptadorTareasCalendario(this, items, ::abrirDetalleGrupo)
    }

    private fun abrirDetalleItem(item: ItemProgramado) {
        if (item.tipo == "tarea") {
            val intent = Intent(this, TareaDetalleActivity::class.java).apply {
                putExtra(TareaDetalleActivity.EXTRA_TAREA_ID, item.id)
                putExtra(TareaDetalleActivity.EXTRA_TAREA_NOMBRE, item.nombre)
                putExtra(TareaDetalleActivity.EXTRA_DISPOSITIVO_NOMBRE, item.nombreDispositivo)
                putExtra(TareaDetalleActivity.EXTRA_TAREA_FECHA, item.fecha)
            }
            resultadoTareaDetalle.launch(intent)
        } else {
            val dispositivo = item.nombreDispositivo.ifBlank { "Sin dispositivo" }
            AlertDialog.Builder(this)
                .setTitle(item.nombre)
                .setMessage("Inspeccion\nFecha: ${item.fecha}\nDispositivo: $dispositivo\n\n${item.descripcion}")
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    private fun configurarAjustes() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val switchOscuro = contenedorPantallas.findViewById<SwitchCompat>(R.id.switch_modo_oscuro)

        if (switchOscuro != null) {
            switchOscuro.isChecked = prefs.getBoolean(PREFS_TEMA_OSCURO, false)
            switchOscuro.setOnCheckedChangeListener { _, activo ->
                prefs.edit().putBoolean(PREFS_TEMA_OSCURO, activo).apply()
                if (activo) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }

    private fun mostrarPantalla(layout: Int) {
        contenedorPantallas.removeAllViews()
        LayoutInflater.from(this).inflate(layout, contenedorPantallas, true)
    }

    private fun configurarBotonAgregar() {
        val boton = contenedorPantallas.findViewById<Button>(R.id.boton_agregar)
        boton?.setOnClickListener {
            val intent = Intent(this, AgregarDispositivoActivity::class.java)
            resultadoAgregarDispositivo.launch(intent)
        }
    }

    class AdaptadorDispositivos(
        private val contexto: Context,
        private val dispositivos: List<Dispositivo>
    ) : BaseAdapter() {

        override fun getCount() = dispositivos.size
        override fun getItem(posicion: Int) = dispositivos[posicion]
        override fun getItemId(posicion: Int) = dispositivos[posicion].id

        override fun getView(posicion: Int, vistaReciclada: View?, parent: ViewGroup): View {
            val vista = vistaReciclada ?: LayoutInflater.from(contexto)
                .inflate(R.layout.item_dispositivo_lista, parent, false)

            val dispositivo = dispositivos[posicion]
            vista.findViewById<TextView>(R.id.texto_nombre).text = dispositivo.nombre
            vista.findViewById<TextView>(R.id.texto_marca).text = dispositivo.marca
            vista.findViewById<TextView>(R.id.texto_modelo).text = dispositivo.modelo

            return vista
        }
    }

    class AdaptadorTareasCalendario(
        private val contexto: Context,
        private val items: List<ItemProgramado>,
        private val onVerClick: (List<ItemProgramado>) -> Unit
    ) : BaseAdapter() {

        private val grupos = items
            .groupBy { Pair(it.fecha, it.nombreDispositivo.ifBlank { "Sin dispositivo" }) }
            .values
            .toList()

        override fun getCount() = grupos.size
        override fun getItem(posicion: Int) = grupos[posicion]
        override fun getItemId(posicion: Int) = grupos[posicion].firstOrNull()?.id ?: posicion.toLong()

        override fun getView(posicion: Int, vistaReciclada: View?, parent: ViewGroup): View {
            val vista = vistaReciclada ?: LayoutInflater.from(contexto)
                .inflate(R.layout.item_tarea_inicio, parent, false)

            val grupo = grupos[posicion]
            val primero = grupo.first()
            val dispositivo = primero.nombreDispositivo.ifBlank { "Sin dispositivo" }
            vista.findViewById<TextView>(R.id.texto_nombre_tarea).text = dispositivo
            vista.findViewById<TextView>(R.id.texto_descripcion_tarea).text = resumenGrupoCalendario(grupo)
            vista.findViewById<TextView>(R.id.texto_nombre_dispositivo).text = "${primero.fecha} - ${textoCantidadGrupoCalendario(grupo)}"
            vista.findViewById<Button>(R.id.boton_ver).apply {
                visibility = View.VISIBLE
                setOnClickListener { onVerClick(grupo) }
            }

            return vista
        }

        private fun resumenGrupoCalendario(grupo: List<ItemProgramado>): String {
            return grupo.joinToString(separator = "\n") { item ->
                val tipo = if (item.tipo == "tarea") "Mantenimiento" else "Inspeccion"
                "$tipo: ${item.nombre}"
            }
        }

        private fun textoCantidadGrupoCalendario(grupo: List<ItemProgramado>): String {
            return if (grupo.size == 1) "1 item" else "${grupo.size} items"
        }
    }
}
