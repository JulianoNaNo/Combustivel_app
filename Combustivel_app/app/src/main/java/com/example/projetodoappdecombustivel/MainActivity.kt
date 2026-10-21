package com.example.projetodoappdecombustivel

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        DemoDataSeeder.seedIfNeeded(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.floatingActionButton2
        ).setOnClickListener {
            abrirPopupCadastro()
        }
    }

    override fun onResume() {
        super.onResume()
        if (findViewById<GridLayout>(R.id.gridVeiculos) != null) {
            atualizarGrade()
        }
    }

    private fun abrirPopupCadastro() {
        val conteudo = layoutInflater.inflate(R.layout.plus_button_popup_layout, null)
        val campoVeiculo = conteudo.findViewById<EditText>(R.id.campoVeiculo)
        val campoKmInicial = conteudo.findViewById<EditText>(R.id.campoKmInicial)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Novo veículo")
            .setView(conteudo)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Adicionar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nome = campoVeiculo.text.toString().trim()
                val kmTexto = campoKmInicial.text.toString().trim()
                val kmInicial = kmTexto.replace(',', '.').toDoubleOrNull()

                if (nome.isBlank()) {
                    campoVeiculo.error = "Digite o nome do veículo"
                } else if (kmInicial == null || kmInicial < 0) {
                    campoKmInicial.error = "Digite um KM inicial válido"
                } else {
                    VehicleStorage.addVehicle(this, nome, kmInicial)
                    atualizarGrade()
                    dialogo.dismiss()
                }
            }
        }
        dialogo.show()
    }

    private fun atualizarGrade() {
        val grid = findViewById<GridLayout>(R.id.gridVeiculos)
        for (index in grid.childCount - 1 downTo 2) {
            grid.removeViewAt(index)
        }

        VehicleStorage.load(this).forEachIndexed { index, veiculo ->
            val linha = index + 1
            val celulaVeiculo = adicionarCelula(grid, veiculo.name, linha, 0)
            val kmAtual = veiculo.currentKm?.let { "${formatarKm(it)} km" } ?: "—"
            adicionarCelula(grid, kmAtual, linha, 1)

            celulaVeiculo.setOnClickListener {
                startActivity(
                    Intent(this, HistoryActivity::class.java)
                        .putExtra(HistoryActivity.EXTRA_VEHICLE_ID, veiculo.id)
                )
            }
        }
    }

    private fun adicionarCelula(
        grid: GridLayout,
        texto: String,
        linha: Int,
        coluna: Int
    ): TextView {
        val margem = dp(4)
        val celula = TextView(this).apply {
            text = texto
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.DKGRAY)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(245, 247, 250))
        }

        val parametros = GridLayout.LayoutParams().apply {
            rowSpec = GridLayout.spec(linha)
            columnSpec = GridLayout.spec(coluna, 1f)
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            setMargins(margem, margem, margem, margem)
        }
        grid.addView(celula, parametros)
        return celula
    }

    private fun formatarKm(km: Double): String =
        DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("pt", "BR"))).format(km)

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()
}
