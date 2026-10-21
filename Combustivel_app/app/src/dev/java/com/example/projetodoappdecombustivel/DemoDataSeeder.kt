package com.example.projetodoappdecombustivel

import android.content.Context
import java.util.Calendar
import java.util.Random
import java.util.UUID

/** Adiciona amostras previsíveis para visualizar os gráficos na variante de desenvolvimento. */
object DemoDataSeeder {
    private const val PREFERENCES_NAME = "demo_data"
    private const val SEEDED_KEY = "vehicles_seeded_v1"
    private const val FUEL_DATA_KEY = "fuel_data_added_v2"
    private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L

    fun seedIfNeeded(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(SEEDED_KEY, false)) {
            val now = System.currentTimeMillis()
            val vehicles = VehicleStorage.load(context)
            vehicles += criarVeiculo(
                name = "Veículo A",
                initialKm = 15_000.0,
                startTime = now - 30L * DAY_IN_MILLIS,
                count = 30,
                interval = Calendar.DAY_OF_YEAR,
                minIncrement = 10,
                maxIncrement = 250,
                randomSeed = 1L
            )
            vehicles += criarVeiculo(
                name = "Veículo B",
                initialKm = 116_000.0,
                startTime = now - 16L * 7L * DAY_IN_MILLIS,
                count = 16,
                interval = Calendar.WEEK_OF_YEAR,
                minIncrement = 100,
                maxIncrement = 1_000,
                randomSeed = 2L
            )
            vehicles += criarVeiculo(
                name = "Veículo C",
                initialKm = 88_000.0,
                startTime = agoraMenosMeses(12),
                count = 12,
                interval = Calendar.MONTH,
                minIncrement = 900,
                maxIncrement = 2_000,
                randomSeed = 3L
            )

            VehicleStorage.save(context, vehicles)
            preferences.edit().putBoolean(SEEDED_KEY, true).apply()
        }

        if (!preferences.getBoolean(FUEL_DATA_KEY, false)) {
            adicionarLitrosAosDadosExistentes(context)
            preferences.edit().putBoolean(FUEL_DATA_KEY, true).apply()
        }
    }

    private fun criarVeiculo(
        name: String,
        initialKm: Double,
        startTime: Long,
        count: Int,
        interval: Int,
        minIncrement: Int,
        maxIncrement: Int,
        randomSeed: Long
    ): VehicleRecord {
        val random = Random(randomSeed)
        val history = mutableListOf<KmRecord>()
        var kmAtual = initialKm
        val calendar = Calendar.getInstance().apply { timeInMillis = startTime }

        history += novoRegistro(kmAtual, calendar.timeInMillis)
        repeat(count) {
            calendar.add(interval, 1)
            val distancia = random.nextInt(maxIncrement - minIncrement + 1) + minIncrement
            kmAtual += distancia
            val autonomiaSimulada = 8.0 + random.nextDouble() * 7.0
            history += novoRegistro(kmAtual, calendar.timeInMillis, distancia / autonomiaSimulada)
        }

        return VehicleRecord(UUID.randomUUID().toString(), name, history)
    }

    private fun novoRegistro(km: Double, timestamp: Long, litersFueled: Double? = null) = KmRecord(
        id = UUID.randomUUID().toString(),
        km = km,
        timestamp = timestamp,
        litersFueled = litersFueled
    )

    private fun adicionarLitrosAosDadosExistentes(context: Context) {
        val veiculosDemo = setOf("Veículo A", "Veículo B", "Veículo C")
        val vehicles = VehicleStorage.load(context)
        var alterado = false

        vehicles.filter { it.name in veiculosDemo }.forEach { vehicle ->
            val ordenados = vehicle.history.sortedBy { it.timestamp }
            val random = Random(vehicle.name.hashCode().toLong())
            for (index in 1 until ordenados.size) {
                val atual = ordenados[index]
                if (atual.litersFueled == null) {
                    val distancia = (atual.km - ordenados[index - 1].km).coerceAtLeast(1.0)
                    val autonomiaSimulada = 8.0 + random.nextDouble() * 7.0
                    val historicoIndex = vehicle.history.indexOfFirst { it.id == atual.id }
                    if (historicoIndex >= 0) {
                        vehicle.history[historicoIndex] = atual.copy(
                            litersFueled = distancia / autonomiaSimulada
                        )
                        alterado = true
                    }
                } else {
                    random.nextDouble()
                }
            }
        }

        if (alterado) VehicleStorage.save(context, vehicles)
    }

    private fun agoraMenosMeses(meses: Int): Long = Calendar.getInstance().apply {
        add(Calendar.MONTH, -meses)
    }.timeInMillis
}
