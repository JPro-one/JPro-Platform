package one.jpro.platform.sipjs.api;

import com.jpro.webapi.JSVariable;
import com.jpro.webapi.PromiseJSVariable;
import com.jpro.webapi.WebAPI;
import one.jpro.platform.sipjs.SipJSUtil;
import one.jpro.platform.sipjs.api.options.InviterOptions;
import one.jpro.platform.sipjs.api.options.UserAgentOptions;
import one.jpro.platform.sipjs.api.session.Invitation;
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
    PromiseJSVariable registerPromise;
    UserAgentOptions options;

    Consumer<Invitation> onInvite;

    private JSVariable onInviteJS;
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
        onInviteJS = webapi.registerJavaFunctionWithVariable(invite -> {
            var inventation = new Invitation(invite, webapi);
            onInvite.accept(inventation);
        });
        webapi.js().eval(optionsVariable.getName() + ".delegate = { onInvite: " + onInviteJS.getName() + " };");

        jsUserAgent = webapi.js().eval("new SIP.UserAgent(" + optionsVariable.getName() + ");");
        jsRegistrator = webapi.js().eval("new SIP.Registerer(" + jsUserAgent.getName() + ");");

        startPromise = webapi.js().eval(jsUserAgent.getName() + ".start();");
        registerPromise = webapi.js().evalAsync(startPromise.getName() + ".then(() => { " + jsRegistrator.getName() + ".register(); });");


        //webapi.executeScript(jsUserAgent.getName() + ".onInvite.addListener(" + jsFun.getName() + ");");
    }

    /**
     * Makes a call to the given target.
     * @param target The target.
     * @param options The options.
     * @return A completable future that completes when the call is established.
     */
    public CompletableFuture<Inviter> makeCall(String target, InviterOptions options) {
        return JSVariable.promiseToFuture(webapi, startPromise).thenApply((v) -> {
            var jsTarget = webapi.js().eval("SIP.UserAgent.makeURI(\"" + target + "\");");
            var jsInviter = webapi.js().eval("new SIP.Inviter(" + jsUserAgent.getName() + ", " + jsTarget.getName() + ");");
            webapi.js().eval(jsInviter.getName() + ".invite(" + options.asJSVariable(webapi).getName() + ");");
            return new Inviter(jsInviter, webapi);
        });
    }

    /**
     * Sets the onInvite consumer.
     * @param onInvite
     */
    public void setOnInvite(Consumer<Invitation> onInvite) {
        this.onInvite = onInvite;
    }

    /**
     * Gets the register promise.
     * It might contains errors.
     * @return
     */
    public PromiseJSVariable getRegisterPromise() {
        return registerPromise;
    }

}
