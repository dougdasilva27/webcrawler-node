package br.com.lett.crawlernode.core.fetcher.models;

import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import br.com.lett.crawlernode.core.fetcher.LettProxy;

public class Request {

  private String url;
  private LettProxy proxy;
  private List<String> proxyServices;
  private String payload;
  private Map<String, String> headers;
  private List<Cookie> cookies;
  private FetcherOptions fetcherOptions;

  // Variables with default values
  private boolean followRedirects = true;
  private int timeout = 10000;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public LettProxy getProxy() {
    return proxy;
  }

  public void setProxy(LettProxy proxy) {
    this.proxy = proxy;
  }

  public List<String> getProxyServices() {
    return proxyServices;
  }

  public void setProxyService(List<String> proxyServices) {
    this.proxyServices = proxyServices;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
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

  public FetcherOptions getFetcherOptions() {
    return fetcherOptions;
  }

  public void setFetcherOptions(FetcherOptions fetcherOptions) {
    this.fetcherOptions = fetcherOptions;
  }

  public boolean isFollowRedirects() {
    return followRedirects;
  }

  public void setFollowRedirects(boolean followRedirects) {
    this.followRedirects = followRedirects;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public static class RequestBuilder {

    private String url;
    private LettProxy proxy;
    private List<String> proxyServices;
    private String payload;
    private Map<String, String> headers;
    private List<Cookie> cookies;
    private FetcherOptions fetcherOptions;

    // Variables with default values
    private boolean followRedirects = true;
    private int timeout = 10000;

    public static RequestBuilder create() {
      return new RequestBuilder();
    }

    public RequestBuilder setUrl(String url) {
      this.url = url;
      return this;
    }

    public RequestBuilder setProxy(LettProxy proxy) {
      this.proxy = proxy;
      return this;
    }

    public RequestBuilder setProxyservice(List<String> proxyServices) {
      this.proxyServices = proxyServices;
      return this;
    }

    public RequestBuilder setPayload(String payload) {
      this.payload = payload;
      return this;
    }

    public RequestBuilder setPayload(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public RequestBuilder setCookies(List<Cookie> cookies) {
      this.cookies = cookies;
      return this;
    }

    public RequestBuilder setFetcheroptions(FetcherOptions fetcherOptions) {
      this.fetcherOptions = fetcherOptions;
      return this;
    }

    public RequestBuilder setUrl(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public RequestBuilder setTimeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    public Request build() {
      Request request = new Request();

      request.setUrl(url);
      request.setCookies(cookies);
      request.setFetcherOptions(fetcherOptions);
      request.setFollowRedirects(followRedirects);
      request.setPayload(payload);
      request.setHeaders(headers);
      request.setProxy(proxy);
      request.setProxyService(proxyServices);
      request.setTimeout(timeout);

      return request;
    }
  }
}
