package one.jpro.platform.sipjs.api.options;

import org.json.JSONObject;

public class SessionDescriptionHandlerOptions {

    public MediaStreamConstraints mediaStreamConstraints;

    /**
     * Creates a new instance of SessionDescriptionHandlerOptions.
     * @param mediaStreamConstraints
     */
    public SessionDescriptionHandlerOptions(MediaStreamConstraints mediaStreamConstraints) {
        this.mediaStreamConstraints = mediaStreamConstraints;
    }

    /**
     * Creates a JSON representation of this object.
     */
    public JSONObject asJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("constraints", mediaStreamConstraints.asJSONObject());
        return jsonObject;
    }
}
