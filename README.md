#### 轻量级 Java 应用消息通知中心

##### 项目地址

https://github.com/yemingfeng/kit-message

##### 项目背景

1. 应用集群部署，并且使用了 local cache。当要清除缓存时，通过 rpc / 消息队列清除，只能清除接收到消息的那个节点，无法清除整个应用集群的 local cache 导致，节点
   2、节点 3 存在脏数据。

![](https://raw.githubusercontent.com/yemingfeng/kit-message/master/img/Selection_014.png)

2. 应用集群部署，存在耗时的计算，为了减少计算资源浪费，某个节点更新后需要通知集群内其他节点更新

![](https://raw.githubusercontent.com/yemingfeng/kit-message/master/img/Selection_015.png)

这里的场景本质是，消息如何广播？ 那么会有人问为什么不使用消息队列？ 因为消息队列无法很**优美**的实现这里的场景。比如说 kafka，使用不同的 consumer_group
就可以实现，但不优雅。所以开启了 kit-message 这个项目。

##### 技术清单

- java11
- springboot
- netty
- redis

##### 核心逻辑

![](https://raw.githubusercontent.com/yemingfeng/kit-message/master/img/Selection_009.png)

- kit-message-center: 消息中心服务，接收 kit-message-client 订阅消息或者发布消息
- kit-message-client：一个 Java 的轻量级 client 实现
- kit-message-producer：基于 kit-message-client 发布消息
- kit-message-consumer：基于 kit-message-client 订阅消息

##### 快速使用

###### server 启动

1. 修改 kit-message-center / application.yml 中 redis 的配置
2. mvn clean package
3. java -jar kit-message-server/target/kit-message-center.jar
4. server 会监听 8800 端口

###### client 使用

发布消息

```java
MessageProducer messageProducer = new MessageProducer("127.0.0.1", 8800);
messageProducer.pub("topic1", "topic1:" + i);
messageProducer.close();
```

订阅消息

```java
MessageConsumer messageConsumer = new MessageConsumer("127.0.0.1", 8800);
messageConsumer.sub("topic1", new BiConsumer<String, String>() {
    @Override
    public void accept(String topic, String payload) {
      System.out.println("topic1_1:" + topic + "\t" + payload);
    }
});
```

###### 线上环境部署

kit-message-server 支持集群部署，建议使用 nginx 做转发。

```
stream {
    upstream kit-message-server {
        server server_ip1:8800;
        server server_ip2:8800; 
    }

    server {
       listen 8800;
       proxy_connect_timeout 1s;
       proxy_timeout 3s;
       proxy_pass kit-message-server;
    }
}
```

##### Q&A

1. 这个项目使用场景？使用消息队列等中间件不香吗？

答：这个项目是基于服务间消息通知这个场景的。解决问题更加明确，也更加轻量。

2. 为什么不直接封装一个 redis-client 进行消息的收发？而是使用 client/server 的模式？

答：kit-message-server 让项目更加通用，接入更加方便，依赖更少，管理维护成本更低。

欢迎提反馈