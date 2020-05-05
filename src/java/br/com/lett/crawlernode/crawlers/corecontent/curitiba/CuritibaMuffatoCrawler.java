package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (11/07/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 3) The sku page identification is done simply looking the URL format combined with some html
 * element.
 * 
 * 4) Availability is crawled from the sku json extracted from a script in the html.
 * 
 * 5) InternalPid is equals internalId for this market.
 * 
 * 6) We have one method for each type of information for a sku (please carry on with this pattern).
 * 
 * Examples: ex1 (available):
 * http://delivery.supermuffato.com.br/leite-em-po-nestle-nan-soy-400g-97756/p?sc=10
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class CuritibaMuffatoCrawler extends Crawler {

   private final String HOME_PAGE = "https://delivery.supermuffato.com.br/";

   public CuritibaMuffatoCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);

   }

   @Override
   public String handleURLBeforeFetch(String curURL) {

      if (curURL.split("\\?")[0].endsWith("/p")) {

         try {
            String url = curURL;
            List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
            List<NameValuePair> paramsNew = new ArrayList<>();

            for (NameValuePair param : paramsOriginal) {
               if (!param.getName().equals("sc")) {
                  paramsNew.add(param);
               }
            }

            paramsNew.add(new BasicNameValuePair("sc", "13"));
            URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

            builder.clearParameters();
            builder.setParameters(paramsNew);

            curURL = builder.build().toString();

            return curURL;

         } catch (URISyntaxException e) {
            return curURL;
         }
      }

      return curURL;

   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

         VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, "supermuffato", HOME_PAGE, cookies, dataFetcher);

         // InternalId
         String internalId = crawlInternalId(doc);

         // InternalPid
         String internalPid = crawlInternalPid(doc);

         // Name
         String name = crawlName(doc);

         // Price
         Float price = crawlMainPagePrice(doc);

         // Categorias
         ArrayList<String> categories = crawlCategories(doc);
         String category1 = getCategory(categories, 0);
         String category2 = getCategory(categories, 1);
         String category3 = getCategory(categories, 2);

         // Sku json from script
         JSONObject skuJson = crawlSkuJson(doc);

         boolean available = crawlAvailability(skuJson);
         JSONObject apiJSON = crawlApi(internalId);
         String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
         String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
         String description = crawlDescription(doc, internalId);
         Integer stock = null;
         Marketplace marketplace = new Marketplace();
         Prices prices = crawlPrices(doc, price);
         RatingsReviews ratingReviews = scrapRatingAndReviews(doc, internalId);

         // ean data in html
         JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);
         String ean = 0 < arrayEan.length() ? arrayEan.getString(0) : null;

         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // create the product
         Product product = new Product();
         product.setUrl(session.getOriginalURL());
         product.setInternalId(internalId);
         product.setInternalPid(internalPid);
         product.setName(name);
         product.setPrice(price);
         product.setPrices(prices);
         product.setCategory1(category1);
         product.setCategory2(category2);
         product.setCategory3(category3);
         product.setRatingReviews(ratingReviews);
         product.setPrimaryImage(primaryImage);
         product.setSecondaryImages(secondaryImages);
         product.setDescription(description);
         product.setStock(stock);
         product.setMarketplace(marketplace);
         product.setAvailable(available);
         product.setEans(eans);

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page.");
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.select(".container.prd-info-container").first() != null;
   }


   /*******************
    * General methods *
    *******************/


   public JSONObject crawlApi(String internalId) {
      String url = HOME_PAGE + "produto/sku/" + internalId;

      Map<String, String> cookie = new HashMap<>();
      cookie.put("cookie",
            "VtexRCMacIdv7=bf4532a0-691e-11ea-9cce-99aa504bfa00;"
                  + " checkout.vtex.com=__ofid=bfdc8a0e98ca409c9ce0de2ed0e43994;"
                  + " StandoutTag=218815c4-ea06-9b66-82d7-44eda9f3de6f;"
                  + " VtexFingerPrint=b8ebfd8d0bd536846f201fe5e783674d;"
                  + " _ga=GA1.4.1536285874.1584539143; _gcl_au=1.1.1895307215.1584539143;"
                  + " _fbp=fb.2.1584539144138.1720978257; newsletter_modal_is_hidden=true;"
                  + " .ASPXAUTH=5F1318BE552E4142E0EE0D4548D575C427F22697772E7BF8C14F226EA657A8CFA5EE9AE1C9D1FADDC66DE18D270C492B6773D2B583E97652D100030EDDECA69C432F007A85D1D22792877BEC55DD21C230CBA1C6A79F2B259B58E56423327D146EACDCC687BF2927C113B09F75603CE533EE1E1DDF0282FB2E8BC861B598005A3113F70AD1749FFB76E2B6C0F7B9F334C5F779DA8A752287A35CE20555464B272C38686E;"
                  + " _ga=GA1.3.2072354686.1584733159; IPI=UrlReferrer=https%3a%2f%2fwww.google.com%2f;"
                  + " __utmz=73079430.1584735021.4.2.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided);"
                  + " __utma=13449269.2072354686.1584733159.1584733162.1585058371.2;"
                  + " __utmz=13449269.1585058371.2.2.utmcsr=google|utmccn=(organic)|utmcmd=organic|utmctr=(not%20provided);"
                  + " stc120214=env:1585058368%7C20200424135928%7C20200324142933%7C2%7C1097393:20210324135933|uid:1584733159823.1498833973.5264597.120214.912890393.:20210324135933|srchist:1097393%3A1585058368%3A20200424135928:20210324135933|tsa:0:20200324142933;"
                  + " _pwz_lead_hash_19796=d834b1f4bd8d394a16c6595c7fb533f5;"
                  + " janus_sid=dec86bdb-4803-4f30-939d-b95b1cd839ac;"
                  + " _gid=GA1.4.461023665.1588620896;"
                  + " _gid=GA1.3.461023665.1588620896;"
                  + " VTEXSC=sc=13;"
                  + " vtex_session=eyJhbGciOiJFUzI1NiIsImtpZCI6IjE4OTgwOENCREIyQTA4NjIyQTNFQzQ0NjIxMzRFQjA0MTQ0NDQ1OTciLCJ0eXAiOiJqd3QifQ.eyJhY2NvdW50LmlkIjoiM2IxMGQzY2QtYjhjMC00YWM2LWE4MDEtNGU3ZTc5Zjk1ZTE0IiwiaWQiOiI4ODJiNWUyMC1hZTcxLTQ2ZTEtOWE4YS02ZTZiZTU2ZmU2ODUiLCJ2ZXJzaW9uIjoyLCJzdWIiOiJzZXNzaW9uIiwiYWNjb3VudCI6InNlc3Npb24iLCJleHAiOjE1ODkzMTM5MjQsImlhdCI6MTU4ODYyMjcyNCwiaXNzIjoidG9rZW4tZW1pdHRlciIsImp0aSI6IjlkNzkwMjIwLTQxNjItNGY1MC1hM2U4LTU4ZWIzNDQwMDJhZSJ9.JV2lv4NGy8-Gb6IJvGrlkvu0rBMm-mMWH-kjDhjcDa3unzrb52fbBFXTHAC16XRxIz8G3a0AXg_N99QW2f4gjg;"
                  + " vtex_segment=eyJjYW1wYWlnbnMiOm51bGwsImNoYW5uZWwiOiIxMyIsInByaWNlVGFibGVzIjpudWxsLCJyZWdpb25JZCI6bnVsbCwidXRtX2NhbXBhaWduIjpudWxsLCJ1dG1fc291cmNlIjpudWxsLCJ1dG1pX2NhbXBhaWduIjpudWxsLCJjdXJyZW5jeUNvZGUiOiJCUkwiLCJjdXJyZW5jeVN5bWJvbCI6IlIkIiwiY291bnRyeUNvZGUiOiJCUkEiLCJjdWx0dXJlSW5mbyI6InB0LUJSIiwiYWRtaW5fY3VsdHVyZUluZm8iOiJwdC1CUiJ9; VtexRCSessionIdv7=0%3A8bf42730-8eda-11ea-8888-71aa08403947; __utma=73079430.1536285874.1584539143.1588620897.1588687990.8; __utmc=73079430; __utmt=1; ISSMB=ScreenMedia=0&UserAcceptMobile=False; SGTS=2660C640CA6ACFB456D10F7975A61534;"
                  + " urlLastSearch=http://delivery.supermuffato.com.br/limpeza/desinfetantes; VtexRCRequestCounter=5; _gat_UA-38452972-2=1; __utmb=73079430.5.10.1588687990");

      Request request = RequestBuilder.create().setHeaders(cookie).setUrl(url).build();
      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (jsonArray.length() > 0) {
         return jsonArray.getJSONObject(0);
      }

      return new JSONObject();
   }

   private String crawlInternalId(Document document) {
      String internalId = null;
      Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
      if (elementInternalID != null) {
         internalId = elementInternalID.text();
      }

      return internalId;
   }


   private String crawlInternalPid(Document document) {
      String internalPid = null;
      Element elementInternalID = document.select(".prd-references .prd-code .skuReference").first();
      if (elementInternalID != null) {
         internalPid = elementInternalID.text();
      }

      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select(".fn.productName").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private Float crawlMainPagePrice(Document document) {
      Float price = null;
      Element elementPrice = document.select(".plugin-preco .preco-a-vista .skuPrice").first();
      if (elementPrice != null) {
         price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      return price;
   }

   private boolean crawlAvailability(JSONObject skuJson) {
      if (skuJson != null && skuJson.has("available")) {
         return skuJson.getBoolean("available");
      }
      return false;
   }

   private ArrayList<String> crawlCategories(Document document) {
      ArrayList<String> categories = new ArrayList<>();
      Elements elementCategories = document.select(".breadcrumb-holder .container .row .bread-crumb ul li a");

      for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
                                                           // is the market name
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String getCategory(ArrayList<String> categories, int n) {
      if (n < categories.size()) {
         return categories.get(n);
      }

      return "";
   }

   private String crawlDescription(Document document, String internalId) {
      StringBuilder description = new StringBuilder();
      Element elementDescription = document.select("#prd-description #prd-accordion-c-one").first();
      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element specificDescription = document.selectFirst("#caracteristicas");

      if (specificDescription != null) {
         description.append(specificDescription.html());
      }

      description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

      return description.toString();
   }

   /**
    * No bank slip payment method in this ecommerce.
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         Element priceFrom = doc.select(".skuListPrice").first();
         if (priceFrom != null) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
         }

         Element installmentElement = doc.select(".skuBestInstallmentNumber").first();

         if (installmentElement != null) {
            Integer installment = Integer.parseInt(installmentElement.text());

            Element valueElement = doc.select(".skuBestInstallmentValue").first();

            if (valueElement != null) {
               Float value = MathUtils.parseFloatWithComma(valueElement.text());

               installmentPriceMap.put(installment, value);
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      return prices;
   }

   /**
    * Get the script having a json variable with the image in it
    * 
    * @return
    */
   private JSONObject crawlSkuJson(Document document) {
      Elements scriptTags = document.getElementsByTag("script");
      JSONObject skuJson = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith("var skuJson_0 = ")) {

               skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
                     + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);

            }
         }
      }

      return skuJson;
   }

   private JSONObject getRating(Document doc, String internalId) {
      JSONObject ratingJson = new JSONObject();
      String idWebsite = getIdWebsite(doc);
      JSONObject response = CrawlerUtils.stringToJson(sendRequestToAPI(internalId, "rating", idWebsite));

      if (response.optJSONArray(internalId) instanceof JSONArray) {
         JSONArray rate = response.getJSONArray(internalId);

         if (rate.length() > 0) {
            ratingJson = rate.getJSONObject(0);
         }
      }

      return ratingJson;
   }

   /**
    * 
    * @param doc
    * @param internalId
    * @return json representing an AdvancedRatingReview object as provided in its documentation.
    */
   private JSONObject getReview(Document doc, String internalId) {
      JSONObject ratingJson = new JSONObject();
      String idWebsite = getIdWebsite(doc);
      JSONArray response = CrawlerUtils.stringToJsonArray(sendRequestToAPI(internalId, "reviews", idWebsite));
      if (response.optJSONObject(0) instanceof JSONObject) {
         JSONObject jsonReviews = response.optJSONObject(0);
         if (jsonReviews.optJSONArray("stats") instanceof JSONArray) {
            JSONArray starts = jsonReviews.optJSONArray("stats");
            ratingJson.put(AdvancedRatingReview.RATING_STAR_1_FIELD, starts.get(0));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_2_FIELD, starts.get(1));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_3_FIELD, starts.get(2));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_4_FIELD, starts.get(3));
            ratingJson.put(AdvancedRatingReview.RATING_STAR_5_FIELD, starts.get(4));
         }
      }

      return ratingJson;
   }

   /**
    * 
    * @param internalId
    * @param type can be only "rating" or "reviews"
    * @param idWebsite
    * @return
    */
   private String sendRequestToAPI(String internalId, String type, String idWebsite) {
      String apiUrl = "https://awsapis3.netreviews.eu/product";
      String payload =
            "{\"query\":\"" + type + "\",\"products\":\"" + internalId + "\",\"idWebsite\":\"" + idWebsite + "\",\"plateforme\":\"br\"}";
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json; charset=UTF-8");
      Request request =
            RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).setPayload(payload).mustSendContentEncoding(false).build();
      return new FetcherDataFetcher().post(session, request).getBody();
   }

   private String getIdWebsite(Document doc) {
      // Filtra os elementos script pela url correta e atributo.

      Optional<Element> optionalUrlToken = doc.select("body > script").stream()
            .filter(x -> (x.hasAttr("src") &&
                  (x.attr("src").startsWith("https://cl.avis-verifies.com"))))
            .findFirst();

      String attr = optionalUrlToken.get().attr("src");

      String[] strings = attr.substring(attr.indexOf("br/")).split("/");

      return strings[strings.length - 4];
   }

   protected RatingsReviews scrapRatingAndReviews(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject rating = getRating(doc, internalId);
      Integer totalReviews = CrawlerUtils.getIntegerValueFromJSON(rating, "count", 0);
      Double avgRating = CrawlerUtils.getDoubleValueFromJSON(rating, "rate", true, false);
      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview(getReview(doc, internalId));
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setDate(session.getDate());
      ratingReviews.setTotalRating(totalReviews);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(avgRating == null ? 0d : avgRating);
      ratingReviews.setInternalId(internalId);

      return ratingReviews;

   }
}
