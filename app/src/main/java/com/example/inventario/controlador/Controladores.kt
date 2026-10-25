package com.example.inventario.controlador

import android.content.Context
import com.example.inventario.datos.BaseDatos
import com.example.inventario.datos.StockInsuficienteException
import com.example.inventario.modelo.Movimiento
import com.example.inventario.modelo.Producto
import com.example.inventario.modelo.TipoMovimiento
import com.example.inventario.modelo.Totales

/** Campos con error al guardar un producto; la vista decide el texto a mostrar. */
data class ErroresProducto(
    val nombre: Boolean = false,
    val precio: Boolean = false,
    val stock: Boolean = false,
    val stockMinimo: Boolean = false,
    /** Nombre del producto que ya usa ese codigo de barras. */
    val codigoRepetidoDe: String? = null,
) {
    val hayErrores get() = nombre || precio || stock || stockMinimo || codigoRepetidoDe != null
}

class ProductoControlador(contexto: Context) {
    private val bd = BaseDatos.obtener(contexto)

    fun listar(busqueda: String = "") = bd.listarProductos(busqueda)

    fun obtener(id: Long) = bd.obtenerProducto(id)

    fun hayProductos() = bd.listarProductos().isNotEmpty()

    /** Valida los datos escritos y crea (id = 0) o actualiza el producto. Devuelve los errores, si hay. */
    fun guardar(
        id: Long,
        nombre: String,
        precioTexto: String,
        stockTexto: String,
        stockMinimoTexto: String,
        codigoTexto: String,
    ): ErroresProducto {
        val nombreLimpio = nombre.trim()
        val precio = precioTexto.replace(',', '.').toDoubleOrNull()
        val stock = stockTexto.toIntOrNull()
        val stockMinimo = stockMinimoTexto.toIntOrNull()
        val codigo = codigoTexto.trim().ifEmpty { null }
        val dueno = codigo?.let { bd.buscarPorCodigo(it) }?.takeIf { it.id != id }

        val errores = ErroresProducto(
            nombre = nombreLimpio.isEmpty(),
            precio = precio == null || precio < 0,
            stock = stock == null || stock < 0,
            stockMinimo = stockMinimo == null || stockMinimo < 0,
            codigoRepetidoDe = dueno?.nombre,
        )
        if (errores.hayErrores || precio == null || stock == null || stockMinimo == null) return errores

        val producto = Producto(id, nombreLimpio, precio, stock, codigo, stockMinimo)
        if (id == 0L) bd.insertarProducto(producto) else bd.actualizarProducto(producto)
        return errores
    }

    fun eliminar(id: Long) = bd.eliminarProducto(id)
}

sealed interface ResultadoMovimiento {
    data class Exito(val producto: Producto, val tipo: TipoMovimiento, val cantidad: Int) : ResultadoMovimiento {
        /** Una salida que deja el stock en el minimo o menos debe avisar al usuario. */
        val requiereAlerta get() = tipo == TipoMovimiento.SALIDA && producto.stockBajo
    }

    data class Invalido(val faltaProducto: Boolean, val cantidadMala: Boolean) : ResultadoMovimiento
    data class SinStock(val disponible: Int) : ResultadoMovimiento
}

class MovimientoControlador(contexto: Context) {
    private val bd = BaseDatos.obtener(contexto)

    fun listar(tipo: TipoMovimiento? = null): List<Movimiento> = bd.listarMovimientos(tipo)

    fun productosDisponibles() = bd.listarProductos()

    fun registrar(productoId: Long?, cantidadTexto: String, tipo: TipoMovimiento): ResultadoMovimiento {
        val cantidad = cantidadTexto.toIntOrNull()?.takeIf { it > 0 }
        if (productoId == null || cantidad == null) return ResultadoMovimiento.Invalido(productoId == null, cantidad == null)
        return try {
            ResultadoMovimiento.Exito(bd.registrarMovimiento(productoId, cantidad, tipo), tipo, cantidad)
        } catch (e: StockInsuficienteException) {
            ResultadoMovimiento.SinStock(e.disponible)
        }
    }
}

class ResumenControlador(contexto: Context) {
    private val bd = BaseDatos.obtener(contexto)

    fun totales(): Totales = bd.obtenerTotales()
    fun top(cantidad: Int = 5) = bd.topPorValor(cantidad)
    fun stockBajo() = bd.listarStockBajo()
}
