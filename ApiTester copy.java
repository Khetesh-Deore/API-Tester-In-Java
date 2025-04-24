import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class ApiTester {
    // ANSI colors and styles
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String GRAY = "\u001B[90m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    // Table characters
    private static final String T_TOP_LEFT = "┌";
    private static final String T_TOP_RIGHT = "┐";
    private static final String T_BOTTOM_LEFT = "└";
    private static final String T_BOTTOM_RIGHT = "┘";
    private static final String T_HORIZONTAL = "─";
    private static final String T_VERTICAL = "│";
    private static final String T_LEFT_MIDDLE = "├";
    private static final String T_RIGHT_MIDDLE = "┤";
    private static final String T_TOP_MIDDLE = "┬";
    private static final String T_BOTTOM_MIDDLE = "┴";
    private static final String T_CROSS = "┼";
    
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        try {
            String method = args[0].toUpperCase();
            String url = args[1];
            Map<String, String> headers = new HashMap<>();
            String body = null;

            // Parse arguments
            for (int i = 2; i < args.length; i++) {
                if (args[i].contains(":")) {
                    String[] header = args[i].split(":", 2);
                    headers.put(header[0].trim(), header[1].trim());
                } else {
                    body = args[i];
                }
            }

            long startTime = System.nanoTime();
            makeRequest(method, url, headers, body);
            double duration = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf(GRAY + "⌛ %.2fs%n" + RESET, duration);

        } catch (Exception e) {
            System.out.println(RED + "✘ " + e.getMessage() + RESET);
            System.exit(1);
        }
    }

    private static void makeRequest(String method, String url, Map<String, String> headers, String body) throws Exception {
        // Print request table
        printRequestTable(method, url, headers, body);

        // Build request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url));

        // Add headers
        if (!headers.isEmpty()) {
            headers.forEach(requestBuilder::header);
        }
        if (body != null && !headers.containsKey("Content-Type")) {
            requestBuilder.header("Content-Type", "application/json");
        }

        // Set method and body
        switch (method) {
            case "GET" -> requestBuilder.GET();
            case "DELETE" -> requestBuilder.DELETE();
            case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            case "PATCH" -> requestBuilder.method("PATCH", HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }

        // Execute request and print response
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), 
                HttpResponse.BodyHandlers.ofString());
        printResponseTable(response);
    }

    private static void printRequestTable(String method, String url, Map<String, String> headers, String body) {
        int width = Math.max(80, url.length() + 10);
        
        // Table header
        System.out.println();
        printTableRow(width, BOLD + "REQUEST" + RESET, true, true);
        
        // Method and URL
        printTableRow(width, CYAN + BOLD + method + RESET + " " + url, false, false);
        
        // Headers if present
        if (!headers.isEmpty()) {
            printTableDivider(width, false);
            printTableRow(width, BOLD + "Headers" + RESET, false, false);
            headers.forEach((key, value) -> 
                printTableRow(width, BLUE + key + RESET + ": " + value, false, false));
        }
        
        // Body if present
        if (body != null) {
            printTableDivider(width, false);
            printTableRow(width, BOLD + "Body" + RESET, false, false);
            Arrays.stream(formatJson(body).split("\n"))
                  .forEach(line -> printTableRow(width, line, false, false));
        }
        
        // Table footer
        printTableDivider(width, true);
    }

    private static void printResponseTable(HttpResponse<String> response) {
        int width = 80;
        String statusColor = response.statusCode() >= 200 && response.statusCode() < 300 ? GREEN :
                           response.statusCode() >= 400 ? RED : YELLOW;
        
        // Table header
        System.out.println();
        printTableRow(width, BOLD + "RESPONSE" + RESET, true, true);
        
        // Status
        printTableRow(width, statusColor + response.statusCode() + " " + 
                     getStatusText(response.statusCode()) + RESET, false, false);
        
        // Essential headers only
        Map<String, List<String>> headers = response.headers().map();
        List<String> essentialHeaders = Arrays.asList(
            "content-type", "content-length", "location", "authorization", "server", "date"
        );
        
        boolean hasEssentialHeaders = headers.keySet().stream()
            .anyMatch(header -> essentialHeaders.contains(header.toLowerCase()));
            
        if (hasEssentialHeaders) {
            printTableDivider(width, false);
            printTableRow(width, BOLD + "Headers" + RESET, false, false);
            headers.forEach((key, values) -> {
                if (essentialHeaders.contains(key.toLowerCase())) {
                    String headerValue = String.join(", ", values);
                    // Truncate long values
                    if (headerValue.length() > 50) {
                        headerValue = headerValue.substring(0, 47) + "...";
                    }
                    printTableRow(width, GRAY + key + RESET + ": " + headerValue, false, false);
                }
            });
        }
        
        // Response body
        if (!response.body().isEmpty()) {
            printTableDivider(width, false);
            printTableRow(width, BOLD + "Body" + RESET, false, false);
            String responseBody = response.body();
            if (responseBody.startsWith("{") || responseBody.startsWith("[")) {
                Arrays.stream(formatJson(responseBody).split("\n"))
                      .forEach(line -> printTableRow(width, line, false, false));
            } else {
                // Truncate long non-JSON responses
                if (responseBody.length() > 200) {
                    responseBody = responseBody.substring(0, 197) + "...";
                }
                printTableRow(width, responseBody, false, false);
            }
        }
        
        // Table footer
        printTableDivider(width, true);
    }

    private static void printTableRow(int width, String content, boolean isHeader, boolean isTop) {
        StringBuilder row = new StringBuilder();
        
        // Left border
        if (isTop) {
            row.append(T_TOP_LEFT);
        } else if (isHeader) {
            row.append(T_LEFT_MIDDLE);
        } else {
            row.append(T_VERTICAL);
        }
        
        // Content
        String paddedContent = String.format(" %-" + (width - 2) + "s ", content);
        row.append(paddedContent);
        
        // Right border
        if (isTop) {
            row.append(T_TOP_RIGHT);
        } else if (isHeader) {
            row.append(T_RIGHT_MIDDLE);
        } else {
            row.append(T_VERTICAL);
        }
        
        System.out.println(row);
    }

    private static void printTableDivider(int width, boolean isBottom) {
        StringBuilder divider = new StringBuilder();
        divider.append(isBottom ? T_BOTTOM_LEFT : T_LEFT_MIDDLE)
               .append(T_HORIZONTAL.repeat(width))
               .append(isBottom ? T_BOTTOM_RIGHT : T_RIGHT_MIDDLE);
        System.out.println(divider);
    }

    private static String formatJson(String json) {
        StringBuilder formatted = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        char[] chars = json.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if (c == '"' && (i == 0 || chars[i-1] != '\\')) {
                inString = !inString;
                formatted.append(inString ? YELLOW : RESET).append(c);
                continue;
            }
            
            if (!inString) {
                switch (c) {
                    case '{', '[' -> {
                        formatted.append(BLUE).append(c).append(RESET)
                                 .append('\n').append("  ".repeat(++indent));
                    }
                    case '}', ']' -> {
                        formatted.append('\n').append("  ".repeat(--indent))
                                 .append(BLUE).append(c).append(RESET);
                    }
                    case ',' -> {
                        formatted.append(c).append('\n').append("  ".repeat(indent));
                    }
                    case ':' -> {
                        formatted.append(c).append(' ');
                    }
                    default -> formatted.append(c);
                }
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }

    private static String getStatusText(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }

    private static void printUsage() {
        int width = 80;
        
        // Print usage in a table
        printTableRow(width, BOLD + "HTTPie-style API Tester" + RESET, true, true);
        printTableRow(width, GRAY + "A lightweight terminal API client" + RESET, false, false);
        printTableDivider(width, false);
        
        printTableRow(width, BOLD + "Usage:" + RESET, false, false);
        printTableRow(width, CYAN + "  java ApiTester" + RESET + " METHOD URL [HEADERS...] [BODY]", false, false);
        printTableDivider(width, false);
        
        printTableRow(width, BOLD + "Examples:" + RESET, false, false);
        printTableRow(width, GRAY + "  # GET request" + RESET, false, false);
        printTableRow(width, "  java ApiTester get api.example.com/users/1", false, false);
        printTableRow(width, GRAY + "  # POST with JSON" + RESET, false, false);
        printTableRow(width, "  java ApiTester post api.example.com/users '{\"name\":\"John\"}'", false, false);
        printTableRow(width, GRAY + "  # With headers" + RESET, false, false);
        printTableRow(width, "  java ApiTester get api.example.com/users 'Authorization: Bearer token'", false, false);
        
        printTableDivider(width, true);
    }
} 