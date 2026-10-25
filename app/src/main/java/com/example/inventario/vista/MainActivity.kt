package com.example.inventario.vista

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.inventario.R
import com.example.inventario.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var enlace: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enlace = ActivityMainBinding.inflate(layoutInflater)
        setContentView(enlace.root)

        // La barra inferior ya maneja su propio inset; aqui solo se cubre arriba y los lados.
        ViewCompat.setOnApplyWindowInsetsListener(enlace.main) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            vista.setPadding(barras.left, barras.top, barras.right, 0)
            insets
        }

        enlace.navegacion.setOnItemSelectedListener { item ->
            mostrar(
                when (item.itemId) {
                    R.id.nav_productos -> ProductosFragment()
                    R.id.nav_movimientos -> MovimientosFragment()
                    else -> ResumenFragment()
                }
            )
            true
        }
        if (savedInstanceState == null) enlace.navegacion.selectedItemId = R.id.nav_resumen
    }

    private fun mostrar(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
            .replace(R.id.contenedor, fragment)
            .commit()
    }
}
