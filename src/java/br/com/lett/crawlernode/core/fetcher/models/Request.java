package br.com.lett.crawlernode.core.fetcher.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;

public class Request {

  private String url;
  private LettProxy proxy;
  private List<String> proxyServices;
  private String payload;
  private Integer timeout;
  private Map<String, String> headers = new HashMap<>();
  private List<Cookie> cookies = new ArrayList<>();
  private FetcherOptions fetcherOptions;
  private List<Integer> statusCodesToIgnore = new ArrayList<>();

  // Variables with default values
  private boolean followRedirects = true;
  private boolean sendContentEncoding = true;
  private boolean sendUserAgent = true;
  private boolean ignoreStatusCode = false;
  private boolean bodyIsRequired = true;

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

  public void setProxyServices(List<String> proxyServices) {
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

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public boolean mustSendContentEncoding() {
    return sendContentEncoding;
  }

  public void setSendContentEncoding(boolean sendContentEncoding) {
    this.sendContentEncoding = sendContentEncoding;
  }

  public boolean mustSendUserAgent() {
    return sendUserAgent;
  }

  public void setSendUserAgent(boolean sendUserAgent) {
    this.sendUserAgent = sendUserAgent;
  }

  public boolean mustIgnoreStatusCode() {
    return ignoreStatusCode;
  }

  public void setIgnoreStatusCode(boolean ignoreStatusCode) {
    this.ignoreStatusCode = ignoreStatusCode;
  }

  public boolean bodyIsRequired() {
    return bodyIsRequired;
  }

  public void setBodyIsRequired(boolean bodyIsRequired) {
    this.bodyIsRequired = bodyIsRequired;
  }

  public List<Integer> getStatusCodesToIgnore() {
    return statusCodesToIgnore;
  }

  public void setStatusCodesToIgnore(List<Integer> statusCodesToIgnore) {
    this.statusCodesToIgnore = statusCodesToIgnore;
  }

  public static class RequestBuilder {

    private String url;
    private LettProxy proxy;
    private List<String> proxyServices;
    private String payload;
    private Map<String, String> headers = new HashMap<>();
    private List<Cookie> cookies = new ArrayList<>();
    private FetcherOptions fetcherOptions;
    private List<Integer> statusCodesToIgnore = new ArrayList<>();

    // Variables with default values
    private boolean followRedirects = true;
    private Integer timeout;
    private boolean sendContentEncoding = true;
    private boolean sendUserAgent = true;
    private boolean ignoreStatusCode = false;
    private boolean bodyIsRequired = true;

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

    public RequestBuilder setHeaders(Map<String, String> headers) {
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

    public RequestBuilder setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    public RequestBuilder setTimeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    public RequestBuilder mustSendContentEncoding(boolean must) {
      this.sendContentEncoding = must;
      return this;
    }

    public RequestBuilder setSendUserAgent(boolean must) {
      this.sendUserAgent = must;
      return this;
    }

    public RequestBuilder setIgnoreStatusCode(boolean ignoreStatusCode) {
      this.ignoreStatusCode = ignoreStatusCode;
      return this;
    }

    public RequestBuilder setBodyIsRequired(boolean bodyIsRequired) {
      this.bodyIsRequired = bodyIsRequired;
      return this;
    }

    public RequestBuilder setStatusCodesToIgnore(List<Integer> statusCodesToIgnore) {
      this.statusCodesToIgnore = statusCodesToIgnore;
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
      request.setProxyServices(proxyServices);
      request.setTimeout(timeout);
      request.setSendContentEncoding(sendContentEncoding);
      request.setSendUserAgent(sendUserAgent);
      request.setBodyIsRequired(bodyIsRequired);
      request.setIgnoreStatusCode(ignoreStatusCode);
      request.setStatusCodesToIgnore(statusCodesToIgnore);

      return request;
    }
  }
}
