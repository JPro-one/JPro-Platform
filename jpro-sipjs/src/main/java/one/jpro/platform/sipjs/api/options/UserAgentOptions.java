package one.jpro.platform.sipjs.api.options;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import org.json.JSONObject;

import java.util.HashMap;

public class UserAgentOptions {

    JSONObject transportOptions;
    JSONObject options;

    public UserAgentOptions() {
        transportOptions = new JSONObject();

        var optionsMap = new HashMap<String, JSONObject>();
        optionsMap.put("transportOptions", transportOptions);
        options = new JSONObject(optionsMap);
    }

    public void addServer(String server) {
        transportOptions.put("server", server);
    }

    public void addAuthorizationPassword(String authorizationPassword) {
        transportOptions.put("authorizationPassword", authorizationPassword);
    }

    public void addAuthorizationUsername(String authorizationUsername) {
        transportOptions.put("authorizationUsername", authorizationUsername);
    }

    public void addUri(String uri) {
        options.put("uri", uri);
    }

    public void addDisplayName(String displayName) {
        options.put("displayName", displayName);
    }

    /**
     * Creates a JSON representation of this object.
     */
    public JSONObject getJson() {
        return options;
    }

    public String toString() {
        return options.toString();
    }

    /**
     * This method is used to create a JSVariable from this object.
     * Especially, the "uri" field is replaced with "UserAgent.makeURI(uri)" because thats how the sip.js library works
     */
    public JSVariable asJSVariable(WebAPI webapi) {
        var jsVariable = webapi.js().eval("(" + toString() + ")");
        // We have to replace "uri" with "UserAgent.makeURI(uri)" because thats how the sip.js library works
        webapi.js().eval("" +
                //"import { UserAgent } from \"sip.js/lib/platform/web\";" +
                "var uri = " + jsVariable.getName() + ".uri; " +
                jsVariable.getName() + ".uri = SIP.UserAgent.makeURI(uri);");
        return jsVariable;
    }


}
