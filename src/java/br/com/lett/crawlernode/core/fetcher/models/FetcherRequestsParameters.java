package br.com.lett.crawlernode.core.fetcher.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;

public class FetcherRequestsParameters {

  private Map<String, String> headers = new HashMap<>();
  private String payload;

  public JSONObject toJson() {
    JSONObject parameters = new JSONObject();

    if (headers != null && !headers.isEmpty()) {
      JSONObject headersOBJ = new JSONObject();

      for (Entry<String, String> entry : headers.entrySet()) {
        headersOBJ.put(entry.getKey(), entry.getValue());
      }

      parameters.put("headers", headersOBJ);
    }

    if (payload != null) {
      parameters.put("payload", payload);
    }

    return parameters;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public FetcherRequestsParameters setHeaders(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  public String getPayload() {
    return payload;
  }

  public FetcherRequestsParameters setPayload(String payload) {
    this.payload = payload;
    return this;
  }

  public FetcherRequestsParameters setCookies(List<Cookie> cookies) {
    if (!cookies.isEmpty()) {
      StringBuilder cookiesHeader = new StringBuilder();

      for (Cookie c : cookies) {
        cookiesHeader.append(c.getName() + "=" + c.getValue() + ";");
      }

      headers.put("Cookie", cookiesHeader.toString());
    }
    return this;
  }
}
