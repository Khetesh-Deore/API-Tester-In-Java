package src.main.java.com.apitester;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("API Tester");

		// Create main layout
		BorderPane mainLayout = new BorderPane();

		// Create left panel for request input
		VBox leftPanel = createRequestPanel();
		mainLayout.setLeft(leftPanel);

		// Create center panel for response
		VBox centerPanel = createResponsePanel();
		mainLayout.setCenter(centerPanel);

		// Create right panel for history
		VBox rightPanel = createHistoryPanel();
		mainLayout.setRight(rightPanel);

		Scene scene = new Scene(mainLayout, 1200, 800);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private VBox createRequestPanel() {
		VBox panel = new VBox(10);
		panel.setPadding(new Insets(10));
		panel.setPrefWidth(400);

		ComboBox<String> methodSelector = new ComboBox<>();
		methodSelector.getItems().addAll("GET", "POST", "PUT", "DELETE");
		methodSelector.setValue("GET");

		TextField urlField = new TextField();
		urlField.setPromptText("Enter URL");

		TextArea requestBody = new TextArea();
		requestBody.setPromptText("Request Body");
		requestBody.setPrefRowCount(10);

		Button sendButton = new Button("Send Request");
		sendButton.setOnAction(e -> sendRequest(methodSelector.getValue(), urlField.getText(), requestBody.getText()));

		panel.getChildren().addAll(
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
		VBox panel = new VBox(10);
		panel.setPadding(new Insets(10));
		panel.setPrefWidth(400);

		formatSelector = new ComboBox<>();
		formatSelector.getItems().addAll("Plain", "JSON", "XML");
		formatSelector.setValue("Plain");
		formatSelector.setOnAction(e -> updateResponseFormat());

		responseBody = new TextArea();
		responseBody.setEditable(false);
		responseBody.setPrefRowCount(30);

		panel.getChildren().addAll(
				new Label("Response Format:"),
				formatSelector,
				new Label("Response:"),
				responseBody);

		return panel;
	}

	private VBox createHistoryPanel() {
		VBox panel = new VBox(10);
		panel.setPadding(new Insets(10));
		panel.setPrefWidth(400);

		requestHistory = FXCollections.observableArrayList();
		historyTable = new TableView<>(requestHistory);

		TableColumn<RequestEntry, String> methodCol = new TableColumn<>("Method");
		methodCol.setCellValueFactory(cellData -> cellData.getValue().methodProperty());

		TableColumn<RequestEntry, String> urlCol = new TableColumn<>("URL");
		urlCol.setCellValueFactory(cellData -> cellData.getValue().urlProperty());

		TableColumn<RequestEntry, String> statusCol = new TableColumn<>("Status");
		statusCol.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

		historyTable.getColumns().addAll(methodCol, urlCol, statusCol);
		historyTable.setPrefHeight(600);

		panel.getChildren().addAll(
				new Label("Request History:"),
				historyTable);

		return panel;
	}

	private void sendRequest(String method, String url, String body) {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(url));

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
			case "DELETE":
				requestBuilder.DELETE();
				break;
		}

		CompletableFuture<HttpResponse<String>> response = client.sendAsync(
				requestBuilder.build(),
				HttpResponse.BodyHandlers.ofString());

		response.thenAccept(r -> {
			currentResponse = r.body();
			updateResponseFormat();
			requestHistory.add(new RequestEntry(method, url, String.valueOf(r.statusCode())));
		});
	}

	private void updateResponseFormat() {
		if (currentResponse.isEmpty())
			return;

		try {
			switch (formatSelector.getValue()) {
				case "JSON":
					try {
						if (currentResponse.trim().startsWith("[")) {
							JSONArray jsonArray = new JSONArray(currentResponse);
							responseBody.setText(jsonArray.toString(2));
						} else {
							JSONObject jsonObject = new JSONObject(currentResponse);
							responseBody.setText(jsonObject.toString(2));
						}
					} catch (Exception e) {
						responseBody.setText("Invalid JSON format: " + e.getMessage());
					}
					break;

				case "XML":
					try {
						Source xmlInput = new StreamSource(new StringReader(currentResponse));
						StringWriter stringWriter = new StringWriter();
						StreamResult xmlOutput = new StreamResult(stringWriter);
						TransformerFactory transformerFactory = TransformerFactory.newInstance();
						Transformer transformer = transformerFactory.newTransformer();
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
						transformer.transform(xmlInput, xmlOutput);
						responseBody.setText(xmlOutput.getWriter().toString());
					} catch (Exception e) {
						responseBody.setText("Invalid XML format: " + e.getMessage());
					}
					break;

				default:
					responseBody.setText(currentResponse);
					break;
			}
		} catch (Exception e) {
			responseBody.setText("Error formatting response: " + e.getMessage());
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