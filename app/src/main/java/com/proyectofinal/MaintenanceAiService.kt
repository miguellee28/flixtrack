package com.proyectofinal

import com.proyectofinal.model.*
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiSource(
    val titulo: String,
    val url: String
)

data class AiMaintenanceResult(
    val dispositivo: Dispositivo,
    val tareas: List<Tarea>,
    val inspecciones: List<Inspeccion>,
    val fuentes: List<AiSource>
)

data class BraveSearchResult(
    val title: String,
    val url: String,
    val description: String
)

class GoogleGenAiClient(
    private val apiKey: String,
    private val model: String = "gemma-4-31b-it"
) {
    fun generateJson(prompt: String, imageBytes: ByteArray? = null, mimeType: String = "image/jpeg"): String {
        require(apiKey.isNotBlank()) { "Falta GOOGLE_GENAI_API_KEY en local.properties" }

        val parts = JSONArray().put(JSONObject().put("text", prompt))
        if (imageBytes != null) {
            parts.put(
                JSONObject()
                    .put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", mimeType)
                            .put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                    )
            )
        }

        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", parts)
                )
            )
        if (imageBytes == null) {
            body.put(
                "generationConfig",
                JSONObject().put("response_mime_type", "application/json")
            )
        }

        val encodedModel = URLEncoder.encode(model, "UTF-8")
        val response = postJson("https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:generateContent", body)
        return extractOutputText(JSONObject(response))
    }

    private fun postJson(urlText: String, body: JSONObject): String {
        var lastError: String? = null
        var lastCode = 0

        repeat(3) { attempt ->
            val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 45000
                readTimeout = 120000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val responseText = readResponse(connection)
            if (connection.responseCode in 200..299) {
                return responseText
            }

            lastCode = connection.responseCode
            lastError = responseText
            if (connection.responseCode !in setOf(429, 500, 503) || attempt == 2) {
                throw IllegalStateException("Gemma respondio con error ${connection.responseCode}: $responseText")
            }
            Thread.sleep(1200L * (attempt + 1))
        }

        throw IllegalStateException("Gemma respondio con error $lastCode: ${lastError.orEmpty()}")
    }

    private fun extractOutputText(json: JSONObject): String {
        json.optString("output_text").takeIf { it.isNotBlank() }?.let { return it }

        val candidates = json.optJSONArray("candidates") ?: JSONArray()
        for (i in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(i) ?: continue
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: continue
            for (j in 0 until parts.length()) {
                val part = parts.optJSONObject(j) ?: continue
                if (part.optBoolean("thought")) continue
                part.optString("text").takeIf { it.isNotBlank() }?.let { return it }
            }
        }

        val steps = json.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            if (step.optString("type") != "model_output") continue
            val content = step.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val block = content.optJSONObject(j) ?: continue
                block.optString("text").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        throw IllegalStateException("Gemma no devolvio texto util")
    }
}

class BraveSearchClient(private val apiKey: String) {
    fun search(query: String, count: Int = 5): List<BraveSearchResult> {
        require(apiKey.isNotBlank()) { "Falta BRAVE_SEARCH_API_KEY en local.properties" }

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlText = "https://api.search.brave.com/res/v1/web/search?q=$encodedQuery&count=$count&search_lang=es&country=us"
        val connection = (URL(urlText).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("X-Subscription-Token", apiKey)
        }

        val responseText = readResponse(connection)
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Brave respondio con error ${connection.responseCode}: $responseText")
        }

        val results = JSONObject(responseText)
            .optJSONObject("web")
            ?.optJSONArray("results")
            ?: JSONArray()
        val parsed = mutableListOf<BraveSearchResult>()
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val url = item.optString("url")
            if (url.isBlank()) continue
            parsed.add(
                BraveSearchResult(
                    title = item.optString("title"),
                    url = url,
                    description = item.optString("description")
                )
            )
        }
        return parsed
    }
}

