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
            commandExc((commandMessage) message, ctx);
        if (message instanceof dataMessage)
            dataExc((dataMessage) message);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
//        Message mess = new Message("received: OK", 1L);
//        ctx.write(mess);
        ctx.flush();
    }

    private void commandExc(commandMessage message, ChannelHandlerContext ctx) throws IOException {
        if (message.getCommand().startsWith(Command.REG)) {
            if (Server.getAuthService().registrate(message.getLogin(), message.getPassword())) {
                message.setCommand(Command.REG_OK);
            } else {
                message.setCommand(Command.REG_NO);
                message.setComment("регистрация не удалась, измените логин ");
            }
        }

        if (message.getCommand().startsWith(Command.AUTH)) {
            if (Server.getAuthService().authenticate(message.getLogin(), message.getPassword())) {
//                if (!Server.isLoginAuthenticated(message.getLogin())) {
                message.setCommand(Command.AUTH_OK);
            } else {
                message.setCommand(Command.AUTH_NO);
                message.setComment("неверный логин или пароль");
            }
        }

        //todo добавить проверку на зарегистрированность
        if (true) {
            if (message.getCommand().startsWith(Command.GET_FILE_LIST)) {
                message.setComment(getServerFilesAsString());
            }

            if (message.getCommand().startsWith(Command.GET_FILES)) {
                sendFile(ctx, message.getComment());
            }

            if (message.getCommand().startsWith(Command.DEL_FILE)) {
                delFile(message.getComment());
            }
        } else {
            message.setComment("вы не авторизованы");
            log.info(message.getCommand() + " " + message.getLogin());
        }

        ctx.writeAndFlush(message);
    }

    private void dataExc(dataMessage message) {
        Path p = Paths.get(Server.storagePath + message.getFileName());
        try (FileOutputStream fos = new FileOutputStream(p.toString(), true)) {
            fos.write(message.getContent());
        } catch (FileNotFoundException e) {
            log.info(": " + e);
        } catch (IOException e) {
            log.info(": " + e);
        }
        String s = ": " + message.getFileName() + " " + message.getFileSize();//+" "+message.getContent().length;
//        message.setFileName("received OK");
//        ctx.writeAndFlush(message); // кидаем назад
    }

    private String getServerFilesAsString() throws IOException {
        return Files.list(Paths.get("server", "storage")).map(p -> p.getFileName().toString()).collect(Collectors.joining("\n"));
    }

    private void sendFile(ChannelHandlerContext ctx, String fileName) {
        long len = 0;
        try {
            len = Files.size(Paths.get(Server.storagePath, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (
                FileInputStream fis2 = new FileInputStream(Server.storagePath + "/" + fileName)) {
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
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + fileName + " " + e);
        }
    }

    private void delFile(String comment) throws IOException {
        Files.delete(Paths.get("server", "storage", comment));
    }

}