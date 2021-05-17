
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {
    private class UserData {
        String login;
        String password;

        public UserData(String login, String password) {
            this.login = login;
            this.password = password;

        }

//        public void setNickname(String nickname) {
//            this.nickname = nickname;
//        }
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
            System.out.println(e.getMessage());
        }
    }

    private void selectAllUsers() {
        String sql = "SELECT login, password FROM user";
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new UserData(rs.getString("login"), rs.getString("password")));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
        }
    }

//    private boolean update(String login) {
//        String sql = "UPDATE user SET nickname = ? "
//                + "WHERE login = ?";
//
//        try (Connection conn = getConnection();
//             PreparedStatement pstmt = conn.prepareStatement(sql)) {
//            pstmt.setString(1, nickName);
//            pstmt.setString(2, login);
//            pstmt.executeUpdate();
//        } catch (SQLException e) {
//            System.out.println(e.getMessage());
//        }
//        return true;
//    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password)) return "nickname";
//            else return "";
        }
        return null;
    }

    @Override
    public boolean registrate(String login, String password) {

        for (UserData user : users) {
            if (user.login.equals(login)) return false;
        }
        users.add(new UserData(login, password));
        insertUser(login, password);
        return true;
    }

    @Override
    public boolean authenticate(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password))
                return true;
        }
        return false;
    }

}