import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import java.io.IOException;

public class Client extends Application {
    static final Logger log = Logger.getLogger(String.valueOf(Client.class));
    static final String fl = "client/tmpStorage/";
    static String login;
    static Stage ps;
    static String title = "CloudStorage";

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("client.fxml"));
            ps = primaryStage;
            primaryStage.setTitle(title);
            primaryStage.setScene(new Scene(root, 400, 600));
            primaryStage.show();
            root.requestFocus();
        } catch (IOException e) {
            log.info("Ошибка запуска сцены: " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }

}
