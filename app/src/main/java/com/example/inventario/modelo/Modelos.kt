package com.example.inventario.modelo

data class Producto(
    val id: Long = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val codigoBarras: String? = null,
    val stockMinimo: Int = STOCK_MINIMO_DEFECTO,
) {
    val valor: Double get() = precio * stock
    val stockBajo: Boolean get() = stock <= stockMinimo

    companion object {
        const val STOCK_MINIMO_DEFECTO = 5
    }
}

enum class TipoMovimiento(val clave: String) {
    ENTRADA("IN"),
    SALIDA("OUT");

    companion object {
        fun desdeClave(clave: String) = entries.first { it.clave == clave }
    }
}

data class Movimiento(
    val id: Long,
    val productoId: Long,
    val nombreProducto: String,
    val cantidad: Int,
    val tipo: TipoMovimiento,
    val fecha: Long,
)

/** Totales que se muestran en la pantalla de resumen. */
data class Totales(
    val valorTotal: Double,
    val productos: Int,
    val unidades: Int,
    val stockBajo: Int,
)
