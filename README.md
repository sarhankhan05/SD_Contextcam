# SD_ContextCam - Intelligent Photo Journal

SD_ContextCam is an Android application that revolutionizes photo organization by automatically tagging and categorizing your photos based on context, time, and location. It features a custom camera, intelligent tagging engine, secure vault, and smart gallery for effortless photo management.

## Features

### 📸 Custom Camera with Context Awareness
- Native Android camera implementation using CameraX
- Context-based tagging during photo capture
- Special vault mode for secure photo storage
- Real-time visual indicators for active tagging sessions

### 🏷️ Intelligent Tagging Engine
- Automatic photo organization based on:
  - Location context
  - Time and date
  - WiFi network information
  - Calendar events
- Smart batch sorting for existing photos
- Manual tag assignment during capture sessions

### 🔐 Secure Vault
- End-to-end encryption for private photos
- Biometric authentication (fingerprint, face unlock)
- PIN-based backup authentication
- Isolated storage separate from device gallery

### 📚 Smart Gallery
- Tag-based photo organization
- Filter photos by tags, dates, or combinations
- Full-screen photo viewing
- Intuitive tag counter display

### 🎯 First-Time User Experience
- Guided onboarding with permission handling
- Smart batch processing for existing photo libraries
- Location permission opt-in for enhanced context

## Architecture

### MVVM Pattern
- **Model**: Room database with SQLite for local data persistence
- **View**: Material Design UI components with responsive layouts
- **ViewModel**: LiveData for reactive UI updates

### Core Components
- **Camera Module**: Custom camera implementation with CameraX
- **Tagging Engine**: Context-aware photo categorization system
- **Vault Module**: AES-256 encryption with biometric authentication
- **Gallery Module**: Tag-based photo browsing and filtering
- **Onboarding Engine**: Smart photo batch processing

## Technical Stack

- **Language**: Java
- **Framework**: Android SDK
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library (SQLite)
- **Camera**: CameraX
- **Security**: Android Security Library (AES-256)
- **Biometrics**: Android Biometric Library
- **UI**: Material Design Components

## Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API level 29+
- Android device or emulator with camera

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/sarhankhan05/SD_Contextcam
   ```

2. Open in Android Studio:
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned repository folder

3. Build the project:
   - Menu → Build → Make Project
   - Or use Gradle command: `./gradlew build`

4. Run the application:
   - Connect an Android device or start an emulator
   - Menu → Run → Run 'app'

## Usage

### First-Time Setup
1. Launch the app to begin onboarding
2. Grant storage permissions when prompted
3. Optionally grant location permissions for context tagging
4. Complete smart batch sorting of existing photos

### Taking Photos
1. Open the camera from the main screen
2. Start a tag session by tapping the tag icon
3. Select an existing tag or create a new one
4. Capture photos (they'll be automatically tagged)
5. For sensitive photos, use the "Vault" tag session

### Viewing Photos
1. Access the gallery from the main screen or camera
2. Browse photos by tags in list view
3. Tap any tag to view associated photos
4. Tap any photo for full-screen viewing
5. Access the vault with biometric authentication

### Vault Access
1. From gallery, tap the "Vault" tag
2. Authenticate using fingerprint/face or PIN
3. View encrypted photos securely

## Project Structure

```
app/
├── src/main/java/com/example/sd_contextcam/
│   ├── data/              # Room entities and DAOs
│   ├── security/          # Encryption utilities
│   ├── viewmodel/         # ViewModel classes
│   ├── onboarding/        # Onboarding components
│   ├── CameraActivity.java
│   ├── GalleryActivity.java
│   ├── VaultActivity.java
│   └── ...                # Other activities
├── src/main/res/
│   ├── layout/            # XML layout files
│   ├── drawable/          # Image resources
│   └── values/            # String, color, dimen resources
└── build.gradle.kts       # App-level build configuration
```

## Dependencies

```kotlin
// CameraX
implementation "androidx.camera:camera-core:1.3.0"
implementation "androidx.camera:camera-camera2:1.3.0"
implementation "androidx.camera:camera-lifecycle:1.3.0"
implementation "androidx.camera:camera-view:1.3.0"

// Room Database
implementation "androidx.room:room-runtime:2.6.1"
annotationProcessor "androidx.room:room-compiler:2.6.1"

// Security
implementation "androidx.security:security-crypto:1.1.0-alpha06"

// Biometrics
implementation "androidx.biometric:biometric:1.1.0"

// Material Design
implementation "com.google.android.material:material:1.13.0"
```

## Scenarios Implemented

### Scenario 1: The First-Time User (The 10k Photo Problem)
- Welcome screen with permission explanation
- Smart batch sorter for existing photos
- Quick tag assignment for large photo collections

### Scenario 2: The Proactive User (The "Tag Session")
- Context-aware tag sessions during photo capture
- Persistent tagging indicators
- Automatic tag application to captured photos

### Scenario 3: The Secure User (The "Vault")
- Encrypted photo storage
- Biometric authentication for vault access
- Isolated photo viewing for sensitive content

### Scenario 4: The "Aha!" Moment (Finding Photos)
- Tag-based photo organization
- Instant photo retrieval by tag
- Combined tag and date filtering

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit your changes: `git commit -am 'Add new feature'`
4. Push to the branch: `git push origin feature-name`
5. Create a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Android CameraX team for the camera implementation framework
- Room Persistence Library for local data storage
- Material Design team for UI components
- Android Security team for encryption utilities

## Support

For support, email sarhankhan05@gmail.com or file an issue on GitHub.

This is a test edit.