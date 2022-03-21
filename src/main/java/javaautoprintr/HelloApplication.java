package javaautoprintr;

import com.company.JFXOptionPane;
import com.company.Resource;
import com.company.SystemInfo;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

//import org.kordamp.bootstrapfx.BootstrapFX;

public class HelloApplication extends Application {
    //    Gson = new GsonBuilder().create();
    public static final Path settingsPath = Paths.get("JSON", "Settings.json");
    public static Settings settings;

    @Override
    public void start(Stage stage) throws IOException {
        System.out.println("Running on: " + SystemInfo.CPU_CORE_COUNT() + " cores");
        if (PrintServiceLookup.lookupDefaultPrintService() == null) {
            System.out.println("Default printservice is null, going further with extensive check.");
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            if (Arrays.stream(printServices).peek(printService -> System.out.println("Checking printservice: " + printService)).allMatch(Objects::isNull)) {
                JFXOptionPane.showMessageDialog("No available printers on this system, program will exit");
                System.exit(-3);
            } else {
                System.out.println("Extensive check is OK");
            }
        } else {
            System.out.println("System has printers");
        }
        settings = new Settings();
        if (settingsPath.toFile().exists()) {
            settings = GsonReader.fJson(GsonReader.fromFile(settingsPath), Settings.class) != null ? GsonReader.fJson(GsonReader.fromFile(settingsPath), Settings.class) : new Settings();
            System.out.println("Retrieved: " + settings + "\n@ path " + settingsPath);
        } else {
            System.out.println("No setting's found!!!\n@ path " + settingsPath);
            if (settingsPath.getParent().toFile().mkdirs() || settingsPath.getParent().toFile().exists()) {
                Files.createFile(settingsPath);
                System.out.println("Generated new setting's \n@ path " + settingsPath);
            } else {
                JFXOptionPane.showMessageDialog("Couldn't create directories and files :S");
            }

        }
        //Load and set resources as UI
        System.out.println("Loading fxml!");
        URL helloViewFxml = Resource.findFile("hello-view.fxml").toUri().toURL();
        helloViewFxml = (helloViewFxml.toString() == null || helloViewFxml.toString().isEmpty()) ? HelloApplication.class.getResource("hello-view.fxml") : helloViewFxml;
        System.out.println("Found: " + helloViewFxml);
        FXMLLoader fxmlLoader = new FXMLLoader(helloViewFxml);
        Scene scene = new Scene(fxmlLoader.load(), -1, -1);

        //Do on program close
        stage.setOnCloseRequest(windowEvent -> {
            if (settings != null) {
                System.out.println("Trying to write: ");
                System.out.println(settings);
                String writeResult = GsonReader.toFile(settingsPath, GsonReader.tJson(settings)) ? "Successfully wrote settings-file." : "Failed to write settings-file";
                System.out.println(writeResult);
                if (!HelloController.printedItems.isEmpty()) {
                    HelloController.printedItems.stream().peek(file -> System.out.println("Deleting file that has been printed: " + file)).forEach(File::deleteOnExit);
                }
                System.exit(0);
            } else {
                System.out.println("Cant update settings with null values!");
            }

        });
        stage.setTitle("JavaAutoPrintr");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }

}
