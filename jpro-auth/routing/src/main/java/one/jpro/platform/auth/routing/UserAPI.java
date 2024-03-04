package one.jpro.platform.auth.routing;

import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.authentication.User;
import org.json.JSONObject;

/**
 * The UserAPI class is a simple
 * class that provides a way to
 * get and set the user object
 * from the session.
 * @author floriankirmaier
 */
public class UserAPI {

    private ObservableMap<String, String> session;

    /**
     * The constructor for the UserAPI class
     * @param session
     */
    public UserAPI(ObservableMap<String, String> session) {
        this.session = session;
    }

    /**
     * The getUser method returns the user
     * object from the session
     * @return User
     */
    public final User getUser() {
        final var userJsonString = session.get("user");
        if (userJsonString != null) {
            final JSONObject userJson = new JSONObject(userJsonString);
            return new User(userJson);
        } else {
            return null;
        }
    }

    /**
     * The setUser method sets the user
     * object in the session
     * @param user
     */
    public final void setUser(User user) {
        if (user != null) {
            session.put("user", user.toJSON().toString());
        } else {
            session.remove("user");
        }
    }
}
