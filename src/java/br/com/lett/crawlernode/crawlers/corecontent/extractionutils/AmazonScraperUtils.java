package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class AmazonScraperUtils {

  private Logger logger;
  private Session session;

  public AmazonScraperUtils(Logger logger, Session session) {
    this.logger = logger;
    this.session = session;
  }

  public List<Cookie> handleCookiesBeforeFetch(String url, List<Cookie> cookies, DataFetcher dataFetcher) {
    Request request;

    if (dataFetcher instanceof FetcherDataFetcher) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "no");

      request = RequestBuilder.create().setUrl(url)
          .setCookies(cookies)
          .setHeaders(headers)
          .setProxyservice(Arrays.asList(ProxyCollection.INFATICA_RESIDENTIAL_BR, ProxyCollection.STORM_RESIDENTIAL_EU,
              ProxyCollection.STORM_RESIDENTIAL_US))
          .mustSendContentEncoding(false)
          .setFetcheroptions(FetcherOptionsBuilder.create().setForbiddenCssSelector("#captchacharacters").build())
          .build();
    } else {
      request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    }

    return CrawlerUtils.fetchCookiesFromAPage(request, "www.amazon.com.br", "/", null, session, dataFetcher);
  }

  public Document fetchProductPage(List<Cookie> cookies, DataFetcher dataFetcher) {
    Document doc = Jsoup.parse(fetchPage(session.getOriginalURL(), new HashMap<>(), cookies, dataFetcher));

    if (doc.selectFirst("#captchacharacters") != null) {
      Logging.printLogWarn(logger, session, "Trying to fetch page again, because this site returned a captcha.");
      doc = Jsoup.parse(fetchPage(session.getOriginalURL(), new HashMap<>(), cookies, dataFetcher));
    }

    return doc;
  }

  /**
   * Fetch html from amazon
   * 
   * @param url
   * @param headers
   * @param cookies
   * @param session
   * @param dataFetcher
   * @return
   */
  public String fetchPage(String url, Map<String, String> headers, List<Cookie> cookies, DataFetcher dataFetcher) {
    String content;

    if (dataFetcher instanceof FetcherDataFetcher) {
      headers.put("Accept-Encoding", "no");

      Request request = RequestBuilder.create()
          .setUrl(url)
          .setCookies(cookies)
          .setHeaders(headers)
          .setProxyservice(Arrays.asList(ProxyCollection.INFATICA_RESIDENTIAL_BR, ProxyCollection.STORM_RESIDENTIAL_EU,
              ProxyCollection.STORM_RESIDENTIAL_US))
          .mustSendContentEncoding(false)
          // We send this selector fetcher try again when returns captcha
          .setFetcheroptions(FetcherOptionsBuilder.create().setForbiddenCssSelector("#captchacharacters").build())
          .build();
      content = dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
        Request requestApache = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
        content = new ApacheDataFetcher().get(session, requestApache).getBody();
      }
    } else {
      Request requestApache = RequestBuilder.create().setUrl(url).setHeaders(headers).setCookies(cookies).build();
      content = dataFetcher.get(session, requestApache).getBody();
    }

    return content;
  }
}
