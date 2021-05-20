public interface AuthService {

    boolean register(String login, String password);

    boolean authenticate(String login, String password);

    boolean isLoginAuthenticated(String login);

    boolean exit(String login);
}