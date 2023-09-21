package one.jpro.platform.sessionmanager;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.logging.Logger;

/**
 * This class handles the management of sessions in an application.
 * It provides the functionality to create, retrieve, and store sessions.
 *
 * @author Florian Kirmaier
 * @author Besmir Beqiri
 */
public class SessionManager {

    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());

    private final File baseDirectory;
    private final String cookieName;
    private static final Random random = new Random();
    private final WeakHashMap<String, ObservableMap<String, String>> sessionCache = new WeakHashMap<>();

    /**
     * Creates a SessionManager object with the given app name.
     *
     * @param appName the name of the application
     */
    public SessionManager(String appName) {
        this(new File(new File(System.getProperty("user.home")), "." + appName).getAbsoluteFile(), "c-" + appName);
    }

    /**
     * Creates a SessionManager object with the given base directory and cookie name.
     *
     * @param baseDirectory the base directory for session storage
     * @param cookieName the name of the cookie used for session tracking
     * @throws SessionException if the session directory cannot be created
     */
    public SessionManager(File baseDirectory, String cookieName) {
        this.baseDirectory = baseDirectory;
        this.cookieName = cookieName;
        if (!baseDirectory.exists()) {
            if (baseDirectory.mkdir()) {
                logger.info("Created session directory: " + baseDirectory);
            } else {
                throw new SessionException("Could not create session directory: " + baseDirectory);
            }
        }
    }

    /**
     * Returns the base directory used for session storage.
     *
     * @return the base directory for session storage
     */
    public File getFolder() {
        return baseDirectory;
    }

    /**
     * Retrieves the session for the given JPro WebAPI.
     *
     * @param webAPI the WebAPI to retrieve the session for
     * @return the session as an ObservableMap containing key-value pairs
     */
    public ObservableMap<String, String> getSession(WebAPI webAPI) {
        String cookieValue = webAPI.getCookies().get(cookieName);
        if (cookieValue == null || !isValidCookie(cookieValue)) {
            cookieValue = null;
        } else {
            File sessionDirectory = new File(baseDirectory, cookieValue);
            if (!sessionDirectory.exists()) {
                cookieValue = null;
            }
        }
        if (cookieValue == null) {
            cookieValue = createUniqueIdentifier();
            webAPI.setCookie(cookieName, cookieValue);
        }
        return getSession(cookieValue);
    }

    /**
     * Retrieves the session for the given session key.
     *
     * @param sessionKey the session key to retrieve the session for
     * @return the session as an ObservableMap containing key-value pairs
     * @throws SessionException if the session retrieval is called from a non-JFX Application Thread
     */
    public ObservableMap<String, String> getSession(String sessionKey) {
        if (!Platform.isFxApplicationThread()) {
            throw new SessionException("Please use the JFX Application Thread!");
        }
        return getSessionCached(sessionKey);
    }

    /**
     * Retrieves the session for the given session key from the session cache.
     *
     * @param sessionKey the session key to retrieve the session for
     * @return the session as an ObservableMap containing key-value pairs
     */
    private ObservableMap<String, String> getSessionCached(String sessionKey) {
        ObservableMap<String, String> res = sessionCache.get(sessionKey);
        if (res != null) return res;
        else {
            res = getSessionImpl(sessionKey);
            sessionCache.put(sessionKey, res);
            return res;
        }
    }

    private ObservableMap<String, String> getSessionImpl(String sessionKey) {
        ObservableMap<String, String> session = FXCollections.observableHashMap();
        if (!baseDirectory.exists()) {
            throw new SessionException("Internal Error: session directory does not exist: " + baseDirectory);
        }
        File cookieDirectory = new File(baseDirectory, sessionKey);
        if (!cookieDirectory.exists()) {
            if (cookieDirectory.mkdir()) {
                logger.info("Created session directory: " + cookieDirectory);
            } else {
                throw new SessionException("Could not create session directory: " + cookieDirectory);
            }
        }

        try {
            for (File file : cookieDirectory.listFiles()) {
                String key = file.getName();
                String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                session.put(key, str);
            }
        } catch (IOException ex) {
            throw new SessionException(ex);
        }

        session.addListener((MapChangeListener<String, String>) change -> {
            String k = change.getKey();
            File f = new File(cookieDirectory, k);
            if (change.wasRemoved()) {
                if (!f.delete()) {
                    logger.warning("Could not delete file: " + f);
                }
            }
            if (change.wasAdded()) {
                try {
                    logger.warning("Saving to: " + f);
                    FileUtils.writeStringToFile(f, change.getValueAdded(), StandardCharsets.UTF_8);
                    if (!f.exists()) {
                        throw new SessionException("Internal Error: file was not written: " + f);
                    }
                } catch (Exception ex) {
                    logger.severe("Error writing session content: " + f);
                }
            }
        });

        return session;
    }

    /**
     * Determines whether a cookie value is valid.
     *
     * @param cookieValue the cookie value to be checked
     * @return {@code true} if the cookie value is a non-negative integer, {@code false} otherwise
     */
    private Boolean isValidCookie(String cookieValue) {
        try {
            return Integer.parseInt(cookieValue) >= 0;
        } catch (NumberFormatException ex) {
            logger.warning("Unexpected cookie format: " + cookieValue);
            return false;
        }
    }

    /**
     * Creates a unique identifier.
     *
     * @return a unique identifier as a String
     */
    private String createUniqueIdentifier() {
        while (true) {
            String newValue = createIdentifier();
            if (!new File(baseDirectory, newValue).exists()) {
                return newValue;
            }
        }
    }

    /**
     * Generates a unique identifier.
     *
     * @return a unique identifier as a String
     */
    private static String createIdentifier() {
        return "" + random.nextInt(Integer.MAX_VALUE);
    }
}
