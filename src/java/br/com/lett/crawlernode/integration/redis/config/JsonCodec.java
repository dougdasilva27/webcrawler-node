package br.com.lett.crawlernode.integration.redis.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.RedisCodec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JsonCodec<T> implements RedisCodec<String, T> {

   private final ObjectMapper objectMapper = new ObjectMapper();

   @Override
   public String decodeKey(ByteBuffer bytes) {
      return StandardCharsets.UTF_8.decode(bytes).toString();
   }

   @Override
   public T decodeValue(ByteBuffer bytes) {
      try {
         return objectMapper.readValue(bytes.array(), new TypeReference<T>() {
         });
      } catch (IOException exception) {
         throw new IllegalStateException(exception);
      }
   }

   @Override
   public ByteBuffer encodeKey(String key) {
      return ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public ByteBuffer encodeValue(T value) {
      try {
         return ByteBuffer.wrap(objectMapper.writeValueAsBytes(value));
      } catch (JsonProcessingException e) {
         throw new IllegalStateException(e);
      }
   }
}
