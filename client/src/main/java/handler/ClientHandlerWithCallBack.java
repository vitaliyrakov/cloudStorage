package handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.application.Platform;
import model.Command;
import model.Message;
import model.commandMessage;
import org.apache.log4j.Logger;

public class ClientHandlerWithCallBack extends SimpleChannelInboundHandler<Message> {
    static final Logger log = Logger.getLogger(String.valueOf(ClientHandlerWithCallBack.class));
    private final CallBack callBack;

    public ClientHandlerWithCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("client connected!");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                Message message) throws Exception {
        if (message instanceof commandMessage && ((commandMessage) message).getCommand().startsWith(Command.AUTH_OK)) {
            log.info("client: "+Command.AUTH_OK);
//            Platform.runLater(() -> responce.);
        }

        if (message instanceof commandMessage && ((commandMessage) message).getCommand().startsWith(Command.REG_OK)) {
            log.info("client: "+Command.REG_OK);
        }

        callBack.call(message);
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
}
