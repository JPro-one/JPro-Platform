module one.jpro.platform.sipjs {
    requires javafx.controls;
    requires jpro.webapi;
    requires org.json;
    requires one.jpro.platform.webrtc;

    exports one.jpro.platform.sipjs;
    exports one.jpro.platform.sipjs.api;
    exports one.jpro.platform.sipjs.api.session;
    exports one.jpro.platform.sipjs.api.options;
}