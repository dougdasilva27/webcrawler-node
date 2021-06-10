package br.com.lett.crawlernode.integration.redis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class JsonCodec<T> implements RedisCodec<String, T> {

   private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe()).create();

   private final Class<T> clazz;

   public JsonCodec(Class<T> clazz) {
      this.clazz = clazz;
   }

   @Override
   public String decodeKey(ByteBuffer bytes) {
      return StandardCharsets.UTF_8.decode(bytes).toString();
   }

   @Override
   public T decodeValue(ByteBuffer bytes) {
      String data = StandardCharsets.UTF_8.decode(bytes).toString();
      return gson.fromJson(data, clazz);
   }

   @Override
   public ByteBuffer encodeKey(String key) {
      return ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public ByteBuffer encodeValue(T value) {
      return ByteBuffer.wrap(gson.toJson(value).getBytes(StandardCharsets.UTF_8));
   }
}
