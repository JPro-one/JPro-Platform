package one.jpro.sound.impl.jpro;

import com.jpro.webapi.WebAPI;
import one.jpro.sound.MediaPlayer;

public class JProSound implements MediaPlayer {

    WebAPI api;
    String name;

    public JProSound(JProMedia media, WebAPI api) {
        if(api != media.webapi) {
            throw new RuntimeException("Media had a different webapi as the Sound");
        }
        this.api = api;
        this.name = media.audioName;

    }

    public void play() {
        api.executeScript("jpro."+name+".play();");
    }

    public void pause() {
        api.executeScript("jpro."+name+".pause();");
    }

    public void stop() {
        api.executeScript("jpro."+name+".pause();");
    }

    public void setVolume(double volume) {
        api.executeScript("jpro."+name+".volume("+volume+");");
    }

    public void setLoop(boolean loop) {
        api.executeScript("jpro."+name+".loop("+loop+");");
    }
}
