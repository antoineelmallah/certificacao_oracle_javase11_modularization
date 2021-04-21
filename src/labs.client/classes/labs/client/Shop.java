/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import labs.file.service.ProductFileManager;
import labs.pm.data.Product;
import labs.pm.service.ProductManager;
import labs.pm.data.Rating;
import labs.pm.data.Review;
import labs.pm.service.ProductManagerException;

/**
 *
 * @author antoi
 */
public class Shop {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
  
        ResourceFormatter formatter = ResourceFormatter.getResourceFormatter("en-GB");
        ProductManager pm = new ProductFileManager();
        try {
            pm.createProduct(164, "Kombucha", BigDecimal.valueOf(1.99), Rating.NOT_RATED);
            pm.reviewProduct(164, Rating.TWO_STAR, "Looks like tea but is it?");
            pm.reviewProduct(164, Rating.FOUR_STAR, "Fine tea.");
            pm.reviewProduct(164, Rating.FOUR_STAR, "This is not tea.");
            pm.reviewProduct(164, Rating.FIVE_STAR, "Perfect!");
            pm.findProducts(p -> p.getPrice().doubleValue() < 2).stream()
                    .forEach(p -> System.out.println(formatter.formatProduct(p)));
            Product product = pm.findProduct(101);
            List<Review> reviews = pm.findReviews(101);
            System.out.println(formatter.formatProduct(product));
            reviews.forEach(r -> System.out.println(formatter.formatReview(r)));
            
        } catch (ProductManagerException ex) {
            Logger.getLogger(Shop.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }

        /*
            ProductManager pm = ProductManager.getInstance();
            
            AtomicInteger clientCount = new AtomicInteger(0);
            
            Callable<String> client = () -> {
            String clientId = "Client "+clientCount.incrementAndGet();
            String threadName = Thread.currentThread().getName();
            int productId = ThreadLocalRandom.current().nextInt(6) + 101;
            String languageTag = ProductManager.getSupportedLocales().stream()
            .skip(ThreadLocalRandom.current().nextInt(4))
            .findFirst()
            .get();
            StringBuilder log = new StringBuilder();
            
            log.append(clientId+" "+threadName+"\n-\tstart of log\t-\n");
            
            log.append(pm.getDiscounts(languageTag).entrySet().stream()
            .map(entry -> entry.getKey() + "\t" + entry.getValue())
            .collect(Collectors.joining("\n")));
            Product product = pm.reviewProduct(productId, Rating.FOUR_STAR, "Yet another review.");
            log.append(product != null ?
            "\nProduct "+productId+" reviewed.\n" :
            "\nProduct "+productId+" not reviewed.");
            pm.printProductReport(productId, languageTag, clientId);
            log.append(clientId+" generated report for "+productId+" product.");
            
            log.append("\n-\tend of log\t\n");
            
            return log.toString();
            };
            
            List<Callable<String>> clients = Stream.generate(() -> client)
            .limit(5)
            .collect(Collectors.toList());
            ExecutorService executorService = Executors.newFixedThreadPool(3);
            try {
            List<Future<String>> results = executorService.invokeAll(clients);
            executorService.shutdown();
            results.stream().forEach(result -> {
            try {
            System.out.println(result.get());
            } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Shop.class.getName()).log(Level.SEVERE, "Error retrieving client log.", ex);
            }
            });
            } catch (InterruptedException ex) {
            Logger.getLogger(Shop.class.getName()).log(Level.SEVERE, "Error invoking clients.", ex);
            }
        */      
    }
    
}
