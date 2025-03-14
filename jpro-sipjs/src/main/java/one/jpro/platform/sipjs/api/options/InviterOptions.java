package one.jpro.platform.sipjs.api.options;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import org.json.JSONObject;

public class InviterOptions {

    SessionDescriptionHandlerOptions sessionDescriptionHandlerOptions;

    /**
     * Creates a new instance of InvitationAcceptOptions.
     * @param sessionDescriptionHandlerOptions
     */
    public InviterOptions(SessionDescriptionHandlerOptions sessionDescriptionHandlerOptions) {
        this.sessionDescriptionHandlerOptions = sessionDescriptionHandlerOptions;
    }

    /**
     * Creates a new instance of InvitationAcceptOptions with default options.
     * @return
     */
    public static InviterOptions createVideoCall() {
        return new InviterOptions(new SessionDescriptionHandlerOptions(new MediaStreamConstraints(true, true)));
    }

    /**
     * Creates a new instance of InvitationAcceptOptions with only video but no audio.
     * @return
     */
    public static InviterOptions createVideoOnlyCall() {
        return new InviterOptions(new SessionDescriptionHandlerOptions(new MediaStreamConstraints(false, true)));
    }

    /**
     * Creates a new instance of InvitationAcceptOptions with default options.
     * @return
     */
    public static InviterOptions createAudioCall() {
        return new InviterOptions(new SessionDescriptionHandlerOptions(new MediaStreamConstraints(true, false)));
    }

    /**
     * Creates a new instance of InvitationAcceptOptions with default options.
     * @return
     */
    public JSONObject asJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sessionDescriptionHandlerOptions", sessionDescriptionHandlerOptions.asJSONObject());
        return jsonObject;
    }

    public JSVariable asJSVariable(WebAPI webapi) {
        return webapi.js().eval("(" + asJSONObject().toString() + ")");
    }
}
