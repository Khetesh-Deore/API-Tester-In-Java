# API Tester

A simple JavaFX application for testing REST APIs. This tool allows you to send HTTP requests (GET, POST, PUT, DELETE) and view formatted responses in plain text, JSON, or XML format.

## Prerequisites

- Java 17 or later
- Maven 3.6 or later

## Building the Application

To build the application, run the following command in the project root directory:

```bash
mvn clean package
```

## Running the Application

After building, you can run the application using:

```bash
mvn javafx:run
```

## Features

- Support for GET, POST, PUT, and DELETE HTTP methods
- Request history tracking
- Response formatting for Plain Text, JSON, and XML
- Easy-to-use graphical interface
- Request headers and body input
- Response status code display

## Usage

1. Select the HTTP method from the dropdown menu
2. Enter the URL for your API endpoint
3. Add any required headers in the Headers text area (one header per line in "Key: Value" format)
4. For POST/PUT requests, enter the request body in the Request Body text area
5. Click "Send Request" to make the API call
6. View the formatted response in the Response Body area
7. Check the request history in the table below

## Note

This is a basic API testing tool and may not include all features found in commercial API testing solutions. It's designed for simple API testing and educational purposes. 