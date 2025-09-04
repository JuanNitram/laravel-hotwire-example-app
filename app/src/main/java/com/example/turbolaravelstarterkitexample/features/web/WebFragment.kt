package com.example.turbolaravelstarterkitexample.features.web

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
        private const val TAG = "WebFragment"
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
        
        // Restore session cookies BEFORE website renders (if we have a saved session)
        restoreSessionIfExists()
        
        setupMenu()
    }

    override fun onFormSubmissionStarted(location: String) {
        menuProgress?.isVisible = true
        isFormSubmissionInProgress = true
        Log.d(TAG, "Form submission started for location: $location")
    }

    override fun onFormSubmissionFinished(location: String) {
        menuProgress?.isVisible = false
        Log.d(TAG, "Form submission finished for location: $location")
        
        // Reset form submission tracking after a delay to allow for redirects
        view?.postDelayed({
            isFormSubmissionInProgress = false
            Log.d(TAG, "Form submission process completed")
        }, 2000) // 2 second delay to allow for redirect and page load
    }

    override fun onVisitErrorReceived(location: String, error: VisitError) {
        Log.e(TAG, "Visit error received for location: $location, error: $error")
        
        if (error is HttpError.ClientError.Unauthorized) {
            Log.d(TAG, "Unauthorized error - clearing session and redirecting to login")
            // Clear saved session when unauthorized (likely logged out)
            clearSavedSession()
            navigator.route(Urls.loginUrl, VisitOptions(action = REPLACE))
        } else {
            super.onVisitErrorReceived(location, error)
        }
    }
    
    override fun onVisitCompleted(location: String, completedOffline: Boolean) {
        super.onVisitCompleted(location, completedOffline)
        
        Log.d(TAG, "Visit completed to location: $location (offline: $completedOffline)")
        
        when {
            // Login success: Successfully visiting dashboard from login means login worked
            isLoginSuccess(location) -> {
                Log.d(TAG, "🎉 Login success detected! Saving session cookies for location: $location")
                // Add a delay to ensure cookies are fully set by the WebView
                view?.postDelayed({
                    saveSessionCookies()
                }, 500) // 500ms delay
            }
            
            // Logout detection: Visiting login page means we've been logged out
            location.contains("/login") && !isFormSubmissionInProgress -> {
                Log.d(TAG, "Logout detected, clearing saved session")
                clearSavedSession()
            }
        }
    }
    
    override fun onVisitStarted(location: String) {
        super.onVisitStarted(location)
        Log.d(TAG, "Visit started to location: $location")
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
            Log.d(TAG, "💾 Saving session cookies for domain: $baseUrl")
            
            // Try multiple domain formats to ensure we get the cookies
            val domains = listOf(baseUrl, "10.0.2.2:8000", "10.0.2.2")
            var cookies: String? = null
            
            for (domain in domains) {
                cookies = cookieManager.getCookie(domain)
                if (!cookies.isNullOrEmpty()) {
                    Log.d(TAG, "Found session cookies for domain: $domain")
                    break
                }
            }
            
            if (!cookies.isNullOrEmpty()) {
                Log.d(TAG, "Session cookies: ${cookies.take(100)}...")
                sharedPreferences.edit()
                    .putString(COOKIES_KEY, cookies)
                    .putString("${COOKIES_KEY}_domain", baseUrl)
                    .putLong("${COOKIES_KEY}_timestamp", System.currentTimeMillis())
                    .apply()
                Log.d(TAG, "✅ Session cookies saved successfully!")
            } else {
                Log.d(TAG, "❌ No session cookies found for domains: $domains")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session cookies", e)
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
                    Log.d(TAG, "🕐 Saved session is too old ($daysSinceLogin days), clearing it")
                    clearSavedSession()
                    return
                }
                
                Log.d(TAG, "🔄 Restoring session cookies from $daysSinceLogin days ago...")
                Log.d(TAG, "Session cookies: ${savedCookies.take(100)}...")
                
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
                Log.d(TAG, "✅ Session restored! Should stay logged in.")
            } else {
                Log.d(TAG, "📱 No saved session found - will show login page")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session", e)
        }
    }
    
    /**
     * Clear saved session (useful for logout)
     */
    private fun clearSavedSession() {
        Log.d(TAG, "🗑️ Clearing saved session")
        sharedPreferences.edit()
            .remove(COOKIES_KEY)
            .remove("${COOKIES_KEY}_domain")
            .remove("${COOKIES_KEY}_timestamp")
            .apply()
        
        // Also clear cookies from CookieManager
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        Log.d(TAG, "✅ Session cleared")
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
            Log.w(TAG, "Failed to extract domain from $url", e)
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