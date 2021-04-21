/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.pm.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 *
 * @author antoi
 */
public abstract class Product implements Rateable<Product>, Serializable {
    
    public static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(.1);
    
    private final int id;
    
    private final String name;
    
    private final BigDecimal price;
    
    private final Rating rating;

    public Product(int id, String name, BigDecimal price, Rating rating) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.rating = rating;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }
    
    public BigDecimal getDiscount() {
        return price.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public Rating getRating() {
        return rating;
    }
    
//    public abstract Product applyRating(Rating newRating);
    
    public LocalDate getBestBefore() {
        return LocalDate.now();
    }

    @Override
    public String toString() {
        return id+", "+name+", "+price+", "+getDiscount()+", "+rating+", "+getBestBefore();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Product other = (Product) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
    
}
