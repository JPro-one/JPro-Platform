package one.jpro.auth.oath2;

import one.jpro.auth.authentication.CredentialValidationException;
import one.jpro.auth.authentication.Credentials;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Credentials specific to the {@link OAuth2AuthenticationProvider}.
 *
 * @author Besmir Beqiri
 */
public class OAuth2Credentials implements Credentials {

    private String code;            // swap code for token
    private String codeVerifier;
    private String redirectUri;
    private JSONObject jwt;         // jwt-bearer tokens can include other kind of generic data
    private String assertion;       // or contain an assertion
    private String password;        // password credentials
    private String username;
    private List<String> scopes;    // control state
    private OAuth2Flow flow;
    private String nonce;

    private String completeRedirectUri;

    /**
     * Default constructor.
     */
    public OAuth2Credentials() {
    }

    public String getCode() {
        return code;
    }

    public OAuth2Credentials setCode(String code) {
        this.code = code;
        return this;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public OAuth2Credentials setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
        return this;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public OAuth2Credentials setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public JSONObject getJwt() {
        return jwt;
    }

    public OAuth2Credentials setJwt(JSONObject jwt) {
        this.jwt = jwt;
        return this;
    }

    public String getAssertion() {
        return assertion;
    }

    public OAuth2Credentials setAssertion(String assertion) {
        this.assertion = assertion;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public OAuth2Credentials setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public OAuth2Credentials setUsername(String username) {
        this.username = username;
        return this;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public OAuth2Credentials setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public OAuth2Flow getFlow() {
        return flow;
    }

    public OAuth2Credentials setFlow(OAuth2Flow flow) {
        this.flow = flow;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public OAuth2Credentials setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    /**
     * Returns the normalized version of the redirect uri.
     * This method is used internally.
     *
     * @return the normalized redirected uri
     */
    private String getNormalizedRedirectUri() {
        if (completeRedirectUri == null) {
            return redirectUri;
        }
        return completeRedirectUri;
    }

    /**
     * Sets the normalized version of the redirect uri.
     * This method is used internally.
     */
    OAuth2Credentials setNormalizedRedirectUri(String completeRedirectUri) {
        this.completeRedirectUri = completeRedirectUri;
        return this;
    }

    @Override
    public <V> void validate(V arg) throws CredentialValidationException {
        OAuth2Flow flow = (OAuth2Flow) arg;
        if (flow == null) {
            throw new CredentialValidationException("flow cannot be null");
        }
        // when there's no access token, validation shall be performed according to each flow
        switch (flow) {
            case AUTH_CODE:
                if (code == null || code.isBlank()) {
                    throw new CredentialValidationException("code cannot be null or blank");
                }
                if (redirectUri != null && redirectUri.isBlank()) {
                    throw new CredentialValidationException("redirectUri cannot be blank");
                }
                break;
            case PASSWORD:
                if (username == null || username.isBlank()) {
                    throw new CredentialValidationException("username cannot be null or blank");
                }
                if (password == null || password.isBlank()) {
                    throw new CredentialValidationException("password cannot be null or blank");
                }
                break;
            case AUTH_JWT:
                if (jwt == null) {
                    throw new CredentialValidationException("jwt cannot be null");
                }
                break;
            case CLIENT:
                // no fields are required
                break;
        }
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        Optional.ofNullable(getCode()).ifPresent(code -> json.put("code", code));
        Optional.ofNullable(getCodeVerifier()).ifPresent(codeVerifier -> json.put("code_verifier", codeVerifier));
        Optional.ofNullable(getNormalizedRedirectUri()).ifPresent(redirectUri -> json.put("redirect_uri", redirectUri));
        Optional.ofNullable(getFlow()).ifPresent(flow -> json.put("flow", flow.getGrantType()));
        Optional.ofNullable(getJwt()).ifPresent(jwt -> json.put("jwt", jwt));
        Optional.ofNullable(getAssertion()).ifPresent(assertion -> json.put("assertion", assertion));
        Optional.ofNullable(getPassword()).ifPresent(password -> json.put("password", password));
        Optional.ofNullable(getUsername()).ifPresent(username -> json.put("username", username));
        Optional.ofNullable(Stream.ofNullable(getScopes())
                .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("scopes", jsonArray));
        Optional.ofNullable(getNonce()).ifPresent(nonce -> json.put("nonce", nonce));
        return json;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
