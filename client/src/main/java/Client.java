import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;

public class Client extends Application {
    static final Logger log = Logger.getLogger(String.valueOf(Client.class));

    @Override
    public void start(Stage primaryStage) throws Exception {
        String log4jConfPath = "client/src/main/resources/log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);

        try {
            Parent root = FXMLLoader.load(getClass().getResource("client.fxml"));
            primaryStage.setTitle("CloudStorage");
            primaryStage.setScene(new Scene(root, 400, 600));
            primaryStage.show();
        }
        catch (IOException e){
            log.info("Ошибка запуска сцены: "+e);
        }
    }

    public static void main(String[] args) throws Exception {
        launch(args);
    }

}
