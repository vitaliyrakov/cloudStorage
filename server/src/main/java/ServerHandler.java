import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import model.Command;
import model.Message;
import model.commandMessage;
import model.dataMessage;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    static final Logger log = Logger.getLogger(String.valueOf(Server.class));

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("client connected!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        if (message instanceof commandMessage) {
            log.info(String.format("%s: Server received command message from %s\nMessage: %s",
                    getClass().getSimpleName(), ctx.channel().remoteAddress(), message));
//            ServiceExecutor.execute(message);
//            ctx.writeAndFlush(message);
//        } else {
//            ctx.fireChannelRead(message);
            commandExc((commandMessage) message, ctx);
        }

        if (message instanceof dataMessage) {
            log.info(String.format("%s: Server received service message from %s\nMessage: %s",
                    getClass().getSimpleName(), ctx.channel().remoteAddress(), message));
//            ServiceExecutor.execute(message);
//            ctx.writeAndFlush(message);

            dataExc((dataMessage) message);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
//        Message mess = new Message("received: OK", 1L);
//        ctx.write(mess);
        ctx.flush();
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

    public static void dataExc(dataMessage message) {
        //        log.debug("received: {}", message);
        Path p = Paths.get(Server.storagePath + message.getFileName());

//        byte[] buf = message.getContent();
//        StringBuilder st = new StringBuilder();
//        for(int i=0; i<buf.length;i++){
//            st.append((char) buf[i]);
//        }
//        log.info("received: "+st);

        try (FileOutputStream fos = new FileOutputStream(p.toString(), true)) {
            fos.write(message.getContent());
        } catch (FileNotFoundException e) {
            log.info(": " + e);
//            e.printStackTrace();
        } catch (IOException e) {
//            e.printStackTrace();
            log.info(": " + e);
        }

        String s = ": " + message.getFileName() + " " + message.getFileSize();//+" "+message.getContent().length;
        log.info("received: " + s);

//        message.setFileName("received OK");
//        ctx.writeAndFlush(message); // кидаем назад
    }

    private void commandExc(commandMessage message, ChannelHandlerContext ctx) throws IOException {
        if (message.getCommand().startsWith(Command.REG)) {
            if (Server.getAuthService().registrate(message.getLogin(), message.getPassword())) {
                message.setCommand(Command.REG_OK);
                log.info(Command.REG_OK + " " + message.getLogin());
            } else {
                message.setCommand(Command.REG_NO);
                message.setComment("регистрация не удалась, измените логин ");
                log.info(Command.REG_NO + " " + message.getLogin());
            }
        }

        if (message.getCommand().startsWith(Command.AUTH)) {
            if (Server.getAuthService().authenticate(message.getLogin(), message.getPassword())) {
//                if (!Server.isLoginAuthenticated(message.getLogin())) {
//                    nickname = newNick;
                message.setCommand(Command.AUTH_OK);
                log.info(Command.AUTH_OK + " " + message.getLogin());
//                    server.subscribe(this);
//                    break;
//                } else {
//                    log.info("С этим логинов уже вошли");
//                    sendMsg("С этим логинов уже вошли");
//            }
            } else {
                message.setCommand(Command.AUTH_NO);
                message.setComment("неверный логин или пароль");
                log.info(Command.AUTH_NO + " " + message.getLogin());
            }
        }

        if (message.getCommand().startsWith(Command.GET_FILE_LIST)) {
            if (true) {
                //todo добавить проверку на зарегистрированность
//                message.setCommand(Command.REG_OK);
                message.setComment(getServerFilesAsString());
                log.info(Command.GET_FILE_LIST);
            } else {
//                message.setCommand(Command.GET_FILE_LIST);
                message.setComment("вы не авторизованы");
                log.info(Command.GET_FILE_LIST + " " + message.getLogin());
            }
        }

        ctx.writeAndFlush(message); // кидаем назад
    }

    private String getServerFilesAsString() throws IOException {
        String s = Files.list(Paths.get("server", "storage")).map(p -> p.getFileName().toString()).collect(Collectors.joining("\n"));
        return s;
    }

}