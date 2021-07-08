package com.kit.message.util;

import com.google.gson.Gson;

public class JsonUtil {

  private static final Gson GSON = new Gson();

  public static String toJson(Object source) {
    return GSON.toJson(source);
  }

  public static <T> T fromJson(String source, Class<T> clz) {
    return GSON.fromJson(source, clz);
  }
}
