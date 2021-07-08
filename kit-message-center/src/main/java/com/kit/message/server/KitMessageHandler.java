package com.kit.message.server;

import com.kit.message.model.Message;
import com.kit.message.service.MessageService;
import com.kit.message.util.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KitMessageHandler extends SimpleChannelInboundHandler<String> {

  private final MessageService messageService;

  public KitMessageHandler(MessageService messageService) {
    this.messageService = messageService;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) {
    Message message = JsonUtil.fromJson(msg, Message.class);
    switch (message.getType()) {
      case PUB:
        messageService.pub(ctx, message.getTopic(), message.getPayload());
        break;
      case REGISTER:
        messageService.register(ctx, message.getTopic());
        break;
      case UNREGISTER:
        messageService.unregister(ctx, message.getTopic());
        break;
      case PING:
        // 接收到了 ping 请求，这里不处理
        break;
      default:
        log.warn("Unknown type: [{}]", message.getType());
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      if (IdleState.READER_IDLE.equals((event.state()))) {
        ctx.close();
      }
    }
  }

  @Override
  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    super.channelUnregistered(ctx);
    messageService.unregister(ctx);
  }
}
