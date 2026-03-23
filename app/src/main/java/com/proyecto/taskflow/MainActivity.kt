package com.proyecto.taskflow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.appbar.MaterialToolbar

/**
 * Esta Activity es el contenedor de toda la app
 * Aquí solo monto la infraestructura de navegación
 * La idea es simple, dibujo un layout con una Toolbar y un NavHostFragment
 * y le doy a Navigation el control del título y de la flecha atrás
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pinto el layout raíz que tiene la Toolbar y el NavHostFragment
        setContentView(R.layout.activity_main)

        // Localizo la Toolbar declarada en activity_main
        // Esta barra la uso como ActionBar para que Navigation pueda manejar título y botón Up
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Oculto el texto "TaskFlow" de la barra superior
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Encuentro el NavHostFragment que actúa como contenedor de pantallas
        // Desde él obtengo el NavController, que es quien sabe a qué destino navegar
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        // Enlazo la ActionBar con el NavController
        // Con esto, el título se actualiza según el destino y la flecha Up vuelve por el back stack
        setupActionBarWithNavController(navController)
    }

    /**
     * Delego el comportamiento de la flecha Up a Navigation
     * Si hay pantallas en el back stack, hace pop hacia atrás
     * Si no, dejo que el comportamiento por defecto cierre la Activity
     */
    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
