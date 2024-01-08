package one.jpro.platform.auth.core.basic;

import one.jpro.platform.auth.core.authentication.User;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static one.jpro.platform.auth.core.utils.AuthUtils.BCRYPT_PASSWORD_ENCODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * In memory UserManager tests.
 *
 * @author Besmir Beqiri
 */
public class InMemoryUserManagerTests {

    private InMemoryUserManager userManager;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        userManager = new InMemoryUserManager();
        userManager.createUser(new UsernamePasswordCredentials("someuser", "somepassword"),
                Set.of("USER"), Map.of("enabled", Boolean.TRUE)).get();
    }

    @Test
    public void testCreateUser() throws ExecutionException, InterruptedException {
        final User user = userManager.loadUserByUsername("someuser").get();
        assertThat(user.getName()).isEqualTo("someuser");
        assertThat(user.getRoles()).containsExactly("USER");
        assertThat(user.getAttributes()).containsEntry("enabled", Boolean.TRUE);
        assertThat(user.hasAttribute("credentials")).isTrue();
    }

    @Test
    public void testUpdateUser() throws ExecutionException, InterruptedException {
        final User user = userManager.loadUserByUsername("someuser").get();
        assertThat(user.getName()).isEqualTo("someuser");
        assertThat(user.getRoles()).containsExactly("USER");
        assertThat(user.getAttributes()).containsEntry("enabled", Boolean.TRUE);
        assertThat(user.hasAttribute("credentials")).isTrue();

        userManager.updateUser("someuser", Set.of("ADMIN"), Map.of("enabled", Boolean.FALSE)).get();
        final User updatedUser = userManager.loadUserByUsername("someuser").get();
        assertThat(updatedUser.getName()).isEqualTo("someuser");
        assertThat(updatedUser.getRoles()).containsExactly("ADMIN");
        assertThat(updatedUser.getAttributes()).containsEntry("enabled", Boolean.FALSE);
        assertThat(updatedUser.hasAttribute("credentials")).isTrue();
    }

    @Test
    public void testDeleteUser() throws ExecutionException, InterruptedException {
        final User user = userManager.loadUserByUsername("someuser").get();
        assertThat(user.getName()).isEqualTo("someuser");
        userManager.deleteUser("someuser").get();
        assertThatExceptionOfType(UserNotFoundException.class)
                .isThrownBy(() -> userManager.loadUserByUsername("someuser").get());
    }

    @Test
    public void testUpdatePassword() throws ExecutionException, InterruptedException {
        userManager.changePassword("someuser", "newpassword").get();
        final User updatedUser = userManager.loadUserByUsername("someuser").get();
        assertThat(updatedUser.getName()).isEqualTo("someuser");
        assertThat(updatedUser.hasAttribute("credentials")).isTrue();
        final JSONObject updatedUserJSON = updatedUser.toJSON();
        final JSONObject updatedCredentialsJSON = updatedUserJSON.getJSONObject("attributes")
                .getJSONObject("credentials");
        assertThat(updatedCredentialsJSON.getString("username")).isEqualTo("someuser");
        assertThat(BCRYPT_PASSWORD_ENCODER.matches("newpassword",
                updatedCredentialsJSON.getString("password"))).isTrue();
    }

    @Test
    public void testUserExists() {
        assertThat(userManager.userExists("someuser")).isTrue();
    }
}
