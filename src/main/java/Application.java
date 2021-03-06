import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.sun.net.httpserver.HttpServer;
import exception.GlobalExceptionHandler;
import handler.*;
import model.order.Order;
import model.product.Product;
import model.product.book.Book;
import model.user.User;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import service.BookService;
import service.OrderService;
import service.ProductService;
import service.UserService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static util.Constants.*;


public class Application {

    private static MongoDatabase database;

    public static void main(String[] args) throws IOException {
        configMongo();
    }

    private static void configMongo() throws IOException {

        ConnectionString connectionString = new ConnectionString("mongodb://localhost");

        // direct serialization of POJOs to and from BSON
        // Book.class is registered explicitly because codes are not found otherwise
        CodecRegistry pojoCodecRegistry = fromProviders(
                PojoCodecProvider.builder().register(model.product.book.Book.class,
                        model.product.Product.class).automatic(true).build());

        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        PojoCodecProvider.builder().register(Product.class);

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();

        MongoClient mongoClient = MongoClients.create(clientSettings);
        database = mongoClient.getDatabase(DATABASE);
        configAPI();
    }

    private static void configAPI() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT),0);

        ObjectMapper mapper = Configuration.getObjectMapper();
        GlobalExceptionHandler exceptionHandler = Configuration.getExceptionHandler();

        MongoCollection<Order> orderCollection = database.getCollection(ORDERS_COLLECTION, Order.class);
        MongoCollection<User> userCollection = database.getCollection(USERS_COLLECTION, User.class);
        MongoCollection<Book> bookCollection = database.getCollection(BOOKS_COLLECTION, Book.class);

        orderCollection.createIndex(Indexes.ascending("issuer._id"));

        UserService userService = new UserService(userCollection);
        BookService bookService = new BookService(bookCollection, orderCollection);

        Map<Class<? extends Product>, ProductService> productServices = new HashMap<>();
        productServices.put(Book.class, bookService);
        OrderService orderService = new OrderService(orderCollection, userService, productServices);

        RegistrationHandler registrationHandler = new RegistrationHandler(mapper, exceptionHandler, userService);
        server.createContext("/api/register", registrationHandler::handle);

        LoginHandler loginHandler = new LoginHandler(mapper, exceptionHandler, userService);
        server.createContext("/api/login", loginHandler::handle);

        UserHandler userHandler = new UserHandler(mapper, exceptionHandler, userService);
        server.createContext("/api/users", userHandler::handle);

        OrderHandler orderHandler = new OrderHandler(mapper, exceptionHandler, orderService);
        server.createContext("/api/orders", orderHandler::handle);

        BookHandler productHandler = new BookHandler(mapper, exceptionHandler, bookService);
        server.createContext("/api/products/books", productHandler::handle);

        server.setExecutor(null);
        server.start();
    }
}
