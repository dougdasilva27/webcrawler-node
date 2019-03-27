package br.com.lett.crawlernode.core.fetcher.models;

import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;

public class Response {

  private String body;
  private String redirectUrl;
  private Map<String, String> headers;
  private List<Cookie> cookies;
  private LettProxy proxyUsed;
  private List<RequestsStatistics> requests;

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public List<Cookie> getCookies() {
    return cookies;
  }

  public void setCookies(List<Cookie> cookies) {
    this.cookies = cookies;
  }

  public LettProxy getProxyUsed() {
    return proxyUsed;
  }

  public void setProxyUsed(LettProxy proxyUsed) {
    this.proxyUsed = proxyUsed;
  }

  public List<RequestsStatistics> getRequests() {
    return requests;
  }

  public void setRequests(List<RequestsStatistics> requests) {
    this.requests = requests;
  }

  public static class ResponseBuilder {
    private String body;
    private String redirectUrl;
    private Map<String, String> headers;
    private List<Cookie> cookies;
    private LettProxy proxyUsed;
    private List<RequestsStatistics> requests;

    public static ResponseBuilder create() {
      return new ResponseBuilder();
    }

    public ResponseBuilder setBody(String body) {
      this.body = body;
      return this;
    }

    public ResponseBuilder setRedirecturl(String redirectUrl) {
      this.redirectUrl = redirectUrl;
      return this;
    }

    public ResponseBuilder setHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public ResponseBuilder setCookies(List<Cookie> cookies) {
      this.cookies = cookies;
      return this;
    }

    public ResponseBuilder setProxyused(LettProxy proxyUsed) {
      this.proxyUsed = proxyUsed;
      return this;
    }

    public ResponseBuilder setRequestsstatistics(List<RequestsStatistics> requests) {
      this.requests = requests;
      return this;
    }

    public Response build() {
      Response response = new Response();

      response.setBody(body);
      response.setCookies(cookies);
      response.setHeaders(headers);
      response.setProxyUsed(proxyUsed);
      response.setRedirectUrl(redirectUrl);
      response.setRequests(requests);

      return response;
    }
  }
}
