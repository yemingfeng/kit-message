package com.kit.message.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Message {

  private Type type;
  private String topic;
  private String payload;

  public enum Type {
    REGISTER, // 注册操作
    UNREGISTER, // 取消注册操作
    PUB, // 发布操作
    PING
  }

  public static Message register(String topic) {
    return new Message(Type.REGISTER, topic, null);
  }

  public static Message unregister(String topic) {
    return new Message(Type.UNREGISTER, topic, null);
  }

  public static Message pub(String topic, String payload) {
    return new Message(Type.PUB, topic, payload);
  }

  public static Message ping() {
    return new Message(Type.PING, null, null);
  }
}