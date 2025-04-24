@echo off
setlocal enabledelayedexpansion

echo Starting API Tester...

REM Clean up previous build
if exist build rd /s /q build
if exist lib rd /s /q lib
if exist src rd /s /q src
mkdir lib
mkdir build
mkdir src\com\apitester

REM Create ApiTester.java
echo Creating ApiTester.java...
(
echo package com.apitester;
echo.
echo import javafx.application.Application;
echo import javafx.geometry.Insets;
echo import javafx.scene.Scene;
echo import javafx.scene.control.*;
echo import javafx.scene.control.cell.PropertyValueFactory;
echo import javafx.scene.control.cell.TextFieldTableCell;
echo import javafx.scene.layout.*;
echo import javafx.stage.Stage;
echo import javafx.scene.paint.Color;
echo import javafx.scene.text.Font;
echo import javafx.scene.text.FontWeight;
echo import javafx.collections.FXCollections;
echo import javafx.collections.ObservableList;
echo import javafx.concurrent.Task;
echo import javafx.scene.input.KeyCode;
echo import javafx.scene.input.KeyEvent;
echo import javafx.scene.web.WebView;
echo import javafx.scene.web.WebEngine;
echo import org.fxmisc.richtext.CodeArea;
echo import org.fxmisc.richtext.LineNumberFactory;
echo import org.fxmisc.richtext.model.StyleSpans;
echo import org.fxmisc.richtext.model.StyleSpansBuilder;
echo import java.util.regex.Matcher;
echo import java.util.regex.Pattern;
echo import java.util.*;
echo import java.net.URI;
echo import java.net.http.HttpClient;
echo import java.net.http.HttpRequest;
echo import java.net.http.HttpResponse;
echo import java.time.Duration;
echo import java.time.LocalDateTime;
echo import java.util.stream.Collectors;
echo.
echo public class ApiTester extends Application {
echo     private final HttpClient httpClient = HttpClient.newBuilder^^()
echo             .version^(HttpClient.Version.HTTP_2^)
echo             .connectTimeout^(Duration.ofSeconds^(10^)^)
echo             .build^(^);
echo.
echo     private final ObservableList^<RequestHistory^> history = FXCollections.observableArrayList^(^);
echo     private final List^<RequestHistory^> requestHistory = new ArrayList^<^>^(^);
echo     private final Map^<String, String^> environmentVariables = new HashMap^<^>^(^);
echo.
echo     private CodeArea requestBodyArea;
echo     private CodeArea responseBodyArea;
echo     private WebView responseView;
echo     private WebEngine webEngine;
echo     private TableView^<RequestHistory^> historyTable;
echo     private TextField urlField;
echo     private ComboBox^<String^> methodComboBox;
echo     private ComboBox^<String^> requestFormatComboBox;
echo     private ComboBox^<String^> responseFormatComboBox;
echo     private TableView^<Header^> headersTable;
echo     private TableView^<FormData^> formDataTable;
echo     private Label statusLabel;
echo     private Label timeLabel;
echo     private Label sizeLabel;
echo     private TabPane requestBodyTabPane;
echo     private long requestStartTime;
echo.
echo     @Override
echo     public void start^(Stage primaryStage^) {
echo         primaryStage.setTitle^("API Tester - Postman Style"^);
echo         BorderPane mainLayout = new BorderPane^(^);
echo         mainLayout.setPadding^(new Insets^(10^)^);
echo         VBox requestPanel = createRequestPanel^(^);
echo         mainLayout.setTop^(requestPanel^);
echo         TabPane responsePanel = createResponsePanel^(^);
echo         mainLayout.setCenter^(responsePanel^);
echo         VBox historyPanel = createHistoryPanel^(^);
echo         mainLayout.setRight^(historyPanel^);
echo         Scene scene = new Scene^(mainLayout, 1200, 800^);
echo         scene.getStylesheets^(^).add^("file:" + System.getProperty^("user.dir"^) + "/styles.css"^);
echo         scene.addEventHandler^(KeyEvent.KEY_PRESSED, event -^> {
echo             if ^(event.getCode^(^) == KeyCode.ENTER ^&^& event.isControlDown^(^)^) {
echo                 sendRequest^(^);
echo             }
echo         }^);
echo         primaryStage.setScene^(scene^);
echo         primaryStage.show^(^);
echo     }
echo.
echo     public static void main^(String[] args^) {
echo         launch^(args^);
echo     }
echo }
) > src\com\apitester\ApiTester.java

REM Download JavaFX SDK
if not exist javafx-sdk.zip (
    echo Downloading JavaFX SDK...
    powershell -Command "Invoke-WebRequest -Uri 'https://download2.gluonhq.com/openjfx/21.0.1/openjfx-21.0.1_windows-x64_bin-sdk.zip' -OutFile 'javafx-sdk.zip'"
    powershell -Command "Expand-Archive -Path 'javafx-sdk.zip' -DestinationPath '.' -Force"
    move javafx-sdk-21.0.1 javafx-sdk
)

REM Download RichTextFX and dependencies
echo Downloading RichTextFX and dependencies...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/richtext/richtextfx/0.11.2/richtextfx-0.11.2.jar' -OutFile 'lib/richtextfx-0.11.2.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/wellbehaved/wellbehavedfx/0.3.3/wellbehavedfx-0.3.3.jar' -OutFile 'lib/wellbehavedfx-0.3.3.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/flowless/flowless/0.7.0/flowless-0.7.0.jar' -OutFile 'lib/flowless-0.7.0.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/fxmisc/undo/undofx/2.1.1/undofx-2.1.1.jar' -OutFile 'lib/undofx-2.1.1.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/reactfx/reactfx/2.0-M5/reactfx-2.0-M5.jar' -OutFile 'lib/reactfx-2.0-M5.jar'"

REM Download JSON and XML processing libraries
echo Downloading JSON and XML processing libraries...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar' -OutFile 'lib/jackson-databind-2.15.2.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar' -OutFile 'lib/jackson-core-2.15.2.jar'"
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar' -OutFile 'lib/jackson-annotations-2.15.2.jar'"

REM Verify all required files are present
if not exist javafx-sdk (
    echo Error: JavaFX SDK not found
    pause
    exit /b 1
)

REM Compile the application
echo Compiling application...
javac -d build ^
      --module-path javafx-sdk/lib ^
      --add-modules javafx.controls,javafx.web ^
      -cp "lib/*" ^
      src/com/apitester/ApiTester.java

if %ERRORLEVEL% neq 0 (
    echo Compilation failed
    pause
    exit /b 1
)

REM Copy resources
copy styles.css build\

REM Run the application
echo Starting application...
java -cp "build;lib/*" ^
     --module-path javafx-sdk/lib ^
     --add-modules javafx.controls,javafx.web ^
     --enable-native-access=javafx.graphics ^
     com.apitester.ApiTester

if %ERRORLEVEL% neq 0 (
    echo Application failed to start
    pause
    exit /b 1
)

pause