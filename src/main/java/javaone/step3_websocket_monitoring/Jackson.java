package javaone.step3_websocket_monitoring;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.util.ByteString;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Jackson {
  private static final ObjectMapper defaultObjectMapper =
    new ObjectMapper().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

  public static <T> Marshaller<T, ByteString> marshaller() {
    return marshaller(defaultObjectMapper);
  }

  public static <T> Marshaller<T, ByteString> marshaller(ObjectMapper mapper) {
    return Marshaller.withFixedContentType(
      ContentTypes.APPLICATION_JSON,
      u -> ByteString.fromString(toJSON(mapper, u))
    );
  }

  public static <T> Unmarshaller<ByteString, T> byteStringUnmarshaller(Class<T> expectedType) {
    return byteStringUnmarshaller(defaultObjectMapper, expectedType);
  }
  
  public static <T> Unmarshaller<HttpEntity, T> unmarshaller(Class<T> expectedType) {
    return unmarshaller(defaultObjectMapper, expectedType);
  }

  public static <T> Unmarshaller<HttpEntity, T> unmarshaller(ObjectMapper mapper, Class<T> expectedType) {
    return Unmarshaller.forMediaType(MediaTypes.APPLICATION_JSON, Unmarshaller.entityToString())
                       .thenApply(s -> fromJSON(mapper, s, expectedType));
  }
  
  public static <T> Unmarshaller<ByteString, T> byteStringUnmarshaller(ObjectMapper mapper, Class<T> expectedType) {
    return Unmarshaller.sync(s -> fromJSON(mapper, s.utf8String(), expectedType));
  }

  private static String toJSON(ObjectMapper mapper, Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Cannot marshal to JSON: " + object, e);
    }
  }

  private static <T> T fromJSON(ObjectMapper mapper, String json, Class<T> expectedType) {
    try {
      return mapper.readerFor(expectedType).readValue(json);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot unmarshal JSON as " + expectedType.getSimpleName(), e);
    }
  }
}
