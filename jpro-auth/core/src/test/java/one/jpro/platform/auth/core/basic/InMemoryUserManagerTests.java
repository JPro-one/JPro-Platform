package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.User;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * In memory UserManager tests.
 *
 * @author Besmir Beqiri
 */
public class InMemoryUserManagerTests {

    private InMemoryUserManager userManager;

    @BeforeEach
    public void setup() {
        userManager = new InMemoryUserManager();
        userManager.createUser(new UsernamePasswordCredentials("someuser", "somepassword"),
                Set.of("USER"), Map.of("enabled", Boolean.TRUE)).join();
    }

    @Test
    public void testCreateUser() {
        final User user = userManager.loadUserByUsername("someuser").join();
        assertThat(user.getName()).isEqualTo("someuser");
        assertThat(user.getRoles()).containsExactly("USER");
        assertThat(user.getAttributes()).containsEntry("enabled", Boolean.TRUE);
        assertThat(user.hasAttribute("credentials")).isTrue();
    }

    @Test
    public void testUpdateUser() {
        final User user = userManager.loadUserByUsername("someuser").join();
        assertThat(user.getName()).isEqualTo("someuser");
        assertThat(user.getRoles()).containsExactly("USER");
        assertThat(user.getAttributes()).containsEntry("enabled", Boolean.TRUE);
        assertThat(user.hasAttribute("credentials")).isTrue();

        userManager.updateUser("someuser", Set.of("ADMIN"), Map.of("enabled", Boolean.FALSE)).join();
        final User updatedUser = userManager.loadUserByUsername("someuser").join();
        assertThat(updatedUser.getName()).isEqualTo("someuser");
        assertThat(updatedUser.getRoles()).containsExactly("ADMIN");
        assertThat(updatedUser.getAttributes()).containsEntry("enabled", Boolean.FALSE);
        assertThat(updatedUser.hasAttribute("credentials")).isTrue();
    }

    @Test
    public void testDeleteUser() {
        final User user = userManager.loadUserByUsername("someuser").join();
        assertThat(user.getName()).isEqualTo("someuser");
        userManager.deleteUser("someuser").join();

        assertThatThrownBy(() -> userManager.loadUserByUsername("someuser").get())
                .hasRootCauseInstanceOf(UserNotFoundException.class)
                .hasRootCauseMessage("User does not exist: someuser");
    }

    @Test
    public void testUpdatePassword() {
        userManager.changePassword("someuser", "newpassword").join();
        final User updatedUser = userManager.loadUserByUsername("someuser").join();
        assertThat(updatedUser.getName()).isEqualTo("someuser");
        assertThat(updatedUser.hasAttribute("credentials")).isTrue();
        final JSONObject updatedUserJSON = updatedUser.toJSON();
        final JSONObject updatedCredentialsJSON = updatedUserJSON.getJSONObject("attributes")
                .getJSONObject("credentials");
        assertThat(updatedCredentialsJSON.getString("username")).isEqualTo("someuser");
        assertThat(updatedCredentialsJSON.getString("password")).matches(encryptedPassword ->
                BCRYPT_PASSWORD_ENCODER.matches("newpassword", encryptedPassword));
    }

    @Test
    public void testUserExists() {
        assertThat(userManager.userExists("someuser")).isTrue();
    }
}
