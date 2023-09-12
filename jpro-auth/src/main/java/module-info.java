/**
 * Module descriptor for the Authorization module.
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

    opens one.jpro.platform.auth;
    exports one.jpro.platform.auth;
    exports one.jpro.platform.auth.api;
    exports one.jpro.platform.auth.authentication;
    exports one.jpro.platform.auth.jwt;
    exports one.jpro.platform.auth.oath2;
    exports one.jpro.platform.auth.oath2.provider;
    exports one.jpro.platform.auth.http;
    exports one.jpro.platform.auth.utils;
}