
import org.apache.log4j.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {
    static final Logger log = Logger.getLogger(String.valueOf(Server.class));
    private class UserData {
        String login;
        String password;
        Boolean auth;

        public UserData(String login, String password, boolean auth) {
            this.login = login;
            this.password = password;
            this.auth = auth;
        }
    }

    private List<UserData> users;

    public SimpleAuthService() {
        users = new ArrayList<>();
        init();
        createTableUser();
        selectAllUsers();
    }

    private void init() {
        try {
            Class.forName("org.sqlite.SQLiteJDBCLoader");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        String URL = "jdbc:sqlite:cloudUsers.db";
        return DriverManager.getConnection(URL);
    }

    private void createTableUser() {
        String sql = "CREATE TABLE IF NOT EXISTS user (\n"
                + "	login text NOT NULL,\n"
                + "	password text NOT NULL\n"
                + ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error(e);
        }
    }

    private void selectAllUsers() {
        String sql = "SELECT login, password FROM user";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new UserData(rs.getString("login"), rs.getString("password"),false));
            }
        } catch (SQLException e) {
            log.error(e);
        }
    }

    private void insertUser(String login, String password) {
        String sql = "INSERT INTO user(login,password) VALUES(?,?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error(e);
        }
    }

    @Override
    public boolean register(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login)) return false;
        }
        users.add(new UserData(login, password, true));
        insertUser(login, password);
        ServerHandler.createDirIfNotExist(login);
        return true;
    }

    @Override
    public boolean authenticate(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password)){
                user.auth = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isLoginAuthenticated(String login) {
        for (UserData user : users) {
            if (user.login.equals(login)) {
                return user.auth;
            }
        }
        return false;
    }
    @Override
    public boolean exit(String login) {
        for (UserData user : users) {
            if (user.login.equals(login)) {
                user.auth = false;
                return true;
            }
        }
        return false;
    }

}