package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloAmericanasCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.americanas.com.br/";
   private static final String URL_PAGE_OFFERS = "https://www.americanas.com.br/parceiros/";
   private static final String MAIN_SELLER_NAME_LOWER = "americanas.com";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Americanas";

   private static final List<String> UserAgent = Arrays.asList(
      "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPad; CPU OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (iPod; CPU iPhone OS 14_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/93.0.4577.39 Mobile/15E148 Safari/604.1",
      "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-A102U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; SM-G960U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-X420) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
      "Mozilla/5.0 (Linux; Android 10; LM-Q710(FGN)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36"
   );

   public SaopauloAmericanasCrawler(Session session) {
      super(session);
      super.subSellers = Arrays.asList("b2w", "lojas americanas", "lojas americanas mg", "lojas americanas rj", "lojas americanas sp", "lojas americanas rs");
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.homePage = HOME_PAGE;
      super.listSelectors = getListSelectors();
      super.urlPageOffers = URL_PAGE_OFFERS;
      super.config.setFetcher(FetchMode.JSOUP);
   }

   private Map<String, String> getListSelectors() {
      Map<String, String> listSelectors = new HashMap<>();
      listSelectors.put("selectorSellerName", ".sold-and-delivery__Seller-sc-1fgd6h1-2");
      listSelectors.put("selectorSellerId", ".src__ButtonUI-sc-16np9zs-3");
      listSelectors.put("offers", ".src__Divider-sc-1y5gtgz-6");
      listSelectors.put("hasPageOffers", ".more-offers__Text-sc-15yqej3-0");

      return listSelectors;
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), this.dataFetcher, cookies, headers, session));
   }

   public static Map<String, String> getHeaders() {
      Random random = new Random();

      Map<String, String> headers = new HashMap<>();
      headers.put("sec-ch-ua", "\"Chromium\";v=\"92\", \" Not A;Brand\";v=\"99\", \"Google Chrome\";v=\"92\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("user-agent", UserAgent.get(random.nextInt(UserAgent.size())));

      return headers;
   }

   public static String fetchPage(String url, DataFetcher df, List<Cookie> cookies, Map<String, String> headers, Session session) {

      Map<String, String> headersAmericanas = getHeaders();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headersAmericanas)
         .setSendUserAgent(false)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         )
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
            )
         )
         .build();

      Response response = df.get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {


         request.setHeaders(getHeaders());
         content = new FetcherDataFetcher().get(session, request).getBody();
      }

      return content;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject reviews) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      JSONArray ratingDistribution = reviews.optJSONArray("ratingDistribution");

      if (ratingDistribution != null) {
         for (Object o : ratingDistribution) {

            JSONObject ratingDistributionObject = (JSONObject) o;

            int ratingValue = ratingDistributionObject.optInt("ratingValue");
            int ratingCount = ratingDistributionObject.optInt("count");

            switch (ratingValue) {
               case 5:
                  star5 = ratingCount;
                  break;
               case 4:
                  star4 = ratingCount;
                  break;
               case 3:
                  star3 = ratingCount;
                  break;
               case 2:
                  star2 = ratingCount;
                  break;
               case 1:
                  star1 = ratingCount;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }




}
