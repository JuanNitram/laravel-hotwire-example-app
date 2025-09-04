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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener { tab ->
            when (tab.itemId) {
                R.id.bottom_nav_dashboard -> {
                    navigator.route(Urls.homeUrl)
                    bottomNav.menu.findItem(R.id.bottom_nav_dashboard).setChecked(true)
                    true
                }

                else -> {
                    navigator.route(Urls.settingsUrl)
                    bottomNav.menu.findItem(R.id.bottom_nav_settings).setChecked(true)
                    true
                }
            }
        }

        when (navigator.location) {
            Urls.homeUrl -> bottomNav.menu.findItem(R.id.bottom_nav_dashboard).setChecked(true)
            Urls.settingsUrl -> bottomNav.menu.findItem(R.id.bottom_nav_settings).setChecked(true)
        }
    }

    @SuppressLint("InflateParams")
    override fun createErrorView(error: VisitError): View {
        return layoutInflater.inflate(R.layout.error_web_home, null)
    }
}