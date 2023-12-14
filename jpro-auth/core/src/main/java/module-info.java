/**
 * Module descriptor for the Auth Core module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.platform.auth {
    requires javafx.graphics;
    requires java.net.http;
    requires jpro.webapi;
    requires org.jetbrains.annotations;
    requires org.json;
    requires jwks.rsa;
    requires org.slf4j;
    requires com.auth0.jwt;
    requires one.jpro.platform.internal.openlink;

    opens one.jpro.platform.auth.core;
    exports one.jpro.platform.auth.core;
    exports one.jpro.platform.auth.core.api;
    exports one.jpro.platform.auth.core.authentication;
    exports one.jpro.platform.auth.core.jwt;
    exports one.jpro.platform.auth.core.oauth2;
    exports one.jpro.platform.auth.core.oauth2.provider;
    exports one.jpro.platform.auth.core.http;
    exports one.jpro.platform.auth.core.utils;
}