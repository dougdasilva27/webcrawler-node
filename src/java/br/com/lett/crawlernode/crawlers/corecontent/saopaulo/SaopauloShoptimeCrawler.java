package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.B2WCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.Pricing;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.*;

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
   protected Document fetch() {
      setHeaders();
      return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
   }

   public static String fetchPage(String url, Session session) {
      Map<String, String> headers = new HashMap<>();
      headers.put("host", "www.shoptime.com.br");
      headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(false)
         .setHeaders(headers)
         .setFetcheroptions(
            FetcherOptions.FetcherOptionsBuilder.create()
               .mustUseMovingAverage(false)
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#px-captcha")
               .build()
         ).setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY
            )
         ).build();


      Response response = new ApacheDataFetcher().get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY
         ));

         content = new JsoupDataFetcher().get(session, request).getBody();
      }

      return content;
   }


   protected Offers scrapOffers(Document doc, String internalId, String internalPid, JSONObject apolloJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      if (!allow3PSellers) {

         setOffersForMainPageSeller(offers, apolloJson, doc);

      } else {

         if (!doc.select(listSelectors.get("hasPageOffers")).isEmpty()) {

            Document sellersDoc = null;
            Elements sellersFromHTML = null;
            Elements sellerMainFromHTML = null;

            String urlOffer = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "a[class^=\"more-offers\"]", "href");
            String offersPageUrl = "";
            if (urlOffer != null) {
               offersPageUrl = urlPageOffers + urlOffer.replace("/parceiros/", "").replaceAll("productSku=([0-9]+)", "productSku=" + internalId);
               sellersDoc = accessOffersPage(offersPageUrl);
               if (sellersDoc != null){
                  sellersFromHTML = sellersDoc.select(listSelectors.get("offers"));
                  sellerMainFromHTML = sellersDoc.select("div[class^=\"src__MainOffer\"]");
               }
            }

            if (sellersFromHTML == null && sellersFromHTML.isEmpty()) {
               offersPageUrl = urlPageOffers + internalPid + "?productSku=" + internalId;
               sellersDoc = accessOffersPage(offersPageUrl);
               sellersFromHTML = sellersDoc != null ? sellersDoc.select(listSelectors.get("offers")) : null;
            }


            if (sellerMainFromHTML != null && !sellerMainFromHTML.isEmpty()) {

               setOffersForSellersPage(offers, sellerMainFromHTML, listSelectors, sellersDoc);
            }

            if (sellersFromHTML != null && !sellersFromHTML.isEmpty()) {

               setOffersForSellersPage(offers, sellersFromHTML, listSelectors, sellersDoc);
            }

         } else {

            setOffersForMainPageSeller(offers, apolloJson, doc);

         }

        /*
               caso sellersFromHTML seja vazio significa que fomos bloqueados
               durante a tentativa de capturar as informações na pagina de sellers
               ou que o produto em questão não possui pagina de sellers.
               Nesse caso devemos capturar apenas as informações da pagina principal.
               */

      }
      return offers;
   }



//   protected void setOffersForSellersPage(Offers offers, Elements sellers, Map<String, String> listSelectors, Document sellersDoc) throws MalformedPricingException, OfferException {
//
//      if (sellers.size() > 0) {
//
//         for (int i = 0; i < sellers.size(); i++) {
//            Element sellerInfo = sellers.get(i);
//            // The Business logic is: if we have more than 1 seller is buy box
//            boolean isBuyBox = sellers.size() > 1;
//            String sellerName = CrawlerUtils.scrapStringSimpleInfo(sellerInfo, listSelectors.get("selectorSellerName"), false);
//            String rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo, listSelectors.get("selectorSellerId"), "href");
//            if (rawSellerId == null) {
//               rawSellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sellerInfo, listSelectors.get("selectorSellerId2"), "href");
//            }
//            String sellerId = scrapSellerIdFromURL(rawSellerId);
//            if (sellers.size() == 1 && sellerId == null) {
//               JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(sellersDoc, "script", "window.__APOLLO_STATE__ =", null, false, true);
//               JSONObject offersJson = SaopauloB2WCrawlersUtils.getJson(jsonSeller, "OffersResult");
//               String keySeller = JSONUtils.getValueRecursive(offersJson, "seller.__ref", String.class);
//               JSONObject jsonInfoSeller = jsonSeller.optJSONObject(keySeller);
//               sellerId = jsonInfoSeller.optString("id");
//            }
//            Integer mainPagePosition = i == 0 ? 1 : null;
//            Integer sellersPagePosition = i + 1;
//
//            if (sellerId == null) {
//               byte[] bytes = sellerName.getBytes(StandardCharsets.US_ASCII);
//               sellerId = Base64.getEncoder().encodeToString(bytes);
//            }
//
//            Pricing pricing = scrapPricingForOffersPage(sellerInfo);
//
//            if (!checkIfHasSellerInOffer(offers, sellerId, pricing, sellerName)) {
//
//               Offer offer = Offer.OfferBuilder.create()
//                  .setInternalSellerId(sellerId)
//                  .setSellerFullName(sellerName)
//                  .setMainPagePosition(mainPagePosition)
//                  .setSellersPagePosition(sellersPagePosition)
//                  .setPricing(pricing)
//                  .setIsBuybox(isBuyBox)
//                  .setIsMainRetailer(false)
//                  .build();
//
//               offers.add(offer);
//            }
//         }
//      }
//   }

}
