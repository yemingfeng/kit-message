package com.kit.message.client;

import com.kit.message.model.Message;
import com.kit.message.util.JsonUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.internal.ConcurrentSet;
import java.io.Closeable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KitMessageClient {

  private final ClientContext clientContext;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<String, Set<BiConsumer<String, String>>> consumers;

  private KitMessageConnection connection;

  public KitMessageClient(ClientContext clientContext) {
    this.consumers = new ConcurrentHashMap<>();
    this.scheduledExecutorService = Executors.newScheduledThreadPool(10);
    this.clientContext = clientContext;
    this.connect();
  }

  public synchronized void connect() {
    connection = new KitMessageConnection();
    connection.connect();

    scheduledExecutorService.scheduleAtFixedRate(() -> {
      // 用心跳的方式，测试下是否可用
      if (connection.detectActive()) {
        return;
      }

      connection.close();
      connection.connect();
    }, 10, 10, TimeUnit.SECONDS);
  }

  public void pub(Message message) {
    connection.send(message);
  }

  public synchronized void sub(String topic, BiConsumer<String, String> consumer) {
    connection.send(Message.register(topic));
    consumers.computeIfAbsent(topic, notUsed -> new ConcurrentSet<>());
    consumers.get(topic).add(consumer);
  }

  public synchronized void unsub(String topic, BiConsumer<String, String> consumer) {
    Set<BiConsumer<String, String>> existed = consumers.get(topic);
    if (existed != null) {
      existed.remove(consumer);
    }
    if (existed == null || existed.size() == 0) {
      connection.send(Message.unregister(topic));
    }
  }

  @SneakyThrows
  private void sleep(long second) {
    TimeUnit.SECONDS.sleep(second);
  }

  @SneakyThrows
  public synchronized void close() {
    connection.close();
  }

  public class KitMessageConnection implements Closeable {

    private Channel channel;
    private NioEventLoopGroup bizGroup;
    private NioEventLoopGroup workerGroup;

    @SneakyThrows
    public synchronized boolean connect() {
      bizGroup = new NioEventLoopGroup();
      workerGroup = new NioEventLoopGroup(1);

      channel = new Bootstrap().group(workerGroup)
          .channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) {
              ChannelPipeline pipeline = ch.pipeline();
              pipeline.addLast(new JsonObjectDecoder());
              pipeline.addLast(new StringEncoder());
              pipeline.addLast(new StringDecoder());
              pipeline.addLast(bizGroup, new SimpleChannelInboundHandler<String>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                  Message message = JsonUtil.fromJson(msg, Message.class);
                  switch (message.getType()) {
                    case PUB: {
                      Set<BiConsumer<String, String>> existed = consumers.get(message.getTopic());
                      if (existed != null) {
                        for (BiConsumer<String, String> consumer : existed) {
                          try {
                            consumer.accept(message.getTopic(), message.getPayload());
                          } catch (Exception e) {
                            log.error("[{}] consume: [{}]-[{}] error", consumer,
                                message.getTopic(), message.getPayload(), e);
                          }
                        }
                      }
                    }
                  }
                }
              });
            }
          })
          .connect(clientContext.getHost(), clientContext.getPort())
          .sync()
          .channel();

      consumers.keySet()
          .forEach(topic -> send(Message.register(topic)));

      return true;
    }

    @SneakyThrows
    public void send(Message msg) {
      channel.writeAndFlush(JsonUtil.toJson(msg)).sync();
    }

    public boolean detectActive() {
      send(Message.ping());
      return true;
    }

    @Override
    @SneakyThrows
    public void close() {
      scheduledExecutorService.shutdownNow();
      scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
      if (channel != null) {
        channel.close().sync();
      }
      if (workerGroup != null) {
        workerGroup.shutdownGracefully().sync();
      }
      if (bizGroup != null) {
        bizGroup.shutdownGracefully().sync();
      }
    }
  }
}