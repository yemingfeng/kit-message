package com.kit.message.consumer;

import com.kit.message.client.ClientContext;
import com.kit.message.client.KitMessageClient;
import java.util.function.BiConsumer;

public class MessageConsumer {

  private final KitMessageClient kitMessageClient;

  public MessageConsumer(String host, int port) {
    ClientContext clientContext = new ClientContext(host, port);
    kitMessageClient = new KitMessageClient(clientContext);
  }

  public void sub(String topic, BiConsumer<String, String> consumer) {
    kitMessageClient.sub(topic, consumer);
  }

  public void unsub(String topic, BiConsumer<String, String> consumer) {
    kitMessageClient.unsub(topic, consumer);
  }

  public void close() {
    kitMessageClient.close();
  }
}
