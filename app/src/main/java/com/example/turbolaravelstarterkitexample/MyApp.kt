package com.example.turbolaravelstarterkitexample

import android.app.Application
import com.example.turbolaravelstarterkitexample.components.FormComponent
import com.example.turbolaravelstarterkitexample.features.web.WebFragment
import com.example.turbolaravelstarterkitexample.features.web.WebHomeFragment
import com.example.turbolaravelstarterkitexample.features.web.WebModalFragment
import dev.hotwire.core.BuildConfig
import dev.hotwire.core.bridge.BridgeComponentFactory
import dev.hotwire.core.bridge.KotlinXJsonConverter
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.navigation.config.defaultFragmentDestination
import dev.hotwire.navigation.config.registerBridgeComponents
import dev.hotwire.navigation.config.registerFragmentDestinations

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        configureApp()
    }

    private fun configureApp() {
        Hotwire.loadPathConfiguration(
            context = this,
            location = PathConfiguration.Location(
                assetFilePath = "json/configuration.json"
            )
        )

        Hotwire.defaultFragmentDestination = WebFragment::class

        Hotwire.registerFragmentDestinations(
            WebFragment::class,
            WebModalFragment::class,
            WebHomeFragment::class,
        )

        Hotwire.registerBridgeComponents(
            BridgeComponentFactory("form", ::FormComponent),
        )

        Hotwire.config.debugLoggingEnabled = BuildConfig.DEBUG
        Hotwire.config.webViewDebuggingEnabled = BuildConfig.DEBUG
        Hotwire.config.jsonConverter = KotlinXJsonConverter()
    }
}