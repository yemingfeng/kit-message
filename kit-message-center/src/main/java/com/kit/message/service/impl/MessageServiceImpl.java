package com.kit.message.service.impl;

import com.kit.message.model.Message;
import com.kit.message.service.MessageService;
import com.kit.message.util.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

  private final StringRedisTemplate stringRedisTemplate;
  private final Map<String, MessageListener> topicListeners;
  private final Map<ChannelHandlerContext, Set<String>> ctxTopics;
  private final Map<String, Set<ChannelHandlerContext>> topicCtxs;
  private final RedisMessageListenerContainer redisMessageListenerContainer;

  public MessageServiceImpl(StringRedisTemplate stringRedisTemplate,
      RedisMessageListenerContainer redisMessageListenerContainer) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.redisMessageListenerContainer = redisMessageListenerContainer;
    this.topicListeners = new HashMap<>();
    this.topicCtxs = new HashMap<>();
    this.ctxTopics = new HashMap<>();

  }

  @Override
  public void register(ChannelHandlerContext ctx, String topic) {
    synchronized (this) {
      if (!topicListeners.containsKey(topic)) {
        MessageListener messageListener = generateMessageListener();
        redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(topic));
        topicListeners.put(topic, messageListener);
      }

      if (!topicCtxs.containsKey(topic)) {
        topicCtxs.put(topic, new HashSet<>());
      }
      topicCtxs.get(topic).add(ctx);

      if (!ctxTopics.containsKey(ctx)) {
        ctxTopics.put(ctx, new HashSet<>());
      }
      ctxTopics.get(ctx).add(topic);
    }
  }

  @Override
  public void unregister(ChannelHandlerContext ctx, String topic) {
    synchronized (this) {
      if (topicCtxs.containsKey(topic)) {
        topicCtxs.get(topic).remove(ctx);
        if (topicCtxs.get(topic).size() == 0) {
          topicCtxs.remove(topic);
          topicListeners.remove(topic);
        }
      }

      if (ctxTopics.containsKey(ctx)) {
        ctxTopics.get(ctx).remove(topic);
      }
    }
  }

  @Override
  public void unregister(ChannelHandlerContext ctx) {
    synchronized (this) {
      Set<String> topics = ctxTopics.remove(ctx);
      if (!CollectionUtils.isEmpty(topics)) {
        topics.forEach(topic -> {
          if (topicCtxs.containsKey(topic)) {
            topicCtxs.get(topic).remove(ctx);
          }
        });
      }
    }
  }

  @Override
  public void pub(ChannelHandlerContext ctx, String topic, String payload) {
    stringRedisTemplate.convertAndSend(topic, payload);
  }

  private MessageListener generateMessageListener() {
    return (message, pattern) -> {
      if (pattern == null) {
        return;
      }
      String topic = new String(pattern);
      String payload = new String(message.getBody());

      Set<ChannelHandlerContext> ctxs = topicCtxs.get(topic);
      if (CollectionUtils.isEmpty(ctxs)) {
        return;
      }

      String messageStr = JsonUtil.toJson(Message.pub(topic, payload));

      for (ChannelHandlerContext ctx : ctxs) {
        try {
          ctx.writeAndFlush(messageStr).sync();
        } catch (Exception e) {
          log.error("Write: [{}] to [{}] error", messageStr, ctx.channel().remoteAddress(), e);
          ctx.close();
        }
      }
    };
  }
}