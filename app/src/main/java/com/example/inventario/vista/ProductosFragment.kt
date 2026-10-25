package com.example.inventario.vista

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventario.R
import com.example.inventario.databinding.FragmentProductosBinding
import com.example.inventario.databinding.ItemProductoBinding
import com.example.inventario.controlador.ProductoControlador
import com.example.inventario.modelo.Producto
import com.example.inventario.util.Formato
import com.google.android.material.snackbar.Snackbar

class ProductosFragment : Fragment(R.layout.fragment_productos) {

    private val controlador by lazy { ProductoControlador(requireContext()) }
    private var _enlace: FragmentProductosBinding? = null
    private val enlace get() = _enlace!!

    private val adaptador = ProductoAdaptador { abrirFormulario(it.id) }

    override fun onViewCreated(vista: View, savedInstanceState: Bundle?) {
        _enlace = FragmentProductosBinding.bind(vista)

        enlace.listaProductos.layoutManager = LinearLayoutManager(requireContext())
        enlace.listaProductos.adapter = adaptador
        enlace.botonNuevo.setOnClickListener { abrirFormulario(0) }
        enlace.campoBusqueda.doAfterTextChanged { cargar() }

        parentFragmentManager.setFragmentResultListener(ProductoSheet.RESULTADO, viewLifecycleOwner) { _, datos ->
            cargar()
            val mensaje = datos.getInt(ProductoSheet.CLAVE_MENSAJE)
            if (mensaje != 0) Snackbar.make(enlace.root, mensaje, Snackbar.LENGTH_SHORT).show()
        }
        cargar()
    }

    private fun abrirFormulario(productoId: Long) {
        ProductoSheet.nuevo(productoId).show(parentFragmentManager, "producto")
    }

    private fun cargar() {
        val busqueda = enlace.campoBusqueda.text?.toString().orEmpty()
        val productos = controlador.listar(busqueda)
        adaptador.submitList(productos)
        enlace.textoVacio.visibility = if (productos.isEmpty()) View.VISIBLE else View.GONE
        enlace.textoVacio.setText(
            if (busqueda.isBlank()) R.string.productos_vacio else R.string.productos_sin_resultados
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _enlace = null
    }
}

class ProductoAdaptador(private val alTocar: (Producto) -> Unit) :
    ListAdapter<Producto, ProductoAdaptador.Fila>(Comparador) {

    class Fila(val enlace: ItemProductoBinding) : RecyclerView.ViewHolder(enlace.root)

    override fun onCreateViewHolder(padre: ViewGroup, tipo: Int) =
        Fila(ItemProductoBinding.inflate(LayoutInflater.from(padre.context), padre, false))

    override fun onBindViewHolder(fila: Fila, posicion: Int) {
        val producto = getItem(posicion)
        val e = fila.enlace
        val contexto = e.root.context

        e.textoInicial.text = producto.nombre.trim().firstOrNull()?.uppercase() ?: "?"
        e.textoNombre.text = producto.nombre
        e.textoPrecio.text = Formato.dinero(producto.precio)
        e.textoCodigo.text = producto.codigoBarras?.let { contexto.getString(R.string.codigo_item, it) }
            ?: contexto.getString(R.string.sin_codigo)

        e.textoStock.text = Formato.entero(producto.stock)
        e.textoUnidad.text = if (producto.stockBajo) contexto.getString(R.string.chip_bajo) else contexto.getString(R.string.uds)

        val atributoFondo = if (producto.stockBajo) com.google.android.material.R.attr.colorErrorContainer
        else com.google.android.material.R.attr.colorPrimaryContainer
        val atributoTexto = if (producto.stockBajo) com.google.android.material.R.attr.colorOnErrorContainer
        else com.google.android.material.R.attr.colorOnPrimaryContainer
        e.bloqueStock.backgroundTintList = android.content.res.ColorStateList.valueOf(
            com.google.android.material.color.MaterialColors.getColor(e.root, atributoFondo)
        )
        val colorTexto = com.google.android.material.color.MaterialColors.getColor(e.root, atributoTexto)
        e.textoStock.setTextColor(colorTexto)
        e.textoUnidad.setTextColor(colorTexto)

        e.root.setOnClickListener { alTocar(producto) }
    }

    private object Comparador : DiffUtil.ItemCallback<Producto>() {
        override fun areItemsTheSame(a: Producto, b: Producto) = a.id == b.id
        override fun areContentsTheSame(a: Producto, b: Producto) = a == b
    }
}
