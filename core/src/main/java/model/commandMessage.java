package model;

public class commandMessage extends Message {
    private String command;
    private String login;
    private String password;
    private String comment;

    public commandMessage(String command, String login, String password) {
        this.command = command;
        this.login = login;
        this.password = password;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "commandMessage{" +
                "command='" + command + '\'' +
                ", login='" + login + '\'' +
//                ", password='" + password + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
