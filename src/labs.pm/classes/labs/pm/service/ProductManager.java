/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.pm.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import labs.pm.data.Product;
import labs.pm.data.Rating;
import labs.pm.data.Review;

/**
 *
 * @author antoi
 */
public interface ProductManager {
    Product createProduct(int id, String name, BigDecimal price, Rating rating) throws ProductManagerException;
    Product createProduct(int id, String name, BigDecimal price, Rating rating, LocalDate bestBefore) throws ProductManagerException;
    Product reviewProduct(int id, Rating rating, String comments) throws ProductManagerException;
    Product findProduct(int id) throws ProductManagerException;
    List<Product> findProducts(Predicate<Product> filter) throws ProductManagerException;
    List<Review> findReviews(int id) throws ProductManagerException;
    Map<Rating, BigDecimal> getDiscounts() throws ProductManagerException;
}
/*
public class ProductManager {
    
    private static final Logger logger = Logger.getLogger(ProductManager.class.getName());
    
    private static final ProductManager pm = new ProductManager();
    
    private final ResourceBundle config = ResourceBundle.getBundle("labs.pm.data.config");
    
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
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();

    private ProductManager() {
        loadAllData();
    }

    public static Set<String> getSupportedLocales() {
        return formatters.keySet();
    }
    
    public Product createProduct(int id, String name, BigDecimal price, Rating rating, LocalDate bestBefore) {
        Product product = null;
        try {
            writeLock.lock();
            product = new Food(id, name, price, rating, bestBefore);
            products.putIfAbsent(product, new ArrayList<>());
            return product;
        } catch (Exception e) {
            logger.log(Level.INFO, "Error adding product "+e.getMessage());
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    public Product createProduct(int id, String name, BigDecimal price, Rating rating) {
        Product product = null;
        try {
            writeLock.lock();
            product = new Drink(id, name, price, rating);
            products.putIfAbsent(product, new ArrayList<>());
            return product;
        } catch (Exception e) {
            logger.log(Level.INFO, "Error adding product "+e.getMessage());
            return null;
        } finally {
            writeLock.unlock();
        }
    }
    
    public Product reviewProduct(int id, Rating rating, String comments) {
        try {
            writeLock.lock();
            return reviewProduct(findProduct(id), rating, comments);
        } catch (ProductManagerException ex) {
            logger.log(Level.INFO, ex.getMessage(), ex);
        } finally {
            writeLock.unlock();
        }
        return null;
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

    public Product findProduct(int id) throws ProductManagerException {
        try {
            readLock.lock();
            return products.keySet().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ProductManagerException("Product with id "+id+" not found."));
        } finally {
            readLock.unlock();
        }
    }
    
    public void printProductReport(int id, String languageTag, String client) {
        try {
            readLock.lock();
            Product product = findProduct(id);            
            printProductReport(product, languageTag, client);
        } catch (ProductManagerException ex) {
            logger.log(Level.INFO, ex.getMessage(), ex);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            readLock.unlock();
        }
    }
    
    private void printProductReport(Product product, String languageTag, String client) throws IOException {
        
        ResourceFormatter formatter = formatters.getOrDefault(languageTag, formatters.get(languageTag));

        List<Review> reviews = products.get(product);
        
        Path productFile = reportsFolder.resolve(MessageFormat.format(config.getString("report.file"), product.getId(), client));

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(productFile, StandardOpenOption.CREATE), "UTF-8"))) {
            
            out.append(formatter.formatProduct(product));

            out.append(System.lineSeparator());

            if (reviews.isEmpty()) {
                out.append(formatter.getText("no.reviews"));
                out.append(System.lineSeparator());
            } else {
                out.append(
                    reviews.stream()
                        .sorted()
                        .map(r -> formatter.formatReview(r) + System.lineSeparator())
                        .collect(Collectors.joining()));
            }
            
        }
                
    }
    
    public void printProducts(Predicate<Product> filter, Comparator<Product> sorter, String languageTag) {

        try {
            readLock.lock();
            ResourceFormatter formatter = formatters.getOrDefault(languageTag, formatters.get(languageTag));

            StringBuilder sb = new StringBuilder();

            this.products.keySet().stream()
                .filter(filter)
                .sorted(sorter)
                .forEach(p -> sb.append(formatter.formatProduct(p)).append('\n'));

            System.out.println(sb);
        } finally {
            readLock.unlock();
        }
    }
    
    private Review parseReview(String text) {
        Review review = null;
        try {
            Object[] values = reviewFormat.parse(text);
            review = new Review(Rateable.convert(Integer.parseInt((String)values[0])), (String)values[1]);
        } catch (ParseException | NumberFormatException ex) {
            logger.log(Level.WARNING, "Error parsing text: '"+text+"'");
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
        } catch (Exception ex) {
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
    
    @SuppressWarnings("unchecked")
    private void restoreData() {
        try {
            Path tempFile = Files.list(tempFolder)
                .filter(f -> f.getFileName().toString().endsWith("tmp"))
                .findFirst()
                .orElseThrow();
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE))) {
                products = (HashMap)in.readObject();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error restoring data.", e);
        }
    }
    
    public Map<String, String> getDiscounts(String languageTag) {
        try {
            readLock.lock();
            ResourceFormatter formatter = formatters.getOrDefault(languageTag, formatters.get(languageTag));
            return products.keySet().stream()
                .collect(
                    Collectors.groupingBy(
                        product -> product.getRating().toString(), 
                        Collectors.collectingAndThen(
                            Collectors.summingDouble(product -> product.getDiscount().doubleValue()), 
                            discount -> formatter.getMoneyFormat().format(discount)
                        )
                    )
                );
        } finally {
            readLock.unlock();
        }
    }
    
    public static ProductManager getInstance() {
        return pm;
    }
}
*/