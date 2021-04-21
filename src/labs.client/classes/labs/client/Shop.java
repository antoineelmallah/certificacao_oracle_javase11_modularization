/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        ServiceLoader<ProductManager> serviceLoader = ServiceLoader.load(ProductManager.class);
        ProductManager pm = serviceLoader.findFirst().get();
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

    }
    
}
