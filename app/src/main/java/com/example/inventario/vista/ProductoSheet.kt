package com.example.inventario.vista

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.example.inventario.R
import com.example.inventario.databinding.SheetProductoBinding
import com.example.inventario.controlador.ProductoControlador
import com.example.inventario.modelo.Producto
import com.example.inventario.util.Formato
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Formulario para crear (productoId = 0) o editar un producto. */
class ProductoSheet : BottomSheetDialogFragment() {

    private var _enlace: SheetProductoBinding? = null
    private val enlace get() = _enlace!!

    private val productoId get() = requireArguments().getLong(ARG_ID)
    private val controlador by lazy { ProductoControlador(requireContext()) }
    private var producto: Producto? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).also {
            it.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

    override fun onCreateView(inflador: LayoutInflater, contenedor: ViewGroup?, savedInstanceState: Bundle?): View {
        _enlace = SheetProductoBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, savedInstanceState: Bundle?) {
        producto = if (productoId != 0L) controlador.obtener(productoId) else null
        val actual = producto

        enlace.envoltorioPrecio.prefixText = Formato.MONEDA
        enlace.textoTitulo.setText(if (actual == null) R.string.form_titulo_nuevo else R.string.form_titulo_editar)
        if (actual != null && savedInstanceState == null) {
            enlace.campoNombre.setText(actual.nombre)
            enlace.campoPrecio.setText(formatearNumero(actual.precio))
            enlace.campoStock.setText(actual.stock.toString())
            enlace.campoStockMinimo.setText(actual.stockMinimo.toString())
            enlace.campoCodigo.setText(actual.codigoBarras.orEmpty())
        } else if (actual == null && savedInstanceState == null) {
            enlace.campoStock.setText("0")
            enlace.campoStockMinimo.setText(Producto.STOCK_MINIMO_DEFECTO.toString())
        }
        enlace.botonEliminar.visibility = if (actual == null) View.GONE else View.VISIBLE

        listOf(
            enlace.campoNombre to enlace.envoltorioNombre,
            enlace.campoPrecio to enlace.envoltorioPrecio,
            enlace.campoStock to enlace.envoltorioStock,
            enlace.campoStockMinimo to enlace.envoltorioStockMinimo,
            enlace.campoCodigo to enlace.envoltorioCodigo,
        ).forEach { (campo, envoltorio) -> campo.doAfterTextChanged { envoltorio.error = null } }

        enlace.botonGuardar.setOnClickListener { guardar() }
        enlace.botonEliminar.setOnClickListener { confirmarEliminar() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun guardar() {
        val errores = controlador.guardar(
            id = productoId,
            nombre = enlace.campoNombre.text.toString(),
            precioTexto = enlace.campoPrecio.text.toString(),
            stockTexto = enlace.campoStock.text.toString(),
            stockMinimoTexto = enlace.campoStockMinimo.text.toString(),
            codigoTexto = enlace.campoCodigo.text.toString(),
        )
        if (errores.hayErrores) {
            if (errores.nombre) enlace.envoltorioNombre.error = getString(R.string.error_nombre)
            if (errores.precio) enlace.envoltorioPrecio.error = getString(R.string.error_precio)
            if (errores.stock) enlace.envoltorioStock.error = getString(R.string.error_stock)
            if (errores.stockMinimo) enlace.envoltorioStockMinimo.error = getString(R.string.error_stock_minimo)
            errores.codigoRepetidoDe?.let { enlace.envoltorioCodigo.error = getString(R.string.error_codigo_repetido, it) }
            return
        }
        terminar(R.string.producto_guardado)
    }

    private fun confirmarEliminar() {
        val actual = producto ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.eliminar_titulo)
            .setMessage(getString(R.string.eliminar_mensaje, actual.nombre))
            .setNegativeButton(R.string.cancelar, null)
            .setPositiveButton(R.string.eliminar) { _, _ ->
                controlador.eliminar(actual.id)
                terminar(R.string.producto_eliminado)
            }
            .show()
    }

    private fun terminar(mensaje: Int) {
        setFragmentResult(RESULTADO, bundleOf(CLAVE_MENSAJE to mensaje))
        dismiss()
    }

    private fun formatearNumero(valor: Double) =
        if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString()

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }

    companion object {
        const val RESULTADO = "producto_guardado"
        const val CLAVE_MENSAJE = "mensaje"
        private const val ARG_ID = "id"

        fun nuevo(productoId: Long) = ProductoSheet().apply { arguments = bundleOf(ARG_ID to productoId) }
    }
}
