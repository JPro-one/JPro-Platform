package one.jpro.sessionmanager;

import com.jpro.webapi.WebAPI;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class SessionManager {

    private static Logger logger = Logger.getLogger("one.jpro.sessionmanager.SessionManager");

    private File baseDirectory;
    private String cookieName;
    private static Random random = new Random();

    public SessionManager(String appName) {
        this(new File(new File(System.getProperty("user.home")), "." + appName).getAbsoluteFile(), "c-"+appName);
    }

    public SessionManager(File baseDirectory, String cookieName) {
        this.baseDirectory = baseDirectory;
        this.cookieName = cookieName;
        if(!baseDirectory.exists()) {
            baseDirectory.mkdir();
        }

    }

    public File getFolder() {
        return baseDirectory;
    }

    public ObservableMap<String,String> getSession(WebAPI webAPI) {
        String cookieValue = webAPI.getCookies().get(cookieName);
        if(!isValidCookie(cookieValue)) {
            cookieValue = null;
        } else {
            File sessionDirectory = new File(baseDirectory, cookieValue);
            if(!sessionDirectory.exists()) {
                cookieValue = null;
            }
        }
        if(cookieValue == null) {
            cookieValue = createUniqueIdentifier();
            webAPI.setCookie(cookieName, cookieValue);
        }
        return getSession(cookieValue);
    }

    public ObservableMap<String,String> getSession(String sessionKey) {
        if(!Platform.isFxApplicationThread()) {
            throw new RuntimeException("Please use the JFX Application Thread!");
        }
        return getSessionCached(sessionKey);
    }

    private WeakHashMap<String, ObservableMap<String,String>> sessionCache = new WeakHashMap<>();
    private ObservableMap<String,String> getSessionCached(String sessionKey) {
        ObservableMap<String,String> res = sessionCache.get(sessionKey);
        if(res != null) return res;
        else {
            res = getSessionImpl(sessionKey);
            sessionCache.put(sessionKey, res);
            return res;
        }
    }

    private ObservableMap<String,String> getSessionImpl(String sessionKey) {
        ObservableMap<String,String> session = FXCollections.observableHashMap();
        if(!baseDirectory.exists()) {
            throw new RuntimeException("Internal Error: session directory does not exist: " + baseDirectory);
        }
        File cookieDirectory = new File(baseDirectory, sessionKey);
        if(!cookieDirectory.exists()) {
            cookieDirectory.mkdir();
        }

        try {
            for(File file: cookieDirectory.listFiles()) {
                String key = file.getName();
                String str = FileUtils.readFileToString(file, Charset.forName("utf-8"));
                session.put(key,str);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        session.addListener((MapChangeListener<String,String>) change -> {
            String k = change.getKey();
            File f = new File(cookieDirectory, k);
            if(change.wasRemoved()) {
                f.delete();
            }
            if(change.wasAdded()) {
                try {
                    logger.warning("Saving to: " + f);
                    FileUtils.writeStringToFile(f, change.getValueAdded(), Charset.forName("utf-8"));
                    if(!f.exists()) {
                        throw new RuntimeException("Internal Error: file was not written: " + f);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error writing session content: " + f);
                }
            }
        });

        return session;
    }

     private Boolean isValidCookie(String cookieValue) {
        try {
            int i = Integer.valueOf(cookieValue);
            return i >= 0 && i <= Integer.MAX_VALUE;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Unexpected cookie format: " + cookieValue);
            return false;
        }
    }

    private String createUniqueIdentifier() {
        while (true) {
            String newValue = createIdentifier();
            if (!new File(baseDirectory, newValue).exists()) {
                return newValue;
            }
        }
    }

    private static String createIdentifier() {
        return "" + random.nextInt(Integer.MAX_VALUE);
    }
}
