package com.example.inventario.vista

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.example.inventario.R
import com.example.inventario.databinding.SheetMovimientoBinding
import com.example.inventario.controlador.MovimientoControlador
import com.example.inventario.controlador.ResultadoMovimiento
import com.example.inventario.modelo.Producto
import com.example.inventario.modelo.TipoMovimiento
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Formulario para registrar una entrada o salida. Actualiza el stock del producto. */
class MovimientoSheet : BottomSheetDialogFragment() {

    private var _enlace: SheetMovimientoBinding? = null
    private val enlace get() = _enlace!!

    private val controlador by lazy { MovimientoControlador(requireContext()) }
    private var productos: List<Producto> = emptyList()
    private var seleccionado: Producto? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).also {
            it.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

    override fun onCreateView(inflador: LayoutInflater, contenedor: ViewGroup?, savedInstanceState: Bundle?): View {
        _enlace = SheetMovimientoBinding.inflate(inflador, contenedor, false)
        return enlace.root
    }

    override fun onViewCreated(vista: View, savedInstanceState: Bundle?) {
        productos = controlador.productosDisponibles()

        enlace.campoProducto.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, productos.map { it.nombre })
        )
        enlace.campoProducto.setOnItemClickListener { _, _, posicion, _ ->
            elegir(productos[posicion])
            enlace.campoCodigo.setText("")
        }

        // Escribir un codigo de barras completo selecciona el producto automaticamente.
        enlace.campoCodigo.doAfterTextChanged { texto ->
            val codigo = texto?.toString()?.trim().orEmpty()
            if (codigo.isEmpty()) return@doAfterTextChanged
            val encontrado = productos.firstOrNull { it.codigoBarras == codigo }
            if (encontrado != null) elegir(encontrado)
        }
        enlace.campoCantidad.doAfterTextChanged { enlace.envoltorioCantidad.error = null }
        enlace.botonRegistrar.setOnClickListener { registrar() }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun elegir(producto: Producto) {
        seleccionado = producto
        enlace.campoProducto.setText(producto.nombre, false)
        enlace.envoltorioProducto.error = null
        enlace.textoStockActual.text = getString(R.string.stock_actual_info, producto.stock)
        enlace.textoStockActual.visibility = View.VISIBLE
    }

    private fun tipoActual() =
        if (enlace.grupoTipo.checkedButtonId == R.id.boton_salida) TipoMovimiento.SALIDA else TipoMovimiento.ENTRADA

    private fun registrar() {
        val tipo = tipoActual()
        when (val resultado = controlador.registrar(seleccionado?.id, enlace.campoCantidad.text.toString(), tipo)) {
            is ResultadoMovimiento.Invalido -> {
                if (resultado.faltaProducto) enlace.envoltorioProducto.error = getString(R.string.error_producto)
                if (resultado.cantidadMala) enlace.envoltorioCantidad.error = getString(R.string.error_cantidad)
            }
            is ResultadoMovimiento.SinStock ->
                enlace.envoltorioCantidad.error = getString(R.string.error_sin_stock, resultado.disponible)
            is ResultadoMovimiento.Exito -> {
                val mensaje = if (tipo == TipoMovimiento.ENTRADA) getString(R.string.entrada_registrada, resultado.cantidad)
                else getString(R.string.salida_registrada, resultado.cantidad)
                val bajo = resultado.producto.takeIf { resultado.requiereAlerta }
                val datos = Bundle().apply {
                    putString(CLAVE_MENSAJE, mensaje)
                    putString(CLAVE_ALERTA, bajo?.nombre)
                    putInt(CLAVE_STOCK, resultado.producto.stock)
                }
                setFragmentResult(RESULTADO, datos)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }

    companion object {
        const val RESULTADO = "movimiento_registrado"
        const val CLAVE_MENSAJE = "mensaje"
        const val CLAVE_ALERTA = "alerta"
        const val CLAVE_STOCK = "stock"
    }
}
