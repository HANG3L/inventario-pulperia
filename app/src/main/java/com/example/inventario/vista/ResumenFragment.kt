package com.example.inventario.vista

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.example.inventario.R
import com.example.inventario.databinding.FragmentResumenBinding
import com.example.inventario.databinding.ItemTopBinding
import com.example.inventario.controlador.ResumenControlador
import com.example.inventario.util.Formato

class ResumenFragment : Fragment(R.layout.fragment_resumen) {

    private val controlador by lazy { ResumenControlador(requireContext()) }

    private var _enlace: FragmentResumenBinding? = null
    private val enlace get() = _enlace!!

    override fun onViewCreated(vista: View, savedInstanceState: Bundle?) {
        _enlace = FragmentResumenBinding.bind(vista)
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val totales = controlador.totales()

        enlace.textoValorTotal.text = Formato.dinero(totales.valorTotal)
        enlace.textoCantidadProductos.text = Formato.entero(totales.productos)
        enlace.textoCantidadUnidades.text = Formato.entero(totales.unidades)

        val bajos = controlador.stockBajo()
        enlace.tarjetaAlerta.visibility = if (bajos.isEmpty()) View.GONE else View.VISIBLE
        if (bajos.isNotEmpty()) {
            enlace.textoAlertaTitulo.text =
                if (bajos.size == 1) getString(R.string.alerta_titulo_uno)
                else getString(R.string.alerta_titulo_plural, bajos.size)
            val visibles = bajos.take(4).joinToString("\n") { "• ${it.nombre} — ${it.stock} / ${it.stockMinimo} ${getString(R.string.uds)}" }
            val restantes = bajos.size - 4
            enlace.textoAlertaDetalle.text = if (restantes > 0) "$visibles\n• +$restantes" else visibles
        }

        val top = controlador.top(5)
        enlace.textoTopVacio.visibility = if (top.isEmpty()) View.VISIBLE else View.GONE
        enlace.listaTop.removeAllViews()
        val mayor = top.firstOrNull()?.valor ?: 1.0
        top.forEachIndexed { indice, producto ->
            val fila = ItemTopBinding.inflate(LayoutInflater.from(requireContext()), enlace.listaTop, false)
            fila.textoPosicion.text = (indice + 1).toString()
            fila.textoNombre.text = producto.nombre
            fila.textoValor.text = Formato.dinero(producto.valor)
            fila.textoDetalle.text = getString(R.string.top_detalle, producto.stock, Formato.dinero(producto.precio))
            fila.barraValor.setProgressCompat((producto.valor / mayor * 1000).toInt(), false)
            enlace.listaTop.addView(fila.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}
