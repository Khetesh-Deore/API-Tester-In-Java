@echo off
echo Compiling and running the application...

REM Create necessary directories
if not exist "bin" mkdir bin
if not exist "lib" mkdir lib

REM Download JavaFX SDK if not present
if not exist "javafx-sdk" (
    echo Downloading JavaFX SDK...
    powershell -Command "Invoke-WebRequest -Uri 'https://download2.gluonhq.com/openjfx/17/openjfx-17_windows-x64_bin-sdk.zip' -OutFile 'javafx-sdk.zip'"
    powershell -Command "Expand-Archive -Path 'javafx-sdk.zip' -DestinationPath '.'"
    del javafx-sdk.zip
)

REM Download RichTextFX if not present
if not exist "lib\richtextfx-0.11.0.jar" (
    echo Downloading RichTextFX...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/richtext/richtextfx/0.11.0/richtextfx-0.11.0.jar' -OutFile 'lib\richtextfx-0.11.0.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/undo/undofx/2.1.1/undofx-2.1.1.jar' -OutFile 'lib\undofx-2.1.1.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/flowless/flowless/0.7.0/flowless-0.7.0.jar' -OutFile 'lib\flowless-0.7.0.jar'"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/reactfx/reactfx/2.0-M5/reactfx-2.0-M5.jar' -OutFile 'lib\reactfx-2.0-M5.jar'"
)

REM Set JavaFX path
set PATH_TO_FX=javafx-sdk-17\lib

REM Set classpath
set CLASSPATH=bin;lib\*;%PATH_TO_FX%\*

REM Compile
javac --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.web -cp "%CLASSPATH%" -d bin ApiTester.java

REM Run
java --module-path "%PATH_TO_FX%" --add-modules javafx.controls,javafx.web -cp "%CLASSPATH%" ApiTester

pause 