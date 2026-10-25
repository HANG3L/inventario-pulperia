package com.example.inventario.util

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formato {
    /** Simbolo de moneda: cambiarlo aqui lo actualiza en toda la app. */
    const val MONEDA = "L "

    private val formatoDinero = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val formatoEntero = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val formatoFecha = SimpleDateFormat("d MMM · h:mm a", Locale.forLanguageTag("es-CR"))

    fun dinero(valor: Double): String = MONEDA + formatoDinero.format(valor)

    fun entero(valor: Int): String = formatoEntero.format(valor)

    fun fecha(milisegundos: Long): String = formatoFecha.format(Date(milisegundos))
}

fun Context.color(@ColorRes recurso: Int): Int = ContextCompat.getColor(this, recurso)

fun View.colorTema(@AttrRes atributo: Int): Int = MaterialColors.getColor(this, atributo)

fun Context.dp(valor: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valor.toFloat(), resources.displayMetrics).toInt()
