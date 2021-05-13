import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import model.Command;
import model.Message;
import model.commandMessage;
import model.dataMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientController implements Initializable {
    private static final String clientPath = "client/clientFiles";
    private byte[] buffer = new byte[102400];
    private int port = 8190;
    private String host = "localhost";
    private ClientNetwork network;

    @FXML
    public ListView<String> listView;
    @FXML
    public TextField login;
    @FXML
    public PasswordField password;

    @FXML
    public void send(ActionEvent actionEvent) throws IOException {
        String fileName = listView.getSelectionModel().getSelectedItem();
        long len = Files.size(Paths.get(clientPath, fileName));

        try (FileInputStream fis2 = new FileInputStream(clientPath + "/" + fileName)) {
            int read;
            while (true) {
                read = fis2.read(buffer);
                if (read == -1) {
                    break;
                }

                byte[] b2 = new byte[read];
                for (int i = 0; i < read; i++) {
                    b2[i] = buffer[i];
                }

                dataMessage mess = new dataMessage(fileName, len);
                mess.setContent(b2);

//                byte[] buf = mess.getContent();
//                StringBuilder st = new StringBuilder();
//                for(int i=0; i<buf.length;i++){
//                    st.append((char) buf[i]);
//                }
//                System.out.println("send: "+st);

//                mess.setContent(buffer);
                network.write(mess);

                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
            }
        }catch (IOException e) {
            System.out.println("Ошибка чтения файла: "+fileName+" "+e);
        }

    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> clientFiles = null;
        try {
            clientFiles = Files.list(Paths.get(clientPath))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        listView.getItems().addAll(clientFiles);

        //        Thread service = new Thread(() -> {
        network = ClientNetwork.getInstance(
                message -> {
                    String msg = "OK";//message.getFileName();
                    Platform.runLater(() -> login.setText(msg));
                }
        );
        //        });
        //        service.setDaemon(true);
        //        service.start();
    }

    public void add(ActionEvent actionEvent) {
//        FileChooser dialog = new FileChooser();
//        dialog.setTitle("Выбор файлов");
//        dialog.getExtensionFilters().addAll(
//                new FileChooser.ExtensionFilter("Все файлы", "*.*")
//        );
//        File result = dialog.showOpenMultipleDialog(window);
//        if (result!=null) System.out.println(result);
//        else System.out.println("отмена");
//
//
    }

    public void registration(ActionEvent actionEvent) {
        commandMessage mess = new commandMessage(Command.REG, login.getText(), password.getText());
        network.write(mess);
    }
}
