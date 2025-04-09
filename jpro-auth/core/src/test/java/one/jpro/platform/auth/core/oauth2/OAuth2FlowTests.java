package one.jpro.platform.auth.core.oauth2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * OAuth2Flow tests.
 *
 * @author Besmir Beqiri
 */
public class OAuth2FlowTests {

    @Test
    public void fetchingOAuth2FlowByGrantTypeShouldGetTheRightOne() {
        for (OAuth2Flow flow : OAuth2Flow.values()) {
            assertEquals(OAuth2Flow.getFlow(flow.getGrantType()), flow);
        }
    }

    @Test
    public void fetchingOAuth2FlowByGrantTypeOutsideOfRangeShouldReturnNull() {
        assertNull(OAuth2Flow.getFlow(""));
        assertNull(OAuth2Flow.getFlow("grant_type"));
    }

    @Test
    public void oauth2FlowProvidesFormattedImplementationOfToStringMethod() {
        for (OAuth2Flow flow : OAuth2Flow.values()) {
            assertEquals(flow.name() + " [" + flow.getGrantType() + "]", flow.toString());
        }
    }
}
