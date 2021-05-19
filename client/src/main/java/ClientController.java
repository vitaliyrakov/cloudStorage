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
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    private static final String clientPath = "client/clientFiles";
    private byte[] buffer = new byte[102400];
    //    private int port = 8189;
//    private String host = "localhost";
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
    @FXML
    public ProgressBar progressBar;
    @FXML
    public ProgressIndicator progressIndicator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        List<String> clientFiles = null;
        setRegView();

        //        Thread service = new Thread(() -> {
        network = ClientNetwork.getInstance(
                message -> {
                    String msg = message.toString();
                    Platform.runLater(() ->
                    {
                        if ((message instanceof commandMessage) &&
                                ((((commandMessage) message).getCommand().startsWith(Command.REG_OK)) ||
                                        (((commandMessage) message).getCommand().startsWith(Command.AUTH_OK)))

                        ) {
                            setWorkView();
                        } else {
                            if ((message instanceof commandMessage) && (((commandMessage) message).getCommand().startsWith(Command.GET_FILE_LIST))) {
                                listView.getItems().removeAll();
                                listView.getItems().addAll(Arrays.asList(((commandMessage) message).getComment().split("\n")));
                            } else {
                                if (message instanceof dataMessage) {
                                    saveFile((dataMessage) message);
                                }
                            }
                        }
                        response.setText(msg);
                    });
                }
        );
        //        });
        //        service.setDaemon(true);
        //        service.start();
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
        //        todo можно добавить сложность пароля
        if (login.getText().isEmpty() || password.getText().isEmpty())
            return false;
        else return true;
    }

    @FXML
    public void add(ActionEvent actionEvent) throws IOException {
        FileChooser dialog = new FileChooser();
        dialog.setTitle("Выбор файлов");
        dialog.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );
        File result = dialog.showOpenDialog(Client.ps);
        if (result != null) {
            listView.getItems().add(result.getName());
            send(result);
        }
    }

    @FXML
    public void del(ActionEvent actionEvent) throws IOException {
        String fileName = listView.getSelectionModel().getSelectedItem();
        commandMessage cm = new commandMessage(Command.DEL_FILE, login.getText(), password.getText());
        cm.setComment(fileName);
        network.write(cm);
        listView.getItems().remove(fileName);
    }

    public void send(File result) throws IOException {
        String fileName = result.getName(); //listView.getSelectionModel().getSelectedItem();
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
                network.write(mess);

                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + fileName + " " + e);
        }
    }

    public void clearResponse() {
        response.setText("");
    }

    @FXML
    public void quit(ActionEvent actionEvent) {
        setRegView();
        //            commandMessage mess;
////            mess = new commandMessage(Command.END, login.getText(), password.getText());
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
        progressBar.setVisible(false);
        progressIndicator.setVisible(false);
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
//        progressBar.setVisible(true);
//        progressIndicator.setVisible(true);
        network.write(new commandMessage(Command.GET_FILE_LIST, login.getText(), password.getText()));
    }

    @FXML
    public void downLoad(MouseEvent mouseEvent) throws InterruptedException {
        commandMessage gf = new commandMessage(Command.GET_FILES, login.getText(), password.getText());
        gf.setComment(listView.getSelectionModel().getSelectedItem());
        network.write(gf);
        Thread.sleep(2000);
//        Path p = Paths.get(Client.fl + gf.getComment());
//        try {
//            Desktop.getDesktop().open(new File(Client.fl + gf.getComment()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        response.setText(gf.getComment());
    }

    public void saveFile(dataMessage mess) {
        Path p = Paths.get(Client.fl + mess.getFileName());
        try (FileOutputStream fos = new FileOutputStream(p.toString(), true)) {
            fos.write(mess.getContent());
        } catch (FileNotFoundException e) {
            System.out.println(": " + e);
//            log.info(": " + e);
//            e.printStackTrace();
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println(": " + e);
//            log.info(": " + e);
        }
    }


}
