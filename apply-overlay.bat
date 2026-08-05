@echo off
echo Updating Git submodules...
git submodule update --init --recursive

echo Applying overlay modifications...
xcopy /E /Y /Q overlay\* syncthing-android-source\

if exist local.properties (
    copy /Y local.properties syncthing-android-source\local.properties > nul
    echo Copied local.properties to submodule.
) else (
    echo sdk.dir=C\:\\Users\\akash\\AppData\\Local\\Android\\Sdk > syncthing-android-source\local.properties
    echo Generated local.properties in submodule.
)
echo Overlay successfully applied!
