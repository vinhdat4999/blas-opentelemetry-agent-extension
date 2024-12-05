package com.blas.blasopentelemetryagentextension.spanprocessor;

import static com.blas.blasopentelemetryagentextension.utils.UrlMaskingUtil.maskUrl;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class BlasSpanProcessor implements SpanProcessor {

  private static final String HTTP_URL_ATTRIBUTE = "http.url";

  @Override
  public void onStart(Context context, ReadWriteSpan span) {
    String originalUrl = span.getAttribute(AttributeKey.stringKey(HTTP_URL_ATTRIBUTE));
    if (originalUrl != null) {
      String maskedUrl = maskUrl(originalUrl);
      span.setAttribute(HTTP_URL_ATTRIBUTE, maskedUrl);
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    // no action
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
