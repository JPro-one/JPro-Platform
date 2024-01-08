package one.jpro.platform.sipjs.api;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.WebAPI;
import one.jpro.platform.sipjs.SipJSUtil;
import one.jpro.platform.sipjs.api.options.InviterOptions;
import one.jpro.platform.sipjs.api.options.UserAgentOptions;
import one.jpro.platform.sipjs.api.session.Inventation;
import one.jpro.platform.sipjs.api.session.Inviter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a SIP user agent.
 */
public class UserAgent {

    WebAPI webapi;

    JSVariable jsUserAgent;
    JSVariable jsRegistrator;

    JSVariable startPromise;
    UserAgentOptions options;

    Consumer<Inventation> onInvite;

    /**
     * Creates a new user agent.
     *
     * @param options The options for the user agent.
     * @param webapi  The webapi.
     */
    public UserAgent(UserAgentOptions options, WebAPI webapi) {
        this.webapi = webapi;
        this.options = options;

        SipJSUtil.loadSipJS(webapi);

        var optionsVariable = options.asJSVariable(webapi);

        // setup onInvite
        var onInviteJS = webapi.registerJavaFunctionWithVariable(invite -> {
            var inventation = new Inventation(invite, webapi);
            onInvite.accept(inventation);
        });
        webapi.executeScript(optionsVariable.getName() + ".delegate = { onInvite: " + onInviteJS.getName() + " };");

        jsUserAgent = webapi.executeScriptWithVariable("new SIP.UserAgent(" + optionsVariable.getName() + ");");
        jsRegistrator = webapi.executeScriptWithVariable("new SIP.Registerer(" + jsUserAgent.getName() + ");");

        startPromise = webapi.executeScriptWithVariable(jsUserAgent.getName() + ".start();");
        webapi.executeScript(startPromise.getName() + ".then(() => { " + jsRegistrator.getName() + ".register(); });");


        //webapi.executeScript(jsUserAgent.getName() + ".onInvite.addListener(" + jsFun.getName() + ");");
    }

    /**
     * Creates a new user agent.
     *
     * @param options The options for the user agent.
     * @param webapi  The webapi.
     * @return The user agent.
     */
    public CompletableFuture<Inviter> makeCall(String target, InviterOptions options) {
        return JSVariable.promiseToFuture(webapi, startPromise).thenApply((v) -> {
            var jsTarget = webapi.executeScriptWithVariable("SIP.UserAgent.makeURI(\"" + target + "\");");
            var jsInviter = webapi.executeScriptWithVariable("new SIP.Inviter(" + jsUserAgent.getName() + ", " + jsTarget.getName() + ");");
            webapi.executeScript(jsInviter.getName() + ".invite(" + options.asJSVariable(webapi).getName() + ");");
            return new Inviter(jsInviter, webapi);
        });
    }

    /**
     * Sets the onInvite consumer.
     * @param onInvite
     */
    public void setOnInvite(Consumer<Inventation> onInvite) {
        this.onInvite = onInvite;
    }

}
