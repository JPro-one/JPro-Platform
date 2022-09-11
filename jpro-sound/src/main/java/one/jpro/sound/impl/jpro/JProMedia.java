package one.jpro.sound.impl.jpro;

import com.jpro.webapi.WebAPI;
import javafx.stage.Stage;
import one.jpro.sound.Media;
import one.jpro.sound.AudioClip;

import java.net.URL;

public class JProMedia implements Media, AudioClip {

    static int idCounter = 0;
    public String getRandomName() {
        idCounter += 1;
        return "audio" + idCounter;
    }

    public WebAPI webapi;
    public String audioName;


    public JProMedia(Stage stage, URL url) {
        webapi = WebAPI.getWebAPI(stage);
        audioName = getRandomName();
        String publicFile = webapi.createPublicFile(url);
        webapi.loadJSFile(getClass().getResource("/js/howler/howler-2.2.0.min.js"));
        webapi.registerValue(audioName, "new Howl({" +
            "src: ['"+publicFile+"']" +
            "});");
    }



    public void play() {
        JProSound sound = new JProSound(this, webapi);
        sound.setVolume(volume);
        sound.play();
    }

    double volume = 1.0;
    public void setVolume(double volume) {
        volume = 1.0;
    }
}
