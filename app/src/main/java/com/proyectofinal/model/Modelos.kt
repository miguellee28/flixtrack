package com.proyectofinal.model

// Entidades de dominio usadas por la aplicación.
data class Dispositivo(
    val id: Long = 0,
    val nombre: String,
    val categoria: String,
    val marca: String,
    val modelo: String,
    val foto: String = ""
)

data class Tarea(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val repetirCada: String,
    val dispositivoId: Long = 0,
    val completada: Boolean = false
)

data class Inspeccion(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val repetirCada: String,
    val dispositivoId: Long = 0,
    val completada: Boolean = false
)

data class TareaConDispositivo(
    val tarea: Tarea,
    val nombreDispositivo: String
)

data class ItemProgramado(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val fecha: String,
    val nombreDispositivo: String,
    val tipo: String // "tarea" or "inspeccion"
)

data class TareaDetalle(
    val id: Long = 0,
    val tareaId: Long,
    val tipo: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val condicion: String = "",
    val notas: String = "",
    val fotos: List<String> = emptyList(),
    val completada: Boolean = false,
    val fechaCompletada: String? = null
)
