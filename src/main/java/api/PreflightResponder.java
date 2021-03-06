package api;

import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpStatus;

import java.io.IOException;

import static util.Constants.*;

public class PreflightResponder {
    public static void sendPreflightCheckResponse(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add(ALLOW_ORIGIN, ALL);
        exchange.getResponseHeaders().add(ALLOW_METHODS, ALLOWED_METHODS);
        exchange.getResponseHeaders().add(ALLOW_HEADERS, ALLOWED_HEADERS);
        exchange.sendResponseHeaders(HttpStatus.SC_ACCEPTED, -1);
    }
}
