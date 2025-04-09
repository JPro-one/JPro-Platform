package one.jpro.platform.auth.routing;

import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.authentication.User;
import org.json.JSONObject;

/**
 * The {@link UserSession} class provides a structured way to handle user authentication and maintain inside the session.
 *
 * @author Florian Kirmaier
 */
public class UserSession {

    private final ObservableMap<String, String> session;

    /**
     * Creates a new instance of the UserAPI class.
     *
     * @param session the session to use for session management
     */
    public UserSession(ObservableMap<String, String> session) {
        this.session = session;
    }

    /**
     * Retrieves the user object from the session.
     *
     * @return the user object retrieved from the session
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
     * Sets the user object in the session.
     *
     * @param user the user object to set in the session
     */
    public final void setUser(User user) {
        if (user != null) {
            session.put("user", user.toJSON().toString());
        } else {
            session.remove("user");
        }
    }

    /**
     * Removes the user object from the session.
     */
    public final void logout() {
        session.remove("user");
    }

    /**
     * Checks if the user is logged in.
     *
     * @return true if the user is logged in, false otherwise
     */
    public final boolean isLoggedIn() {
        return getUser() != null;
    }
}
