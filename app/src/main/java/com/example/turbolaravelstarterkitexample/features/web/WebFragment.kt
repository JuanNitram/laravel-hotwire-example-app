package com.example.turbolaravelstarterkitexample.features.web

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import com.example.turbolaravelstarterkitexample.R
import com.example.turbolaravelstarterkitexample.Urls
import dev.hotwire.core.turbo.errors.HttpError
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.visit.VisitAction.REPLACE
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebFragment

@HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
open class WebFragment : HotwireWebFragment() {
    
    companion object {
        private const val PREFS_NAME = "WebFragmentPrefs"
        private const val COOKIES_KEY = "saved_cookies"
        private var hasRestoredSession = false
    }
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var cookieManager: CookieManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize SharedPreferences and CookieManager
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cookieManager = CookieManager.getInstance()
        
        // Configure cookie manager
        setupCookieManager()
        
        // Restore session cookies only once per app session (not on every fragment creation)
        if (!hasRestoredSession) {
            restoreSessionIfExists()
            hasRestoredSession = true
        }
        
        setupMenu()
    }

    override fun onFormSubmissionStarted(location: String) {
        menuProgress?.isVisible = true
        isFormSubmissionInProgress = true
    }

    override fun onFormSubmissionFinished(location: String) {
        menuProgress?.isVisible = false
        
        // Reset form submission tracking after a delay to allow for redirects
        view?.postDelayed({
            isFormSubmissionInProgress = false
        }, 2000) // 2 second delay to allow for redirect and page load
    }

    override fun onVisitErrorReceived(location: String, error: VisitError) {
        if (error is HttpError.ClientError.Unauthorized) {
            // Clear saved session when unauthorized (likely logged out)
            clearSavedSession()
            navigator.route(Urls.loginUrl, VisitOptions(action = REPLACE))
        } else {
            super.onVisitErrorReceived(location, error)
        }
    }
    
    override fun onVisitCompleted(location: String, completedOffline: Boolean) {
        super.onVisitCompleted(location, completedOffline)
        
        when {
            // Login success: Successfully visiting dashboard from login means login worked
            isLoginSuccess(location) -> {
                // Add a delay to ensure cookies are fully set by the WebView
                view?.postDelayed({
                    saveSessionCookies()
                }, 500) // 500ms delay
            }
            
            // Logout detection: Visiting login page means we've been logged out
            location.contains("/login") && !isFormSubmissionInProgress -> {
                clearSavedSession()
            }
        }
    }
    
    override fun onVisitStarted(location: String) {
        super.onVisitStarted(location)
    }

    private fun setupMenu() {
        toolbarForNavigation()?.inflateMenu(R.menu.menu)
    }

    private val menuProgress: MenuItem?
        get() = toolbarForNavigation()?.menu?.findItem(R.id.menu_progress)
    
    /**
     * Configure the CookieManager to accept cookies
     */
    private fun setupCookieManager() {
        cookieManager.setAcceptCookie(true)
        // Note: Third-party cookies will be handled automatically by the WebView
    }
    
    /**
     * Save session cookies after successful login
     */
    private fun saveSessionCookies() {
        try {
            val baseUrl = extractDomainFromUrl(Urls.homeUrl)
            
            // Try multiple domain formats to ensure we get the cookies
            val domains = listOf(baseUrl, "10.0.2.2:8000", "10.0.2.2")
            var cookies: String? = null
            
            for (domain in domains) {
                cookies = cookieManager.getCookie(domain)
                if (!cookies.isNullOrEmpty()) {
                    break
                }
            }
            
            if (!cookies.isNullOrEmpty()) {
                sharedPreferences.edit()
                    .putString(COOKIES_KEY, cookies)
                    .putString("${COOKIES_KEY}_domain", baseUrl)
                    .putLong("${COOKIES_KEY}_timestamp", System.currentTimeMillis())
                    .apply()
            }
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    /**
     * Restore session cookies BEFORE website renders (if we have a saved session)
     */
    private fun restoreSessionIfExists() {
        try {
            val savedCookies = sharedPreferences.getString(COOKIES_KEY, null)
            val savedDomain = sharedPreferences.getString("${COOKIES_KEY}_domain", null)
            val savedTimestamp = sharedPreferences.getLong("${COOKIES_KEY}_timestamp", 0)
            
            if (!savedCookies.isNullOrEmpty()) {
                // Check if session is not too old (optional: expire after 30 days)
                val daysSinceLogin = (System.currentTimeMillis() - savedTimestamp) / (1000 * 60 * 60 * 24)
                
                if (daysSinceLogin > 30) {
                    clearSavedSession()
                    return
                }
                
                // Use the saved domain or fallback to extracted domain
                val baseUrl = savedDomain ?: extractDomainFromUrl(Urls.homeUrl)
                
                // Set cookies for multiple domain formats
                val domains = listOf(baseUrl, "10.0.2.2:8000", "10.0.2.2")
                val cookieArray = savedCookies.split(";")
                
                for (domain in domains) {
                    for (cookie in cookieArray) {
                        if (cookie.trim().isNotEmpty()) {
                            cookieManager.setCookie(domain, cookie.trim())
                        }
                    }
                }
                
                // Ensure cookies are written to persistent storage
                cookieManager.flush()
            }
        } catch (e: Exception) {
            // Handle silently
        }
    }
    
    /**
     * Clear saved session (useful for logout)
     */
    private fun clearSavedSession() {
        sharedPreferences.edit()
            .remove(COOKIES_KEY)
            .remove("${COOKIES_KEY}_domain")
            .remove("${COOKIES_KEY}_timestamp")
            .apply()
        
        // Also clear cookies from CookieManager
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        
        // Reset the restoration flag so session can be restored again after next login
        hasRestoredSession = false
    }
    
    /**
     * Extract domain from URL for cookie management
     */
    private fun extractDomainFromUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val host = uri.host
            val port = uri.port
            
            when {
                host != null && port != -1 -> "$host:$port"
                host != null -> host
                else -> {
                    // Fallback: extract manually
                    val cleaned = url.replace("http://", "").replace("https://", "")
                    val domainPart = cleaned.split("/")[0]
                    domainPart
                }
            }
        } catch (e: Exception) {
            // Fallback for Android emulator
            "10.0.2.2:8000"
        }
    }
    
    /**
     * Check if the current location indicates successful login
     * Login success is detected when we successfully visit the dashboard from a non-dashboard page
     */
    private fun isLoginSuccess(location: String): Boolean {
        val isDashboard = location.contains("/dashboard")
        val wasNotOnDashboard = !wasOnDashboard
        
        // Update tracking for next time
        wasOnDashboard = isDashboard
        
        return isDashboard && wasNotOnDashboard
    }
    
    // Track if we were on dashboard in the previous visit
    private var wasOnDashboard = false
    
    // Track if we're currently in a form submission process
    private var isFormSubmissionInProgress = false
    
}