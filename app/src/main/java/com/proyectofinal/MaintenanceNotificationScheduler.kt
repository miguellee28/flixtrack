package com.proyectofinal

import com.proyectofinal.data.DispositivoRepository
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object MaintenanceNotificationScheduler {
    const val ACTION_NOTIFY = "com.proyectofinal.action.MAINTENANCE_NOTIFY"
    const val EXTRA_ID = "extra_id"
    const val EXTRA_TYPE = "extra_type"
    const val EXTRA_DEVICE_NAME = "extra_device_name"

    const val TYPE_TASK = "tarea"
    const val TYPE_INSPECTION = "inspeccion"

    fun scheduleAll(context: Context) {
        val appContext = context.applicationContext
        val repository = DispositivoRepository(appContext)
        val dispositivos = repository.obtenerTodos().associateBy { it.id }

        repository.obtenerTareas()
            .filterNot { it.completada }
            .forEach { tarea ->
                scheduleItem(
                    context = appContext,
                    id = tarea.id,
                    type = TYPE_TASK,
                    deviceName = dispositivos[tarea.dispositivoId]?.nombre.orEmpty(),
                    date = tarea.fecha
                )
            }

        repository.obtenerInspecciones()
            .filterNot { it.completada }
            .forEach { inspeccion ->
                scheduleItem(
                    context = appContext,
                    id = inspeccion.id,
                    type = TYPE_INSPECTION,
                    deviceName = dispositivos[inspeccion.dispositivoId]?.nombre.orEmpty(),
                    date = inspeccion.fecha
                )
            }
    }

    private fun scheduleItem(
        context: Context,
        id: Long,
        type: String,
        deviceName: String,
        date: String
    ) {
        val triggerAtMillis = triggerAtEight(date, type) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(type, id),
            Intent(context, MaintenanceNotificationReceiver::class.java).apply {
                action = ACTION_NOTIFY
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_DEVICE_NAME, deviceName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun triggerAtEight(date: String, type: String): Long? {
        val localDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val triggerAtMillis = localDate
            .atTime(LocalTime.of(8, 0))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (triggerAtMillis <= System.currentTimeMillis() && type == TYPE_TASK) {
            return nextEightInTheMorning()
        }
        return triggerAtMillis.takeIf { it > System.currentTimeMillis() }
    }

    private fun nextEightInTheMorning(): Long {
        val now = java.time.LocalDateTime.now()
        val nextEight = now.toLocalDate().atTime(LocalTime.of(8, 0)).let { todayAtEight ->
            if (todayAtEight.isAfter(now)) todayAtEight else todayAtEight.plusDays(1)
        }
        return nextEight
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun requestCode(type: String, id: Long): Int {
        return "$type:$id".hashCode() and Int.MAX_VALUE
    }
}
