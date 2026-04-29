package Utils;

import Entities.Marketplace.Produit;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static List<Produit> cartItems = new ArrayList<>();

    public static void addProduct(Produit p) {
        if (!cartItems.contains(p)) {
            cartItems.add(p);
        }
    }

    public static void removeProduct(Produit p) {
        cartItems.remove(p);
    }

    public static List<Produit> getCartItems() {
        return cartItems;
    }

    public static void clearCart() {
        cartItems.clear();
    }
    
    public static double getTotal() {
        double total = 0;
        for (Produit p : cartItems) {
            total += p.getPrix();
        }
        return total;
    }
}
