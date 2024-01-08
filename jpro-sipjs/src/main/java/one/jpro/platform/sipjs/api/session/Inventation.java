package one.jpro.platform.sipjs.api.session;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import one.jpro.platform.sipjs.api.options.InvitationAcceptOptions;
import one.jpro.platform.sipjs.api.session.Session;

/**
 * Represents an incoming call.
 */
public class Inventation extends Session {
    public Inventation(JSVariable session, WebAPI webapi) {
        super(session, webapi);
    }

    public void accept(InvitationAcceptOptions options) {
        webapi.executeScript(session.getName() + ".accept(" + options.asJSVariable(webapi).getName() + ");");
    }

    public void reject() {
        webapi.executeScript(session.getName() + ".reject();");
    }
}
