package com.blas.blasopentelemetryagentextension.utils;

import static com.blas.blascommon.utils.StringUtils.AMPERSAND;
import static com.blas.blascommon.utils.StringUtils.ASTERISK;
import static com.blas.blascommon.utils.StringUtils.COMMA;
import static com.blas.blascommon.utils.StringUtils.EQUAL;
import static com.blas.blasopentelemetryagentextension.constant.EnvironmentVariable.OTEL_INSTRUMENTATION_BLAS_MASKED_PATTERNS;
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

  private static final List<String> DEFAULT_SENSITIVE_KEYS = asList("apiKey", "pass", "password",
      "key", "user", "username", "secret");

  private static final List<String> DEFAULT_SENSITIVE_PATTERNS = asList(
      "https://api.telegram.org/bot([a-zA-Z0-9_-]+):([a-zA-Z0-9_-]+)");

  private static final Set<String> MASKED_TAGS;
  private static final Set<Pattern> MASKED_PATTERNS;

  static {
    // Initialize MASKED_TAGS
    String maskedAttribute = System.getenv(OTEL_INSTRUMENTATION_BLAS_MASKED_TAGS.name());
    List<String> maskedKeys = DEFAULT_SENSITIVE_KEYS;
    if (maskedAttribute != null) {
      maskedKeys.addAll(asList(maskedAttribute.split(COMMA)));
    }
    MASKED_TAGS = new HashSet<>(maskedKeys);

    // Initialize MASKED_PATTERNS
    String maskedPatternsAttribute = System.getenv(
        OTEL_INSTRUMENTATION_BLAS_MASKED_PATTERNS.name());
    List<String> patterns = DEFAULT_SENSITIVE_PATTERNS;
    if (maskedPatternsAttribute != null) {
      patterns.addAll(asList(maskedPatternsAttribute.split(COMMA)));
    }

    // Convert string patterns to compiled Pattern objects
    MASKED_PATTERNS = new HashSet<>();
    for (String pattern : patterns) {
      MASKED_PATTERNS.add(Pattern.compile(pattern));
    }
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
      return maskSensitivePattern(maskedUrl);
    } catch (URISyntaxException exception) {
      return url;
    }
  }

  public static String maskSensitivePattern(String url) {
    StringBuilder result = new StringBuilder(url);

    for (Pattern pattern : MASKED_PATTERNS) {
      Matcher matcher = pattern.matcher(url);
      while (matcher.find()) {
        for (int groupIndex = 1; groupIndex <= matcher.groupCount(); groupIndex++) {
          int start = matcher.start(groupIndex);
          int end = matcher.end(groupIndex);
          result.replace(start, end, ASTERISK.repeat(end - start));
        }
      }
    }
    return result.toString();
  }


  private static String maskQueryParams(String query) {
    Map<String, String> queryParams = parseQueryParams(query);

    queryParams.forEach((key, value) -> {
      if (isSensitive(key)) {
        queryParams.put(key, ASTERISK.repeat(value.length()));
      }
    });

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
