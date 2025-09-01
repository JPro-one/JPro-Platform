package one.jpro.platform.auth.core.basic;

import io.jsonwebtoken.io.Encoders;
import one.jpro.platform.auth.core.authentication.CredentialValidationException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Tests for UsernamePasswordCredentials.
 * <p>
 * These tests verify:
 * - That invalid input (null or blank username, null password) throws the expected exception.
 * - That valid credentials pass validation.
 * - That the HTTP Authorization header and JSON representation are generated correctly.
 * - The proper implementation of equals, hashCode, and the overall validation behavior.
 * <p>
 * Additional tests check:
 * - Special characters in credentials.
 * - Negative equality tests (against null and other types).
 *
 * @author Besmir Beqiri
 * @author Indrit Beqiri
 */
public class UsernamePasswordCredentialsTests {

    @ParameterizedTest(name = "Invalid username: \"{0}\" should throw CredentialValidationException")
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    @DisplayName("Should throw exception when username is null or blank")
    public void shouldThrowExceptionForInvalidUsername(String invalidUsername) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(invalidUsername, "password");
        assertThatExceptionOfType(CredentialValidationException.class)
                .isThrownBy(() -> credentials.validate(null))
                .withMessage("Username cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when password is null")
    public void shouldThrowExceptionForNullPassword() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("username", null);
        assertThatExceptionOfType(CredentialValidationException.class)
                .isThrownBy(() -> credentials.validate(null))
                .withMessage("Password cannot be null");
    }

    @Test
    @DisplayName("Should validate successfully for valid credentials")
    public void shouldValidateForValidCredentials() {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("user@mail.com", "securePassword");
        // Valid credentials should not throw any exception.
        credentials.validate(null);
    }

    @Test
    @DisplayName("Credentials with identical username and password should be equal")
    public void shouldBeEqualForIdenticalCredentials() {
        UsernamePasswordCredentials credentials1 =
                new UsernamePasswordCredentials("name@mail.com", "password");
        UsernamePasswordCredentials credentials2 =
                new UsernamePasswordCredentials("name@mail.com", "password");
        assertThat(credentials1).isEqualTo(credentials2);
    }

    @Test
    @DisplayName("Credentials with different username or password should not be equal")
    public void shouldNotBeEqualForDifferentCredentials() {
        UsernamePasswordCredentials cred1 = new UsernamePasswordCredentials("a@mail.com", "secret");
        UsernamePasswordCredentials cred2 = new UsernamePasswordCredentials("b@mail.com", "secret");
        UsernamePasswordCredentials cred3 = new UsernamePasswordCredentials("a@mail.com", "differentSecret");

        assertThat(cred1).isNotEqualTo(cred2);
        assertThat(cred1).isNotEqualTo(cred3);
    }

    @Test
    @DisplayName("Should not be equal to null or different type")
    public void shouldNotBeEqualToOtherObjects() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("user@mail.com", "password");
        assertThat(credentials).isNotEqualTo(null);
        assertThat(credentials).isNotEqualTo("Some String");
    }

    @Test
    @DisplayName("Hash code should be consistent based on username and password")
    public void shouldHaveConsistentHashCode() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("some_username", "some_password");
        int expectedHash = Objects.hash(credentials.getUsername(), credentials.getPassword());
        assertThat(credentials.hashCode()).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("Should return correct HTTP Authorization header for simple credentials")
    public void shouldReturnCorrectHttpAuthorizationHeader() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("username", "password");
        String expectedAuthHeader = "Basic " +
                Base64.getEncoder().encodeToString((credentials.getUsername() + ":" + credentials.getPassword())
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.toHttpAuthorization()).isEqualTo(expectedAuthHeader);
    }

    @ParameterizedTest(name = "HTTP header for username=\"{0}\" and password=\"{1}\"")
    @DisplayName("Should return correct HTTP Authorization header for credentials with special characters")
    @ValueSource(strings = {"user@example.com:pa$$w0rd!", "ünîçødé:密码"})
    public void shouldReturnCorrectHttpAuthorizationHeaderWithSpecialCharacters(String input) {
        // The input string format is username:password
        String[] parts = input.split(":");
        String username = parts[0];
        String password = parts[1];
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials(username, password);
        String expectedAuthHeader = "Basic " +
                Base64.getEncoder().encodeToString((username + ":" + password)
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(credentials.toHttpAuthorization()).isEqualTo(expectedAuthHeader);
    }

    @Test
    @DisplayName("toJSON should provide the expected JSON representation")
    public void shouldReturnCorrectJsonRepresentation() {
        UsernamePasswordCredentials credentials =
                new UsernamePasswordCredentials("some_username", "some_password");
        JSONObject credentialsJSON = credentials.toJSON();
        assertAll("JSON Representation",
                () -> assertThat(credentialsJSON.has("username")).as("Username field exists").isTrue(),
                () -> assertThat(credentialsJSON.has("password")).as("Password field exists").isTrue(),
                () -> assertThat(credentialsJSON.getString("username"))
                        .as("Username value matches")
                        .isEqualTo(credentials.getUsername()),
                () -> {
                    String encodedPassword = credentialsJSON.getString("password");
                    // Verify that the encoded password is not equal to plain text.
                    assertThat(encodedPassword).isNotEqualTo("some_password");
                    // Verify that the encoded password correctly matches the original when checked with the encoder.
                    assertThat(BCRYPT_PASSWORD_ENCODER.matches("some_password", encodedPassword))
                            .as("Encoded password should match the raw password")
                            .isTrue();
                }
        );
    }
}
