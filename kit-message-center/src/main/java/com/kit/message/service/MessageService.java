package com.kit.message.service;

import io.netty.channel.ChannelHandlerContext;

public interface MessageService {

  void register(ChannelHandlerContext ctx, String topic);

  void unregister(ChannelHandlerContext ctx, String topic);

  void unregister(ChannelHandlerContext ctx);

  void pub(ChannelHandlerContext ctx, String topic, String payload);

}
