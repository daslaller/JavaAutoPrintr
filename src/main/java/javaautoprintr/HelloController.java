package javaautoprintr;

import com.company.JFXOptionPane;
import com.company.SystemInfo;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;
import javaautoprintr.HelloApplication;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.web.WebView;

import javax.print.PrintService;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HelloController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private JFXButton addFolderButton_JFXButton;

    @FXML
    private ListView<File> inputFolderList_ListView;

    @FXML
    private JFXToggleButton enableAutoPrint_JFXToggleButton;

    @FXML
    private WebView tempView;


    @FXML
    private Label statusLabel_Label;

    @FXML
    private JFXCheckBox startProgram_JFXCheckBox;

    @FXML
    private JFXCheckBox autoPrintOnStart_JFXCheckBox;

    @FXML
    private ProgressIndicator runIndicatorProgressIndicator;

    static public final LinkedHashSet<File> printedItems = new LinkedHashSet<>();
    public final ObservableList<File> items = FXCollections.observableArrayList();
    Timer folderPollTimer = new Timer();
    PrintService printer;
    ExecutorService es = Executors.newFixedThreadPool(SystemInfo.CPU_CORE_COUNT());

    @FXML
    void addFolderButton_JFXButton_ActionPerformed(ActionEvent event) {
        File file = JFXOptionPane.showSelectDirectoryChooser(((Node) event.getTarget()).getScene().getWindow());
        if (file != null && file.exists() && file.isDirectory()) {
            items.add(file);
            System.out.println("Choose dir: " + file + " Saving to: " + HelloApplication.settings);
        } else {
            JFXOptionPane.showMessageDialog("No directory selected.");
        }
    }

    @SuppressWarnings("EmptyMethod")
    @FXML
    void enableAutoPrint_JFXToggleButton_ActionPerformed(ActionEvent event) {
        if (enableAutoPrint_JFXToggleButton.isSelected()) {

        } else {

        }
    }

    @FXML
    void startProgram_JFXCheckBox_ActionPerformed(ActionEvent event) {

    }

    @FXML
    void autoPrintOnStart_JFXCheckBox_ActionPerformed(ActionEvent event) {

    }

    @SuppressWarnings("rawtypes")
    @FXML
    void initialize() {
        assert addFolderButton_JFXButton != null : "fx:id=\"addFolderButton_JFXButton\" was not injected: check your FXML file 'hello-view.fxml'.";
        inputFolderList_ListView.setItems(items);
        autoPrintOnStart_JFXCheckBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> HelloApplication.settings.autoPrint = autoPrintOnStart_JFXCheckBox.isSelected());
        startProgram_JFXCheckBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> {

            //Should put jar files here for autostart
            String path = HelloApplication.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decode = URLDecoder.decode(path);
            String userDir = SystemInfo.SysInfo.USER_DIR.toString();
            System.out.println("Pre decode: " + path + "\nAfter decode: " + decode + "\nUser: " + userDir);
//            Files.copy();

        });
        enableAutoPrint_JFXToggleButton.selectedProperty().addListener((observableValue, oldValue, newValue) -> {
            if (enableAutoPrint_JFXToggleButton.isSelected()) {
                System.out.println("IS SELECTED");
                System.out.println("Items: " + items);
            } else {
                System.out.println("NOT SELECTED");
//                folderPollTimer.cancel();
//                enableAutoPrint_JFXToggleButton.setSelected(false);
            }
        });
        enableAutoPrint_JFXToggleButton.setSelected(HelloApplication.settings.autoPrint);
        autoPrintOnStart_JFXCheckBox.setSelected(HelloApplication.settings.autoPrint);


        inputFolderList_ListView.setCellFactory(fileListView -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem();
            ListCell<File> cell = new ListCell<>() {

                @Override
                protected void updateItem(File item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item.toString());
                    } else {
                        setText("");
                    }
                }
            };
            deleteItem.textProperty().bind(Bindings.format("Delete \"%s\"", cell.itemProperty()));
            deleteItem.setOnAction(event -> inputFolderList_ListView.getItems().remove(cell.getItem()));
            contextMenu.getItems().addAll(deleteItem);
            cell.setContextMenu(contextMenu);
            return cell;
        });
        printer = PrintPDF.dialog();
        items.addListener((ListChangeListener<? super File>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().stream().peek((file -> {
                        try {
                            System.out.println("Registered directory watcher for directory: " + file);
                            System.out.println("Dessignating printer for chosen directory to: " + printer);
                            PrintPDF printPDF = new PrintPDF(printer);
//                            printPDF.setSelectedService(printer);
                            Task thread = DirectoryWatch.watchDirectory(file.toPath(), true, printPDF);
                            runIndicatorProgressIndicator.setProgress(-1);
                            assert thread != null : "was null when not able to";
                            Thread t = new Thread(thread);
                            t.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })).map(File::toString).forEach((it) -> {
                        if (!HelloApplication.settings.inputFolders.contains(it)) {
                            HelloApplication.settings.inputFolders.add(it);
                        }
                    });
                }
                if (change.wasRemoved()) {
                    List<String> collect = change.getRemoved().stream().map(File::toString).collect(Collectors.toList());
                    HelloApplication.settings.inputFolders.removeAll(collect);
                }
            }
        });
        items.addAll(HelloApplication.settings.inputFolders.stream().map(Paths::get).map(Path::toFile).collect(Collectors.toList()));



    }

    public void safeUpdateStatus(String text) {
        Platform.runLater(() -> statusLabel_Label.setText(text));
    }
}