class MaintenanceAiService(
    private val genAiClient: GoogleGenAiClient,
    private val searchClient: BraveSearchClient
) {
    fun generateInspectionFeedback(
        dispositivo: Dispositivo,
        detalles: List<TareaDetalle>
    ): String {
        val inspecciones = detalles.filter { it.tipo == "inspeccion" && (it.condicion.isNotBlank() || it.notas.isNotBlank()) }
        if (inspecciones.isEmpty()) {
            return "Aun no hay inspecciones completadas con estado. Cuando marques Bueno, Regular o Malo, la IA podra darte un resumen del estado del dispositivo."
        }

        val inspeccionesTexto = inspecciones.joinToString("\n") { detalle ->
            "- ${detalle.nombre}: estado=${detalle.condicion.ifBlank { "sin estado" }}, notas=${detalle.notas.ifBlank { "sin notas" }}, fecha=${detalle.fechaCompletada ?: "sin fecha"}"
        }

        val prompt = """
            Lee las inspecciones completadas de este dispositivo y da feedback util para el usuario.

            Dispositivo:
            nombre=${dispositivo.nombre}
            categoria=${dispositivo.categoria}
            marca=${dispositivo.marca}
            modelo=${dispositivo.modelo}

            Inspecciones:
            $inspeccionesTexto

            Devuelve solo JSON valido con esta forma:
            {
              "feedback": "texto corto en español"
            }

            Reglas:
            - Si todos los estados estan en bueno, di que todo funciona correctamente y menciona que debe continuar con mantenimiento preventivo.
            - Si hay regular, explica que debe vigilarse y que accion concreta revisar.
            - Si hay malo, indica prioridad alta y que debe atenderse pronto.
            - Usa lenguaje simple, maximo 4 oraciones.
            - No inventes piezas especificas si no aparecen en inspecciones/notas.
        """.trimIndent()

        val json = parseJsonObject(genAiClient.generateJson(prompt))
        return json.optString("feedback").ifBlank { generarFeedbackLocal(detalles) }
    }

    fun generateSchedule(
        nombre: String,
        categoria: String,
        marca: String,
        modelo: String,
        imageBytes: ByteArray?,
        onProgress: (String) -> Unit = {}
    ): AiMaintenanceResult {
        onProgress("Identificando el dispositivo...")
        val identified = identifyDevice(nombre, categoria, marca, modelo, imageBytes)
        val queryBase = when {
            identified.marca.isNotBlank() && identified.modelo.isNotBlank() -> "${identified.marca} ${identified.modelo}"
            identified.marca.isNotBlank() -> listOf(identified.marca, identified.nombre, identified.categoria)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            else -> listOf(identified.nombre, identified.categoria).filter { it.isNotBlank() }.joinToString(" ")
        }
        if (queryBase.isBlank()) {
            throw IllegalArgumentException("Agrega una foto o escribe marca/modelo para buscar mantenimiento")
        }

        onProgress("Buscando fuentes en la web...")
        val queries = buildQueries(queryBase, identified.marca, identified.modelo, identified.categoria)
        val searchResults = queries
            .flatMapIndexed { index, query ->
                onProgress("Buscando fuentes ${index + 1}/${queries.size}...")
                searchClient.search(query, count = 4)
            }
            .distinctBy { it.url }
            .take(10)

        if (searchResults.isEmpty()) {
            throw IllegalStateException("No se encontraron fuentes en Brave para este dispositivo")
        }

        onProgress("Analizando ${searchResults.size} fuentes encontradas...")
        return buildSchedule(identified, searchResults)
    }

    private fun identifyDevice(
        nombre: String,
        categoria: String,
        marca: String,
        modelo: String,
        imageBytes: ByteArray?
    ): Dispositivo {
        if (marca.isNotBlank() && modelo.isNotBlank()) {
            return Dispositivo(nombre = nombre.ifBlank { "$marca $modelo" }, categoria = categoria, marca = marca, modelo = modelo)
        }

        val descripcionImagen = if (imageBytes != null) {
            genAiClient.generateJson(
                "Identify the device or product in this image. Answer briefly with likely brand and model if possible.",
                imageBytes
            )
        } else {
            ""
        }

        val prompt = """
            Identifica el dispositivo con la informacion disponible.
            Campos escritos por el usuario:
            nombre=$nombre
            categoria=$categoria
            marca=$marca
            modelo=$modelo
            descripcionImagen=$descripcionImagen

            Devuelve solo JSON valido con esta forma exacta:
            {
              "nombre": "nombre comun del dispositivo",
              "categoria": "categoria breve",
              "marca": "marca probable",
              "modelo": "modelo probable"
            }
            Si no puedes identificar algo, usa el valor escrito por el usuario o una cadena vacia.
        """.trimIndent()

        val json = parseJsonObject(genAiClient.generateJson(prompt))
        val detectedMarca = json.optString("marca").ifBlank { marca }
        val detectedModelo = json.optString("modelo").ifBlank { modelo }
        return Dispositivo(
            nombre = json.optString("nombre").ifBlank { nombre.ifBlank { "$detectedMarca $detectedModelo".trim() } },
            categoria = json.optString("categoria").ifBlank { categoria },
            marca = detectedMarca,
            modelo = detectedModelo
        )
    }

    private fun buildSchedule(device: Dispositivo, searchResults: List<BraveSearchResult>): AiMaintenanceResult {
        val fuentesTexto = searchResults.joinToString("\n") { result ->
            "- ${result.title}\n  URL: ${result.url}\n  Descripcion: ${result.description}"
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prompt = """
            Usa estas fuentes web para proponer un calendario practico de mantenimiento para el dispositivo.
            Si no hay una fuente exacta para el modelo, puedes usar fuentes de modelos similares, manuales de la misma marca,
            o recomendaciones tecnicas para la misma categoria de equipo.
            No escribas disculpas ni frases como "las fuentes son generales" en las descripciones.

            Dispositivo:
            nombre=${device.nombre}
            categoria=${device.categoria}
            marca=${device.marca}
            modelo=${device.modelo}

            Fecha base para fechaInicial: $today

            Fuentes:
            $fuentesTexto

            Devuelve solo JSON valido con esta forma exacta:
            {
              "dispositivo": {
                "nombre": "",
                "categoria": "",
                "marca": "",
                "modelo": ""
              },
              "mantenimientos": [
                {
                  "nombre": "",
                  "descripcion": "",
                  "fechaInicial": "$today",
                  "repetirCada": "Una vez|Semana|Mes|6 meses|1 año"
                }
              ],
              "inspecciones": [
                {
                  "nombre": "",
                  "descripcion": "",
                  "fechaInicial": "$today",
                  "repetirCada": "Una vez|Semana|Mes|6 meses|1 año"
                }
              ],
              "fuentes": [
                { "titulo": "", "url": "" }
              ]
            }
            Maximo 4 mantenimientos y 4 inspecciones. Usa espanol claro.
            En cada descripcion escribe que debe hacer el usuario, con acciones concretas y verificables.
            Ejemplos de buen estilo:
            - "Limpiar o reemplazar filtros, retirar polvo de rejillas y verificar que el flujo de aire no este obstruido."
            - "Revisar conexiones, buscar fugas visibles, escuchar ruidos anormales y confirmar que el equipo arranque sin vibraciones."
            Evita descripciones vagas como "mantenimiento general" o explicaciones sobre falta de fuentes exactas.
        """.trimIndent()

        val json = parseJsonObject(genAiClient.generateJson(prompt))
        val deviceJson = json.optJSONObject("dispositivo") ?: JSONObject()
        val parsedDevice = Dispositivo(
            nombre = deviceJson.optString("nombre").ifBlank { device.nombre },
            categoria = deviceJson.optString("categoria").ifBlank { device.categoria },
            marca = deviceJson.optString("marca").ifBlank { device.marca },
            modelo = deviceJson.optString("modelo").ifBlank { device.modelo }
        )
        return AiMaintenanceResult(
            dispositivo = parsedDevice,
            tareas = parseTareas(json.optJSONArray("mantenimientos"), today),
            inspecciones = parseInspecciones(json.optJSONArray("inspecciones"), today),
            fuentes = parseSources(json.optJSONArray("fuentes")).ifEmpty {
                searchResults.take(5).map { AiSource(it.title, it.url) }
            }
        )
    }

    private fun buildQueries(base: String, marca: String, modelo: String, categoria: String): List<String> {
        val manufacturer = marca.lowercase(Locale.getDefault()).replace(" ", "")
        return listOf(
            "\"$base\" maintenance schedule",
            "\"$base\" mantenimiento manual",
            if (manufacturer.isNotBlank() && modelo.isNotBlank()) "site:$manufacturer.com \"$modelo\" manual" else "$base manual",
            listOf(marca, categoria, "maintenance guide").filter { it.isNotBlank() }.joinToString(" "),
            listOf(categoria, "maintenance checklist").filter { it.isNotBlank() }.joinToString(" ")
        ).distinct()
    }

    private fun parseTareas(items: JSONArray?, defaultDate: String): List<Tarea> {
        return parseScheduledItems(items).map {
            Tarea(
                nombre = it.nombre,
                descripcion = it.descripcion,
                fecha = it.fecha.ifBlank { defaultDate },
                repetirCada = normalizarRepeticion(it.repetirCada)
            )
        }
    }

    private fun parseInspecciones(items: JSONArray?, defaultDate: String): List<Inspeccion> {
        return parseScheduledItems(items).map {
            Inspeccion(
                nombre = it.nombre,
                descripcion = it.descripcion,
                fecha = it.fecha.ifBlank { defaultDate },
                repetirCada = normalizarRepeticion(it.repetirCada)
            )
        }
    }

    private data class ScheduledItem(
        val nombre: String,
        val descripcion: String,
        val fecha: String,
        val repetirCada: String
    )

    private fun parseScheduledItems(items: JSONArray?): List<ScheduledItem> {
        val parsed = mutableListOf<ScheduledItem>()
        val array = items ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val nombre = item.optString("nombre")
            if (nombre.isBlank()) continue
            parsed.add(
                ScheduledItem(
                    nombre = nombre,
                    descripcion = item.optString("descripcion"),
                    fecha = item.optString("fechaInicial"),
                    repetirCada = item.optString("repetirCada")
                )
            )
        }
        return parsed
    }

    private fun parseSources(items: JSONArray?): List<AiSource> {
        val parsed = mutableListOf<AiSource>()
        val array = items ?: JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString("url")
            if (url.isBlank()) continue
            parsed.add(AiSource(item.optString("titulo"), url))
        }
        return parsed
    }

    private fun normalizarRepeticion(value: String): String {
        val normalized = value.lowercase(Locale.getDefault())
        return when {
            normalized.contains("semana") -> "Semana"
            normalized.contains("6") && normalized.contains("mes") -> "6 meses"
            normalized.contains("mes") -> "Mes"
            normalized.contains("ano") || normalized.contains("año") -> "1 año"
            else -> "Una vez"
        }
    }

    private fun parseJsonObject(text: String): JSONObject {
        val trimmed = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return JSONObject(extraerObjetoJson(trimmed))
    }

    private fun extraerObjetoJson(text: String): String {
        val start = text.indexOf('{')
        if (start < 0) return text

        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val char = text[i]
            when {
                escaped -> escaped = false
                char == '\\' && inString -> escaped = true
                char == '"' -> inString = !inString
                !inString && char == '{' -> depth++
                !inString && char == '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, i + 1)
                    }
                }
            }
        }
        return text
    }

    companion object {
        fun generarFeedbackLocal(detalles: List<TareaDetalle>): String {
            val inspecciones = detalles.filter { it.tipo == "inspeccion" && (it.condicion.isNotBlank() || it.notas.isNotBlank()) }
            if (inspecciones.isEmpty()) {
                return "Aun no hay inspecciones completadas con estado. Completa una inspeccion para recibir feedback."
            }
            val malas = inspecciones.count { it.condicion == "malo" }
            val regulares = inspecciones.count { it.condicion == "regular" }
            return when {
                malas > 0 -> "Hay $malas inspeccion(es) en mal estado. Revisa esas observaciones cuanto antes y atiende el problema antes de seguir usando el dispositivo de forma normal."
                regulares > 0 -> "Hay $regulares inspeccion(es) en estado regular. El dispositivo puede seguir funcionando, pero conviene vigilar esas partes y programar una revision preventiva."
                else -> "Todo funciona correctamente segun las inspecciones registradas. Mantente al dia con el calendario preventivo para conservar el buen estado del dispositivo."
            }
        }
    }
}

private fun readResponse(connection: HttpURLConnection): String {
    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    return BufferedReader(InputStreamReader(stream ?: connection.inputStream)).use { reader ->
        buildString {
            var line = reader.readLine()
            while (line != null) {
                append(line)
                line = reader.readLine()
            }
        }
    }
}
