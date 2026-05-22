package pl.allegro.tech.hermes.common.logging;

import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * Utility class for managing MDC (Mapped Diagnostic Context) with automatic cleanup.
 *
 * <p>Provides methods to execute code with logging context that is automatically cleaned up after
 * execution, even if an exception occurs.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // For methods that return a value
 * return LoggingContext.withLogging(SUBSCRIPTION_NAME, qualifiedName, () -> {
 *   // your code here - all logs will include subscription-name in MDC
 *   return result;
 * });
 *
 * // For void methods
 * LoggingContext.runWithLogging(SUBSCRIPTION_NAME, qualifiedName, () -> {
 *   // your code here - all logs will include subscription-name in MDC
 * });
 * }</pre>
 */
public final class LoggingContext {

  /**
   * Executes a supplier with a logging context that is automatically cleaned up.
   *
   * <p>The provided key-value pair will be added to the MDC for the duration of the supplier
   * execution and automatically removed for parent call afterwards, or the previous value is
   * restored for nested calls.
   *
   * @param key the MDC key (e.g., "subscription-name")
   * @param value the MDC value (e.g., "group.topic$subscription")
   * @param supplier the code to execute with the logging context
   * @param <T> the return type
   * @return the result of the supplier
   */
  public static <T> T withLogging(String key, String value, Supplier<T> supplier) {
    String previousValue = null;
    boolean parentCall = false;
    try {
      previousValue = MDC.get(key);
      parentCall = previousValue == null;
      MDC.put(key, value);
      return supplier.get();
    } finally {
      if (parentCall) {
        MDC.remove(key);
      } else { // restore previous value for nested calls
        MDC.put(key, previousValue);
      }
    }
  }

  /**
   * Executes a runnable with a logging context that is automatically cleaned up.
   *
   * <p>The provided key-value pair will be added to the MDC for the duration of the runnable
   * execution and automatically removed for parent call afterwards, or the previous value is
   * restored for nested calls.
   *
   * @param key the MDC key (e.g., "subscription-name")
   * @param value the MDC value (e.g., "group.topic$subscription")
   * @param runnable the code to execute with the logging context
   */
  public static void runWithLogging(String key, String value, Runnable runnable) {
    String previousValue = null;
    boolean parentCall = false;
    try {
      previousValue = MDC.get(key);
      parentCall = previousValue == null;
      MDC.put(key, value);
      runnable.run();
    } finally {
      if (parentCall) {
        MDC.remove(key);
      } else { // restore previous value for nested calls
        MDC.put(key, previousValue);
      }
    }
  }
}
