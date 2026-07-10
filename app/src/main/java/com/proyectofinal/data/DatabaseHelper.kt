package com.proyectofinal.data

// Gestión local de la base de datos SQLite.
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "proyecto_final.db"
        const val DATABASE_VERSION = 3

        // Tabla Dispositivos
        const val TABLE_DISPOSITIVOS = "dispositivos"
        const val COL_ID = "id"
        const val COL_NOMBRE = "nombre"
        const val COL_CATEGORIA = "categoria"
        const val COL_MARCA = "marca"
        const val COL_MODELO = "modelo"
        const val COL_FOTO = "foto"

        // Tabla Tareas
        const val TABLE_TAREAS = "tareas"
        const val COL_TAREA_ID = "id"
        const val COL_TAREA_NOMBRE = "nombre"
        const val COL_TAREA_DESCRIPCION = "descripcion"
        const val COL_TAREA_FECHA = "fecha"
        const val COL_TAREA_REPETIR = "repetir_cada"
        const val COL_TAREA_DISPOSITIVO_ID = "dispositivo_id"
        const val COL_TAREA_COMPLETADA = "completada"

        // Tabla Inspecciones
        const val TABLE_INSPECCIONES = "inspecciones"
        const val COL_INSPECCION_ID = "id"
        const val COL_INSPECCION_NOMBRE = "nombre"
        const val COL_INSPECCION_DESCRIPCION = "descripcion"
        const val COL_INSPECCION_FECHA = "fecha"
        const val COL_INSPECCION_REPETIR = "repetir_cada"
        const val COL_INSPECCION_DISPOSITIVO_ID = "dispositivo_id"
        const val COL_INSPECCION_COMPLETADA = "completada"

        // Tabla TareaDetalles
        const val TABLE_TAREA_DETALLES = "tarea_detalles"
        const val COL_DETALLE_ID = "id"
        const val COL_DETALLE_TAREA_ID = "tarea_id"
        const val COL_DETALLE_TIPO = "tipo"
        const val COL_DETALLE_NOMBRE = "nombre"
        const val COL_DETALLE_DESCRIPCION = "descripcion"
        const val COL_DETALLE_CONDICION = "condicion"
        const val COL_DETALLE_NOTAS = "notas"
        const val COL_DETALLE_FOTOS = "fotos"
        const val COL_DETALLE_COMPLETADA = "completada"
        const val COL_DETALLE_FECHA_COMPLETADA = "fecha_completada"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_DISPOSITIVOS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE TEXT NOT NULL,
                $COL_CATEGORIA TEXT NOT NULL,
                $COL_MARCA TEXT NOT NULL,
                $COL_MODELO TEXT NOT NULL,
                $COL_FOTO TEXT DEFAULT ''
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_TAREAS (
                $COL_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TAREA_NOMBRE TEXT NOT NULL,
                $COL_TAREA_DESCRIPCION TEXT NOT NULL,
                $COL_TAREA_FECHA TEXT NOT NULL,
                $COL_TAREA_REPETIR TEXT NOT NULL,
                $COL_TAREA_DISPOSITIVO_ID INTEGER,
                $COL_TAREA_COMPLETADA INTEGER DEFAULT 0,
                FOREIGN KEY ($COL_TAREA_DISPOSITIVO_ID) REFERENCES $TABLE_DISPOSITIVOS($COL_ID)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_INSPECCIONES (
                $COL_INSPECCION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_INSPECCION_NOMBRE TEXT NOT NULL,
                $COL_INSPECCION_DESCRIPCION TEXT NOT NULL,
                $COL_INSPECCION_FECHA TEXT NOT NULL,
                $COL_INSPECCION_REPETIR TEXT NOT NULL,
                $COL_INSPECCION_DISPOSITIVO_ID INTEGER,
                $COL_INSPECCION_COMPLETADA INTEGER DEFAULT 0,
                FOREIGN KEY ($COL_INSPECCION_DISPOSITIVO_ID) REFERENCES $TABLE_DISPOSITIVOS($COL_ID)
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_TAREA_DETALLES (
                $COL_DETALLE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DETALLE_TAREA_ID INTEGER NOT NULL,
                $COL_DETALLE_TIPO TEXT NOT NULL,
                $COL_DETALLE_NOMBRE TEXT NOT NULL,
                $COL_DETALLE_DESCRIPCION TEXT DEFAULT '',
                $COL_DETALLE_CONDICION TEXT DEFAULT '',
                $COL_DETALLE_NOTAS TEXT DEFAULT '',
                $COL_DETALLE_FOTOS TEXT DEFAULT '',
                $COL_DETALLE_COMPLETADA INTEGER DEFAULT 0,
                $COL_DETALLE_FECHA_COMPLETADA TEXT DEFAULT NULL,
                FOREIGN KEY ($COL_DETALLE_TAREA_ID) REFERENCES $TABLE_TAREAS($COL_TAREA_ID)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_TAREAS ADD COLUMN $COL_TAREA_COMPLETADA INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_INSPECCIONES ADD COLUMN $COL_INSPECCION_COMPLETADA INTEGER DEFAULT 0")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_TAREA_DETALLES (
                    $COL_DETALLE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_DETALLE_TAREA_ID INTEGER NOT NULL,
                    $COL_DETALLE_TIPO TEXT NOT NULL,
                    $COL_DETALLE_NOMBRE TEXT NOT NULL,
                    $COL_DETALLE_DESCRIPCION TEXT DEFAULT '',
                    $COL_DETALLE_CONDICION TEXT DEFAULT '',
                    $COL_DETALLE_NOTAS TEXT DEFAULT '',
                    $COL_DETALLE_FOTOS TEXT DEFAULT '',
                    $COL_DETALLE_COMPLETADA INTEGER DEFAULT 0,
                    $COL_DETALLE_FECHA_COMPLETADA TEXT DEFAULT NULL,
                    FOREIGN KEY ($COL_DETALLE_TAREA_ID) REFERENCES $TABLE_TAREAS($COL_TAREA_ID)
                )
            """)
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_DISPOSITIVOS ADD COLUMN $COL_FOTO TEXT DEFAULT ''")
        }
    }
}
