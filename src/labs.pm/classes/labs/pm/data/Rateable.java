/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.pm.data;

/**
 *
 * @author antoi
 */
@FunctionalInterface // Não compila se houver mais de um método abstrato na interface
public interface Rateable<T> {
    
    public static final Rating DEFAULT_RATING = Rating.NOT_RATED;
    
    T applyRating(Rating rating);
    
    public default T applyRating(int stars) {
        return applyRating(convert(stars));
    }
    
    public static Rating convert(int stars) {
        return stars >= 0 && stars <= 5 ? Rating.values()[stars] : DEFAULT_RATING;
    }
    
    public default Rating getRating() {
        return DEFAULT_RATING;
    }
    
}
