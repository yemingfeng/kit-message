package com.kit.message;

import com.kit.message.consumer.MessageConsumer;
import java.util.function.BiConsumer;

public class ConsumerMain {

  public static void main(String[] args) throws InterruptedException {
    MessageConsumer messageConsumer = new MessageConsumer("127.0.0.1", 8800);

    messageConsumer.sub("topic1", new BiConsumer<String, String>() {
      @Override
      public void accept(String topic, String payload) {
        System.out.println("topic1_1:" + topic + "\t" + payload);
      }
    });

    messageConsumer.sub("topic2", new BiConsumer<String, String>() {
      @Override
      public void accept(String topic, String payload) {
        System.out.println("topic2:" + topic + "\t" + payload);
      }
    });

    BiConsumer<String, String> topic12Consumer = new BiConsumer<String, String>() {
      @Override
      public void accept(String topic, String payload) {
        System.out.println("topic1_2:" + topic + "\t" + payload);
      }
    };
    messageConsumer.sub("topic1", topic12Consumer);

    Thread.sleep(10000);
    messageConsumer.unsub("topic1", topic12Consumer);

    Thread.sleep(100000000);

    messageConsumer.close();
  }
}