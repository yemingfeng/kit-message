package com.kit.message.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisConfig implements LettuceClientConfigurationBuilderCustomizer {

  @Override
  public void customize(LettuceClientConfigurationBuilder clientConfigurationBuilder) {
    ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions
        .builder()
        .enablePeriodicRefresh(Duration.ofSeconds(60))
        .enableAdaptiveRefreshTrigger(RefreshTrigger.ASK_REDIRECT, RefreshTrigger.UNKNOWN_NODE)
        .build();

    ClusterClientOptions clusterClientOptions = ClusterClientOptions
        .builder(ClientOptions.builder().timeoutOptions(TimeoutOptions.enabled()).build())
        .topologyRefreshOptions(clusterTopologyRefreshOptions)
        .validateClusterNodeMembership(false)
        .build();
    clientConfigurationBuilder.clientOptions(clusterClientOptions);
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      @Autowired RedisConnectionFactory redisConnectionFactory) {
    RedisMessageListenerContainer redisMessageListenerContainer =
        new RedisMessageListenerContainer();
    redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
    return redisMessageListenerContainer;
  }
}