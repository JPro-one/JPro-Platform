package one.jpro.platform.auth.core.jwt;

import one.jpro.platform.auth.core.authentication.Options;
import org.json.JSONObject;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Options describing a JWT (JSON Web Token).
 *
 * @author Besmir Beqiri
 */
public class JWTOptions implements Options {

    /**
     * Default leeway for JWT expiration time validation in seconds.
     */
    private static final long DEFAULT_LEEWAY = 0;

    /**
     * Default cache size for storing generated JWTs or validation data.
     */
    private static final long DEFAULT_CACHE_SIZE = 5;

    /**
     * Default duration for JWT to be considered as expired after its issuance.
     */
    private static final Duration DEFAULT_EXPIRES_IN = Duration.ofHours(8); // 8 hours

    private String issuer;
    private String subject;
    private List<String> audience;
    private List<String> claims;
    private long leeway = DEFAULT_LEEWAY;
    private boolean ignoreIssuedAt;

    private long cacheSize = DEFAULT_CACHE_SIZE;
    private Duration expiresIn = DEFAULT_EXPIRES_IN;

    private String nonceAlgorithm;

    /**
     * Default constructor.
     */
    public JWTOptions(){
    }

    /**
     * Copy constructor for JWTOptions. Initializes a new instance of JWTOptions by copying
     * configuration from another instance.
     *
     * @param other the JWT options to copy
     */
    public JWTOptions(JWTOptions other) {
        this.issuer = other.issuer;
        this.subject = other.subject;
        this.audience = other.audience;
        this.claims = other.claims;
        this.leeway = other.leeway;
        this.ignoreIssuedAt = other.ignoreIssuedAt;
        this.cacheSize = other.cacheSize;
        this.expiresIn = other.expiresIn;
        this.nonceAlgorithm = other.nonceAlgorithm;
    }

    /**
     * Gets the issuer claim that identifies the principal that issued the JWT.
     *
     * @return the issuer identifier
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer claim that identifies the principal that issued the JWT.
     *
     * @param issuer the issuer identifier
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    /**
     * Gets the subject claim that identifies the principal that is the subject of the JWT.
     *
     * @return the subject identifier
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject claim that identifies the principal that is the subject of the JWT.
     *
     * @param subject the subject identifier
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * Gets the audience claim that identifies the recipients that the JWT is intended for.
     *
     * @return the list of audience identifiers
     */
    public List<String> getAudience() {
        return audience;
    }

    /**
     * Sets the audience claim that identifies the recipients that the JWT is intended for.
     *
     * @param audience the list of audience identifiers
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setAudience(List<String> audience) {
        this.audience = audience;
        return this;
    }

    /**
     * Gets the claims intended for the JWT token.
     *
     * @return the list of claims
     */
    public List<String> getClaims() {
        return claims;
    }

    /**
     * Sets the claims for the JWT token.
     *
     * @param claims the list of claims to be set for the JWT token
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setClaims(List<String> claims) {
        this.claims = claims;
        return this;
    }

    /**
     * Gets the leeway for the JWT token's expiration time validation in seconds.
     *
     * @return the leeway in seconds
     */
    public long getLeeway() {
        return leeway;
    }

    /**
     * Sets the leeway for the JWT token's expiration time validation in seconds.
     *
     * @param leeway the leeway in seconds
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setLeeway(long leeway) {
        this.leeway = leeway;
        return this;
    }

    /**
     * Checks if the "issued at" claim (iat) is being ignored for the JWT validation.
     *
     * @return true if the "issued at" claim is being ignored; false otherwise
     */
    public boolean isIgnoreIssuedAt() {
        return ignoreIssuedAt;
    }

    /**
     * Sets whether the "issued at" claim (iat) should be ignored for the JWT validation.
     *
     * @param ignoreIssuedAt flag indicating whether to ignore the "issued at" claim
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setIgnoreIssuedAt(boolean ignoreIssuedAt) {
        this.ignoreIssuedAt = ignoreIssuedAt;
        return this;
    }

    /**
     * Gets the size of the cache for storing generated JWTs or validation data.
     *
     * @return the cache size
     */
    public long getCacheSize() {
        return cacheSize;
    }

    /**
     * Sets the size of the cache for storing generated JWTs or validation data.
     *
     * @param cacheSize the cache size
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
        return this;
    }

    /**
     * Gets the duration after which the JWT should be considered expired.
     *
     * @return the {@link Duration} until the token expires
     */
    public Duration getExpiresIn() {
        return expiresIn;
    }

    /**
     * Gets the duration after which the JWT should be considered expired.
     *
     * @return the {@link Duration} until the token expires
     */
    public JWTOptions setExpiresIn(Duration expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    /**
     * Gets the algorithm used to generate a nonce for the JWT token.
     *
     * @return the nonce algorithm
     */
    public String getNonceAlgorithm() {
        return nonceAlgorithm;
    }

    /**
     * Sets the algorithm used to generate a nonce for the JWT token.
     *
     * @param nonceAlgorithm the nonce algorithm
     * @return the current instance of {@link JWTOptions} for method chaining
     */
    public JWTOptions setNonceAlgorithm(String nonceAlgorithm) {
        this.nonceAlgorithm = nonceAlgorithm;
        return this;
    }

    /**
     * Converts the current JWT options to a JSON object suitable for serialization.
     * This includes all the JWT claim settings and additional options such as leeway,
     * cache size, and expiry duration.
     *
     * @return a {@link JSONObject} representing the JWT options
     */
    @Override
    public JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        Optional.ofNullable(getIssuer()).ifPresent(issuer -> json.put("issuer", issuer));
        Optional.ofNullable(getSubject()).ifPresent(subject -> json.put("subject", subject));
        Optional.ofNullable(getAudience()).ifPresent(audience -> json.put("audience", audience));
        Optional.ofNullable(getClaims()).ifPresent(claims -> json.put("claims", claims));
        json.put("leeway", getLeeway());
        json.put("ignore_issued_at", isIgnoreIssuedAt());
        json.put("cache_size", getCacheSize());
        json.put("expires_in", getExpiresIn().getSeconds()); // in seconds
        Optional.ofNullable(getNonceAlgorithm())
                .ifPresent(nonceAlgorithm -> json.put("nonceAlgorithm", nonceAlgorithm));
        return json;
    }
}
