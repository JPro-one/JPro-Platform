package one.jpro.platform.auth.routing.buttons;

import javafx.scene.control.Button;
import javafx.scene.image.Image;

import java.net.URL;

public class GoogleLoginButton extends Button {

    public GoogleLoginButton() {

        setStyle("-fx-background-color: transparent; " +
                "-fx-background-image: url(/one/jpro/platform/auth/routing/buttons/google/web_light_sq_SI@2x.png);" +
                "-fx-background-size: cover;" +
                "-fx-pref-width: 175;" +
                "-fx-pref-height: 40;");
    }
}
