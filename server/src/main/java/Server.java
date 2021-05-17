import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.log4j.Logger;

public class Server {
    static final Logger log = Logger.getLogger(String.valueOf(Server.class));
    protected static final String storagePath = "server/storage";
    private final int port = 8189;
    private static AuthService authService;

    public Server() {
        authService = new SimpleAuthService();
//        authService.registration("rvv", "rvv", "rvv");
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new LoggingHandler(LogLevel.INFO),
//                                    new ChunkedWriteHandler(),
                                    new ServerHandler()
                            );
                        }
                    });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info("Server started");
            future.channel().closeFuture().sync(); // block

        } catch (Exception e) {
            log.error("e = ", e);
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static AuthService getAuthService() {
        return authService;
    }

    public static void main(String[] args) {
        new Server();
    }

    public static boolean isLoginAuthenticated(String login) {
//        for (ClientHandler c : clients) {
//            if (c.getLogin().equals(login)) {
//                return true;
//            }
//        }
        return false;
    }

}