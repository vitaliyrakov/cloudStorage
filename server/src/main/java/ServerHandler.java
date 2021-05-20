import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import model.Command;
import model.Message;
import model.commandMessage;
import model.dataMessage;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    static final Logger log = Logger.getLogger(String.valueOf(Server.class));
    private byte[] buffer = new byte[102400];

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("client connected!");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("client disconnected.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        log.info("channel closed.");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        if (message instanceof commandMessage)
            executeCommand((commandMessage) message, ctx);
        if (message instanceof dataMessage)
            saveData((dataMessage) message);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void executeCommand(commandMessage message, ChannelHandlerContext ctx) throws IOException {
        if (message.getCommand().startsWith(Command.REG)) {
            if (Server.getAuthService().register(message.getLogin(), message.getPassword())) {
                message.setCommand(Command.REG_OK);
            } else {
                message.setCommand(Command.REG_NO);
                message.setComment("регистрация не удалась, измените логин ");
            }
        } else {
            if (message.getCommand().startsWith(Command.AUTH)) {
                if (Server.getAuthService().authenticate(message.getLogin(), message.getPassword())) {
                    message.setCommand(Command.AUTH_OK);
                } else {
                    message.setCommand(Command.AUTH_NO);
                    message.setComment("неверный логин или пароль");
                }
            } else {
                if (Server.getAuthService().isLoginAuthenticated(message.getLogin())) {
                    if (message.getCommand().startsWith(Command.GET_FILE_LIST))
                        message.setComment(getServerFilesAsString(message.getLogin()));

                    if (message.getCommand().startsWith(Command.GET_FILES))
                        message.setCommand(sendFile(ctx, message)
                                ? Command.GET_FILES_OK : Command.GET_FILES_NOTOK);

                    if (message.getCommand().startsWith(Command.DEL_FILE))
                        message.setCommand(delFile(message.getLogin(), message.getComment())
                                ? Command.DEL_FILE_OK : Command.DEL_FILE_NOTOK);

                    if (message.getCommand().startsWith(Command.END))
                        message.setCommand(Server.getAuthService().exit(message.getLogin())
                                ? Command.END_OK : Command.END_NOTOK);

                } else {
                    message.setComment("вы не авторизованы");
                    log.info(message.getCommand() + " " + message.getLogin());
                }
            }
        }
        ctx.writeAndFlush(message);
    }

    private void saveData(dataMessage message) {
        createDirIfNotExist(message.getLogin());
        Path p = Paths.get(Server.storagePath, message.getLogin(), message.getFileName());
        try (FileOutputStream fos = new FileOutputStream(p.toString(), true)) {
            fos.write(message.getContent());
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.info(e);
        }
    }

    private String getServerFilesAsString(String login) throws IOException {
        createDirIfNotExist(login);
        return Files.list(Paths.get(Server.storagePath, login)).map(p -> p.getFileName().toString()).collect(Collectors.joining("\n"));
    }

    private boolean sendFile(ChannelHandlerContext ctx, commandMessage message) throws IOException {
        String fileName = message.getComment();
        long len = Files.size(Paths.get(Server.storagePath, message.getLogin(), fileName));
        try (FileInputStream fis2 = new FileInputStream(Server.storagePath + message.getLogin() + "/" + fileName)) {
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
                ctx.write(mess);

                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
            }
            return true;

        } catch (IOException e) {
            log.error("Ошибка чтения файла: " + fileName + " " + e);
            return false;
        }
    }

    private boolean delFile(String login, String fileName) {
        try {
            Files.delete(Paths.get(Server.storagePath, login, fileName));
        } catch (IOException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    public static void createDirIfNotExist(String login) {
        Path fdir = Paths.get(Server.storagePath, login);
        if (Files.notExists(fdir)) {
            try {
                Files.createDirectory(fdir);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }
}