package com.example.turbolaravelstarterkitexample.features.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.turbolaravelstarterkitexample.R
import com.example.turbolaravelstarterkitexample.Urls
import com.example.turbolaravelstarterkitexample.data.MenuItem
import com.example.turbolaravelstarterkitexample.data.MenuService
import com.example.turbolaravelstarterkitexample.data.ImageUploadService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import dev.hotwire.core.turbo.errors.VisitError
import dev.hotwire.core.turbo.visit.VisitAction.REPLACE
import dev.hotwire.core.turbo.visit.VisitOptions
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink

@HotwireDestinationDeepLink(uri = "hotwire://fragment/web/home")
class WebHomeFragment : WebFragment() {
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var fabCamera: FloatingActionButton
    private lateinit var loadingScreen: View
    private lateinit var mainContent: View
    private val menuService = MenuService()
    private val imageUploadService = ImageUploadService()
    private var dynamicMenuItems: List<MenuItem> = emptyList()
    private var isClosingDrawer = false
    private var pendingNavigation: String? = null
    private var pendingBottomNavigation: String? = null
    
    // Camera properties
    private var currentPhotoPath: String? = null
    private var currentPhotoUri: Uri? = null
    
    // Activity result launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            android.util.Log.d("Camera", "Camera permission denied")
        }
    }
    
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            currentPhotoUri?.let { uri ->
                uploadImage(uri)
            } ?: run {
                android.util.Log.e("Camera", "currentPhotoUri is null!")
            }
        }
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomNav = view.findViewById(R.id.bottom_nav)
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navigationView = view.findViewById(R.id.nav_view)
        fabCamera = view.findViewById(R.id.fab_camera)
        loadingScreen = view.findViewById(R.id.loading_screen)
        mainContent = view.findViewById(R.id.main_content)
        
        // Show loading screen initially
        showLoadingScreen()
        
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
                    // Use same deferred navigation pattern as drawer
                    val currentLocation = navigator.location.orEmpty()
                    if (!currentLocation.contains("/dashboard")) {
                        pendingBottomNavigation = Urls.homeUrl
                        // Small delay to let tab selection animation start
                        view?.postDelayed({
                            pendingBottomNavigation?.let { url ->
                                navigator.route(url) // Same as drawer - no VisitOptions
                                pendingBottomNavigation = null
                            }
                        }, 50)
                    }
                    true
                }
                R.id.bottom_nav_settings -> {
                    // Use same deferred navigation pattern as drawer
                    val currentLocation = navigator.location.orEmpty()
                    android.util.Log.d("BottomNav", "Settings clicked. Current location: $currentLocation")
                    if (!currentLocation.contains("/settings")) {
                        // Try to use the same URL as drawer (from API) if available, fallback to static
                        val settingsUrl = dynamicMenuItems.find { it.title == "Settings" }?.url ?: Urls.settingsUrl
                        android.util.Log.d("BottomNav", "Navigating to settings: $settingsUrl")
                        pendingBottomNavigation = settingsUrl
                        // Small delay to let tab selection animation start
                        view?.postDelayed({
                            pendingBottomNavigation?.let { url ->
                                android.util.Log.d("BottomNav", "Executing settings navigation: $url")
                                navigator.route(url) // Same as drawer - no VisitOptions
                                pendingBottomNavigation = null
                            }
                        }, 50)
                    } else {
                        android.util.Log.d("BottomNav", "Already on settings, skipping navigation")
                    }
                    true
                }
                else -> false
            }
        }

        updateBottomNavState()
        
        // Configure camera FAB
        fabCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
        
        // Load dynamic menu and configure drawer navigation
        loadDynamicMenu()
        
        // Update UI for initial page
        updateUIForCurrentPage(navigator.location.orEmpty())
    }
    
    private fun loadDynamicMenu() {
        lifecycleScope.launch {
            try {
                dynamicMenuItems = menuService.fetchMenuItems()
                
                // Update the navigation view with dynamic menu
                setupDrawerNavigation()
            } catch (e: Exception) {
                android.util.Log.e("DynamicMenu", "Error loading dynamic menu", e)
                // Setup with empty menu as fallback
                setupDrawerNavigation()
            }
        }
    }
    
    private fun setupDrawerNavigation() {
        // Configure hamburger menu icon and click
        toolbarForNavigation()?.apply {
            setNavigationIcon(R.drawable.ic_menu_24)
            setNavigationOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        
        // Clear existing menu and populate with dynamic items
        val menu = navigationView.menu
        menu.clear()
        
        // Add dynamic menu items
        dynamicMenuItems.forEachIndexed { index, menuItem ->
            val menuId = android.view.View.generateViewId()
            val item = menu.add(0, menuId, index, menuItem.title)
            
            // Set icon based on the icon name from API
            val iconRes = when (menuItem.icon) {
                "home" -> R.drawable.baseline_home_24
                "settings" -> R.drawable.baseline_manage_accounts_24
                "person" -> R.drawable.baseline_person_24
                "help" -> R.drawable.baseline_help_24
                else -> R.drawable.baseline_home_24 // fallback
            }
            item.setIcon(iconRes)
            
        }
        
        // Add drawer listener for smooth transitions
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Optional: Add custom slide animations here
            }
            
            override fun onDrawerOpened(drawerView: View) {
                // Drawer opened
            }
            
            override fun onDrawerClosed(drawerView: View) {
                // Execute pending navigation when drawer is fully closed
                pendingNavigation?.let { url ->
                    android.util.Log.d("Drawer", "Executing pending navigation: $url")
                    navigator.route(url)
                    pendingNavigation = null
                    isClosingDrawer = false
                }
            }
            
            override fun onDrawerStateChanged(newState: Int) {
                // Drawer state changed
            }
        })
        
        // Configure drawer menu item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // Find the corresponding dynamic menu item
            val selectedItem = dynamicMenuItems.find { item ->
                menuItem.title == item.title
            }
            
            selectedItem?.let { item ->
                // Extract path from URL for comparison
                val urlPath = item.url.substringAfter(Urls.baseUrl)
                val currentPath = navigator.location.orEmpty().substringAfter(Urls.baseUrl)
                
                android.util.Log.d("Drawer", "Item clicked: ${item.title}, URL: ${item.url}")
                android.util.Log.d("Drawer", "Current path: $currentPath, URL path: $urlPath")
                
                // Navigate to the URL from the API
                if (!currentPath.contains(urlPath)) {
                    android.util.Log.d("Drawer", "Navigating to: ${item.url}")
                    // Set flag and store pending navigation
                    isClosingDrawer = true
                    pendingNavigation = item.url
                    
                    // Close drawer - navigation will happen in onDrawerClosed callback
                    drawerLayout.closeDrawer(GravityCompat.START, true)
                } else {
                    android.util.Log.d("Drawer", "Already on same page, just closing drawer")
                    // Close drawer immediately if already on same page
                    drawerLayout.closeDrawer(GravityCompat.START, true)
                }
            }
            
            true
        }
    }
    
    private fun navigateWithDirection(url: String, targetSection: String) {
        val currentLocation = navigator.location.orEmpty()
        val isGoingToDashboard = targetSection == "dashboard"
        val isComingFromSettings = currentLocation.contains("/settings")
        
        // Set navigation direction for CSS animations
        when {
            isGoingToDashboard && isComingFromSettings -> {
                setNavigationDirection("right-to-left")
            }
            !isGoingToDashboard && currentLocation.contains("/dashboard") -> {
                setNavigationDirection("left-to-right")
            }
            else -> {
                setNavigationDirection("default")
            }
        }
        
        // Use normal navigation - direction will be controlled by CSS
        navigator.route(url, VisitOptions(action = REPLACE))
    }
    
    private fun setNavigationDirection(direction: String) {
        try {
            // Inject CSS and set direction attribute
            val jsCode = """
                // Load custom CSS if not already loaded
                if (!document.getElementById('custom-transitions-css')) {
                    var link = document.createElement('link');
                    link.id = 'custom-transitions-css';
                    link.rel = 'stylesheet';
                    link.href = '${Urls.baseUrl}/css/transitions.css';
                    document.head.appendChild(link);
                }
                
                // Set navigation direction
                document.body.setAttribute('data-navigation-direction', '$direction');
                
                // Clear direction after animation
                setTimeout(function() {
                    document.body.removeAttribute('data-navigation-direction');
                }, 300);
            """.trimIndent()
            
            // Execute JavaScript in WebView
            // Note: This would need to be implemented through the WebView
        } catch (e: Exception) {
            android.util.Log.e("Navigation", "Error setting navigation direction", e)
        }
    }
    
    private fun isMainSection(): Boolean {
        val location = navigator.location.orEmpty()
        return location.contains("/dashboard") || location.contains("/settings")
    }
    
    override fun onVisitStarted(location: String) {
        super.onVisitStarted(location)
        android.util.Log.d("Navigation", "Visit started: $location")
    }
    
    override fun onVisitCompleted(location: String, completedOffline: Boolean) {
        super.onVisitCompleted(location, completedOffline)
        android.util.Log.d("Navigation", "Visit completed: $location, offline: $completedOffline")
        
        // Only interfere with drawer if we're not in the middle of closing it
        if (!isClosingDrawer && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START, false) // Force close without animation
        }
        
        updateBottomNavState()
        updateUIForCurrentPage(location)
    }
    
    private fun updateBottomNavState() {
        val currentLocation = navigator.location.orEmpty()
        
        // Update bottom navigation
        when {
            currentLocation.contains("/dashboard") -> {
                bottomNav.menu.findItem(R.id.bottom_nav_dashboard).isChecked = true
            }
            currentLocation.contains("/settings") -> {
                bottomNav.menu.findItem(R.id.bottom_nav_settings).isChecked = true
            }
        }
        
        // Update drawer navigation (dynamic menu)
        val menu = navigationView.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            menuItem.isChecked = false
            
            // Find corresponding dynamic menu item
            val dynamicItem = dynamicMenuItems.find { item ->
                menuItem.title == item.title
            }
            
            dynamicItem?.let { item ->
                val urlPath = item.url.substringAfter(Urls.baseUrl)
                val currentPath = currentLocation.substringAfter(Urls.baseUrl)
                
                if (currentPath.contains(urlPath)) {
                    menuItem.isChecked = true
                    android.util.Log.d("DynamicMenu", "Checked: ${item.title}")
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    override fun createErrorView(error: VisitError): View {
        return layoutInflater.inflate(R.layout.error_web_home, null)
    }
    
    // Camera functions
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun openCamera() {
        val photoFile = createImageFile()
        photoFile?.let { file ->
            currentPhotoPath = file.absolutePath
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            
            android.util.Log.d("Camera", "Opening camera with URI: $currentPhotoUri")
            takePictureLauncher.launch(currentPhotoUri)
        }
    }
    
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            
            File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
        } catch (ex: Exception) {
            android.util.Log.e("Camera", "Error creating image file", ex)
            null
        }
    }
    
    private fun uploadImage(imageUri: Uri) {
        // Get session cookies from the WebView
        val sessionCookies = getSessionCookies()
        
        lifecycleScope.launch {
            try {
                val imageId = imageUploadService.uploadImage(requireContext(), imageUri, sessionCookies)
                
                if (imageId != null && imageId.isNotEmpty()) {
                    val navigationUrl = "${Urls.baseUrl}/images/$imageId"
                    // Navigate to image view with the returned ID
                    navigator.route(navigationUrl)
                } else {
                    android.util.Log.e("Camera", "Image upload failed - imageId is null or empty: '$imageId'")
                }
            } catch (e: Exception) {
                android.util.Log.e("Camera", "Exception in uploadImage", e)
            }
        }
    }
    
    private fun getSessionCookies(): String {
        return try {
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie(Urls.baseUrl)
            cookies ?: ""
        } catch (e: Exception) {
            android.util.Log.e("Camera", "Error getting session cookies", e)
            ""
        }
    }
    
    private fun showLoadingScreen() {
        loadingScreen.visibility = View.VISIBLE
        mainContent.visibility = View.GONE
    }
    
    private fun hideLoadingScreen() {
        loadingScreen.visibility = View.GONE
        mainContent.visibility = View.VISIBLE
    }
    
    private fun hideAllNativeUI() {
        // Hide all native UI elements immediately
        bottomNav.visibility = View.GONE
        fabCamera.visibility = View.GONE
        navigationView.visibility = View.GONE
        
        // Hide hamburger menu icon
        val toolbar = toolbarForNavigation()
        toolbar?.setNavigationIcon(null)
        
    }
    
    private fun updateUIForCurrentPage(location: String) {
        // Hide loading screen first
        hideLoadingScreen()
        
        val isLoginPage = location.contains("/login")
        
        if (isLoginPage) {
            // Hide all elements for login page
            hideAllNativeUI()
        } else {
            // Show all elements for other pages
            bottomNav.visibility = View.VISIBLE
            fabCamera.visibility = View.VISIBLE
            navigationView.visibility = View.VISIBLE
            
            // Show hamburger menu icon
            val toolbar = toolbarForNavigation()
            toolbar?.setNavigationIcon(R.drawable.ic_menu_24)
        }
        
    }
}