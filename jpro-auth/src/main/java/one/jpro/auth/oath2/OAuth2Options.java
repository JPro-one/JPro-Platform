package one.jpro.auth.oath2;

import one.jpro.auth.authentication.Options;
import one.jpro.auth.jwt.JWTOptions;
import one.jpro.auth.utils.AuthUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Options describing how an OAuth2 {@link HttpClient} will make connection.
 *
 * @author Besmir Beqiri
 */
public class OAuth2Options implements Options {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Options.class);

    // Defaults
    private static final OAuth2Flow FLOW = OAuth2Flow.AUTH_CODE;
    private static final String AUTHORIZATION_PATH = "/oauth/authorize";
    private static final String TOKEN_PATH = "/oauth/token";
    private static final String REVOCATION_PATH = "/oauth/revoke";
    private static final JWTOptions JWT_OPTIONS = new JWTOptions();
    private static final String SCOPE_SEPARATOR = " ";
    private static final boolean VERIFY_TOKEN = true;
    private static final boolean VALIDATE_ISSUER = true;
    private static final long JWK_DEFAULT_AGE = -1L; // seconds of JWK default age (-1 means no rotation)
    private static final Pattern TENANT_PATTERN = Pattern.compile("\\{(tenant|tenantid|realm)}");

    private OAuth2Flow flow;
    private List<String> supportedResponseTypes;
    private List<String> supportedResponseModes;
    private List<String> supportedGrantTypes;
    private List<String> supportedSubjectTypes;
    private List<String> supportedScopes;
    private List<String> supportedIdTokenSigningAlgValues;
    private List<String> supportedTokenEndpointAuthMethods;
    private List<String> supportedClaims;
    private List<String> supportedCodeChallengeMethods;
    private List<String> supportedIntrospectionEndpointAuthMethods;
    private List<String> supportedRevocationEndpointAuthMethods;
    private boolean supportedRequestParameter;
    private List<String> supportedRequestObjectSigningAlgValues;
    private String authorizationPath;
    private String tokenPath;
    private String revocationPath;
    private String scopeSeparator;
    private boolean verifyToken;
    // this is an openid-connect extension
    private boolean validateIssuer;
    private String logoutPath;
    private String userInfoPath;
    // extra parameters to be added while requesting the user info
    private JSONObject userInfoParams;
    // introspection RFC7662 https://tools.ietf.org/html/rfc7662
    private String introspectionPath;
    // JWK path RFC7517 https://tools.ietf.org/html/rfc7517
    private String jwkPath;
    //seconds of JWKs lifetime
    private long jwkMaxAge;
    // OpenID non standard
    private String tenant;

    private String site;
    private String clientId;
    private String clientSecret;

    // assertion RFC7521 https://tools.ietf.org/html/rfc7521
    private String clientAssertionType;
    private String clientAssertion;

    private String userAgent;
    private JSONObject headers;
    private List<PubSecKeyOptions> pubSecKeys;
    private JWTOptions jwtOptions;
    // extra parameters to be added while requesting a token
    private JSONObject extraParams;

    /**
     * Default constructor.
     */
    public OAuth2Options() {
        flow = FLOW;
        verifyToken = VERIFY_TOKEN;
        validateIssuer = VALIDATE_ISSUER;
        authorizationPath = AUTHORIZATION_PATH;
        tokenPath = TOKEN_PATH;
        revocationPath = REVOCATION_PATH;
        scopeSeparator = SCOPE_SEPARATOR;
        jwtOptions = JWT_OPTIONS;
        jwkMaxAge = JWK_DEFAULT_AGE;
    }

    /**
     * Copy constructor.
     *
     * @param other the OAuth2 options to copy
     */
    public OAuth2Options(OAuth2Options other) {
        flow = other.flow;
        supportedResponseTypes = other.supportedResponseTypes;
        supportedResponseModes = other.supportedResponseModes;
        supportedGrantTypes = other.supportedGrantTypes;
        supportedSubjectTypes = other.supportedSubjectTypes;
        supportedScopes = other.supportedScopes;
        supportedIdTokenSigningAlgValues = other.supportedIdTokenSigningAlgValues;
        supportedTokenEndpointAuthMethods = other.supportedTokenEndpointAuthMethods;
        supportedClaims = other.supportedClaims;
        supportedCodeChallengeMethods = other.supportedCodeChallengeMethods;
        supportedIntrospectionEndpointAuthMethods = other.supportedIntrospectionEndpointAuthMethods;
        supportedRevocationEndpointAuthMethods = other.supportedRevocationEndpointAuthMethods;
        supportedRequestParameter = other.supportedRequestParameter;
        supportedRequestObjectSigningAlgValues = other.supportedRequestObjectSigningAlgValues;
        authorizationPath = other.authorizationPath;
        tokenPath = other.tokenPath;
        revocationPath = other.revocationPath;
        scopeSeparator = other.scopeSeparator;
        verifyToken = other.verifyToken;
        validateIssuer = other.validateIssuer;
        logoutPath = other.logoutPath;
        userInfoPath = other.userInfoPath;
        introspectionPath = other.introspectionPath;
        jwkPath = other.jwkPath;
        jwkMaxAge = other.jwkMaxAge;
        tenant = other.tenant;
        site = other.site;
        clientId = other.clientId;
        clientSecret = other.clientSecret;
        clientAssertionType = other.clientAssertionType;
        clientAssertion = other.clientAssertion;
        userAgent = other.userAgent;
        pubSecKeys = other.pubSecKeys;
        jwtOptions = other.jwtOptions;
        // extra parameters
        if (other.extraParams != null) {
            extraParams = new JSONObject(other.extraParams.toString());
        }
        // headers
        if (other.headers != null) {
            headers = new JSONObject(other.headers.toString());
        }
        // user info params
        if (other.userInfoParams != null) {
            userInfoParams = new JSONObject(other.userInfoParams.toString());
        }
    }

    public OAuth2Flow getFlow() {
        return flow;
    }

    public OAuth2Options setFlow(OAuth2Flow flow) {
        this.flow = flow;
        return this;
    }

    public List<String> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    public OAuth2Options setSupportedResponseTypes(List<String> supportedResponseTypes) {
        this.supportedResponseTypes = supportedResponseTypes;
        return this;
    }

    public OAuth2Options addSupportedResponseType(String supportedResponseType) {
        if (supportedResponseTypes == null) {
            supportedResponseTypes = new ArrayList<>();
        }
        supportedResponseTypes.add(supportedResponseType);
        return this;
    }

    public List<String> getSupportedResponseModes() {
        return supportedResponseModes;
    }

    public OAuth2Options setSupportedResponseModes(List<String> supportedResponseModes) {
        this.supportedResponseModes = supportedResponseModes;
        return this;
    }

    public OAuth2Options addSupportedResponseMode(String supportedResponseMode) {
        if (supportedResponseModes == null) {
            supportedResponseModes = new ArrayList<>();
        }
        supportedResponseModes.add(supportedResponseMode);
        return this;
    }

    public List<String> getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    public OAuth2Options setSupportedGrantTypes(List<String> supportedGrantTypes) {
        this.supportedGrantTypes = supportedGrantTypes;
        return this;
    }

    public OAuth2Options addSupportedGrantType(String supportedGrantType) {
        if (supportedGrantTypes == null) {
            supportedGrantTypes = new ArrayList<>();
        }
        supportedGrantTypes.add(supportedGrantType);
        return this;
    }

    public List<String> getSupportedSubjectTypes() {
        return supportedSubjectTypes;
    }

    public OAuth2Options setSupportedSubjectTypes(List<String> supportedSubjectTypes) {
        this.supportedSubjectTypes = supportedSubjectTypes;
        return this;
    }

    public OAuth2Options addSupportedSubjectType(String supportedSubjectType) {
        if (supportedSubjectTypes == null) {
            supportedSubjectTypes = new ArrayList<>();
        }
        supportedSubjectTypes.add(supportedSubjectType);
        return this;
    }

    public List<String> getSupportedIdTokenSigningAlgValues() {
        return supportedIdTokenSigningAlgValues;
    }

    public OAuth2Options setSupportedIdTokenSigningAlgValues(List<String> supportedIdTokenSigningAlgValues) {
        this.supportedIdTokenSigningAlgValues = supportedIdTokenSigningAlgValues;
        return this;
    }

    public OAuth2Options addSupportedIdTokenSigningAlgValue(String supportedIdTokenSigningAlgValue) {
        if (supportedIdTokenSigningAlgValues == null) {
            supportedIdTokenSigningAlgValues = new ArrayList<>();
        }
        supportedIdTokenSigningAlgValues.add(supportedIdTokenSigningAlgValue);
        return this;
    }

    public List<String> getSupportedScopes() {
        return supportedScopes;
    }

    public OAuth2Options setSupportedScopes(List<String> supportedScopes) {
        this.supportedScopes = supportedScopes;
        return this;
    }

    public OAuth2Options addSupportedScope(String supportedScope) {
        if (supportedScopes == null) {
            supportedScopes = new ArrayList<>();
        }
        supportedScopes.add(supportedScope);
        return this;
    }

    public List<String> getSupportedTokenEndpointAuthMethods() {
        return supportedTokenEndpointAuthMethods;
    }

    public OAuth2Options setSupportedTokenEndpointAuthMethods(List<String> supportedTokenEndpointAuthMethods) {
        this.supportedTokenEndpointAuthMethods = supportedTokenEndpointAuthMethods;
        return this;
    }

    public OAuth2Options addSupportedTokenEndpointAuthMethod(String supportedTokenEndpointAuthMethod) {
        if (supportedTokenEndpointAuthMethods == null) {
            supportedTokenEndpointAuthMethods = new ArrayList<>();
        }
        supportedTokenEndpointAuthMethods.add(supportedTokenEndpointAuthMethod);
        return this;
    }

    public List<String> getSupportedClaims() {
        return supportedClaims;
    }

    public OAuth2Options setSupportedClaims(List<String> supportedClaims) {
        this.supportedClaims = supportedClaims;
        return this;
    }

    public OAuth2Options addSupportedClaim(String supportedClaim) {
        if (supportedClaims == null) {
            supportedClaims = new ArrayList<>();
        }
        supportedClaims.add(supportedClaim);
        return this;
    }

    public List<String> getSupportedCodeChallengeMethods() {
        return supportedCodeChallengeMethods;
    }

    public OAuth2Options setSupportedCodeChallengeMethods(List<String> supportedCodeChallengeMethods) {
        this.supportedCodeChallengeMethods = supportedCodeChallengeMethods;
        return this;
    }

    public OAuth2Options addSupportedCodeChallengeMethod(String supportedCodeChallengeMethod) {
        if (supportedCodeChallengeMethods == null) {
            supportedCodeChallengeMethods = new ArrayList<>();
        }
        supportedCodeChallengeMethods.add(supportedCodeChallengeMethod);
        return this;
    }

    public List<String> getSupportedIntrospectionEndpointAuthMethods() {
        return supportedIntrospectionEndpointAuthMethods;
    }

    public OAuth2Options setSupportedIntrospectionEndpointAuthMethods(List<String> supportedIntrospectionEndpointAuthMethods) {
        this.supportedIntrospectionEndpointAuthMethods = supportedIntrospectionEndpointAuthMethods;
        return this;
    }

    public OAuth2Options addSupportedIntrospectionEndpointAuthMethod(String supportedIntrospectionEndpointAuthMethod) {
        if (supportedIntrospectionEndpointAuthMethods == null) {
            supportedIntrospectionEndpointAuthMethods = new ArrayList<>();
        }
        supportedIntrospectionEndpointAuthMethods.add(supportedIntrospectionEndpointAuthMethod);
        return this;
    }

    public List<String> getSupportedRevocationEndpointAuthMethods() {
        return supportedRevocationEndpointAuthMethods;
    }

    public OAuth2Options setSupportedRevocationEndpointAuthMethods(List<String> supportedRevocationEndpointAuthMethods) {
        this.supportedRevocationEndpointAuthMethods = supportedRevocationEndpointAuthMethods;
        return this;
    }

    public OAuth2Options addSupportedRevocationEndpointAuthMethod(String supportedRevocationEndpointAuthMethod) {
        if (supportedRevocationEndpointAuthMethods == null) {
            supportedRevocationEndpointAuthMethods = new ArrayList<>();
        }
        supportedRevocationEndpointAuthMethods.add(supportedRevocationEndpointAuthMethod);
        return this;
    }

    public boolean isSupportedRequestParameter() {
        return supportedRequestParameter;
    }

    public OAuth2Options setSupportedRequestParameter(boolean supportedRequestParameter) {
        this.supportedRequestParameter = supportedRequestParameter;
        return this;
    }

    public List<String> getSupportedRequestObjectSigningAlgValues() {
        return supportedRequestObjectSigningAlgValues;
    }

    public OAuth2Options setSupportedRequestObjectSigningAlgValues(List<String> supportedRequestObjectSigningAlgValues) {
        this.supportedRequestObjectSigningAlgValues = supportedRequestObjectSigningAlgValues;
        return this;
    }

    public OAuth2Options addSupportedRequestObjectSigningAlgValue(String supportedRequestObjectSigningAlgValue) {
        if (supportedRequestObjectSigningAlgValues == null) {
            supportedRequestObjectSigningAlgValues = new ArrayList<>();
        }
        supportedRequestObjectSigningAlgValues.add(supportedRequestObjectSigningAlgValue);
        return this;
    }

    /**
     * Returns the URL of the authorization server's authorization endpoint.
     *
     * @return an URL as a string
     */
    public String getAuthorizationPath() {
        return computePath(authorizationPath);
    }

    /**
     * Sets the URL of the authorization server's authorization endpoint.
     *
     * @param authorizationPath an URL as a string
     */
    public OAuth2Options setAuthorizationPath(String authorizationPath) {
        this.authorizationPath = authorizationPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's token endpoint.
     *
     * @return an URL as a string
     */
    public String getTokenPath() {
        return computePath(tokenPath);
    }

    /**
     * Sets the URL of the authorization server's token endpoint.
     *
     * @param tokenPath an URL as a string
     */
    public OAuth2Options setTokenPath(String tokenPath) {
        this.tokenPath = tokenPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's revocation endpoint.
     *
     * @return an URL as a string
     */
    public String getRevocationPath() {
        return computePath(revocationPath);
    }

    /**
     * Sets the URL of the authorization server's revocation endpoint.
     *
     * @param revocationPath an URL as a string
     */
    public OAuth2Options setRevocationPath(String revocationPath) {
        this.revocationPath = revocationPath;
        return this;
    }

    public String getScopeSeparator() {
        return scopeSeparator;
    }

    public OAuth2Options setScopeSeparator(String scopeSeparator) {
        this.scopeSeparator = scopeSeparator;
        return this;
    }

    public boolean isVerifyToken() {
        return verifyToken;
    }

    public OAuth2Options setVerifyToken(boolean verifyToken) {
        this.verifyToken = verifyToken;
        return this;
    }

    public boolean isValidateIssuer() {
        return validateIssuer;
    }

    public OAuth2Options setValidateIssuer(boolean validateIssuer) {
        this.validateIssuer = validateIssuer;
        return this;
    }

    /**
     * Returns the URL of the authorization server's logout endpoint.
     *
     * @return an URL as a string
     */
    public String getLogoutPath() {
        return computePath(logoutPath);
    }

    /**
     * Sets the URL of the authorization server's logout endpoint.
     *
     * @param logoutPath an URL as a string
     */
    public OAuth2Options setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's userinfo endpoint.
     *
     * @return an URL as a string
     */
    public String getUserInfoPath() {
        return computePath(userInfoPath);
    }

    /**
     * Sets the URL of the authorization server's userinfo endpoint.
     *
     * @param userInfoPath an URL as a string
     */
    public OAuth2Options setUserInfoPath(String userInfoPath) {
        this.userInfoPath = userInfoPath;
        return this;
    }

    public JSONObject getUserInfoParams() {
        return userInfoParams;
    }

    public OAuth2Options setUserInfoParams(JSONObject userInfoParams) {
        this.userInfoParams = userInfoParams;
        return this;
    }

    /**
     * Returns the URL of the authorization server's introspection endpoint.
     *
     * @return an URL as a string
     */
    public String getIntrospectionPath() {
        return computePath(introspectionPath);
    }

    /**
     * Sets the URL of the authorization server's introspection endpoint.
     *
     * @param introspectionPath an URL as a string
     */
    public OAuth2Options setIntrospectionPath(String introspectionPath) {
        this.introspectionPath = introspectionPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's JSON Web Key Set endpoint.
     *
     * @return an URL as a string
     */
    public String getJwkPath() {
        return computePath(jwkPath);
    }

    /**
     * Sets the URL of the authorization server's JSON Web Key Set endpoint.
     *
     * @param jwkPath an URL as a string
     */
    public OAuth2Options setJwkPath(String jwkPath) {
        this.jwkPath = jwkPath;
        return this;
    }

    public long getJwkMaxAge() {
        return jwkMaxAge;
    }

    public OAuth2Options setJwkMaxAge(long jwkMaxAge) {
        this.jwkMaxAge = jwkMaxAge;
        return this;
    }

    public String getTenant() {
        return tenant;
    }

    public OAuth2Options setTenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    public String getSite() {
        // remove trailing slash if present
        if (site != null && site.endsWith("/")) {
            site = site.substring(0, site.length() - 1);
        }
        return replaceVariables(site);
    }

    public OAuth2Options setSite(String site) {
        this.site = site;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public OAuth2Options setClientId(String clientId) {
        this.clientId = AuthUtils.requireNonNullOrBlank(clientId, "Client id cannot be null or blank");
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public OAuth2Options setClientSecret(String clientSecret) {
        this.clientSecret = AuthUtils.requireNonNullOrBlank(clientSecret, "Client secret cannot be null or blank");
        return this;
    }

    public String getClientAssertionType() {
        return clientAssertionType;
    }

    public OAuth2Options setClientAssertionType(String clientAssertionType) {
        this.clientAssertionType = clientAssertionType;
        return this;
    }

    public String getClientAssertion() {
        return clientAssertion;
    }

    public OAuth2Options setClientAssertion(String clientAssertion) {
        this.clientAssertion = clientAssertion;
        return this;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public OAuth2Options setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public JSONObject getHeaders() {
        return headers;
    }

    public OAuth2Options setHeaders(JSONObject headers) {
        this.headers = headers;
        return this;
    }

    public List<PubSecKeyOptions> getPubSecKeys() {
        return pubSecKeys;
    }

    public OAuth2Options setPubSecKeys(List<PubSecKeyOptions> pubSecKeys) {
        this.pubSecKeys = pubSecKeys;
        return this;
    }

    public OAuth2Options addPubSecKeys(PubSecKeyOptions pubSecKey) {
        if (pubSecKeys == null) {
            pubSecKeys = new ArrayList<>();
        }
        pubSecKeys.add(pubSecKey);
        return this;
    }

    public JWTOptions getJWTOptions() {
        return jwtOptions;
    }

    public OAuth2Options setJWTOptions(JWTOptions jwtOptions) {
        this.jwtOptions = jwtOptions;
        return this;
    }

    public JSONObject getExtraParams() {
        return extraParams;
    }

    public OAuth2Options setExtraParams(JSONObject extraParams) {
        this.extraParams = extraParams;
        return this;
    }

    /**
     * Complete the path if its relative prepending the site.
     *
     * @param path the path
     * @return the complete path as a string
     */
    private String computePath(String path) {
        if (path != null && path.charAt(0) == '/') {
            if (site != null) {
                // remove trailing slash if present
                if (site.endsWith("/")) {
                    site = site.substring(0, site.length() - 1);
                }
                path = site + path;
            }
        }

        return replaceVariables(path);
    }

    /**
     * Replace the tenant/realm variable in the path.
     *
     * @param path the path
     * @return the path with the tenant/realm variable replaced
     */
    public String replaceVariables(@Nullable final String path) {
        if (path != null) {
            final Matcher matcher = TENANT_PATTERN.matcher(path);
            if (matcher.find()) {
                if (tenant == null || tenant.isBlank()) {
                    throw new IllegalStateException("The tenant value is null or blank.");
                }
                return matcher.replaceAll(tenant);
            }
        }

        return path;
    }

    /**
     * Validate the configuration.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validate() throws IllegalStateException {
        if (flow == null) {
            throw new IllegalStateException("Missing OAuth2 flow: [AUTH_CODE, PASSWORD, CLIENT, AUTH_JWT]");
        }

        switch (flow) {
            case AUTH_CODE:
            case AUTH_JWT:
                if (clientAssertion == null && clientAssertionType == null) {
                    // not using client assertion
                    if (clientId == null) {
                        throw new IllegalStateException("Missing configuration: [clientId]");
                    }
                } else {
                    if (clientAssertion == null || clientAssertionType == null) {
                        throw new IllegalStateException("Missing configuration: [clientAssertion] and [clientAssertionType]");
                    }
                }
                break;
            case PASSWORD:
                if (clientAssertion == null && clientAssertionType == null) {
                    // not using client assertion
                    if (clientId == null) {
                        log.debug("If you are using Client OAuth2 Resource Owner flow, please specify [clientId]");
                    }
                } else {
                    if (clientAssertion == null || clientAssertionType == null) {
                        throw new IllegalStateException("Missing configuration: [clientAssertion] and [clientAssertionType]");
                    }
                }
                break;
        }
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();

        Optional.ofNullable(getFlow()).ifPresent(flow -> json.put("flow", flow.getGrantType()));
        Optional.ofNullable(getSite()).ifPresent(site -> json.put("site", site));
        Optional.ofNullable(getClientId()).ifPresent(clientId -> json.put("client_id", clientId));
        Optional.ofNullable(getClientSecret()).ifPresent(clientSecret -> json.put("client_secret", clientSecret));
        Optional.ofNullable(getTenant()).ifPresent(tenant -> json.put("tenant", tenant));
        Optional.ofNullable(Stream.ofNullable(getSupportedResponseTypes())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_response_types", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedResponseModes())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_response_modes", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedGrantTypes())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_grant_types", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedSubjectTypes())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_subject_types", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedScopes())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_scopes", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedIdTokenSigningAlgValues())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_id_token_signing_alg_values", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedTokenEndpointAuthMethods())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_token_endpoint_auth_methods", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedClaims())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_claims", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedCodeChallengeMethods())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_code_challenge_methods", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedIntrospectionEndpointAuthMethods())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_introspection_endpoint_auth_methods", jsonArray));
        Optional.ofNullable(Stream.ofNullable(getSupportedRevocationEndpointAuthMethods())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_revocation_endpoint_auth_methods", jsonArray));
        json.put("supported_request_parameter", isSupportedRequestParameter());
        Optional.ofNullable(Stream.ofNullable(getSupportedRequestObjectSigningAlgValues())
                        .collect(Collector.of(JSONArray::new, JSONArray::putAll, JSONArray::putAll)))
                .filter(jsonArray -> !jsonArray.isEmpty())
                .ifPresent(jsonArray -> json.put("supported_request_object_signing_alg_values", jsonArray));
        Optional.ofNullable(getAuthorizationPath())
                .ifPresent(authorizationPath -> json.put("authorization_path", authorizationPath));
        Optional.ofNullable(getTokenPath()).ifPresent(tokenPath -> json.put("token_path", tokenPath));
        Optional.ofNullable(getRevocationPath())
                .ifPresent(revocationPath -> json.put("revocation_path", revocationPath));
        Optional.ofNullable(getScopeSeparator())
                .ifPresent(scopeSeparator -> json.put("scope_separator", scopeSeparator));
        json.put("validate_issuer", isValidateIssuer());
        Optional.ofNullable(getLogoutPath()).ifPresent(logoutPath -> json.put("end_session_endpoint", logoutPath));
        Optional.ofNullable(getUserInfoPath()).ifPresent(userInfoPath -> json.put("user_info_path", userInfoPath));
        Optional.ofNullable(getIntrospectionPath())
                .ifPresent(introspectionPath -> json.put("introspection_path", introspectionPath));
        Optional.ofNullable(getJwkPath()).ifPresent(jwks_uri -> json.put("jwks_uri", jwks_uri));
        json.put("jwk_max_age", getJwkMaxAge());
        Optional.ofNullable(getClientAssertion())
                .ifPresent(clientAssertion -> json.put("client_assertion", clientAssertion));
        Optional.ofNullable(getClientAssertionType())
                .ifPresent(clientAssertionType -> json.put("client_assertion_type", clientAssertionType));
        Optional.ofNullable(getUserAgent()).ifPresent(userAgent -> json.put("user_agent", userAgent));
        Optional.ofNullable(getPubSecKeys()).ifPresent(pubSecKeyOptions -> json.put("pub_sec_keys", pubSecKeyOptions));
        Optional.ofNullable(getJWTOptions()).ifPresent(jwtOptions -> json.put("jwt_options", jwtOptions.toJSON()));
        Optional.ofNullable(getExtraParams()).ifPresent(extraParams -> json.put("extra_params", extraParams));
        Optional.ofNullable(getHeaders()).ifPresent(headers -> json.put("headers", headers));
        Optional.ofNullable(getUserInfoParams()).ifPresent(userInfoParams -> json.put("user_info_params", userInfoParams));
        return json;
    }
}
