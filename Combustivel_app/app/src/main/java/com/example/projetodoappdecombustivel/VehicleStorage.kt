package com.example.projetodoappdecombustivel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class KmRecord(
    val id: String,
    val km: Double,
    val timestamp: Long,
    val litersFueled: Double? = null
)

data class VehicleRecord(
    val id: String,
    val name: String,
    val history: MutableList<KmRecord>
) {
    val currentKm: Double?
        get() = history.maxByOrNull { it.timestamp }?.km
}

object VehicleStorage {
    private const val PREFERENCES_NAME = "vehicle_records"
    private const val VEHICLES_KEY = "vehicles"

    fun load(context: Context): MutableList<VehicleRecord> {
        val saved = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(VEHICLES_KEY, "[]") ?: "[]"
        val vehicles = mutableListOf<VehicleRecord>()
        val vehicleArray = runCatching { JSONArray(saved) }.getOrNull() ?: return vehicles

        for (vehicleIndex in 0 until vehicleArray.length()) {
            val vehicleJson = vehicleArray.optJSONObject(vehicleIndex) ?: continue
            val history = mutableListOf<KmRecord>()
            val historyArray = vehicleJson.optJSONArray("history") ?: JSONArray()

            for (recordIndex in 0 until historyArray.length()) {
                val recordJson = historyArray.optJSONObject(recordIndex) ?: continue
                history += KmRecord(
                    id = recordJson.optString("id", UUID.randomUUID().toString()),
                    km = recordJson.optDouble("km", Double.NaN),
                    timestamp = recordJson.optLong("timestamp", 0L),
                    litersFueled = if (recordJson.has("litersFueled") && !recordJson.isNull("litersFueled")) {
                        recordJson.optDouble("litersFueled").takeIf { it > 0.0 }
                    } else {
                        null
                    }
                )
            }

            vehicles += VehicleRecord(
                id = vehicleJson.optString("id", UUID.randomUUID().toString()),
                name = vehicleJson.optString("name", "Veículo"),
                history = history
            )
        }
        return vehicles
    }

    fun save(context: Context, vehicles: List<VehicleRecord>) {
        val vehicleArray = JSONArray()
        vehicles.forEach { vehicle ->
            val historyArray = JSONArray()
            vehicle.history.forEach { record ->
                historyArray.put(
                    JSONObject()
                        .put("id", record.id)
                        .put("km", record.km)
                        .put("timestamp", record.timestamp)
                        .put("litersFueled", record.litersFueled)
                )
            }
            vehicleArray.put(
                JSONObject()
                    .put("id", vehicle.id)
                    .put("name", vehicle.name)
                    .put("history", historyArray)
            )
        }

        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(VEHICLES_KEY, vehicleArray.toString())
            .apply()
    }

    fun addVehicle(context: Context, name: String, initialKm: Double): VehicleRecord {
        val vehicles = load(context)
        val vehicle = VehicleRecord(
            id = UUID.randomUUID().toString(),
            name = name,
            history = mutableListOf(
                KmRecord(
                    id = UUID.randomUUID().toString(),
                    km = initialKm,
                    timestamp = System.currentTimeMillis()
                )
            )
        )
        vehicles += vehicle
        save(context, vehicles)
        return vehicle
    }

    fun addRefuel(context: Context, vehicleId: String, km: Double, litersFueled: Double): Boolean {
        val vehicles = load(context)
        val vehicle = vehicles.firstOrNull { it.id == vehicleId } ?: return false
        val nextTimestamp = System.currentTimeMillis().coerceAtLeast(
            (vehicle.history.maxOfOrNull { it.timestamp } ?: 0L) + 1L
        )
        vehicle.history += KmRecord(
            id = UUID.randomUUID().toString(),
            km = km,
            timestamp = nextTimestamp,
            litersFueled = litersFueled
        )
        save(context, vehicles)
        return true
    }

    fun removeKm(context: Context, vehicleId: String, recordId: String): Boolean {
        val vehicles = load(context)
        val vehicle = vehicles.firstOrNull { it.id == vehicleId } ?: return false
        val removed = vehicle.history.removeAll { it.id == recordId }
        if (removed) save(context, vehicles)
        return removed
    }

    fun find(context: Context, vehicleId: String): VehicleRecord? =
        load(context).firstOrNull { it.id == vehicleId }
}
