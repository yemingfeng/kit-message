package com.kit.message.producer;

import com.kit.message.client.ClientContext;
import com.kit.message.client.KitMessageClient;
import com.kit.message.model.Message;

public class MessageProducer {

  private final KitMessageClient kitMessageClient;

  public MessageProducer(String host, int port) {
    ClientContext clientContext = new ClientContext(host, port);
    this.kitMessageClient = new KitMessageClient(clientContext);
  }

  public void pub(String topic, String payload) {
    kitMessageClient.pub(Message.pub(topic, payload));
  }

  public void close() {
    kitMessageClient.close();
  }
}