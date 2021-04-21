/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package labs.client;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;
import labs.pm.data.Product;
import labs.pm.data.Review;

/**
 *
 * @author antoi
 */
public class ResourceFormatter {

    private Locale locale;
    private ResourceBundle resources;
    private DateTimeFormatter dateFormat;
    private NumberFormat moneyFormat;

    public ResourceFormatter(Locale locale) {
        this.locale = locale;
        this.resources = ResourceBundle.getBundle("labs.client.resources", locale);
        this.dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).localizedBy(locale);
        this.moneyFormat = NumberFormat.getCurrencyInstance(locale);
    }

    public String formatProduct(Product product) {
        return MessageFormat.format(resources.getString("products"), 
            product.getName(),
            moneyFormat.format(product.getPrice()),
            product.getRating(),
            dateFormat.format(product.getBestBefore()));
    }

    public String formatReview(Review review) {
        return MessageFormat.format(resources.getString("review"), 
            review.getRating(),
            review.getComments());
    }

    public String getText(String key) {
        return resources.getString(key);
    }

    public NumberFormat getMoneyFormat() {
        return moneyFormat;
    }

    public static ResourceFormatter getResourceFormatter(String languageTag) {
        return new ResourceFormatter(Locale.forLanguageTag(languageTag));
    }
}
