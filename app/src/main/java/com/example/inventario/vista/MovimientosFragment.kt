package com.example.inventario.vista

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.R
import com.example.inventario.databinding.FragmentMovimientosBinding
import com.example.inventario.databinding.ItemMovimientoBinding
import com.example.inventario.controlador.MovimientoControlador
import com.example.inventario.modelo.Movimiento
import com.example.inventario.modelo.TipoMovimiento
import com.example.inventario.util.Formato
import com.example.inventario.util.color
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MovimientosFragment : Fragment(R.layout.fragment_movimientos) {

    private val controlador by lazy { MovimientoControlador(requireContext()) }
    private var _enlace: FragmentMovimientosBinding? = null
    private val enlace get() = _enlace!!

    private val adaptador = MovimientoAdaptador()

    override fun onViewCreated(vista: View, savedInstanceState: Bundle?) {
        _enlace = FragmentMovimientosBinding.bind(vista)

        enlace.listaMovimientos.layoutManager = LinearLayoutManager(requireContext())
        enlace.listaMovimientos.adapter = adaptador
        enlace.botonRegistrar.setOnClickListener {
            if (controlador.productosDisponibles().isEmpty()) {
                Snackbar.make(enlace.root, R.string.sin_productos_mov, Snackbar.LENGTH_LONG).show()
            } else {
                MovimientoSheet().show(parentFragmentManager, "movimiento")
            }
        }
        enlace.grupoFiltro.setOnCheckedStateChangeListener { _, _ -> cargar() }

        parentFragmentManager.setFragmentResultListener(MovimientoSheet.RESULTADO, viewLifecycleOwner) { _, datos ->
            cargar()
            enlace.listaMovimientos.scrollToPosition(0)
            val mensaje = datos.getString(MovimientoSheet.CLAVE_MENSAJE)
            if (mensaje != null) Snackbar.make(enlace.root, mensaje, Snackbar.LENGTH_SHORT).show()

            val bajo = datos.getString(MovimientoSheet.CLAVE_ALERTA)
            if (bajo != null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setIcon(R.drawable.ic_alerta)
                    .setTitle(R.string.alerta_stock_titulo)
                    .setMessage(getString(R.string.alerta_stock_mensaje, bajo, datos.getInt(MovimientoSheet.CLAVE_STOCK)))
                    .setPositiveButton(R.string.aceptar, null)
                    .show()
            }
        }
        cargar()
    }

    private fun cargar() {
        val tipo = when (enlace.grupoFiltro.checkedChipId) {
            R.id.filtro_entradas -> TipoMovimiento.ENTRADA
            R.id.filtro_salidas -> TipoMovimiento.SALIDA
            else -> null
        }
        val movimientos = controlador.listar(tipo)
        adaptador.submitList(movimientos)
        enlace.textoVacio.visibility = if (movimientos.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}

class MovimientoAdaptador : ListAdapter<Movimiento, MovimientoAdaptador.Fila>(Comparador) {

    class Fila(val enlace: ItemMovimientoBinding) : RecyclerView.ViewHolder(enlace.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipo: Int) =
        Fila(ItemMovimientoBinding.inflate(LayoutInflater.from(padre.context), padre, false))

    override fun onBindViewHolder(fila: Fila, posicion: Int) {
        val movimiento = getItem(posicion)
        val e = fila.enlace
        val contexto = e.root.context
        val esEntrada = movimiento.tipo == TipoMovimiento.ENTRADA

        val colorFuerte = contexto.color(if (esEntrada) R.color.entrada else R.color.salida)
        val colorSuave = contexto.color(if (esEntrada) R.color.entrada_suave else R.color.salida_suave)

        e.iconoTipo.setImageResource(if (esEntrada) R.drawable.ic_entrada else R.drawable.ic_salida)
        e.iconoTipo.imageTintList = ColorStateList.valueOf(colorFuerte)
        e.iconoTipo.backgroundTintList = ColorStateList.valueOf(colorSuave)

        e.textoNombre.text = movimiento.nombreProducto
        val etiqueta = contexto.getString(if (esEntrada) R.string.entrada else R.string.salida)
        e.textoFecha.text = "$etiqueta · ${Formato.fecha(movimiento.fecha)}"
        e.textoCantidad.text = (if (esEntrada) "+" else "-") + Formato.entero(movimiento.cantidad)
        e.textoCantidad.setTextColor(colorFuerte)
    }

    private object Comparador : DiffUtil.ItemCallback<Movimiento>() {
        override fun areItemsTheSame(a: Movimiento, b: Movimiento) = a.id == b.id
        override fun areContentsTheSame(a: Movimiento, b: Movimiento) = a == b
    }
}
