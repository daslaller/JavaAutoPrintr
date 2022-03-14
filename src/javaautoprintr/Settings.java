package javaautoprintr;


import java.util.ArrayList;
import java.util.List;


public class Settings {
    List<String> inputFolders = new ArrayList<>();
    boolean autoPrint = true;
    boolean autoStart = false;

    @Override
    public String toString() {
        return "Settings{" +
                "inputFolders=" + inputFolders +
                '}';
    }
}

