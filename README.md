# Pill Mate

**Your Personal Health & Medication Companion**

Pill Mate is a modern Android application built to help users manage medication adherence, track health vitals, and coordinate with caregivers. By combining offline-first reliability with AI-powered insights, Pill Mate ensures you never miss a dose.

---

## Project Architecture

This project follows **Clean Architecture** principles combined with the **MVVM (Model-View-ViewModel)** pattern. This separation of concerns ensures the code is testable, scalable, and easy for multiple developers to work on simultaneously.

### Directory Structure & Guidelines

```text
├───java
│   └───com.example.medipal
│       ├───data                # DATA LAYER: Source of all data (Local & Remote)
│       │   ├───local           # Room Persistence: SQLite for offline capability
│       │   │   ├───dao         # Database queries (Data Access Objects)
│       │   │   ├───database    # Room Database configuration
│       │   │   └───entity      # DB Table models (Internal use only)
│       │   ├───mapper          # Converters (e.g., Entity -> Domain Model)
│       │   ├───remote          # Cloud/Network: Retrofit & Firebase
│       │   │   ├───api         # Interface definitions for External APIs
│       │   │   ├───dto         # Data Transfer Objects (JSON models)
│       │   │   └───firebase    # Auth, Cloud Sync, and Caregiver sharing
│       │   ├───repository      # Concrete implementations of Domain Repositories
│       │   └───service         # Device services (GPS, Camera, Sensors)
│       ├───di                  # DEPENDENCY INJECTION: Hilt/Koin module setup
│       ├───domain              # DOMAIN LAYER: Pure Business Logic & Rules
│       │   ├───model           # UI-ready models (The "Truth" for the app)
│       │   ├───repository      # Interfaces (Contracts) for the Data layer
│       │   ├───service         # Logic-heavy services (AI Engine, Schedule Logic)
│       │   └───usecase         # Single-action logic (e.g., "AddMedicationUseCase")
│       ├───presentation        # UI LAYER: Visual components and State
│       │   ├───navigation      # NavHost and Route definitions
│       │   ├───ui              # Jetpack Compose UI
│       │   │   ├───components  # Reusable widgets (Buttons, Cards, Modals)
│       │   │   ├───screens     # Full-page Composables (Home, Settings, AI)
│       │   │   └───theme       # Colors, Typography, and Design Tokens
│       │   └───viewmodel       # State holders; bridges Domain to UI
│       ├───util                # Helpers: Date formatters, Validators, Extensions
│       └───workers             # WorkManager: Background sync & low-stock alerts
└───res                         # ANDROID RESOURCES (Images, Strings, XML)
    ├───drawable                # Icons and vector graphics
    ├───values                  # strings.xml, colors.xml, themes.xml
    └───xml                     # Network security & App configurations
```
