package one.jpro.platform.auth.core.oauth2;

import one.jpro.platform.auth.core.authentication.Options;
import one.jpro.platform.auth.core.jwt.JWTOptions;
import one.jpro.platform.auth.core.utils.AuthUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * This class represents the configuration options for OAuth2 authentication and authorization.
 * It encapsulates a variety of settings and parameters that are used during the OAuth2 flow,
 * including client credentials, token and authorization endpoints, supported response types,
 * and other custom configurations required for OAuth2 operations.
 * <p>
 * Instances of {@code OAuth2Options} can be customized to suit specific OAuth2 workflows,
 * allowing for the setup of different authentication and authorization schemes, like
 * authorization code flow, client credentials flow, or implicit flow. It also supports
 * various advanced configurations such as custom headers, JWT options, and public/secret keys.
 * To ensure that all necessary OAuth2 parameters are correctly configured, it provides utility
 * methods to validate and adjust the configurations as needed.
 *
 * @author Besmir Beqiri
 */
public class OAuth2Options implements Options {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2Options.class);

    private static final OAuth2Flow FLOW = OAuth2Flow.AUTH_CODE;
    private static final String AUTHORIZATION_PATH = "/oauth/authorize";
    private static final String TOKEN_PATH = "/oauth/token";
    private static final String REVOCATION_PATH = "/oauth/revoke";
    private static final JWTOptions JWT_OPTIONS = new JWTOptions();
    private static final String SCOPE_SEPARATOR = " ";
    private static final boolean VERIFY_TOKEN = true;
    private static final boolean VALIDATE_ISSUER = true;
    private static final boolean USE_LOOPBACK_IP_ADDRESS = false;
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
    private boolean validateIssuer; // this is an openid-connect extension
    private boolean useLoopbackIpAddress;
    private String logoutPath;
    private String userInfoPath; // extra parameters to be added while requesting the user info
    private JSONObject userInfoParams; // introspection RFC7662 https://tools.ietf.org/html/rfc7662
    private String introspectionPath; // JWK path RFC7517 https://tools.ietf.org/html/rfc7517
    private String jwkPath;
    private long jwkMaxAge; //seconds of JWKs lifetime
    private String tenant; // OpenID non standard
    private String site;
    private String clientId;
    private String clientSecret;
    private String clientAssertionType; // assertion RFC7521 https://tools.ietf.org/html/rfc7521
    private String clientAssertion;
    private String userAgent;
    private JSONObject headers;
    private List<PubSecKeyOptions> pubSecKeys;
    private JWTOptions jwtOptions;
    private JSONObject extraParams; // extra parameters to be added while requesting a token

    /**
     * Default constructor.
     */
    public OAuth2Options() {
        flow = FLOW;
        verifyToken = VERIFY_TOKEN;
        validateIssuer = VALIDATE_ISSUER;
        useLoopbackIpAddress = USE_LOOPBACK_IP_ADDRESS;
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

    /**
     * Gets the OAuth2 flow type.
     *
     * @return the current OAuth2 flow
     */
    public OAuth2Flow getFlow() {
        return flow;
    }

    /**
     * Sets the OAuth2 flow type.
     *
     * @param flow the OAuth2 flow to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setFlow(OAuth2Flow flow) {
        this.flow = flow;
        return this;
    }

    /**
     * Gets the supported response types.
     *
     * @return a list of supported response types
     */
    public List<String> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    /**
     * Sets the supported response types.
     *
     * @param supportedResponseTypes a list of supported response types to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedResponseTypes(List<String> supportedResponseTypes) {
        this.supportedResponseTypes = supportedResponseTypes;
        return this;
    }

    /**
     * Adds a supported response type to the existing list.
     *
     * @param supportedResponseType a supported response type to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedResponseType(String supportedResponseType) {
        if (supportedResponseTypes == null) {
            supportedResponseTypes = new ArrayList<>();
        }
        supportedResponseTypes.add(supportedResponseType);
        return this;
    }

    /**
     * Gets the supported response modes.
     *
     * @return a list of supported response modes
     */
    public List<String> getSupportedResponseModes() {
        return supportedResponseModes;
    }

    /**
     * Sets the supported response modes.
     *
     * @param supportedResponseModes a list of supported response modes to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedResponseModes(List<String> supportedResponseModes) {
        this.supportedResponseModes = supportedResponseModes;
        return this;
    }

    /**
     * Adds a supported response mode to the existing list.
     *
     * @param supportedResponseMode a supported response mode to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedResponseMode(String supportedResponseMode) {
        if (supportedResponseModes == null) {
            supportedResponseModes = new ArrayList<>();
        }
        supportedResponseModes.add(supportedResponseMode);
        return this;
    }

    /**
     * Gets the supported grant types.
     *
     * @return a list of supported grant types
     */
    public List<String> getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    /**
     * Sets the supported grant types.
     *
     * @param supportedGrantTypes a list of supported grant types to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedGrantTypes(List<String> supportedGrantTypes) {
        this.supportedGrantTypes = supportedGrantTypes;
        return this;
    }

    /**
     * Adds a supported grant type to the existing list.
     *
     * @param supportedGrantType a supported grant type to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedGrantType(String supportedGrantType) {
        if (supportedGrantTypes == null) {
            supportedGrantTypes = new ArrayList<>();
        }
        supportedGrantTypes.add(supportedGrantType);
        return this;
    }

    /**
     * Gets the supported subject types.
     *
     * @return a list of supported subject types
     */
    public List<String> getSupportedSubjectTypes() {
        return supportedSubjectTypes;
    }

    /**
     * Sets the supported subject types.
     *
     * @param supportedSubjectTypes a list of supported subject types to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedSubjectTypes(List<String> supportedSubjectTypes) {
        this.supportedSubjectTypes = supportedSubjectTypes;
        return this;
    }

    /**
     * Adds a supported subject type to the existing list.
     *
     * @param supportedSubjectType a supported subject type to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedSubjectType(String supportedSubjectType) {
        if (supportedSubjectTypes == null) {
            supportedSubjectTypes = new ArrayList<>();
        }
        supportedSubjectTypes.add(supportedSubjectType);
        return this;
    }

    /**
     * Gets the supported ID token signing algorithm values.
     *
     * @return a list of supported ID token signing algorithm values
     */
    public List<String> getSupportedIdTokenSigningAlgValues() {
        return supportedIdTokenSigningAlgValues;
    }

    /**
     * Sets the supported ID token signing algorithm values.
     *
     * @param supportedIdTokenSigningAlgValues a list of supported ID token signing algorithm values to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedIdTokenSigningAlgValues(List<String> supportedIdTokenSigningAlgValues) {
        this.supportedIdTokenSigningAlgValues = supportedIdTokenSigningAlgValues;
        return this;
    }

    /**
     * Adds a supported ID token signing algorithm value to the existing list.
     *
     * @param supportedIdTokenSigningAlgValue a supported ID token signing algorithm value to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedIdTokenSigningAlgValue(String supportedIdTokenSigningAlgValue) {
        if (supportedIdTokenSigningAlgValues == null) {
            supportedIdTokenSigningAlgValues = new ArrayList<>();
        }
        supportedIdTokenSigningAlgValues.add(supportedIdTokenSigningAlgValue);
        return this;
    }

    /**
     * Gets the supported scopes.
     *
     * @return a list of supported scopes
     */
    public List<String> getSupportedScopes() {
        return supportedScopes;
    }

    /**
     * Sets the supported scopes.
     *
     * @param supportedScopes a list of supported scopes to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedScopes(List<String> supportedScopes) {
        this.supportedScopes = supportedScopes;
        return this;
    }

    /**
     * Adds a supported scope to the existing list.
     *
     * @param supportedScope a supported scope to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedScope(String supportedScope) {
        if (supportedScopes == null) {
            supportedScopes = new ArrayList<>();
        }
        supportedScopes.add(supportedScope);
        return this;
    }

    /**
     * Gets the supported token endpoint authentication methods.
     *
     * @return a list of supported token endpoint authentication methods
     */
    public List<String> getSupportedTokenEndpointAuthMethods() {
        return supportedTokenEndpointAuthMethods;
    }

    /**
     * Sets the supported token endpoint authentication methods.
     *
     * @param supportedTokenEndpointAuthMethods a list of supported token endpoint authentication methods to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedTokenEndpointAuthMethods(List<String> supportedTokenEndpointAuthMethods) {
        this.supportedTokenEndpointAuthMethods = supportedTokenEndpointAuthMethods;
        return this;
    }

    /**
     * Adds a supported token endpoint authentication method to the existing list.
     *
     * @param supportedTokenEndpointAuthMethod a supported token endpoint authentication method to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedTokenEndpointAuthMethod(String supportedTokenEndpointAuthMethod) {
        if (supportedTokenEndpointAuthMethods == null) {
            supportedTokenEndpointAuthMethods = new ArrayList<>();
        }
        supportedTokenEndpointAuthMethods.add(supportedTokenEndpointAuthMethod);
        return this;
    }

    /**
     * Gets the supported claims.
     *
     * @return a list of supported claims
     */
    public List<String> getSupportedClaims() {
        return supportedClaims;
    }

    /**
     * Sets the supported claims.
     *
     * @param supportedClaims a list of supported claims to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedClaims(List<String> supportedClaims) {
        this.supportedClaims = supportedClaims;
        return this;
    }

    /**
     * Adds a supported claim to the existing list.
     *
     * @param supportedClaim a supported claim to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedClaim(String supportedClaim) {
        if (supportedClaims == null) {
            supportedClaims = new ArrayList<>();
        }
        supportedClaims.add(supportedClaim);
        return this;
    }

    /**
     * Gets the supported code challenge methods.
     *
     * @return a list of supported code challenge methods
     */
    public List<String> getSupportedCodeChallengeMethods() {
        return supportedCodeChallengeMethods;
    }

    /**
     * Sets the supported code challenge methods.
     *
     * @param supportedCodeChallengeMethods a list of supported code challenge methods to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedCodeChallengeMethods(List<String> supportedCodeChallengeMethods) {
        this.supportedCodeChallengeMethods = supportedCodeChallengeMethods;
        return this;
    }

    /**
     * Adds a supported code challenge method to the existing list.
     *
     * @param supportedCodeChallengeMethod a supported code challenge method to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedCodeChallengeMethod(String supportedCodeChallengeMethod) {
        if (supportedCodeChallengeMethods == null) {
            supportedCodeChallengeMethods = new ArrayList<>();
        }
        supportedCodeChallengeMethods.add(supportedCodeChallengeMethod);
        return this;
    }

    /**
     * Gets the supported introspection endpoint authentication methods.
     *
     * @return a list of supported introspection endpoint authentication methods
     */
    public List<String> getSupportedIntrospectionEndpointAuthMethods() {
        return supportedIntrospectionEndpointAuthMethods;
    }

    /**
     * Sets the supported introspection endpoint authentication methods.
     *
     * @param supportedIntrospectionEndpointAuthMethods a list of supported introspection
     *                                                  endpoint authentication methods to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedIntrospectionEndpointAuthMethods(List<String> supportedIntrospectionEndpointAuthMethods) {
        this.supportedIntrospectionEndpointAuthMethods = supportedIntrospectionEndpointAuthMethods;
        return this;
    }

    /**
     * Adds a supported introspection endpoint authentication method to the existing list.
     *
     * @param supportedIntrospectionEndpointAuthMethod a supported introspection endpoint
     *                                                 authentication method to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedIntrospectionEndpointAuthMethod(String supportedIntrospectionEndpointAuthMethod) {
        if (supportedIntrospectionEndpointAuthMethods == null) {
            supportedIntrospectionEndpointAuthMethods = new ArrayList<>();
        }
        supportedIntrospectionEndpointAuthMethods.add(supportedIntrospectionEndpointAuthMethod);
        return this;
    }

    /**
     * Gets the supported revocation endpoint authentication methods.
     *
     * @return a list of supported revocation endpoint authentication methods
     */
    public List<String> getSupportedRevocationEndpointAuthMethods() {
        return supportedRevocationEndpointAuthMethods;
    }

    /**
     * Sets the supported revocation endpoint authentication methods.
     *
     * @param supportedRevocationEndpointAuthMethods a list of supported revocation endpoint
     *                                               authentication methods to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedRevocationEndpointAuthMethods(List<String> supportedRevocationEndpointAuthMethods) {
        this.supportedRevocationEndpointAuthMethods = supportedRevocationEndpointAuthMethods;
        return this;
    }

    /**
     * Adds a supported revocation endpoint authentication method to the existing list.
     *
     * @param supportedRevocationEndpointAuthMethod a supported revocation endpoint
     *                                              authentication method to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addSupportedRevocationEndpointAuthMethod(String supportedRevocationEndpointAuthMethod) {
        if (supportedRevocationEndpointAuthMethods == null) {
            supportedRevocationEndpointAuthMethods = new ArrayList<>();
        }
        supportedRevocationEndpointAuthMethods.add(supportedRevocationEndpointAuthMethod);
        return this;
    }

    /**
     * Checks if the request parameter is supported.
     *
     * @return {@code true} if the request parameter is supported, otherwise {@code false}.
     */
    public boolean isSupportedRequestParameter() {
        return supportedRequestParameter;
    }

    /**
     * Sets whether the request parameter is supported.
     *
     * @param supportedRequestParameter a boolean indicating whether the request parameter is supported
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedRequestParameter(boolean supportedRequestParameter) {
        this.supportedRequestParameter = supportedRequestParameter;
        return this;
    }

    /**
     * Gets the supported request object signing algorithm values.
     *
     * @return a list of supported request object signing algorithm values
     */
    public List<String> getSupportedRequestObjectSigningAlgValues() {
        return supportedRequestObjectSigningAlgValues;
    }

    /**
     * Sets the supported request object signing algorithm values.
     *
     * @param supportedRequestObjectSigningAlgValues a list of supported request object signing algorithm values to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSupportedRequestObjectSigningAlgValues(List<String> supportedRequestObjectSigningAlgValues) {
        this.supportedRequestObjectSigningAlgValues = supportedRequestObjectSigningAlgValues;
        return this;
    }

    /**
     * Adds a supported request object signing algorithm value to the existing list.
     *
     * @param supportedRequestObjectSigningAlgValue a supported request object signing algorithm value to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
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
     * @return a URL as a string
     */
    public String getAuthorizationPath() {
        return computePath(authorizationPath);
    }

    /**
     * Sets the URL of the authorization server's authorization endpoint.
     *
     * @param authorizationPath a URL as a string
     */
    public OAuth2Options setAuthorizationPath(String authorizationPath) {
        this.authorizationPath = authorizationPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's token endpoint.
     *
     * @return a URL as a string
     */
    public String getTokenPath() {
        return computePath(tokenPath);
    }

    /**
     * Sets the URL of the authorization server's token endpoint.
     *
     * @param tokenPath a URL as a string
     */
    public OAuth2Options setTokenPath(String tokenPath) {
        this.tokenPath = tokenPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's revocation endpoint.
     *
     * @return a URL as a string
     */
    public String getRevocationPath() {
        return computePath(revocationPath);
    }

    /**
     * Sets the URL of the authorization server's revocation endpoint.
     *
     * @param revocationPath a URL as a string
     */
    public OAuth2Options setRevocationPath(String revocationPath) {
        this.revocationPath = revocationPath;
        return this;
    }

    /**
     * Gets the scope separator used in OAuth2 requests.
     *
     * @return the scope separator as a string
     */
    public String getScopeSeparator() {
        return scopeSeparator;
    }

    /**
     * Sets the scope separator to be used in OAuth2 requests.
     *
     * @param scopeSeparator the scope separator as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setScopeSeparator(String scopeSeparator) {
        this.scopeSeparator = scopeSeparator;
        return this;
    }

    /**
     * Checks if token verification is enabled.
     *
     * @return {@code true} if token verification is enabled, otherwise {@code false}.
     */
    public boolean isVerifyToken() {
        return verifyToken;
    }

    /**
     * Enables or disables token verification.
     *
     * @param verifyToken {@code true} to enable token verification, {@code false} to disable it
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setVerifyToken(boolean verifyToken) {
        this.verifyToken = verifyToken;
        return this;
    }

    /**
     * Checks if issuer validation is enabled.
     *
     * @return {@code true} if issuer validation is enabled, otherwise {@code false}
     */
    public boolean isValidateIssuer() {
        return validateIssuer;
    }

    /**
     * Enables or disables issuer validation.
     *
     * @param validateIssuer {@code true} to enable issuer validation, {@code false} to disable it
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setValidateIssuer(boolean validateIssuer) {
        this.validateIssuer = validateIssuer;
        return this;
    }

    /**
     * Checks if loopback IP address is used.
     *
     * @return {@code true} if loopback IP address is used, otherwise {@code false}.
     */
    public boolean isUseLoopbackIpAddress() {
        return useLoopbackIpAddress;
    }

    /**
     * Sets whether to use loopback IP address.
     *
     * @param useLoopbackIpAddress {@code true} to use loopback IP address, {@code false} otherwise
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setUseLoopbackIpAddress(boolean useLoopbackIpAddress) {
        this.useLoopbackIpAddress = useLoopbackIpAddress;
        return this;
    }

    /**
     * Returns the URL of the authorization server's logout endpoint.
     *
     * @return a URL as a string.
     */
    public String getLogoutPath() {
        return computePath(logoutPath);
    }

    /**
     * Sets the URL of the authorization server's logout endpoint.
     *
     * @param logoutPath a URL as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's userinfo endpoint.
     *
     * @return a URL as a string.
     */
    public String getUserInfoPath() {
        return computePath(userInfoPath);
    }

    /**
     * Sets the URL of the authorization server's userinfo endpoint.
     *
     * @param userInfoPath a URL as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setUserInfoPath(String userInfoPath) {
        this.userInfoPath = userInfoPath;
        return this;
    }

    /**
     * Gets the user information parameters.
     *
     * @return a {@code JSONObject} containing user information parameters
     */
    public JSONObject getUserInfoParams() {
        return userInfoParams;
    }

    /**
     * Sets the user information parameters.
     *
     * @param userInfoParams a {@code JSONObject} containing user information parameters
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setUserInfoParams(JSONObject userInfoParams) {
        this.userInfoParams = userInfoParams;
        return this;
    }

    /**
     * Returns the URL of the authorization server's introspection endpoint.
     *
     * @return a URL as a string
     */
    public String getIntrospectionPath() {
        return computePath(introspectionPath);
    }

    /**
     * Sets the URL of the authorization server's introspection endpoint.
     *
     * @param introspectionPath a URL as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setIntrospectionPath(String introspectionPath) {
        this.introspectionPath = introspectionPath;
        return this;
    }

    /**
     * Returns the URL of the authorization server's JSON Web Key Set (JWKS) endpoint.
     *
     * @return a URL as a string
     */
    public String getJwkPath() {
        return computePath(jwkPath);
    }

    /**
     * Sets the URL of the authorization server's JSON Web Key Set (JWKS) endpoint.
     *
     * @param jwkPath a URL as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setJwkPath(String jwkPath) {
        this.jwkPath = jwkPath;
        return this;
    }

    /**
     * Gets the maximum age of the JWK set before it is refreshed.
     *
     * @return the maximum age in milliseconds
     */
    public long getJwkMaxAge() {
        return jwkMaxAge;
    }

    /**
     * Sets the maximum age of the JWK set before it needs to be refreshed.
     *
     * @param jwkMaxAge the maximum age in milliseconds
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setJwkMaxAge(long jwkMaxAge) {
        this.jwkMaxAge = jwkMaxAge;
        return this;
    }

    /**
     * Gets the tenant identifier used in OAuth2 requests.
     *
     * @return the tenant identifier as a string
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Sets the tenant identifier to be used in OAuth2 requests.
     *
     * @param tenant the tenant identifier as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setTenant(String tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * Gets the site URL used in forming OAuth2 endpoints.
     *
     * @return the site URL as a string
     */
    public String getSite() {
        // remove trailing slash if present
        if (site != null && site.endsWith("/")) {
            site = site.substring(0, site.length() - 1);
        }
        return replaceVariables(site);
    }

    /**
     * Sets the site URL to be used in forming OAuth2 endpoints.
     *
     * @param site the site URL as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setSite(String site) {
        this.site = site;
        return this;
    }

    /**
     * Gets the client ID used for OAuth2 authentication.
     *
     * @return the client ID as a string
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the client ID to be used for OAuth2 authentication.
     *
     * @param clientId the client ID as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setClientId(String clientId) {
        this.clientId = AuthUtils.requireNonNullOrBlank(clientId, "Client id cannot be null or blank");
        return this;
    }

    /**
     * Gets the client secret used for OAuth2 authentication.
     *
     * @return the client secret as a string
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Sets the client secret to be used for OAuth2 authentication.
     *
     * @param clientSecret the client secret as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setClientSecret(String clientSecret) {
        this.clientSecret = AuthUtils.requireNonNullOrBlank(clientSecret,
                "Client secret cannot be null or blank");
        return this;
    }

    /**
     * Gets the client assertion type used in OAuth2 authentication.
     *
     * @return the client assertion type as a string
     */
    public String getClientAssertionType() {
        return clientAssertionType;
    }

    /**
     * Sets the client assertion type to be used in OAuth2 authentication.
     *
     * @param clientAssertionType the client assertion type as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setClientAssertionType(String clientAssertionType) {
        this.clientAssertionType = clientAssertionType;
        return this;
    }

    /**
     * Gets the client assertion used for OAuth2 authentication.
     *
     * @return the client assertion as a string
     */
    public String getClientAssertion() {
        return clientAssertion;
    }

    /**
     * Sets the client assertion to be used for OAuth2 authentication.
     *
     * @param clientAssertion the client assertion as a string
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setClientAssertion(String clientAssertion) {
        this.clientAssertion = clientAssertion;
        return this;
    }

    /**
     * Gets the user agent string to be used in OAuth2 requests.
     *
     * @return the user agent string
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets the user agent string to be used in OAuth2 requests.
     *
     * @param userAgent the user agent string to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Gets the custom headers to be sent in OAuth2 requests.
     *
     * @return a {@code JSONObject} containing the custom headers
     */
    public JSONObject getHeaders() {
        return headers;
    }

    /**
     * Sets custom headers to be sent in OAuth2 requests.
     *
     * @param headers a {@code JSONObject} containing the custom headers to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setHeaders(JSONObject headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Gets the public and secret key options used in OAuth2 authentication.
     *
     * @return a list of {@code PubSecKeyOptions}
     */
    public List<PubSecKeyOptions> getPubSecKeys() {
        return pubSecKeys;
    }

    /**
     * Sets the public and secret key options to be used in OAuth2 authentication.
     *
     * @param pubSecKeys a list of {@code PubSecKeyOptions} to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setPubSecKeys(List<PubSecKeyOptions> pubSecKeys) {
        this.pubSecKeys = pubSecKeys;
        return this;
    }

    /**
     * Adds a public and secret key option to the existing list for OAuth2 authentication.
     *
     * @param pubSecKey a {@code PubSecKeyOptions} object to add
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options addPubSecKeys(PubSecKeyOptions pubSecKey) {
        if (pubSecKeys == null) {
            pubSecKeys = new ArrayList<>();
        }
        pubSecKeys.add(pubSecKey);
        return this;
    }

    /**
     * Gets the JWT options used in OAuth2 authentication.
     *
     * @return a {@code JWTOptions} object representing the JWT options
     */
    public JWTOptions getJWTOptions() {
        return jwtOptions;
    }

    /**
     * Sets the JWT options to be used in OAuth2 authentication.
     *
     * @param jwtOptions a {@code JWTOptions} object representing the JWT options to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setJWTOptions(JWTOptions jwtOptions) {
        this.jwtOptions = jwtOptions;
        return this;
    }

    /**
     * Gets extra parameters to be sent in OAuth2 requests.
     *
     * @return a {@code JSONObject} containing the extra parameters
     */
    public JSONObject getExtraParams() {
        return extraParams;
    }

    /**
     * Sets extra parameters to be included in OAuth2 requests.
     *
     * @param extraParams a {@code JSONObject} containing the extra parameters to set
     * @return the current instance of {@code OAuth2Options} for method chaining
     */
    public OAuth2Options setExtraParams(JSONObject extraParams) {
        this.extraParams = extraParams;
        return this;
    }

    /**
     * Completes the path if it's relative by prepending the site URL.
     *
     * @param path the path to be completed
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
     * Replaces the tenant/realm variable in the given path.
     *
     * @param path the path with potential variables
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
     * Validates the OAuth2 configuration for completeness and consistency.
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
                        logger.debug("If you are using Client OAuth2 Resource Owner flow, please specify [clientId]");
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
