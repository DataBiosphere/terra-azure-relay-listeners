package org.broadinstitute.listener.relay.wss;

import com.microsoft.azure.relay.WebSocketChannel;
import com.microsoft.azure.relay.WriteMode;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.lang.NonNull;

// Note: These methods are a stopgap.
// They should be removed when the Azure Relay SDK provides the functionality
// to read and write text data over the websocket connection.
public class WebSocketTextIOUtils {

  public static CompletableFuture<String> readTextAsync(@NonNull WebSocketChannel webSocketChannel)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method readMethod = webSocketChannel.getClass().getDeclaredMethod("readTextAsync");
    readMethod.setAccessible(true);

    return (CompletableFuture<String>) readMethod.invoke(webSocketChannel);
  }

  public static CompletableFuture<Void> writeTextAsync(
      @NonNull WebSocketChannel webSocketChannel, String text)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method writeAsync =
        webSocketChannel
            .getClass()
            .getDeclaredMethod(
                "writeAsync", Object.class, Duration.class, boolean.class, WriteMode.class);
    writeAsync.setAccessible(true);

    return (CompletableFuture<Void>)
        writeAsync.invoke(webSocketChannel, text, Duration.ofSeconds(30), true, WriteMode.TEXT);
  }
}
