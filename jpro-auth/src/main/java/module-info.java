/**
 * Module descriptor for the Authorization module.
 *
 * @author Besmir Beqiri
 */
module one.jpro.auth {
    requires javafx.graphics;
    requires java.net.http;
    requires jpro.webapi;
    requires org.jetbrains.annotations;
    requires org.json;
    requires jwks.rsa;
    requires org.slf4j;
    requires com.auth0.jwt;
    requires jdk.httpserver;

    opens one.jpro.auth;
    exports one.jpro.auth;
    exports one.jpro.auth.api;
    exports one.jpro.auth.authentication;
    exports one.jpro.auth.jwt;
    exports one.jpro.auth.oath2;
    exports one.jpro.auth.oath2.provider;
    exports one.jpro.auth.http;
    exports one.jpro.auth.utils;
}