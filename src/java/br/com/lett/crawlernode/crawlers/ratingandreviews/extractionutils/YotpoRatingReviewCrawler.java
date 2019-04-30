package br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class YotpoRatingReviewCrawler {
  
  private Session session;
  protected Logger logger;
  private List<Cookie> cookies;
  
  public YotpoRatingReviewCrawler(Session session, List<Cookie> cookies, Logger logger) {
    this.session = session;
    this.cookies = cookies;
    this.logger = logger;
  }
  
  /**
   * General method to extract ratings from Yotpo API.
   * 
   * @param productUrl page URL
   * @param productPid ID used on Yotpo api (can be different from productId)
   * @param dataFetcher
   * @return
   */
  public Document extractRatingsFromYotpo(String productUrl, String productPid, String appKey, DataFetcher dataFetcher) {
    Document doc = new Document("");

    String methods = "[{\"method\":\"bottomline\",\"params\":{\"pid\":\"" + productPid + "\",\"link\":\""
        + productUrl + "\",\"skip_average_score\":false,\"main_widget_pid\":\"" + productPid + "\"}}]";
    
    try {
      String methodsEncoded = URLEncoder.encode(methods, "UTF-8");
    
      String payload = "methods=" + methodsEncoded + "&app_key=" + appKey;
    
      Request request = RequestBuilder.create().setUrl("https://staticw2.yotpo.com/batch").setCookies(cookies).setPayload(payload).build();
      String response = dataFetcher.post(session, request).getBody();
      
      JSONArray arr = CrawlerUtils.stringToJsonArray(response);
      
      doc = new Document("");
      
      for(Object o : arr) {
        JSONObject json = (JSONObject) o;
        
        String responseHtml = json.has("result") ? json.getString("result") : null;
        
        doc.append(responseHtml);
      }
      
    } catch (UnsupportedEncodingException e) {
      Logging.printLogError(logger, session, "Could not encode url for Yotpo API");
    }
    
    return doc;
  }
}
