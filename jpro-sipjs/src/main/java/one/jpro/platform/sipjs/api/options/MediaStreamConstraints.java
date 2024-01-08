package one.jpro.platform.sipjs.api.options;

import org.json.JSONObject;

public class MediaStreamConstraints {

    boolean audio;
    boolean video;

    /**
     * Creates a new instance of MediaStreamConstraints.
     * @param audio
     * @param video
     */
    public MediaStreamConstraints(boolean audio, boolean video) {
        this.audio = audio;
        this.video = video;
    }

    public JSONObject asJSONObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("audio", audio);
        jsonObject.put("video", video);
        return jsonObject;
    }
}
