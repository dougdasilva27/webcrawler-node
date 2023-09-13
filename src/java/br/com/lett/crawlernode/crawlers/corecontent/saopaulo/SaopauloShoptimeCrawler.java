package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offers;
import org.apache.http.cookie.Cookie;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SaopauloShoptimeCrawler extends B2WCrawler {

   private static final String HOME_PAGE = "https://www.shoptime.com.br/";
   private static final String URL_PAGE_OFFERS = "https://www.shoptime.com.br/parceiros/";
   private static final String MAIN_SELLER_NAME_LOWER = "shoptime";
   private static final String MAIN_SELLER_NAME_LOWER_FROM_HTML = "Shoptime";


   public SaopauloShoptimeCrawler(Session session) {
      super(session);
      super.subSellers = new ArrayList<>();
      super.sellerNameLower = MAIN_SELLER_NAME_LOWER;
      super.sellerNameLowerFromHTML = MAIN_SELLER_NAME_LOWER_FROM_HTML;
      super.config.setFetcher(FetchMode.JSOUP);
      super.urlPageOffers = URL_PAGE_OFFERS;
      super.homePage = HOME_PAGE;
   }

   @Override
   public String fetchPage(String url, DataFetcher df, List<Cookie> cookies, Map<String, String> headers, Session session) {

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
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
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_AR,
               ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
            )
         )
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher(), new JsoupDataFetcher()), session);

      return response.getBody();
   }

   protected Offers scrapOffers(Document doc, String internalId, String internalPid, JSONObject apolloJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      if (!allow3PSellers) {

         setOffersForMainPageSeller(offers, apolloJson, doc);

      } else {
         setOffersForMainPageSeller(offers, apolloJson, doc);

         Elements sellerMainFromHTML = null;
         Document sellersDoc = null;
         Elements sellersFromHTML = doc.select(listSelectors.get("offers"));
         if (!doc.select(listSelectors.get("hasPageOffers")).isEmpty()) {
            if (sellersFromHTML.isEmpty()) {
               String urlOffer = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "a[class^=\"more-offers\"]", "href");
               String offersPageUrl = "";

               if (urlOffer != null && !urlOffer.isEmpty()) {
                  offersPageUrl = urlPageOffers + urlOffer.replace("/parceiros/", "").replaceAll("productSku=([0-9]+)", "productSku=" + internalId);
                  sellersDoc = accessOffersPage(offersPageUrl);
                  if (sellersDoc != null) {
                     sellersFromHTML = sellersDoc.select(listSelectors.get("offers"));
                     sellerMainFromHTML = sellersDoc.select("div[class^=\"src__MainOffer\"]");
                  }
               }

               if (sellersFromHTML == null && sellersFromHTML.isEmpty()) {
                  offersPageUrl = urlPageOffers + internalPid + "?productSku=" + internalId;
                  sellersDoc = accessOffersPage(offersPageUrl);
                  sellersFromHTML = sellersDoc != null ? sellersDoc.select(listSelectors.get("offers")) : null;
               }
            }

            if (sellerMainFromHTML != null && !sellerMainFromHTML.isEmpty()) {

               setOffersForSellersPage(offers, sellerMainFromHTML, listSelectors, sellersDoc);
            }

            if (sellersFromHTML != null && !sellersFromHTML.isEmpty()) {

               setOffersForSellersPage(offers, sellersFromHTML, listSelectors, sellersDoc);
            }

         }

      }

      return offers;
   }
}
