package com.apitester;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.json.JSONObject;
import org.json.JSONArray;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class ApiTester extends Application {
	private TextArea responseBody;
	private TableView<RequestEntry> historyTable;
	private ObservableList<RequestEntry> requestHistory;
	private ComboBox<String> formatSelector;
	private String currentResponse = "";
	private Label statusLabel;
	private Label timeLabel;
	private Label sizeLabel;
	private long requestStartTime;

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("API Tester");

		// Create main layout
		BorderPane mainLayout = new BorderPane();
		mainLayout.setPadding(new Insets(20));
		mainLayout.getStyleClass().add("root");

		// Create left panel for request input
		VBox leftPanel = createRequestPanel();
		leftPanel.getStyleClass().add("request-panel");
		mainLayout.setLeft(leftPanel);

		// Create center panel for response
		VBox centerPanel = createResponsePanel();
		centerPanel.getStyleClass().add("response-panel");
		mainLayout.setCenter(centerPanel);

		// Create right panel for history
		VBox rightPanel = createHistoryPanel();
		rightPanel.getStyleClass().add("history-panel");
		mainLayout.setRight(rightPanel);

		// Create bottom panel for error information
		VBox errorInfoPanel = createErrorInfoPanel();
		errorInfoPanel.getStyleClass().add("error-info-panel");
		mainLayout.setBottom(errorInfoPanel);

		Scene scene = new Scene(mainLayout, 1200, 800);
		scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private VBox createErrorInfoPanel() {
		VBox panel = new VBox(10);
		panel.setPadding(new Insets(20));
		panel.setPrefHeight(200);

		Label title = new Label("HTTP Status Codes and Their Meanings");
		title.getStyleClass().add("title");

		TextArea errorInfo = new TextArea();
		errorInfo.setEditable(false);
		errorInfo.setText(
				"200 OK: The request has succeeded.\n" +
				"400 Bad Request: The server could not understand the request due to invalid syntax.\n" +
				"401 Unauthorized: The client must authenticate itself to get the requested response.\n" +
				"403 Forbidden: The client does not have access rights to the content.\n" +
				"404 Not Found: The server can not find the requested resource.\n" +
				"500 Internal Server Error: The server has encountered a situation it doesn't know how to handle.\n" +
				"502 Bad Gateway: The server was acting as a gateway or proxy and received an invalid response from the upstream server.\n" +
				"503 Service Unavailable: The server is not ready to handle the request."
		);
		errorInfo.setPrefRowCount(8);
		errorInfo.setPrefWidth(800);

		panel.getChildren().addAll(title, errorInfo);

		return panel;
	}

	private VBox createRequestPanel() {
		VBox panel = new VBox(15);
		panel.setPadding(new Insets(20));
		panel.setPrefWidth(400);

		Label title = new Label("Request");
		title.getStyleClass().add("title");

		ComboBox<String> methodSelector = new ComboBox<>();
		methodSelector.getItems().addAll("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"); // Added PATCH, HEAD, OPTIONS
		methodSelector.setValue("GET");
		methodSelector.setPrefWidth(200);

		TextField urlField = new TextField();
		urlField.setPromptText("Enter URL");
		urlField.setPrefWidth(400);

		TextArea requestBody = new TextArea();
		requestBody.setPromptText("Request Body");
		requestBody.setPrefRowCount(10);
		requestBody.setPrefWidth(400);

		Button sendButton = new Button("Send Request");
		sendButton.setPrefWidth(200);
		sendButton.setOnAction(e -> {
			requestStartTime = System.currentTimeMillis();
			sendRequest(methodSelector.getValue(), urlField.getText(), requestBody.getText());
		});

		panel.getChildren().addAll(
				title,
				new Label("Method:"),
				methodSelector,
				new Label("URL:"),
				urlField,
				new Label("Request Body:"),
				requestBody,
				sendButton);

		return panel;
	}

	private VBox createResponsePanel() {
		VBox panel = new VBox(15);
		panel.setPadding(new Insets(20));
		panel.setPrefWidth(400);

		Label title = new Label("Response");
		title.getStyleClass().add("title");

		formatSelector = new ComboBox<>();
		formatSelector.getItems().addAll("Plain", "JSON", "XML", "HTML", "YAML"); // Added HTML and YAML
		formatSelector.setValue("Plain");
		formatSelector.setPrefWidth(200);
		formatSelector.setOnAction(e -> updateResponseFormat());

		responseBody = new TextArea();
		responseBody.setEditable(false);
		responseBody.setPrefRowCount(30);
		responseBody.setPrefWidth(400);
		responseBody.getStyleClass().add("response-area");

		// Preview Button
		Button previewButton = new Button("Preview Response");
		previewButton.setPrefWidth(200);
		previewButton.setOnAction(e -> previewResponse());

		// Status information
		HBox statusBox = new HBox(10);
		statusLabel = new Label();
		timeLabel = new Label();
		sizeLabel = new Label();
		statusBox.getChildren().addAll(statusLabel, timeLabel, sizeLabel);

		panel.getChildren().addAll(
				title,
				new Label("Response Format:"),
				formatSelector,
				statusBox,
				new Label("Response:"),
				responseBody,
				previewButton); // Added preview button

		return panel;
	}

	private VBox createHistoryPanel() {
		VBox panel = new VBox(15);
		panel.setPadding(new Insets(20));
		panel.setPrefWidth(400);

		Label title = new Label("Request History");
		title.getStyleClass().add("title");

		requestHistory = FXCollections.observableArrayList();
		historyTable = new TableView<>(requestHistory);
		historyTable.getStyleClass().add("history-table");
		historyTable.setPrefHeight(600);

		TableColumn<RequestEntry, String> methodCol = new TableColumn<>("Method");
		methodCol.setCellValueFactory(cellData -> cellData.getValue().methodProperty());
		methodCol.setPrefWidth(80);

		TableColumn<RequestEntry, String> urlCol = new TableColumn<>("URL");
		urlCol.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
		urlCol.setPrefWidth(200);

		TableColumn<RequestEntry, String> statusCol = new TableColumn<>("Status");
		statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
		statusCol.setPrefWidth(80);

		historyTable.getColumns().addAll(methodCol, urlCol, statusCol);

		panel.getChildren().addAll(
				title,
				historyTable);

		return panel;
	}

	private void sendRequest(String method, String url, String body) {
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Content-Type", "application/json"); // Ensure JSON content type

			switch (method) {
				case "GET":
					requestBuilder.GET();
					break;
				case "POST":
					requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
					break;
				case "PUT":
					requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
					break;
				case "PATCH":
					requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body));
					break;
				case "DELETE":
					requestBuilder.DELETE();
					break;
				case "HEAD":
					requestBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody()); // Added HEAD
					break;
				case "OPTIONS":
					requestBuilder.method("OPTIONS", HttpRequest.BodyPublishers.noBody()); // Added OPTIONS
					break;
			}

			CompletableFuture<HttpResponse<String>> response = client.sendAsync(
					requestBuilder.build(),
					HttpResponse.BodyHandlers.ofString());

			response.thenAccept(r -> {
				currentResponse = r.body();
				updateResponseFormat();
				requestHistory.add(new RequestEntry(method, url, String.valueOf(r.statusCode())));

				// Update status information
				long duration = System.currentTimeMillis() - requestStartTime;
				statusLabel.setText("Status: " + r.statusCode());
				statusLabel.getStyleClass()
						.add(r.statusCode() >= 200 && r.statusCode() < 300 ? "status-success" : "status-error");
				timeLabel.setText("Time: " + duration + "ms");
				sizeLabel.setText("Size: " + formatSize(r.body().length()));
			});
		} catch (Exception e) {
			statusLabel.setText("Error: " + e.getMessage());
			statusLabel.getStyleClass().add("status-error");
		}
	}

	private String formatSize(long bytes) {
		if (bytes < 1024)
			return bytes + " B";
		if (bytes < 1024 * 1024)
			return String.format("%.2f KB", bytes / 1024.0);
		return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
	}

	private void updateResponseFormat() {
		try {
			String formattedResponse = currentResponse;
			switch (formatSelector.getValue()) {
				case "JSON":
					formattedResponse = formatJson(currentResponse);
					break;
				case "XML":
					formattedResponse = formatXml(currentResponse);
					break;
				case "HTML":
					formattedResponse = formatHtml(currentResponse); // Added HTML formatting
					break;
				case "YAML":
					formattedResponse = formatYaml(currentResponse); // Added YAML formatting
					break;
			}
			responseBody.setText(formattedResponse);
		} catch (Exception e) {
			responseBody.setText("Error formatting response: " + e.getMessage());
		}
	}

	private String formatHtml(String html) {
		// Implement HTML formatting logic here
		return html;
	}

	private String formatYaml(String yaml) {
		// Implement YAML formatting logic here
		return yaml;
	}

	private void previewResponse() {
		// Implement preview logic here, possibly opening a new window or dialog
	}

	private String formatJson(String json) {
		try {
			return new JSONObject(json).toString(4);
		} catch (Exception e) {
			try {
				return new JSONArray(json).toString(4);
			} catch (Exception e2) {
				return json;
			}
		}
	}

	private String formatXml(String xml) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			Source source = new StreamSource(new StringReader(xml));
			StringWriter writer = new StringWriter();
			transformer.transform(source, new StreamResult(writer));
			return writer.toString();
		} catch (Exception e) {
			return xml;
		}
	}

	public static class RequestEntry {
		private final String method;
		private final String url;
		private final String status;

		public RequestEntry(String method, String url, String status) {
			this.method = method;
			this.url = url;
			this.status = status;
		}

		public javafx.beans.property.StringProperty methodProperty() {
			return new javafx.beans.property.SimpleStringProperty(method);
		}

		public javafx.beans.property.StringProperty urlProperty() {
			return new javafx.beans.property.SimpleStringProperty(url);
		}

		public javafx.beans.property.StringProperty statusProperty() {
			return new javafx.beans.property.SimpleStringProperty(status);
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}