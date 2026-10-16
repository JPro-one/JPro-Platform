package one.jpro.platform.auth.example.oauth;

import com.microsoft.playwright.Page;
import one.jpro.platform.auth.example.oauth.page.LoginPage;
import one.jpro.platform.playwright.JProInput;
import one.jpro.platform.playwright.JProPlaywrightTest;
import one.jpro.platform.playwright.JProWait;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Logs in to the {@code oauth} example through a real Keycloak, started in Docker on a free port with the
 * realm from {@code keycloak/myrealm.json}. Run with {@code ./gradlew :jpro-auth:example:keycloakTest}.
 */
@Tag("keycloak")
public class KeycloakLoginPlaywrightTest extends JProPlaywrightTest {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.4.0";
    private static final File LOGS_DIR = new File(projectRoot(), "jpro-auth/example/logs");

    private static String container;
    private static int keycloakPort;

    @BeforeAll
    static void startKeycloakAndApp() throws Exception {
        keycloakPort = freePort();
        container = "jpro-keycloak-test-" + keycloakPort;
        Path importDir = Files.createTempDirectory("keycloak-import");
        try (InputStream realm = KeycloakLoginPlaywrightTest.class.getResourceAsStream("/keycloak/myrealm.json")) {
            String json = new String(realm.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("${APP_PORT}", String.valueOf(PORT));
            Files.writeString(importDir.resolve("myrealm.json"), json);
        }
        docker("run", "-d", "--name", container, "-p", keycloakPort + ":8080",
                "-e", "KC_BOOTSTRAP_ADMIN_USERNAME=admin", "-e", "KC_BOOTSTRAP_ADMIN_PASSWORD=admin",
                "-v", importDir.toAbsolutePath() + ":/opt/keycloak/data/import:ro",
                KEYCLOAK_IMAGE, "start-dev", "--import-realm");
        String realmUrl = "http://localhost:" + keycloakPort + "/realms/myrealm";
        JProWait.waitUntil(60_000, () -> isUp(realmUrl + "/.well-known/openid-configuration"),
                () -> "Keycloak at " + realmUrl + " not ready");

        startServer(LOGS_DIR, Map.of(
                        "KEYCLOAK_TEST_SITE", "http://localhost:" + keycloakPort + "/realms/{realm}",
                        "KEYCLOAK_TEST_CLIENT_ID", "jpro-example",
                        "KEYCLOAK_TEST_CLIENT_SECRET", "jpro-example-secret"),
                gradleCommand(":jpro-auth:example:jproStart", "-Psample=oauth"));
    }

    @AfterAll
    static void stopAppAndKeycloak() throws Exception {
        try {
            stopAndAssertNoServerErrors(LOGS_DIR, gradleCommand(":jpro-auth:example:jproStop"));
        } finally {
            if (container != null) docker("rm", "-f", container);
        }
    }

    @Test
    void loginThroughKeycloakShowsTheSignedInUser() {
        Page page = newPage();
        try {
            page.navigate(BASE_URL + LoginPage.KEYCLOAK_PROVIDER_PATH);
            waitForRunning(page);
            page.locator(".jpro-id-sign-in-button").click();

            page.waitForURL("http://localhost:" + keycloakPort + "/realms/myrealm/**");
            page.fill("#username", "test");
            page.fill("#password", "test");
            page.click("#kc-login");

            page.waitForURL(BASE_URL + "/**");
            waitForRunning(page);
            JProWait.waitUntil(() -> String.valueOf(JProInput.getText(page, ".jpro-id-signed-in-user"))
                            .contains("test@example.com"),
                    () -> "signed-in label: " + JProInput.getText(page, ".jpro-id-signed-in-user"));
        } catch (RuntimeException | AssertionError e) {
            screenshot(page, "keycloak-login-failure");
            throw e;
        } finally {
            page.context().close();
        }
    }

    private static boolean isUp(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void docker(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("docker"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).inheritIO().start();
        assertEquals(0, process.waitFor(), "docker " + String.join(" ", args));
    }
}
