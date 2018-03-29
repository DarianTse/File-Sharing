import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;


public class Client extends Application {

    //Socket info
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    String host = "127.0.0.1";
    int port = 20000;

    //UI
    private ListView<String> localFiles;
    private ListView<String> serverFiles;

    private Label hostLabel;
    private TextField ipField;
    private Label debugLabel;
    private Button download;
    private Button requestFiles;
    private Button setLocalDirectory;
    private Button upload;

    //File path of local folder
    private File localDirectory = null;

    //The file selected in the serverFiles list
    private String selectedServerFile;
    //The file selected in the localFiles list
    private String selectedLocalFile;

    public static void main(String[] args) throws IOException {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Assignment 02");
        primaryStage.setResizable(false);

        BorderPane borderPane = new BorderPane();


        //Create list views
        ObservableList<String> localFilesList = FXCollections.<String>observableArrayList();
        this.localFiles = new ListView<>(localFilesList);
        this.localFiles.setPrefSize(360, 480);
        this.localFiles.setOrientation(Orientation.VERTICAL);
        borderPane.setLeft(localFiles);

        ObservableList<String> serverFilesList = FXCollections.<String>observableArrayList();
        this.serverFiles = new ListView<>(serverFilesList);
        this.serverFiles.setPrefSize(360, 480);
        this.serverFiles.setOrientation(Orientation.VERTICAL);
        borderPane.setRight(serverFiles);


        //HBOX items
        this.hostLabel = new Label("Server IP:");

        this.ipField = new TextField();
        ipField.setText("127.0.0.1");

        this.debugLabel = new Label("");

        this.setLocalDirectory = new Button("Local Directory");
        this.setLocalDirectory.setDefaultButton(true);
        this.setLocalDirectory.setOnAction(e -> assignDirectory(primaryStage));

        this.download = new Button("Download File");
        this.download.setDefaultButton(true);
        this.download.setOnAction(e -> downloadFile());

        this.upload = new Button("Upload File");
        this.upload.setDefaultButton(true);
        this.upload.setOnAction(e -> uploadFile());

        this.requestFiles = new Button("Request Files");
        this.requestFiles.setDefaultButton(true);
        this.requestFiles.setOnAction(e -> requestServerFiles());

        HBox hbox = new HBox();
        hbox.setSpacing(1);
        //Assign buttons to hbox
        hbox.getChildren().addAll(setLocalDirectory, hostLabel, ipField, debugLabel, requestFiles, upload,  download);
        borderPane.setTop(hbox);

        Scene scene = new Scene(borderPane, 720, 480);
        primaryStage.setScene((scene));
        primaryStage.show();
    }

    public void assignDirectory(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        localDirectory = directoryChooser.showDialog(stage);

        updateDirectory();
    }

    public void updateDirectory() {
        if (localDirectory.isDirectory()) { //If its a directory
            File[] contents = localDirectory.listFiles();
            for (File current : contents) {
                if (!localFiles.getItems().contains(current.getName())) {
                    addToLocalList(current.getName());
                }
            }
        }
    }

    public void addToLocalList(String fileName) {
        this.localFiles.getItems().add(fileName);
    }

    public void addToServerList(String fileName) {
        this.serverFiles.getItems().add(fileName);
    }


    public void requestServerFiles() {
        try {
            setupConnection();
            if (socket == null) return;

            //send request message
            out.print("DIR " + "STUB" + " \r\n");
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (!serverFiles.getItems().contains(line)) {
                    addToServerList(line);
                }
            }

            closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Download selected server file
    public void downloadFile() {
        selectedServerFile = serverFiles.getSelectionModel().getSelectedItem();
        if(selectedServerFile == null){debugLabel.setText("No server file selected to download");return;}
        try {
            setupConnection();
            if (socket == null) {
                return;
            }

            //Send message to server
            out.print("DOWNLOAD " + selectedServerFile + " \r\n");
            out.flush();

            String line;

            //Write to local file
            File newFile = new File(localDirectory + "/" + selectedServerFile);
            //if it doesnt exist
            if (!newFile.exists()) {
                newFile.createNewFile();

                PrintWriter fileOut = new PrintWriter(newFile);
                while ((line = in.readLine()) != null) {
                    fileOut.println(line);
                }
                fileOut.close();
            }
            closeConnection();
            if (localDirectory != null) {
                updateDirectory();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(){
        selectedLocalFile = localFiles.getSelectionModel().getSelectedItem();
        if(selectedLocalFile == null){debugLabel.setText("No local file selected to upload"); return;}
        try {
            setupConnection();
            if (socket == null) {
                return;
            }

            File file = new File(localDirectory, selectedLocalFile);

            if(!file.exists()){
                debugLabel.setText("Could not find file in local folder");
            } else{
                byte[] content = new byte[(int)file.length()];
                FileInputStream fileIn = new FileInputStream(file);
                fileIn.read(content);
                fileIn.close();

                String data = new String(content); //Content inside file
                //Send message to server
                out.print("UPLOAD " + selectedLocalFile + " \r\n");
                out.print(data);
                out.flush();
            }

            closeConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        requestServerFiles();
    }

    private void setupConnection() throws IOException {
        debugLabel.setText("");
        try {
            socket = new Socket(ipField.getText(), port);
        } catch (SocketException e) {
            debugLabel.setText("Invalid IP");
            socket = null;
            e.printStackTrace();
        }

        if (socket != null) {
            in = new BufferedReader(new InputStreamReader((socket.getInputStream())));
            out = new PrintWriter(socket.getOutputStream());
        }
    }

    private void closeConnection() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}