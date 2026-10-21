package com.example.projetodoappdecombustivel

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private lateinit var vehicleId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        vehicleId = intent.getStringExtra(EXTRA_VEHICLE_ID) ?: run {
            finish()
            return
        }

        findViewById<Button>(R.id.botaoAdicionarKm).setOnClickListener {
            abrirPopupAdicionarKm()
        }
        findViewById<ToggleButton>(R.id.alternarVisualizacaoHistorico).setOnCheckedChangeListener { _, mostrarGrafico ->
            atualizarModoVisualizacao(mostrarGrafico)
        }
        atualizarHistorico()
    }

    private fun abrirPopupAdicionarKm() {
        val conteudo = layoutInflater.inflate(R.layout.dialog_add_km, null)
        val campoKm = conteudo.findViewById<EditText>(R.id.campoKmHistorico)
        val campoLitros = conteudo.findViewById<EditText>(R.id.campoLitrosAbastecidos)
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Registrar abastecimento")
            .setView(conteudo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val kmTexto = campoKm.text.toString().trim()
                val km = kmTexto.replace(',', '.').toDoubleOrNull()
                val litrosTexto = campoLitros.text.toString().trim()
                val litros = litrosTexto.replace(',', '.').toDoubleOrNull()
                val veiculo = VehicleStorage.find(this, vehicleId)

                when {
                    km == null || km < 0 -> campoKm.error = "Digite um KM válido"
                    litros == null || litros <= 0 -> campoLitros.error = "Digite uma quantidade de litros válida"
                    veiculo == null -> {
                        dialogo.dismiss()
                        finish()
                    }
                    veiculo.currentKm != null && km < veiculo.currentKm!! -> {
                        campoKm.error = "O KM deve ser igual ou maior que ${formatarKm(veiculo.currentKm!!)}"
                    }
                    VehicleStorage.addRefuel(this, vehicleId, km, litros) -> {
                        atualizarHistorico()
                        dialogo.dismiss()
                    }
                    else -> campoKm.error = "Não foi possível adicionar o registro"
                }
            }
        }
        dialogo.show()
    }

    private fun atualizarHistorico() {
        val veiculo = VehicleStorage.find(this, vehicleId) ?: run {
            finish()
            return
        }

        findViewById<TextView>(R.id.nomeVeiculoHistorico).text = veiculo.name
        findViewById<TextView>(R.id.kmAtualHistorico).text =
            "KM atual: ${veiculo.currentKm?.let { "${formatarKm(it)} km" } ?: "—"}"

        val lista = findViewById<LinearLayout>(R.id.listaHistorico)
        lista.removeAllViews()
        val registros = veiculo.history.sortedByDescending { it.timestamp }
        findViewById<KmChartView>(R.id.graficoKmHistorico)
            .definirRegistros(registros.sortedBy { it.timestamp })

        if (registros.isEmpty()) {
            lista.addView(TextView(this).apply {
                text = "Nenhum registro de KM."
                textSize = 16f
                setPadding(dp(8), dp(12), dp(8), dp(12))
            })
            return
        }

        registros.forEach { registro ->
            val linha = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(8), dp(8), dp(8))
                setBackgroundColor(Color.rgb(245, 247, 250))
            }

            val data = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                .format(Date(registro.timestamp))
            linha.addView(TextView(this).apply {
                val litros = registro.litersFueled?.let { "${formatarKm(it)} L abastecidos\n" }
                    ?: "Abastecimento inicial\n"
                text = "${formatarKm(registro.km)} km\n$litros$data"
                textSize = 16f
                setTextColor(Color.DKGRAY)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            linha.addView(Button(this).apply {
                text = "Remover"
                setOnClickListener {
                    VehicleStorage.removeKm(this@HistoryActivity, vehicleId, registro.id)
                    atualizarHistorico()
                }
            })

            val parametrosLinha = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            lista.addView(linha, parametrosLinha)
        }
    }

    private fun atualizarModoVisualizacao(mostrarGrafico: Boolean) {
        findViewById<ScrollView>(R.id.scrollListaHistorico).visibility =
            if (mostrarGrafico) View.GONE else View.VISIBLE
        findViewById<KmChartView>(R.id.graficoKmHistorico).visibility =
            if (mostrarGrafico) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tituloConteudoHistorico).text =
            if (mostrarGrafico) "Autonomia (km/L)" else "Registros"
    }

    private fun formatarKm(km: Double): String =
        DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("pt", "BR"))).format(km)

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_VEHICLE_ID = "extra_vehicle_id"
    }
}
