package com.kit.message.server;

import com.kit.message.service.MessageService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KitMessageServer {

  @Value("${kit-message.server.port:8800}")
  private int port;

  @Value("${kit-message.server.idle:10}")
  private int idle;

  private final NioEventLoopGroup bizGroup;
  private final NioEventLoopGroup bossGroup;
  private final NioEventLoopGroup workerGroup;
  private final MessageService messageService;

  public KitMessageServer(MessageService messageService) {
    this.messageService = messageService;
    this.bizGroup = new NioEventLoopGroup();
    this.bossGroup = new NioEventLoopGroup(1);
    this.workerGroup = new NioEventLoopGroup();
  }

  @PostConstruct
  public void init() throws InterruptedException {
    new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childHandler(new ChannelInitializer<NioSocketChannel>() {

          @Override
          protected void initChannel(NioSocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new JsonObjectDecoder());
            pipeline.addLast(new StringEncoder());
            pipeline.addLast(new StringDecoder());
            pipeline.addLast(new IdleStateHandler(idle, 0, 0, TimeUnit.SECONDS));
            pipeline.addLast(bizGroup, new KitMessageHandler(messageService));
          }
        })
        .bind(port)
        .sync();
    log.info("Center server bind: [{}]", port);
  }

  @PreDestroy
  @SneakyThrows
  public void destroy() {
    bossGroup.shutdownGracefully().sync();
    workerGroup.shutdownGracefully().sync();
    bizGroup.shutdownGracefully().sync();
  }
}
