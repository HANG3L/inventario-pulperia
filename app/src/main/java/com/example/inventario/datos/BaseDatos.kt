package com.example.inventario.datos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.inventario.modelo.Movimiento
import com.example.inventario.modelo.Producto
import com.example.inventario.modelo.Totales
import com.example.inventario.modelo.TipoMovimiento

class StockInsuficienteException(val disponible: Int) : Exception()

class BaseDatos private constructor(contexto: Context) :
    SQLiteOpenHelper(contexto.applicationContext, NOMBRE, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL CHECK (price >= 0),
                stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
                barcode TEXT UNIQUE,
                min_stock INTEGER NOT NULL DEFAULT ${Producto.STOCK_MINIMO_DEFECTO}
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                qty INTEGER NOT NULL CHECK (qty > 0),
                type TEXT NOT NULL CHECK (type IN ('IN', 'OUT')),
                date INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_movements_date ON movements(date DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) = Unit

    // ---------- Productos ----------

    fun listarProductos(busqueda: String = ""): List<Producto> {
        val texto = busqueda.trim()
        val cursor = if (texto.isEmpty()) {
            readableDatabase.rawQuery("SELECT $COLUMNAS_PRODUCTO FROM products ORDER BY name COLLATE NOCASE", null)
        } else {
            val patron = "%$texto%"
            readableDatabase.rawQuery(
                "SELECT $COLUMNAS_PRODUCTO FROM products WHERE name LIKE ? OR barcode LIKE ? ORDER BY name COLLATE NOCASE",
                arrayOf(patron, patron),
            )
        }
        return cursor.use { c -> generateSequence { if (c.moveToNext()) leerProducto(c) else null }.toList() }
    }

    fun obtenerProducto(id: Long): Producto? =
        readableDatabase.rawQuery("SELECT $COLUMNAS_PRODUCTO FROM products WHERE id = ?", arrayOf(id.toString()))
            .use { if (it.moveToFirst()) leerProducto(it) else null }

    fun buscarPorCodigo(codigo: String): Producto? =
        readableDatabase.rawQuery("SELECT $COLUMNAS_PRODUCTO FROM products WHERE barcode = ?", arrayOf(codigo.trim()))
            .use { if (it.moveToFirst()) leerProducto(it) else null }

    fun insertarProducto(producto: Producto): Long =
        writableDatabase.insertOrThrow("products", null, valoresDe(producto))

    fun actualizarProducto(producto: Producto) {
        writableDatabase.update("products", valoresDe(producto), "id = ?", arrayOf(producto.id.toString()))
    }

    fun eliminarProducto(id: Long) {
        writableDatabase.delete("products", "id = ?", arrayOf(id.toString()))
    }

    // ---------- Movimientos ----------

    /**
     * Ajusta el stock y guarda el movimiento en una sola transaccion.
     * Devuelve el producto con su stock ya actualizado.
     */
    fun registrarMovimiento(productoId: Long, cantidad: Int, tipo: TipoMovimiento): Producto {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val actual = obtenerProducto(productoId) ?: error("Producto inexistente")
            val nuevoStock = when (tipo) {
                TipoMovimiento.ENTRADA -> actual.stock + cantidad
                TipoMovimiento.SALIDA -> actual.stock - cantidad
            }
            if (nuevoStock < 0) throw StockInsuficienteException(actual.stock)

            db.execSQL("UPDATE products SET stock = ? WHERE id = ?", arrayOf<Any>(nuevoStock, productoId))
            db.insertOrThrow(
                "movements", null,
                ContentValues().apply {
                    put("product_id", productoId)
                    put("qty", cantidad)
                    put("type", tipo.clave)
                    put("date", System.currentTimeMillis())
                },
            )
            db.setTransactionSuccessful()
            return actual.copy(stock = nuevoStock)
        } finally {
            db.endTransaction()
        }
    }

    fun listarMovimientos(tipo: TipoMovimiento? = null, limite: Int = 200): List<Movimiento> {
        val filtro = if (tipo != null) "WHERE m.type = '${tipo.clave}'" else ""
        val cursor = readableDatabase.rawQuery(
            """
            SELECT m.id, m.product_id, p.name, m.qty, m.type, m.date
            FROM movements m JOIN products p ON p.id = m.product_id
            $filtro
            ORDER BY m.date DESC, m.id DESC
            LIMIT $limite
            """.trimIndent(),
            null,
        )
        return cursor.use { c ->
            generateSequence {
                if (c.moveToNext()) {
                    Movimiento(
                        id = c.getLong(0),
                        productoId = c.getLong(1),
                        nombreProducto = c.getString(2),
                        cantidad = c.getInt(3),
                        tipo = TipoMovimiento.desdeClave(c.getString(4)),
                        fecha = c.getLong(5),
                    )
                } else null
            }.toList()
        }
    }

    // ---------- Resumen ----------

    fun obtenerTotales(): Totales =
        readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM(price * stock), 0), COUNT(*), COALESCE(SUM(stock), 0),
                   COALESCE(SUM(CASE WHEN stock <= min_stock THEN 1 ELSE 0 END), 0)
            FROM products
            """.trimIndent(),
            null,
        ).use {
            it.moveToFirst()
            Totales(it.getDouble(0), it.getInt(1), it.getInt(2), it.getInt(3))
        }

    fun topPorValor(cantidad: Int = 5): List<Producto> =
        readableDatabase.rawQuery(
            "SELECT $COLUMNAS_PRODUCTO FROM products WHERE price * stock > 0 ORDER BY price * stock DESC LIMIT $cantidad",
            null,
        ).use { c -> generateSequence { if (c.moveToNext()) leerProducto(c) else null }.toList() }

    fun listarStockBajo(): List<Producto> =
        readableDatabase.rawQuery(
            "SELECT $COLUMNAS_PRODUCTO FROM products WHERE stock <= min_stock ORDER BY stock, name COLLATE NOCASE",
            null,
        ).use { c -> generateSequence { if (c.moveToNext()) leerProducto(c) else null }.toList() }

    // ---------- Utilidades ----------

    private fun valoresDe(p: Producto) = ContentValues().apply {
        put("name", p.nombre)
        put("price", p.precio)
        put("stock", p.stock)
        put("barcode", p.codigoBarras)
        put("min_stock", p.stockMinimo)
    }

    private fun leerProducto(c: Cursor) = Producto(
        id = c.getLong(0),
        nombre = c.getString(1),
        precio = c.getDouble(2),
        stock = c.getInt(3),
        codigoBarras = c.getString(4),
        stockMinimo = c.getInt(5),
    )

    companion object {
        private const val NOMBRE = "inventario.db"
        private const val VERSION = 1
        private const val COLUMNAS_PRODUCTO = "id, name, price, stock, barcode, min_stock"

        @Volatile
        private var instancia: BaseDatos? = null

        fun obtener(contexto: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: BaseDatos(contexto).also { instancia = it }
            }
    }
}
