package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import static br.com.lett.crawlernode.util.CrawlerUtils.setCookie;

public class AmazonScraperUtils {

   private final Logger logger;
   private final Session session;
   public static final String HOST = "www.amazon.com.br";

   public static final String SELLER_NAME = "amazon.com.br";
   public static final String SELLER_NAME_2 = "amazon.com";
   public static final String SELLER_NAME_3 = "Amazon";
   public static Map<String, String> listSelectors = getListSelectors();


   protected Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.ELO.toString());


   public AmazonScraperUtils(Logger logger, Session session) {
      this.logger = logger;
      this.session = session;
   }

   private static Map<String, String> getListSelectors() {
      Map<String, String> listSelectors = new HashMap<>();
      listSelectors.put("iconArrowOffer", ".a-icon.a-icon-arrow.a-icon-small.arrow-icon");
      listSelectors.put("linkOffer", "#olp_feature_div span.a-declarative .a-link-normal");

      return listSelectors;
   }

   public List<Cookie> handleCookiesBeforeFetch(String url, List<Cookie> cookies) {
      Response response = getRequestCookies(url, cookies);
      return fetchCookiesFromAPage(response, "www.amazon.com.br", "/");
   }

   public static List<Cookie> fetchCookiesFromAPage(Response response, String domain, String path) {
      List<Cookie> cookies = new ArrayList<>();

      List<Cookie> cookiesResponse = response.getCookies();
      for (Cookie cookieResponse : cookiesResponse) {
         cookies.add(setCookie(cookieResponse.getName(), cookieResponse.getValue(), domain, path));
      }

      return cookies;
   }


   public Response getRequestCookies(String url, List<Cookie> cookies) {

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "no");
      headers.put("authority", "www.amazon.com.br");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("service-worker-navigation-preload", "true");
      headers.put("rrt", "200");
      headers.put("cache-control", "max-age=0");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");

      List<String> proxies = Arrays.asList(
         ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY
      );

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create()
            .mustRetrieveStatistics(true)
            .mustUseMovingAverage(false)
            .setForbiddenCssSelector("#captchacharacters").build())
         .build();
      Response response;
      List<DataFetcher> fetchers = List.of(new JsoupDataFetcher(), new FetcherDataFetcher(), new ApacheDataFetcher());
      int attempt = 0;
      do {
         response = CrawlerUtils.retryRequest(request, session, fetchers.get(attempt), true);
         attempt++;
         if (response.isSuccess() && response.getCookies().size() > 0) {
            return response;
         }
      } while (attempt < fetchers.size());
      Logging.printLogError(logger, session, "Request for get cookies failed!");
      return response;
   }

   public Response fetchProductPageResponse(List<Cookie> cookies,DataFetcher dataFetcher) {
      return fetchResponse(session.getOriginalURL(), new HashMap<>(), cookies);
   }

   /**
    * Fetch html from amazon
    */
   public String fetchPage(String url, Map<String, String> headers, List<Cookie> cookies,DataFetcher dataFetcher) {
      Response response = fetchResponse(url, headers, cookies);
      if (response != null) {
         return response.getBody();
      }
      return null;
   }

   private Response fetchResponse(String url, Map<String, String> headers, List<Cookie> cookies) {

      List<String> proxies = Arrays.asList(
         ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
         ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY);
      Request request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(proxies)
         .setFetcheroptions(FetcherOptionsBuilder.create()
            .setForbiddenCssSelector("#captchacharacters").build())
         .build();
      List<DataFetcher> fetchers = List.of(new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher());
      int attempt = 0;
      while (attempt < fetchers.size()) {
         DataFetcher fetcher = fetchers.get(attempt);
         if (fetcher instanceof FetcherDataFetcher) {
            headers.put("Accept-Encoding", "no");
            request.setFetcherOptions(FetcherOptionsBuilder.create()
               .mustRetrieveStatistics(true)
               .setForbiddenCssSelector("#captchacharacters").build());
         }
         Response response = CrawlerUtils.retryRequest(request, session, fetcher, true);
         attempt++;
         if (response.isSuccess()) {
            return response;
         }
      }
      return null;
   }

   /**
    * @param images   - array present on html
    * @param doc      - html
    * @param host     - host of image url ex: www.amazon.com or www.amazon.com.br or www.amazon.com.mx
    * @param protocol - http or https
    * @return
    */
   public String scrapPrimaryImage(JSONArray images, Document doc, String host, String protocol) {
      String primaryImage = null;

      if (images.length() > 0) {
         JSONObject image = images.getJSONObject(0);

         if (image.has("mainUrl") && !image.isNull("mainUrl")) {
            primaryImage = image.get("mainUrl").toString().trim();
         } else if (image.has("thumbUrl") && !image.isNull("thumbUrl")) {
            primaryImage = image.get("thumbUrl").toString().trim();
         } else if (image.has("hiRes") && !image.isNull("hiRes")) {
            primaryImage = image.get("hiRes").toString().trim();
         } else if (image.has("large") && !image.isNull("large")) {
            primaryImage = image.get("large").toString().trim();
         } else if (image.has("thumb") && !image.isNull("thumb")) {
            primaryImage = image.get("thumb").toString().trim();
         }

      } else {
         Element img = doc.select("#ebooksImageBlockContainer img").first();

         if (img != null) {
            primaryImage = img.attr("src").trim();
         }
      }

      return primaryImage;
   }


   /**
    * @param images   - array present on html
    * @param host     - host of image url ex: www.amazon.com or www.amazon.com.br or www.amazon.com.mx
    * @param protocol - http or https
    * @return
    */
   public List<String> scrapSecondaryImages(JSONArray images, String host, String protocol) {
      List<String> secondaryImages = new ArrayList<>();

      for (int i = 1; i < images.length(); i++) { // first index is the primary Image
         JSONObject imageJson = images.getJSONObject(i);

         String image = null;

         if (imageJson.has("mainUrl") && !imageJson.isNull("mainUrl")) {
            image = imageJson.get("mainUrl").toString().trim();
         } else if (imageJson.has("thumbUrl") && !imageJson.isNull("thumbUrl")) {
            image = imageJson.get("thumbUrl").toString().trim();
         } else if (imageJson.has("hiRes") && !imageJson.isNull("hiRes")) {
            image = imageJson.get("hiRes").toString().trim();
         } else if (imageJson.has("large") && !imageJson.isNull("large")) {
            image = imageJson.get("large").toString().trim();
         } else if (imageJson.has("thumb") && !imageJson.isNull("thumb")) {
            image = imageJson.get("thumb").toString().trim();
         }

         if (image != null) {
            secondaryImages.add(image);
         }

      }

      return secondaryImages;
   }

   /**
    * Get json of images inside html
    *
    * @param doc
    * @return
    */
   public JSONArray scrapImagesJSONArray(Document doc) {
      JSONArray images = new JSONArray();

      JSONObject data = scrapImagesJson(doc);

      if (data.has("imageGalleryData")) {
         images = data.getJSONArray("imageGalleryData");
      } else if (data.has("colorImages")) {
         JSONObject colorImages = data.getJSONObject("colorImages");

         if (colorImages.has("initial")) {
            images = colorImages.getJSONArray("initial");
         }
      } else if (data.has("initial")) {
         images = data.getJSONArray("initial");
      }

      return images;
   }

   private JSONObject scrapImagesJson(Document doc) {
      JSONObject data = new JSONObject();

      String firstIndex = "vardata=";
      String lastIndex = "};";

      // this keys are to identify images JSON
      String idNormalImages = "imageGalleryData";
      String idColorImages = "colorImages";

      Elements scripts = doc.select("script[type=\"text/javascript\"]");
      for (Element e : scripts) {
         // This json can be broken, we need to remove additional ','
         String script = e.html()
            .replace(" ", "")
            .replaceAll("\n", "")
            .replace(",}", "}")
            .replace(",]", "]");

         if (script.contains(firstIndex) && script.contains(lastIndex) && (script.contains(idColorImages) || script.contains(idNormalImages))) {
            String json = CrawlerUtils.extractSpecificStringFromScript(script, firstIndex, false, lastIndex, false);
            if (json != null && json.trim().startsWith("{") && json.trim().endsWith("}")) {

               try {
                  data = new JSONObject(json.trim());
               } catch (JSONException e1) {
                  Logging.printLogWarn(logger, session, e1.getMessage());

                  // This case we try to scrap initialJsonArray, because the complete json is not valid
                  String initialJson = CrawlerUtils.extractSpecificStringFromScript(json, "initial':", false, "},'", false);
                  if (initialJson != null && initialJson.trim().startsWith("[") && initialJson.trim().endsWith("]")) {
                     try {
                        data = new JSONObject().put("initial", new JSONArray(initialJson.trim()));
                     } catch (JSONException e2) {
                        Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e2));
                     }
                  }

               }
            }

            break;
         }
      }

      return data;
   }

   public String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("input[name^=ASIN]").first();
      Element internalIdElementSpecial = doc.select("input.askAsin").first();


      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      } else if (internalIdElementSpecial != null) {
         internalId = internalIdElementSpecial.val();
      } else {
         internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#all-offers-display-params", "data-qid");
      }

      return internalId;
   }

   public String crawlName(Document document) {
      String name = null;

      Element nameElement = document.select("#centerCol h1#title").first();
      Element nameElementSpecial = document.select("#productTitle").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      } else if (nameElementSpecial != null) {
         name = nameElementSpecial.text().trim();
      }

      return name;
   }

   public String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      Element prodInfoElement = doc.selectFirst("#prodDetails");
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(doc, "#productDescription > p > span", true);
      String featureDescription = CrawlerUtils.scrapStringSimpleInfo(doc, "#productOverview_feature_div tbody", false);

      Elements elementsDescription =
         doc.select("#detail-bullets_feature_div, #detail_bullets_id, #feature-bullets, #bookDescription_feature_div, #aplus_feature_div");

      for (Element e : elementsDescription) {
         description.append(e.html().replace("noscript", "div"));
      }

      Elements longDescription = doc.select(".feature[id^=btfContent]");

      for (Element e : longDescription) {
         Element compare = e.select("#compare").first();

         if (compare == null) {
            description.append(e.html());
         }
      }

      Elements scripts = doc.select("script[type=\"text/javascript\"]");
      String token = "var iframeContent =";

      for (Element e : scripts) {
         String script = e.html();

         if (script.contains(token)) {

            if (script.contains("iframeDocument.getElementById")) {
               continue;
            }

            String iframeDesc = CrawlerUtils.extractSpecificStringFromScript(script, token, false, ";", false);

            try {
               description.append(URLDecoder.decode(iframeDesc, "UTF-8"));
            } catch (UnsupportedEncodingException ex) {
               Logging.printLogError(logger, session, CommonMethods.getStackTrace(ex));
            }

            break;
         }
      }

      if (prodInfoElement != null) {
         description.append(prodInfoElement);
      }
      if (productDescription != null && !productDescription.isEmpty()) {
         description.append(productDescription);
      }

      if (featureDescription != null && !featureDescription.isEmpty()) {
         description.append(featureDescription);
      }

      return description.toString();
   }


   public List<String> crawlEan(Document doc) {
      String ean = null;

      List<String> eanKeys = Arrays.asList("código de barras:", "ean:", "eans:", "código de barras", "codigo de barras", "ean", "eans", "EAN");

      Elements attributes = doc.select(".a-keyvalue.prodDetTable tbody tr");
      for (Element att : attributes) {
         String key = CrawlerUtils.scrapStringSimpleInfo(att, ".prodDetSectionEntry", true);

         if (key != null && eanKeys.contains(key.toLowerCase())) {
            ean = CrawlerUtils.scrapStringSimpleInfo(att, ".a-size-base.prodDetAttrValue", true);

            break;
         }
      }

      return ean != null ? new ArrayList<>(Arrays.asList(ean.split(","))) : null;

   }

   public Offer scrapMainPageOffer(Document doc) throws OfferException, MalformedPricingException {
      String seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox .tabular-buybox-text[tabular-attribute-name=\"Vendido por\"] span", false);
      if (seller == null) {
         seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text", false);
      }

      String sellerUrl = CrawlerUtils.scrapUrl(doc, "#tabular-buybox-truncate-1 .a-truncate-full .tabular-buybox-text a", "href", "https", HOST);
      String sellerId = scrapSellerIdByUrl(sellerUrl);

      if (seller == null) {
         seller = CrawlerUtils.scrapStringSimpleInfo(doc, "#merchant-info", false);

         if (seller != null && seller.contains("vendido por")) {
            seller = CommonMethods.getLast(seller.split("vendido por")).trim();

            if (seller.endsWith(".")) {
               seller = seller.substring(0, seller.length() - 1);
            }
         }
      }

      if (seller != null && !seller.isEmpty()) {
         boolean isMainRetailer = seller.equalsIgnoreCase(SELLER_NAME) || seller.equalsIgnoreCase(SELLER_NAME_2) || seller.equalsIgnoreCase(SELLER_NAME_3);
         Pricing pricing = scrapMainPagePricing(doc);
         List<String> sales = new ArrayList<>();
         sales.add(CrawlerUtils.calculateSales(pricing));
         if (sellerId == null) {
            sellerId = CommonMethods.toSlug(seller);
         }
         String promoBuyMoreThanOne = CrawlerUtils.scrapStringSimpleInfo(doc, ".promoPriceBlockMessage div span", true);
         if (promoBuyMoreThanOne != null && !promoBuyMoreThanOne.isEmpty()) {
            sales.add(promoBuyMoreThanOne);
         }

         return Offer.OfferBuilder.create()
            .setInternalSellerId(sellerId)
            .setSellerFullName(seller)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(isMainRetailer)
            .setPricing(pricing)
            .setSales(sales)
            .build();
      }

      return null;
   }


   public String scrapSellerIdByUrl(String sellerUrl) {
      String sellerId = null;

      if (sellerUrl != null) {
         for (String parameter : sellerUrl.split("&")) {
            if (parameter.startsWith("seller=")) {
               sellerId = parameter.split("=")[1];
               break;
            }
         }
      }

      return sellerId;
   }

   public Pricing scrapMainPagePricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#priceblock_ourprice", null, true, ',', session);

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#priceblock_dealprice, #priceblock_saleprice, #unifiedPrice_feature_div #conditionalPrice .a-color-price", null, false, ',', session);

         if (spotlightPrice == null) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#soldByThirdParty span", null, false, ',', session);
         }
         if (spotlightPrice == null) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[id=price]", null, false, ',', session);
         }
         if (spotlightPrice == null) {
            spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price > span", null, false, ',', session);
         }
      }

      CreditCards creditCards = scrapCreditCardsFromSellersPage(doc, spotlightPrice);
      Double savings = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#dealprice_savings .priceBlockSavingsString",
         null, false, ',', session);

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#buyBoxInner .a-list-item span:nth-child(2n)", null, false, ',', session);
      if (savings != null) {
         priceFrom = spotlightPrice + savings;
      }
      if (priceFrom == null) {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[id=listPrice]", null, false, ',', session);
      }

      if (priceFrom == null) {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.a-size-small.a-color-secondary.aok-align-center.basisPrice > span > span.a-offscreen", null, false, ',', session);
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setPriceFrom(priceFrom)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   public String scrapSellerName(Element oferta) {
      String name = "";
      if (oferta != null) {
         String rawSallerName = CrawlerUtils.scrapStringSimpleInfoByAttribute(oferta, ".a-button-inner input", "aria-label");

         if (rawSallerName == null) {
            rawSallerName = CrawlerUtils.scrapStringSimpleInfo(oferta, ".a-button-inner span .a-offscreen", false);
         }

         String split = rawSallerName != null ? rawSallerName.split("do vendedor ")[1] : null;
         name = split != null ? split.split("e preço")[0] : null;
      }
      return name;
   }


   public Pricing scrapSellersPagePricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price span", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price .a-offscreen", null, false, ',', session);
      }
      CreditCards creditCards = scrapCreditCardsFromSellersPage(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   public Pricing scrapSellersPagePricingInBuyBox(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-size-medium.a-color-price", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-price .a-offscreen", null, false, ',', session);
      }
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".a-size-medium.a-color-base", null, false, ',', session);
      }

      CreditCards creditCards = scrapCreditCardsFromSellersPage(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   public CreditCards scrapCreditCardsFromSellersPage(Element doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   public RatingsReviews crawlRating(Document document, String internalId) {

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      RatingsReviews ratingReviews = new RatingsReviews();

      if (document.select("#cm-cr-dp-no-reviews-message").isEmpty()) {
         ratingReviews.setDate(session.getDate());

         if (internalId != null) {
            Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(document,
               "#acrCustomerReviewText, #reviews-medley-cmps-expand-head > #dp-cmps-expand-header-last > span:not([class])", true, 0);
            Double avgRating = getTotalAvgRating(document);

            ratingReviews.setInternalId(internalId);
            ratingReviews.setTotalRating(totalNumOfEvaluations);
            ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
            ratingReviews.setAverageOverallRating(avgRating);
            ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReviews(document, totalNumOfEvaluations));
         }
      }

      return ratingReviews;
   }

   public AdvancedRatingReview scrapAdvancedRatingReviews(Document doc, Integer totalNumOfEvaluations) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      if (totalNumOfEvaluations > 0) {
         for (Element starElement : doc.select("#histogramTable tr")) {
            Integer star = CrawlerUtils.scrapIntegerFromHtml(starElement, ".aok-nowrap .a-size-base a", true, 0);
            Integer percentage = CrawlerUtils.scrapIntegerFromHtml(starElement, ".a-text-right .a-size-base a", true, 0);
            Integer total = percentage > 0 ? Math.round((totalNumOfEvaluations * (percentage / 100f))) : 0;

            if (star == 1) {
               star1 = total;
            } else if (star == 2) {
               star2 = total;
            } else if (star == 3) {
               star3 = total;
            } else if (star == 4) {
               star4 = total;
            } else if (star == 5) {
               star5 = total;
            }
         }
      }


      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }

   public Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;
      Element reviews =
         doc.select("#reviewsMedley [data-hook=rating-out-of-text], #reviews-medley-cmps-expand-head > #dp-cmps-expand-header-last span.a-icon-alt")
            .first();

      String text;

      if (reviews != null) {
         text = reviews.ownText().trim();
      } else {
         text = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".reviewCountTextLinkedHistogram[title]", "title");
      }
      String avgText;

      if (text != null && text.contains("de")) {
         if (text.contains(",")) {
            avgText = text.split("de")[0].replaceAll("[^0-9,]", "").replace(",", ".").trim();

         } else {
            avgText = text.split("de")[0].replaceAll("[^.0-9]", "").trim();
         }

         if (!avgText.isEmpty()) {
            avgRating = Double.parseDouble(avgText);
         }
      }

      return avgRating;
   }

   public boolean isProductPage(Document doc) {
      return doc.select("#dp").first() != null;
   }


   /**
    * @param document
    * @return
    */
   public CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select("#wayfinding-breadcrumbs_feature_div ul li:not([class]) a");

      for (Element e : elementCategories) {
         String cat = e.ownText().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   public void getOffersFromBuyBox(Element oferta, int pos, Offers offers) throws MalformedPricingException, OfferException {

      String name = CrawlerUtils.scrapStringSimpleInfo(oferta, ".a-size-small.mbcMerchantName", true);

      Pricing pricing = scrapSellersPagePricingInBuyBox(oferta);
      String sellerUrl = CrawlerUtils.scrapUrl(oferta, ".a-size-small.a-link-normal:first-child", "href", "https", AmazonScraperUtils.HOST);

      String sellerId = scrapSellerIdByUrl(sellerUrl);
      if (name != null) {
         boolean isMainRetailer = name.equalsIgnoreCase(AmazonScraperUtils.SELLER_NAME) || name.equalsIgnoreCase(AmazonScraperUtils.SELLER_NAME_2) || name.equalsIgnoreCase(AmazonScraperUtils.SELLER_NAME_3);

         if (sellerId == null) {
            sellerId = CommonMethods.toSlug(AmazonScraperUtils.SELLER_NAME);
         }

         offers.add(Offer.OfferBuilder.create()
            .setInternalSellerId(sellerId)
            .setSellerFullName(name)
            .setSellersPagePosition(pos)
            .setIsBuybox(false)
            .setIsMainRetailer(isMainRetailer)
            .setPricing(pricing)
            .build());

      }
   }

   public void getOffersFromOfferPage(Element oferta, int pos, Offers offers) throws MalformedPricingException, OfferException {

      String name = scrapSellerName(oferta).trim();

      Pricing pricing = scrapSellersPagePricing(oferta);
      String sellerUrl = CrawlerUtils.scrapUrl(oferta, ".a-size-small.a-link-normal:first-child", "href", "https", AmazonScraperUtils.HOST);

      String sellerId = scrapSellerIdByUrl(sellerUrl);

      boolean isMainRetailer = name.equalsIgnoreCase(AmazonScraperUtils.SELLER_NAME) || name.equalsIgnoreCase(AmazonScraperUtils.SELLER_NAME_2) || name.equalsIgnoreCase(AmazonScraperUtils.SELLER_NAME_3);

      if (sellerId == null) {
         sellerId = CommonMethods.toSlug(AmazonScraperUtils.SELLER_NAME);
      }

      if (!offers.contains(sellerId) && !offers.containsSeller(name)) {

         offers.add(Offer.OfferBuilder.create()
            .setInternalSellerId(sellerId)
            .setSellerFullName(name)
            .setSellersPagePosition(pos)
            .setIsBuybox(false)
            .setIsMainRetailer(isMainRetailer)
            .setPricing(pricing)
            .build());

      }

   }

}
