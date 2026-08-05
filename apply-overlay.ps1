# Initialize and update the submodule recursively
Write-Host "Updating Git submodules..."
git submodule update --init --recursive

# Copy overlay files on top of the submodule
Write-Host "Applying overlay modifications..."
if (Test-Path "overlay") {
    Copy-Item -Path "overlay\*" -Destination "syncthing-android-source\" -Recurse -Force
    Write-Host "Overlay successfully applied!"
} else {
    Write-Warning "Overlay directory not found!"
}

# Ensure local.properties is copied into the submodule for build configuration
if (Test-Path "local.properties") {
    Copy-Item "local.properties" "syncthing-android-source\local.properties" -Force
    Write-Host "Copied local.properties to submodule."
} else {
    $sdkPath = "sdk.dir=C\:\\Users\\akash\\AppData\\Local\\Android\\Sdk"
    $sdkPath | Out-File -FilePath "syncthing-android-source\local.properties" -Encoding utf8
    Write-Host "Generated local.properties in submodule."
}
