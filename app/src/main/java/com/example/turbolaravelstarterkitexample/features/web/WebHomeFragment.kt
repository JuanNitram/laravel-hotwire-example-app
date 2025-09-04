package com.example.turbolaravelstarterkitexample.features.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.turbolaravelstarterkitexample.R
import com.example.turbolaravelstarterkitexample.Urls
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink

@HotwireDestinationDeepLink(uri = "hotwire://fragment/web/home")
class WebHomeFragment : WebFragment() {
    private lateinit var bottomNav: BottomNavigationView
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNav = view.findViewById(R.id.bottom_nav)

        bottomNav.setOnItemSelectedListener { tab ->
            when (tab.itemId) {
                R.id.bottom_nav_dashboard -> {
                    navigator.route(Urls.homeUrl)
                    true
                }
                R.id.bottom_nav_settings -> {
                    navigator.route(Urls.settingsUrl)
                    true
                }
                else -> false
            }
        }

        updateBottomNavState()
    }
    
    override fun onVisitCompleted(location: String, completedOffline: Boolean) {
        super.onVisitCompleted(location, completedOffline)
        
        updateBottomNavState()
    }
    
    private fun updateBottomNavState() {
        when {
            navigator.location?.contains("/dashboard") == true -> {
                bottomNav.menu.findItem(R.id.bottom_nav_dashboard).isChecked = true
            }
            navigator.location?.contains("/settings") == true -> {
                bottomNav.menu.findItem(R.id.bottom_nav_settings).isChecked = true
            }
        }
    }

    @SuppressLint("InflateParams")
    override fun createErrorView(error: VisitError): View {
        return layoutInflater.inflate(R.layout.error_web_home, null)
    }
}