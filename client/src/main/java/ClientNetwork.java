import handler.CallBack;
import handler.ClientHandlerWithCallBack;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import model.Message;
import model.dataMessage;
import org.apache.log4j.Logger;

public class ClientNetwork {
    private SocketChannel clientChannel;
    private final CallBack callBack;
    public static ClientNetwork INSTANCE;
    static final Logger log = Logger.getLogger(String.valueOf(ClientNetwork.class));

    public static ClientNetwork getInstance(CallBack callBack) {
        if (INSTANCE == null) {
            INSTANCE = new ClientNetwork(callBack);
        }
        return INSTANCE;
    }

    private ClientNetwork(CallBack c) {
        this.callBack = c;
        Thread service = new Thread(() -> {
//        new Thread(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                clientChannel = channel;
                                channel.pipeline().addLast(
                                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                        new ObjectEncoder(),
                                        new LoggingHandler(LogLevel.INFO),
                                        new ClientHandlerWithCallBack(callBack)
                                );
                            }
                        });

                ChannelFuture future = bootstrap.connect("localhost", 8190).sync();
                log.info("client network started");
                future.channel().closeFuture().sync(); //block
            } catch (Exception e) {
                log.info("e=", e);
            } finally {
                group.shutdownGracefully();
            }
        });
        service.setDaemon(true);
        service.start();
//                .start();
    }

    public void write(Message message) {
        log.info("write: " + message.toString());
        clientChannel.writeAndFlush(message);
    }
}
