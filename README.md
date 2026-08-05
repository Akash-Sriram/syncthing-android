# Syncthing Android - Overlay Customizations

This repository contains custom modifications for the Syncthing Android app (specifically, an in-app downloader to get the latest stable and pre-release releases directly from the GitHub releases page).

Instead of maintaining a complex fork branch, this project uses an **Overlay Structure**. The official `researchxxl/syncthing-android` repository is tracked as a Git submodule, and our custom files are copied on top of it.

## Repository Structure

- `overlay/` - Contains our customized and new files matching the structure of the official project.
- `syncthing-android-source/` - The Git submodule containing the official Syncthing Android codebase.
- `apply-overlay.ps1` / `apply-overlay.bat` - Scripts to pull the submodule and apply the overlay modifications.


