package one.jpro.auth.jwt;

import one.jpro.auth.authentication.Options;
import org.json.JSONObject;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Options describing a JWT (Json Web Token).
 *
 * @author Besmir Beqiri
 */
public class JWTOptions implements Options {

    private static final long DEFAULT_LEEWAY = 0;
    private static final long DEFAULT_CACHE_SIZE = 5;
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
     * Copy constructor.
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

    public String getIssuer() {
        return issuer;
    }

    public JWTOptions setIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public JWTOptions setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public List<String> getAudience() {
        return audience;
    }

    public JWTOptions setAudience(List<String> audience) {
        this.audience = audience;
        return this;
    }

    public List<String> getClaims() {
        return claims;
    }

    public JWTOptions setClaims(List<String> claims) {
        this.claims = claims;
        return this;
    }

    public long getLeeway() {
        return leeway;
    }

    public JWTOptions setLeeway(long leeway) {
        this.leeway = leeway;
        return this;
    }

    public boolean isIgnoreIssuedAt() {
        return ignoreIssuedAt;
    }

    public JWTOptions setIgnoreIssuedAt(boolean ignoreIssuedAt) {
        this.ignoreIssuedAt = ignoreIssuedAt;
        return this;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public JWTOptions setCacheSize(long cacheSize) {
        this.cacheSize = cacheSize;
        return this;
    }

    public Duration getExpiresIn() {
        return expiresIn;
    }

    public JWTOptions setExpiresIn(Duration expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public String getNonceAlgorithm() {
        return nonceAlgorithm;
    }

    public JWTOptions setNonceAlgorithm(String nonceAlgorithm) {
        this.nonceAlgorithm = nonceAlgorithm;
        return this;
    }

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
