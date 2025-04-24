@echo off
echo Downloading JavaFX SDK...

REM Clean up previous download
if exist "javafx-sdk-17" rmdir /s /q javafx-sdk-17
if exist "javafx-sdk.zip" del javafx-sdk.zip

REM Download JavaFX SDK
powershell -Command "$ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://download2.gluonhq.com/openjfx/17/openjfx-17_windows-x64_bin-sdk.zip' -OutFile 'javafx-sdk.zip'"

if exist "javafx-sdk.zip" (
    echo Extracting JavaFX SDK...
    powershell -Command "Expand-Archive -Path 'javafx-sdk.zip' -DestinationPath '.' -Force"
    del javafx-sdk.zip
    
    if exist "javafx-sdk-17\lib\javafx.controls.jar" (
        echo JavaFX SDK downloaded successfully!
    ) else (
        echo JavaFX SDK extraction failed!
    )
) else (
    echo Failed to download JavaFX SDK!
)

pause 