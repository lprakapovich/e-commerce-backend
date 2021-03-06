package handler;

import api.Method;
import api.Response;
import api.StatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import exception.BadRequestException;
import exception.GlobalExceptionHandler;
import model.product.book.Book;
import org.bson.types.ObjectId;
import org.codehaus.plexus.util.StringUtils;
import service.BookService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static api.Message.CANT_RESOLVE_HTTP_METHOD;
import static util.Constants.ID;
import static util.Utils.getIdFromPath;
import static util.Utils.splitQueryList;

public class BookHandler extends Handler {

    private final BookService bookService;

    public BookHandler(ObjectMapper objectMapper, GlobalExceptionHandler exceptionHandler, BookService service) {
        super(objectMapper, exceptionHandler);
        this.bookService = service;
    }

    protected byte[] resolveRequest(HttpExchange exchange) throws IOException {
        return handleBookRequests(exchange, splitQueryList(exchange.getRequestURI().getRawQuery()));
    }

    private byte[] handleBookRequests(HttpExchange exchange, Map<String, List<String>> params) throws IOException {
        byte[] response;
        Method method = Method.valueOf(exchange.getRequestMethod());
        switch (method) {
            case GET:
                Response<Object> get = handleGet(exchange, params);
                response = getResponseBodyAsBytes(get, exchange);
                break;
            case POST:
                Response<String> post = handlePost(exchange);
                response = getResponseBodyAsBytes(post, exchange);
                break;
            case PUT:
                Response<Book> put = handlePut(exchange);
                response = getResponseBodyAsBytes(put, exchange);
                break;
            case DELETE:
                Response delete = handleDelete(params);
                response = getResponseBodyAsBytes(delete, exchange);
                break;
            default:
                throw new BadRequestException(CANT_RESOLVE_HTTP_METHOD);
        }

        return response;
    }

    private Response<Object> handleGet(HttpExchange exchange, Map<String, List<String>> params) {
        String idFromPath = getIdFromPath(exchange.getRequestURI().getRawPath());
              return Response.builder()
                .headers(getHeaders())
                .body(StringUtils.isNotBlank(idFromPath) ? bookService.get(new ObjectId(idFromPath)) : bookService.find(params))
                .status(StatusCode.OK)
                .build();
    }

    private Response<String> handlePost(HttpExchange exchange) {
        Book book = readRequestBody(exchange.getRequestBody(), Book.class);
        String newId = bookService.create(book);
        return Response.<String>builder()
                .headers(getHeaders())
                .status(StatusCode.CREATED)
                .body(newId)
                .build();
    }

    private Response<Book> handlePut(HttpExchange exchange) {
        Book updated = bookService.update(readRequestBody(exchange.getRequestBody(), Book.class));
        return Response.<Book>builder()
                .body(updated)
                .headers(getHeaders())
                .status(StatusCode.OK)
                .build();
    }

    // TODO refactor
    private Response handleDelete(Map<String, List<String>> params) {
        String id = params.get(ID).get(0);
        bookService.delete(id);
        return Response.builder()
                .status(StatusCode.NO_CONTENT)
                .build();
    }
}
