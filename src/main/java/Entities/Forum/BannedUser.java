package Entities.Forum;

import javafx.beans.property.*;

/**
 * Simple JavaFX-observable model for display in the Banned Users table.
 */
public class BannedUser {

    private final IntegerProperty id    = new SimpleIntegerProperty();
    private final StringProperty  username = new SimpleStringProperty();
    private final StringProperty  email    = new SimpleStringProperty();
    private final IntegerProperty strikes  = new SimpleIntegerProperty();

    public BannedUser(int id, String username, String email, int strikes) {
        this.id.set(id);
        this.username.set(username);
        this.email.set(email);
        this.strikes.set(strikes);
    }

    public int    getId()       { return id.get(); }
    public String getUsername() { return username.get(); }
    public String getEmail()    { return email.get(); }
    public int    getStrikes()  { return strikes.get(); }

    public IntegerProperty idProperty()       { return id; }
    public StringProperty  usernameProperty() { return username; }
    public StringProperty  emailProperty()    { return email; }
    public IntegerProperty strikesProperty()  { return strikes; }
}
