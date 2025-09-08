package com.example.turbolaravelstarterkitexample.features.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import com.example.turbolaravelstarterkitexample.R
import com.example.turbolaravelstarterkitexample.Urls
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.visit.VisitAction.REPLACE
import dev.hotwire.core.turbo.visit.VisitOptions
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
        
        // Override back button behavior for main sections
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // For dashboard/settings, back button should not navigate between them
            // Only allow back if going to login or exiting app
            if (isMainSection()) {
                // Do nothing - disable back navigation between main sections
                return@addCallback
            }
            // For other sections, allow normal back behavior
            isEnabled = false
            requireActivity().onBackPressed()
        }

        bottomNav.setOnItemSelectedListener { tab ->
            when (tab.itemId) {
                R.id.bottom_nav_dashboard -> {
                    // Only navigate if not already on dashboard
                    if (!navigator.location.orEmpty().contains("/dashboard")) {
                        // Use normal navigation for caching, hide back arrow manually
                        navigator.route(Urls.homeUrl)
                    }
                    true
                }
                R.id.bottom_nav_settings -> {
                    // Only navigate if not already on settings
                    if (!navigator.location.orEmpty().contains("/settings")) {
                        // Use normal navigation for caching, hide back arrow manually
                        navigator.route(Urls.settingsUrl)
                    }
                    true
                }
                else -> false
            }
        }

        updateBottomNavState()
        
        // Hide navigation icon from toolbar
        configureToolbar()
    }
    
    private fun configureToolbar() {
        // Remove back arrow from toolbar for bottom navigation sections
        toolbarForNavigation()?.apply {
            navigationIcon = null
            setNavigationOnClickListener(null)
        }
    }
    
    private fun isMainSection(): Boolean {
        val location = navigator.location.orEmpty()
        return location.contains("/dashboard") || location.contains("/settings")
    }
    
    override fun onVisitStarted(location: String) {
        super.onVisitStarted(location)
        
        // Optimize navigation by enabling cache for dashboard/settings
        if (location.contains("/dashboard") || location.contains("/settings")) {
            // Don't show loading indicators for cached navigation between main sections
            view?.post {
                // Hide any loading indicators for fast transitions
                configureToolbar()
            }
        }
    }
    
    override fun onVisitCompleted(location: String, completedOffline: Boolean) {
        super.onVisitCompleted(location, completedOffline)
        
        updateBottomNavState()
        
        // Ensure toolbar configuration is maintained after navigation
        configureToolbar()
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