package one.jpro.platform.sipjs.api.session;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import one.jpro.platform.sipjs.api.session.Session;

/**
 * Represents an outgoing call.
 */
public class Inviter extends Session {

    public Inviter(JSVariable session, WebAPI webapi) {
        super(session, webapi);
    }

    public void cancel() {
        webapi.js().eval(session.getName() + ".cancel();");
    }
}
