public interface AuthService {
    String getNicknameByLoginAndPassword(String login, String password);

    boolean registrate(String login, String password);

    boolean authenticate(String login, String password);
}