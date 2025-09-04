# Laravel Hotwire Android App

A native Android application that integrates with a Laravel backend using Hotwire Native, featuring persistent user sessions across app restarts.

## 🚀 Features

- **Hotwire Native Integration**: Seamless web-to-native experience
- **Persistent Sessions**: Stay logged in across app restarts
- **Smart Cookie Management**: Automatic session restoration without login interference
- **Clean Architecture**: Focused, maintainable codebase
- **Comprehensive Logging**: Easy debugging with detailed logs

## 📱 Session Persistence

The app implements intelligent session management:

### How It Works

1. **Login Success Detection**: Automatically detects successful login when redirected to dashboard
2. **Session Storage**: Saves session cookies securely in device storage
3. **Automatic Restoration**: Restores session on app start (before web content loads)
4. **Smart Expiry**: Sessions expire after 30 days for security
5. **Clean Logout**: Automatically clears session when logging out

### Session Flow

```
📱 App Launch
   ↓
🔍 Check for saved session
   ├─ ✅ Session exists → Restore cookies → Dashboard (logged in)
   └─ ❌ No session → Login page

🔐 Login Process
   ↓
📝 Submit credentials
   ├─ ✅ Success → Dashboard → Save session cookies
   └─ ❌ Failed → Stay on login page

🚪 Logout
   ↓
🗑️ Clear session → Login page
```

## 🛠 Setup & Installation

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 28+
- Kotlin 1.8+
- Laravel backend running on `http://10.0.2.2:8000`

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd laravel-hotwire-example/app
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the `app` directory

3. **Configure Backend URL**
   - Update `Urls.kt` if your Laravel backend runs on a different URL:
   ```kotlin
   object Urls {
       private val baseUrl = "http://your-backend-url:8000"
       // ...
   }
   ```

4. **Build and Run**
   ```bash
   ./gradlew installDebug
   ```

## 🔧 Configuration

### Backend Requirements

Your Laravel backend should have:

- **Authentication routes** (`/login`, `/dashboard`)
- **Session-based authentication** (cookies)
- **CSRF protection** enabled
- **Hotwire Native support** (optional but recommended)

### Test Credentials

Default test user (ensure this exists in your Laravel app):
- **Email**: `test@test.com`
- **Password**: `password`

## 📊 Debugging & Logs

The app provides comprehensive logging for session management:

### Key Log Messages

```
📱 App Start:
"📱 No saved session found - will show login page"
"🔄 Restoring session cookies from X days ago..."
"✅ Session restored! Should stay logged in."

🔐 Login:
"🎉 Login success detected! Saving session cookies"
"💾 Saving session cookies for domain: 10.0.2.2:8000"
"✅ Session cookies saved successfully!"

🚪 Logout:
"🗑️ Clearing saved session"
"✅ Session cleared"
```

### Viewing Logs

In Android Studio:
1. Open **Logcat**
2. Filter by tag: `WebFragment`
3. Look for emoji-prefixed messages

## 🏗 Architecture

### Key Components

- **`WebFragment`**: Main web container with session management
- **`MainActivity`**: Entry point and navigation setup
- **`MyApp`**: Application configuration
- **Session Management**: Cookie persistence logic

### Session Storage

Sessions are stored using Android's `SharedPreferences`:

```kotlin
// Stored data
- saved_cookies: Session cookie string
- saved_cookies_domain: Domain for cookies
- saved_cookies_timestamp: Login timestamp
```

### Security Features

- **Automatic Expiry**: Sessions expire after 30 days
- **Secure Storage**: Uses Android's private app storage
- **Clean Logout**: Removes all session data on logout
- **Error Handling**: Clears invalid sessions automatically

## 🧪 Testing

### Manual Testing

1. **Fresh Login Test**:
   - Clear app data
   - Launch app → Should show login
   - Login with valid credentials → Should reach dashboard
   - Check logs for session save confirmation

2. **Session Persistence Test**:
   - After successful login, close app completely
   - Relaunch app → Should go directly to dashboard
   - Check logs for session restoration

3. **Session Expiry Test**:
   - Manually set old timestamp in SharedPreferences
   - Relaunch app → Should show login page
   - Check logs for expiry message

4. **Logout Test**:
   - While logged in, logout through web interface
   - Should clear session and show login page

### Automated Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## 🔍 Troubleshooting

### Common Issues

**Login redirects back to login page:**
- Check Laravel logs for authentication errors
- Verify CSRF tokens are being sent correctly
- Ensure session cookies are being set by Laravel

**Session not persisting:**
- Check logs for "Session cookies saved successfully"
- Verify cookies contain session data (not just CSRF tokens)
- Check if session expires on Laravel side

**App crashes on startup:**
- Check for missing dependencies
- Verify Android SDK version compatibility
- Review crash logs in Logcat

### Debug Commands

```bash
# Clear app data (fresh start)
adb shell pm clear com.example.turbolaravelstarterkitexample

# View app logs
adb logcat | grep WebFragment

# Check SharedPreferences
adb shell run-as com.example.turbolaravelstarterkitexample ls shared_prefs/
```

## 📚 Dependencies

### Main Dependencies

- **Hotwire Native**: `dev.hotwire:core:1.1.3`
- **Navigation Fragments**: `dev.hotwire:navigation-fragments:1.1.3`
- **Kotlin Serialization**: `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0`
- **Material Design**: `com.google.android.material:material`

### Development Dependencies

- **Android Gradle Plugin**: `8.x`
- **Kotlin**: `1.9.x`
- **Compose Compiler**: Latest stable

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:

1. Check the [troubleshooting section](#-troubleshooting)
2. Review the [logs](#-debugging--logs) for error messages
3. Open an issue with detailed logs and steps to reproduce

---

**Built with ❤️ using Hotwire Native and Laravel**
