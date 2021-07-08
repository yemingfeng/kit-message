package com.kit.message;

import com.kit.message.producer.MessageProducer;
import java.util.Random;

public class ProducerMain {

  public static void main(String[] args) throws InterruptedException {
    MessageProducer messageProducer = new MessageProducer("127.0.0.1", 8800);
    for (int i = 0; i < 10000; i++) {
      if (new Random().nextInt() % 2 == 0) {
        messageProducer.pub("topic1", "topic1:" + i);
      } else {
        messageProducer.pub("topic2", "topic2:" + i);
      }
      Thread.sleep(1000);
    }

    messageProducer.close();
  }
}