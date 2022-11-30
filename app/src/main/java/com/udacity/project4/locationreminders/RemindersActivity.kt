package com.udacity.project4.locationreminders

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.udacity.project4.R

class RemindersActivity : AppCompatActivity() {

    private lateinit var nav_host_fragment : NavHostFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)
        nav_host_fragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                nav_host_fragment.navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}