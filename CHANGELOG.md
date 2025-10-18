# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-10-18

### Added
- Initial release of SD_ContextCam
- Custom camera implementation with CameraX
- Intelligent tagging engine with Room database
- Secure vault with AES-256 encryption
- Smart gallery with tag-based organization
- First-time user onboarding with smart batch sorting
- Biometric authentication for vault access
- Full-screen photo viewing capability
- MVVM architecture with LiveData and ViewModel
- Material Design UI components

### Features
- **Camera Module**: Native Android camera with context-aware tagging
- **Tagging Engine**: Automatic photo organization based on location, time, and context
- **Vault Module**: Encrypted storage for sensitive photos with biometric access
- **Gallery Module**: Tag-based photo browsing and filtering
- **Onboarding Engine**: Smart processing of existing photo libraries
- **Security**: End-to-end encryption for vault photos

### Technical Implementation
- Room database for local photo and tag storage
- CameraX for custom camera functionality
- Android Security Library for encryption
- BiometricPrompt API for secure authentication
- RecyclerView for efficient list and grid displays
- MVVM architecture for clean separation of concerns

## [0.1.0] - 2025-10-15

### Added
- Project initialization
- Basic project structure
- Initial UI layouts
- Core dependencies configuration