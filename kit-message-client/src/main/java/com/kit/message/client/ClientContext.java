package com.kit.message.client;

import lombok.Getter;

public class ClientContext {

  @Getter
  private final String host;
  @Getter
  private final int port;

  public ClientContext(String host, int port) {
    this.host = host;
    this.port = port;
  }
}