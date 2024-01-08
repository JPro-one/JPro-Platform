package one.jpro.platform.sipjs.api.options;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import org.json.JSONObject;

/**
 * Options for accepting an incoming call.
 */
public class InvitationAcceptOptions {

    SessionDescriptionHandlerOptions sessionDescriptionHandlerOptions;

    /**
     * Creates a new instance of InvitationAcceptOptions.
     * @param sessionDescriptionHandlerOptions
     */
    public InvitationAcceptOptions(SessionDescriptionHandlerOptions sessionDescriptionHandlerOptions) {
        this.sessionDescriptionHandlerOptions = sessionDescriptionHandlerOptions;
    }

    /**
     * Creates a new instance of InvitationAcceptOptions with default options.
     * @return
     */
    public static InvitationAcceptOptions createVideoCall() {
        return new InvitationAcceptOptions(new SessionDescriptionHandlerOptions(new MediaStreamConstraints(true, true)));
    }

    /**
     * Creates a new instance of InvitationAcceptOptions with default options.
     * @return
     */
    public static InvitationAcceptOptions createAudioCall() {
        return new InvitationAcceptOptions(new SessionDescriptionHandlerOptions(new MediaStreamConstraints(true, false)));
    }

    public JSONObject asJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sessionDescriptionHandlerOptions", sessionDescriptionHandlerOptions.asJSONObject());
        return jsonObject;
    }

    public JSVariable asJSVariable(WebAPI webapi) {
        return webapi.executeScriptWithVariable("(" + asJSONObject().toString() + ")");
    }
}
