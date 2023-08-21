package one.jpro.auth.oath2;

/**
 * OAuth2 flows.
 *
 * @author Besmir Beqiri
 */
public enum OAuth2Flow {

    AUTH_CODE("authorization_code"),
    PASSWORD("password"),
    CLIENT("client_credentials"),
    AUTH_JWT("urn:ietf:params:oauth:grant-type:jwt-bearer");

    private final String grantType;

    OAuth2Flow(String grantType) {
        this.grantType = grantType;
    }

    public String getGrantType() {
        return grantType;
    }

    public static OAuth2Flow getFlow(String grantType) {
        for (var flow : values()) {
            if (flow.getGrantType().equals(grantType)) {
                return flow;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name() + " [" + grantType + "]";
    }
}
