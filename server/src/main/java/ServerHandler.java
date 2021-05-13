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
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    static final Logger log = Logger.getLogger(String.valueOf(Server.class));

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("client connected!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        if (message instanceof commandMessage) {
            log.debug(String.format("%s: Server received command message from %s\nMessage: %s",
                    getClass().getSimpleName(), ctx.channel().remoteAddress(), message));
//            ServiceExecutor.execute(message);
//            ctx.writeAndFlush(message);
//        } else {
//            ctx.fireChannelRead(message);
            commandExc((commandMessage) message);
        }

        if (message instanceof dataMessage) {
            log.debug(String.format("%s: Server received service message from %s\nMessage: %s",
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
        Path p = Paths.get("./server/storage/" + message.getFileName());

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

    private void commandExc(commandMessage message) {
        if (message.getCommand().startsWith(Command.REG)) {
            boolean regSuccessful = Server.getAuthService().registration(message.getLogin(), message.getPassword());
            if (regSuccessful) {
                log.info(Command.REG_OK);
            } else {
                log.info(Command.REG_NO);
            }

        }

    }

}