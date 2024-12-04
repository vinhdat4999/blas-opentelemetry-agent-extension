package com.blas.blasopentelemetryagentextension.utils;

import static com.blas.blascommon.utils.StringUtils.AMPERSAND;
import static com.blas.blascommon.utils.StringUtils.COMMA;
import static com.blas.blascommon.utils.StringUtils.EQUAL;
import static com.blas.blasopentelemetryagentextension.constant.EnvironmentVariable.OTEL_INSTRUMENTATION_BLAS_MASKED_TAGS;
import static java.util.Arrays.asList;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UrlMaskingUtil {

  private static final List<String> DEFAULT_SENSITIVE_KEYS = asList("apiKey", "password", "key",
      "user", "username", "pass", "secret");
  private static final String MASKED_VALUE = "*****";
  private static final Pattern BOT_TOKEN_PATTERN = Pattern.compile(
      "bot([a-zA-Z0-9_-]+):([a-zA-Z0-9_-]+)");
  private static final Set<String> MASKED_TAGS;

  static {
    String maskedAttribute = System.getenv(OTEL_INSTRUMENTATION_BLAS_MASKED_TAGS.name());
    List<String> maskedKeys = DEFAULT_SENSITIVE_KEYS;
    if (maskedAttribute != null) {
      maskedKeys = asList(maskedAttribute.split(COMMA));
    }
    MASKED_TAGS = new HashSet<>(maskedKeys);
  }

  public static String maskUrl(String url) {
    try {
      URI uri = new URI(url);
      String path = uri.getPath();
      String query = uri.getQuery();

      if (query != null && !query.isEmpty()) {
        query = maskQueryParams(query);
      }

      String maskedUrl = new URI(uri.getScheme(), uri.getAuthority(), path, query,
          uri.getFragment()).toString();
      return maskTelegramBotUrl(maskedUrl);
    } catch (URISyntaxException exception) {
      return url;
    }
  }

  private static String maskTelegramBotUrl(String url) {
    Matcher matcher = BOT_TOKEN_PATTERN.matcher(url);

    if (matcher.find()) {
      String maskedToken = "bot*****:*****";
      return url.replace(matcher.group(0), maskedToken);
    }

    return url;
  }

  private static String maskQueryParams(String query) {
    Map<String, String> queryParams = parseQueryParams(query);

    for (String key : queryParams.keySet()) {
      if (isSensitive(key)) {
        queryParams.put(key, MASKED_VALUE);
      }
    }

    return buildQueryString(queryParams);
  }

  private static Map<String, String> parseQueryParams(String query) {
    Map<String, String> params = new HashMap<>();
    String[] pairs = query.split(AMPERSAND);

    for (String pair : pairs) {
      String[] keyValue = pair.split(EQUAL);
      if (keyValue.length == 2) {
        params.put(keyValue[0], keyValue[1]);
      }
    }

    return params;
  }

  private static boolean isSensitive(String key) {
    for (String sensitiveKey : MASKED_TAGS) {
      if (key.equalsIgnoreCase(sensitiveKey)) {
        return true;
      }
    }
    return false;
  }

  private static String buildQueryString(Map<String, String> params) {
    StringBuilder queryBuilder = new StringBuilder();

    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (!queryBuilder.isEmpty()) {
        queryBuilder.append(AMPERSAND);
      }
      queryBuilder.append(entry.getKey())
          .append(EQUAL)
          .append(entry.getValue());
    }

    return queryBuilder.toString();
  }
}
