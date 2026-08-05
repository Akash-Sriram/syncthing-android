# Syncthing Android - Overlay Customizations

This repository contains custom modifications for the Syncthing Android app (specifically, an in-app downloader to get the latest stable and pre-release releases directly from the GitHub releases page).

Instead of maintaining a complex fork branch, this project uses an **Overlay Structure**. The official `researchxxl/syncthing-android` repository is tracked as a Git submodule, and our custom files are copied on top of it.

## Repository Structure

- `overlay/` - Contains our customized and new files matching the structure of the official project.
- `syncthing-android-source/` - The Git submodule containing the official Syncthing Android codebase.
- `apply-overlay.ps1` / `apply-overlay.bat` - Scripts to pull the submodule and apply the overlay modifications.

## Getting Started & Building

1. **Clone the repository:**
   ```bash
   git clone --recursive https://github.com/Akash-Sriram/syncthing-android.git
   cd syncthing-android
   ```

2. **Apply the overlay:**
   Run the setup/overlay script to initialize the submodules and apply modifications:
   - **PowerShell:**
     ```powershell
     .\apply-overlay.ps1
     ```
   - **Windows CMD:**
     ```cmd
     apply-overlay.bat
     ```

3. **Build the application:**
   Navigate into the submodule source folder and build the app using Gradle:
   ```bash
   cd syncthing-android-source
   # Build the debug APK
   ./gradlew.bat assembleDebug
   ```
