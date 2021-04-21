/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.file.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import labs.client.ResourceFormatter;
import labs.pm.data.Drink;
import labs.pm.data.Food;
import labs.pm.data.Product;
import labs.pm.data.Rateable;
import labs.pm.data.Rating;
import labs.pm.data.Review;
import labs.pm.service.ProductManager;
import labs.pm.service.ProductManagerException;

/**
 *
 * @author antoi
 */
public class ProductFileManager implements ProductManager{

    private static final Logger logger = Logger.getLogger(ProductManager.class.getName());

    private final ResourceBundle config = ResourceBundle.getBundle("labs.file.service.config");
    
    private final Path reportsFolder = Path.of(config.getString("reports.folder"));
    private final Path dataFolder = Path.of(config.getString("data.folder"));
    private final Path tempFolder = Path.of(config.getString("temp.folder"));

    private final MessageFormat reviewFormat = new MessageFormat(config.getString("review.data.format"));
    private final MessageFormat productFormat = new MessageFormat(config.getString("product.data.format"));
    
    private static final Map<String, ResourceFormatter> formatters = Map.of(
        "en-GB", new ResourceFormatter(Locale.UK),
        "en-US", new ResourceFormatter(Locale.US),
        "fr-FR", new ResourceFormatter(Locale.FRANCE),
        "ru-RU", new ResourceFormatter(new Locale("ru", "RU")),
        "zh-CN", new ResourceFormatter(Locale.CHINA)
    );

    private Map<Product, List<Review>> products = new HashMap<>();

    public ProductFileManager() {
        loadAllData();
    }

    @Override
    public Product createProduct(int id, String name, BigDecimal price, Rating rating) throws ProductManagerException {
        Product product = new Drink(id, name, price, rating);
        products.putIfAbsent(product, new ArrayList<>());
        return product;
    }

    @Override
    public Product createProduct(int id, String name, BigDecimal price, Rating rating, LocalDate bestBefore) throws ProductManagerException {
        Product product = new Food(id, name, price, rating, bestBefore);
        products.putIfAbsent(product, new ArrayList<>());
        return product;
    }

    @Override
    public Product reviewProduct(int id, Rating rating, String comments) throws ProductManagerException {
        return reviewProduct(findProduct(id), rating, comments);
    }
    
    @Override
    public Product findProduct(int id) throws ProductManagerException {
        return products.keySet().stream()
            .filter(p -> p.getId() == id)
            .findFirst()
            .orElseThrow(() -> new ProductManagerException("Product with id "+id+" not found."));
    }

    @Override
    public List<Product> findProducts(Predicate<Product> filter) throws ProductManagerException {
        return products.keySet().stream()
            .filter(filter)
            .collect(Collectors.toList());
    }

    @Override
    public List<Review> findReviews(int id) throws ProductManagerException {
        return products.get(findProduct(id));
    }

    @Override
    public Map<Rating, BigDecimal> getDiscounts() throws ProductManagerException {
        return products.keySet().stream()
            .collect(
                Collectors.groupingBy(
                    product -> product.getRating(), 
                    Collectors.collectingAndThen(
                        Collectors.summingDouble(product -> product.getDiscount().doubleValue()), 
                        discount -> BigDecimal.valueOf(discount)
                    )
                )
            );
    }
    
    private Product reviewProduct(Product product, Rating rating, String comments) {
        List<Review> reviews = products.get(product);
        products.remove(product, reviews);
        reviews.add(new Review(rating, comments));
        
        Double average = reviews.stream()
            .mapToInt(r -> r.getRating().ordinal())
            .average()
            .orElse(0);
        
        product = product.applyRating(Rateable.convert((int) Math.round(average)));
        products.put(product, reviews);
        
        return product;
    }
    
    private Review parseReview(String text) {
        Review review = null;
        try {
            Object[] values = reviewFormat.parse(text);
            review = new Review(Rateable.convert(Integer.parseInt((String)values[0])), (String)values[1]);
        } catch (ParseException | NumberFormatException ex) {
            logger.log(Level.WARNING, "Error parsing text: '{0}'", text);
        }
        return review;
    }
    
    private Product parseProduct(String text) {
        Product product = null;
        try {
            Object[] values = productFormat.parse(text);
            int id = Integer.parseInt((String)values[1]);
            String name = (String) values[2];
            BigDecimal price = BigDecimal.valueOf(Double.parseDouble((String)values[3]));
            Rating rating = Rateable.convert(Integer.parseInt((String) values[4]));
            switch ((String)values[0]) {
                case "D":
                    product = new Drink(id, name, price, rating);
                    break;
                case "F":
                    LocalDate bestBefore = LocalDate.parse((String) values[5]);
                    product = new Food(id, name, price, rating, bestBefore);
            }
        } catch (ParseException | NumberFormatException | DateTimeParseException ex) {
            logger.log(Level.WARNING, "Error parsing product '"+text+"'.", ex);
        }
        return product;
    }
    
    private void loadAllData() {
        try {
            products = Files.list(dataFolder)
                .filter(f -> f.toString().contains("product"))
//                    .peek(f -> System.out.println(" +++ "+f))
                .map(f -> loadProduct(f))
                .filter(p -> p != null)
                .collect(Collectors.toMap(product -> product, product -> loadReviews(product)));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error loading data.", ex);
        }
    }
    
    private Product loadProduct(Path file) {
        Product product = null;
        try {
            product = parseProduct(Files.lines(dataFolder.resolve(file), Charset.forName("UTF-8"))
                    .findFirst().orElseThrow());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error parsing product.", ex);
        }
        return product;
    }
    
    private List<Review> loadReviews(Product product) {
        List<Review> reviews = null;
        Path file = dataFolder.resolve(MessageFormat.format(config.getString("reviews.data.file"), product.getId()));
        if (Files.notExists(file)) {
            reviews = new ArrayList<>(0);
        } else {
            try {
                reviews = Files.lines(file, Charset.forName("UTF-8"))
                        .map(text -> parseReview(text))
                        .filter(review -> review != null)
                        .collect(Collectors.toList());
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error loading reviews.", ex);
            }
        }
        return reviews;
    }
    
    private void dumpData() {
        try {
            if (Files.notExists(tempFolder)) {
                Files.createDirectory(tempFolder);
            }
            Path tempFile = tempFolder.resolve(MessageFormat.format(config.getString("temp.file"), Instant.now().getEpochSecond()));
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tempFile, StandardOpenOption.CREATE))) {
                out.writeObject(products);
                //products = new HashMap<>();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error dumping data.", e);
        }
    }
    
    private void restoreData() {
        try {
            Path tempFile = Files.list(tempFolder)
                .filter(f -> f.getFileName().toString().endsWith("tmp"))
                .findFirst()
                .orElseThrow();
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE))) {
                products = (HashMap)in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error restoring data.", e);
        }
    }

}
