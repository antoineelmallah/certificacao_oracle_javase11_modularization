/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.pm.data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 *
 * @author antoi
 */
public class Food extends Product {
    
    private LocalDate bestBefore;

    public Food(int id, String name, BigDecimal price, Rating rating, LocalDate bestBefore) {
        super(id, name, price, rating);
        this.bestBefore = bestBefore;
    }

    @Override
    public Product applyRating(Rating newRating) {
        return new Food(getId(), getName(), getPrice(), newRating, getBestBefore());
    }
    
}
