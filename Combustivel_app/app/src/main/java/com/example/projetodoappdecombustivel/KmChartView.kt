package com.example.projetodoappdecombustivel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KmChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var registros: List<KmRecord> = emptyList()

    private val linhaGrafico = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(21, 101, 192)
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val pontoGrafico = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(21, 101, 192)
        style = Paint.Style.FILL
    }
    private val grade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 224, 230)
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = sp(12f)
    }
    private val textoPequeno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = sp(11f)
    }
    private val formatoKm = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("pt", "BR")))
    private val formatoData = SimpleDateFormat("dd/MM/yy", Locale("pt", "BR"))

    fun definirRegistros(novosRegistros: List<KmRecord>) {
        registros = novosRegistros.sortedBy { it.timestamp }
        contentDescription = if (registros.isEmpty()) {
            "Gráfico sem registros de quilometragem"
        } else {
            "Gráfico da quilometragem ao longo do tempo, com ${registros.size} registros"
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (registros.isEmpty()) {
            texto.textAlign = Paint.Align.CENTER
            canvas.drawText("Nenhum registro de KM.", width / 2f, height / 2f, texto)
            texto.textAlign = Paint.Align.LEFT
            return
        }

        val margemEsquerda = dp(68f)
        val margemDireita = dp(20f)
        val margemSuperior = dp(28f)
        val margemInferior = dp(54f)
        val esquerda = margemEsquerda
        val direita = (width - margemDireita).coerceAtLeast(esquerda + dp(40f))
        val topo = margemSuperior
        val base = (height - margemInferior).coerceAtLeast(topo + dp(40f))

        val menorOriginal = registros.minOf { it.km }
        val maiorOriginal = registros.maxOf { it.km }
        val folga = if (maiorOriginal == menorOriginal) {
            maxOf(1.0, kotlin.math.abs(maiorOriginal) * 0.05)
        } else {
            (maiorOriginal - menorOriginal) * 0.08
        }
        val menorKm = (menorOriginal - folga).coerceAtLeast(0.0)
        val maiorKm = maiorOriginal + folga
        val faixaKm = (maiorKm - menorKm).takeIf { it > 0.0 } ?: 1.0

        textoPequeno.textAlign = Paint.Align.RIGHT
        for (divisao in 0..4) {
            val proporcao = divisao / 4f
            val y = topo + (base - topo) * proporcao
            val valor = maiorKm - faixaKm * proporcao
            canvas.drawLine(esquerda, y, direita, y, grade)
            canvas.drawText(formatoKm.format(valor), esquerda - dp(8f), y + dp(4f), textoPequeno)
        }
        textoPequeno.textAlign = Paint.Align.LEFT
        canvas.drawText("KM", esquerda, topo - dp(10f), texto)
        canvas.drawLine(esquerda, topo, esquerda, base, grade)

        val primeiroTempo = registros.first().timestamp
        val ultimoTempo = registros.last().timestamp
        val faixaTempo = ultimoTempo - primeiroTempo
        val caminho = Path()
        registros.forEachIndexed { indice, registro ->
            val proporcaoX = when {
                registros.size == 1 -> 0.5f
                faixaTempo > 0L -> (registro.timestamp - primeiroTempo).toFloat() / faixaTempo.toFloat()
                else -> indice.toFloat() / (registros.lastIndex).coerceAtLeast(1)
            }
            val x = esquerda + (direita - esquerda) * proporcaoX
            val proporcaoY = ((maiorKm - registro.km) / faixaKm).toFloat()
            val y = topo + (base - topo) * proporcaoY
            if (indice == 0) caminho.moveTo(x, y) else caminho.lineTo(x, y)
        }
        canvas.drawPath(caminho, linhaGrafico)

        registros.forEachIndexed { indice, registro ->
            val proporcaoX = when {
                registros.size == 1 -> 0.5f
                faixaTempo > 0L -> (registro.timestamp - primeiroTempo).toFloat() / faixaTempo.toFloat()
                else -> indice.toFloat() / (registros.lastIndex).coerceAtLeast(1)
            }
            val x = esquerda + (direita - esquerda) * proporcaoX
            val proporcaoY = ((maiorKm - registro.km) / faixaKm).toFloat()
            val y = topo + (base - topo) * proporcaoY
            canvas.drawCircle(x, y, dp(4f), pontoGrafico)
        }

        textoPequeno.textAlign = Paint.Align.LEFT
        canvas.drawText(formatoData.format(Date(primeiroTempo)), esquerda, base + dp(24f), textoPequeno)
        textoPequeno.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatoData.format(Date(ultimoTempo)), direita, base + dp(24f), textoPequeno)
        textoPequeno.textAlign = Paint.Align.LEFT
    }

    private fun dp(valor: Float): Float = valor * resources.displayMetrics.density

    private fun sp(valor: Float): Float = valor * resources.displayMetrics.scaledDensity
}
