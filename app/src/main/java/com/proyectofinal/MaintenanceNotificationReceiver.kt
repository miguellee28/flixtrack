package com.proyectofinal

import com.proyectofinal.data.DispositivoRepository
import com.proyectofinal.model.ItemProgramado
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

class MaintenanceNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MaintenanceNotificationScheduler.ACTION_NOTIFY) return

        val id = intent.getLongExtra(MaintenanceNotificationScheduler.EXTRA_ID, -1L)
        val type = intent.getStringExtra(MaintenanceNotificationScheduler.EXTRA_TYPE).orEmpty()
        if (id <= 0L || type.isBlank()) return

        val item = findPendingItem(context, id, type) ?: return
        val isOverdueTask = type == MaintenanceNotificationScheduler.TYPE_TASK && isOverdue(item.fecha)
        if (!canPostNotifications(context)) {
            if (isOverdueTask) {
                MaintenanceNotificationScheduler.scheduleAll(context)
            }
            return
        }

        createChannel(context)
        val isTask = type == MaintenanceNotificationScheduler.TYPE_TASK
        val title = when {
            isOverdueTask -> "Mantenimiento atrasado"
            isTask -> "Mantenimiento hoy"
            else -> "Inspeccion hoy"
        }
        val deviceName = item.nombreDispositivo.ifBlank {
            intent.getStringExtra(MaintenanceNotificationScheduler.EXTRA_DEVICE_NAME).orEmpty()
        }
        val text = if (deviceName.isBlank()) {
            item.nombre
        } else {
            "$deviceName: ${item.nombre}"
        }

        val openIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calendario)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(type, id), notification)
        if (isOverdueTask) {
            MaintenanceNotificationScheduler.scheduleAll(context)
        }
    }

    private fun findPendingItem(context: Context, id: Long, type: String): ItemProgramado? {
        val repository = DispositivoRepository(context.applicationContext)
        val dispositivos = repository.obtenerTodos().associateBy { it.id }

        return when (type) {
            MaintenanceNotificationScheduler.TYPE_TASK -> {
                repository.obtenerTareas()
                    .firstOrNull { it.id == id && !it.completada }
                    ?.let { tarea ->
                        ItemProgramado(
                            id = tarea.id,
                            nombre = tarea.nombre,
                            descripcion = tarea.descripcion,
                            fecha = tarea.fecha,
                            nombreDispositivo = dispositivos[tarea.dispositivoId]?.nombre.orEmpty(),
                            tipo = type
                        )
                    }
            }
            MaintenanceNotificationScheduler.TYPE_INSPECTION -> {
                repository.obtenerInspecciones()
                    .firstOrNull { it.id == id && !it.completada }
                    ?.let { inspeccion ->
                        ItemProgramado(
                            id = inspeccion.id,
                            nombre = inspeccion.nombre,
                            descripcion = inspeccion.descripcion,
                            fecha = inspeccion.fecha,
                            nombreDispositivo = dispositivos[inspeccion.dispositivoId]?.nombre.orEmpty(),
                            tipo = type
                        )
                    }
            }
            else -> null
        }
    }

    private fun isOverdue(date: String): Boolean {
        val itemDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        return !itemDate.isAfter(LocalDate.now())
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mantenimiento",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Recordatorios de mantenimiento e inspeccion"
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationId(type: String, id: Long): Int {
        return "notification:$type:$id".hashCode() and Int.MAX_VALUE
    }

    companion object {
        private const val CHANNEL_ID = "maintenance_reminders"
    }
}
