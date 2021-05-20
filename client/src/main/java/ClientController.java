import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import model.Command;
import model.commandMessage;
import model.dataMessage;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    static final Logger log = Logger.getLogger(String.valueOf(ClientController.class));
    private byte[] buffer = new byte[102400];
    private ClientNetwork network;

    @FXML
    public ListView<String> listView;
    @FXML
    public TextField login;
    @FXML
    public Label response;
    @FXML
    public PasswordField password;
    @FXML
    public Button reg;
    @FXML
    public Button auth;
    @FXML
    public Button add;
    @FXML
    public Button del;
    @FXML
    public Button quit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setRegView();

        network = ClientNetwork.getInstance(
                message -> {
                    String msg = message.toString();
                    Platform.runLater(() ->
                    {
                        if (message instanceof commandMessage) {
                            String comMess = ((commandMessage) message).getCommand();
                            if ((comMess.startsWith(Command.REG_OK)) || (comMess.startsWith(Command.AUTH_OK))) {
                                Client.login = ((commandMessage) message).getLogin();
                                setWorkView();
                                Client.ps.setTitle(Client.title + " - пользователь: " + Client.login);
                                network.write(new commandMessage(Command.GET_FILE_LIST, login.getText(), password.getText()));
                            }
                            if ((comMess.startsWith(Command.REG_NO)) || (comMess.startsWith(Command.AUTH_NO)))
                                response.setText(((commandMessage) message).getComment());

                            if (comMess.startsWith(Command.GET_FILE_LIST)) {
                                listView.getItems().clear();
                                if (!((commandMessage) message).getComment().isEmpty())
                                    listView.getItems().addAll(Arrays.asList(((commandMessage) message).getComment().split("\n")));
                            }
                        }

                        if (message instanceof dataMessage)
                            saveFile((dataMessage) message);
                    });
                }
        );
    }

    @FXML
    public void registrate(ActionEvent actionEvent) {
        if (!checkLogPass()) response.setText("Введите логин и пароль");
        else network.write(new commandMessage(Command.REG, login.getText(), password.getText()));
    }

    @FXML
    public void authenticate(ActionEvent actionEvent) {
        if (!checkLogPass()) response.setText("Введите логин и пароль");
        else network.write(new commandMessage(Command.AUTH, login.getText(), password.getText()));
    }

    private boolean checkLogPass() {
        if (login.getText().isEmpty() || password.getText().isEmpty())
            return false;
        else return true;
    }

    @FXML
    public void add(ActionEvent actionEvent) throws IOException {
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Выбор файлов");
        dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Все файлы", "*.*"));
        File result = dialog.showOpenDialog(Client.ps);
        if (result != null) {
            listView.getItems().add(result.getName());
            upLoad(result);
        }
    }

    @FXML
    public void del(ActionEvent actionEvent) throws IOException {
        String fileName = listView.getSelectionModel().getSelectedItem();
        commandMessage cm = new commandMessage(Command.DEL_FILE, login.getText(), password.getText());
        cm.setComment(fileName);
        network.write(cm);
        listView.getItems().remove(fileName);

        Path fp = Paths.get(Client.fl, Client.login, fileName);
        if (Files.exists(fp)) {
            try {
                Files.delete(fp);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

    @FXML
    public void quit(ActionEvent actionEvent) {
        listView.getItems().clear();
        Client.ps.setTitle(Client.title);
        network.write(new commandMessage(Command.END, login.getText(), password.getText()));
        setRegView();
    }

    @FXML
    public void downLoad(MouseEvent mouseEvent) throws InterruptedException {
        if (!listView.getItems().isEmpty()) {
            commandMessage gf = new commandMessage(Command.GET_FILES, login.getText(), password.getText());
            gf.setComment(listView.getSelectionModel().getSelectedItem());
            network.write(gf);
//        Thread.sleep(2000);
            Path p = Paths.get(Client.fl, Client.login, gf.getComment());
//        try {
//            Desktop.getDesktop().open(new File(p.toString()));
//            Desktop.getDesktop().open(new File(p.toString()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        }
    }

    public void upLoad(File result) throws IOException {
        String fileName = result.getName();
        long len = Files.size(Paths.get(result.toString()));

        try (FileInputStream fis2 = new FileInputStream(result.getAbsolutePath())) {
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
                mess.setLogin(Client.login);
                network.write(mess);

                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
            }
        } catch (IOException e) {
            log.info("Ошибка чтения файла: " + fileName + " " + e);
        }
    }

    public void clearResponse() {
        response.setText("");
    }

    public void setRegView() {
        login.setVisible(true);
        login.setText("");
        password.setVisible(true);
        password.setText("");
        reg.setVisible(true);
        response.setText("");
        listView.setVisible(false);
        add.setVisible(false);
        del.setVisible(false);
        quit.setVisible(false);
        auth.setVisible(true);
    }

    public void setWorkView() {
        login.setVisible(false);
        password.setVisible(false);
        reg.setVisible(false);
        auth.setVisible(false);
        quit.setVisible(true);
        listView.setVisible(true);
        add.setVisible(true);
        del.setVisible(true);
    }

    public void saveFile(dataMessage mess) {
        createDirIfNotExist(Client.login);

        Path p = Paths.get(Client.fl, Client.login, mess.getFileName());
        try {
            if (Files.notExists(p) || Files.size(p) < mess.getFileSize()) {

                try (FileOutputStream fos = new FileOutputStream(p.toString(), true)) {
                    fos.write(mess.getContent());
                } catch (FileNotFoundException e) {
                    log.info(": " + e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createDirIfNotExist(String login) {
        Path fdir = Paths.get(Client.fl, Client.login);
        if (Files.notExists(fdir)) {
            try {
                Files.createDirectory(fdir);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }

}
