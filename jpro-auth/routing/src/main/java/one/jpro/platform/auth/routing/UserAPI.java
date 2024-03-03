package one.jpro.platform.auth.routing;

import javafx.collections.ObservableMap;
import one.jpro.platform.auth.core.authentication.User;
import org.json.JSONObject;

public class UserAPI {

    ObservableMap<String, String> session;

    public UserAPI(ObservableMap<String, String> session) {
        this.session = session;
    }

    public final User getUser() {
        final var userJsonString = session.get("user");
        if (userJsonString != null) {
            final JSONObject userJson = new JSONObject(userJsonString);
            return new User(userJson);
        } else {
            return null;
        }
    }

    public final void setUser(User value) {
        if (value != null) {
            session.put("user", value.toJSON().toString());
        } else {
            session.remove("user");
        }
    }
}
