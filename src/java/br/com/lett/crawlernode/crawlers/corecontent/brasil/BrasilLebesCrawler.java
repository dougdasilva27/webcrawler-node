package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/*********************************************************************************************************************
 * Crawling notes (19/08/2016):
 * 
 * 1) For this crawler, we have one URL for multiple skus. 2) There is no stock information for skus
 * in this ecommerce by the time this crawler was made. 3) There is no marketplace in this ecommerce
 * by the time this crawler was made. 4) The sku page identification is done simply looking for an
 * specific html element. 5) If the sku is unavailable, it's price is not displayed. 6) The price of
 * sku, found in json script, is wrong when the same is unavailable, then it is not crawled. 7)
 * There is internalPid for skus in this ecommerce. The internalPid is a number that is the same for
 * all the variations of a given sku. 8) The primary image is the first image on the secondary
 * images. *
 * 
 * Examples: ex1 (available):
 * http://www.lebes.com.br/gaveteiro-madesa-tutti-colors-34256p1a-rosa-se-568992/p ex2
 * (unavailable): http://www.lebes.com.br/receptor-analogico-century-nanobox-sem-antena-557510/p ex3
 * (color variations): http://www.lebes.com.br/maquina-multibebidas-tres-coracoes-preto-1-543433/p
 *
 *******************************************************************************************************************/

public class BrasilLebesCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lebes.com.br/";

   public BrasilLebesCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc, session.getOriginalURL())) {

         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(doc);
         ArrayList<String> categories = crawlCategories(doc);
         String category1 = getCategory(categories, 0);
         String category2 = getCategory(categories, 1);
         String category3 = getCategory(categories, 2);
         String description = crawlDescription(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         Integer stock = null;
         Map<String, Float> marketplaceMap = crawlMarketplace(doc);
         Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

         JSONArray arraySkus = crawlSkuJsonArray(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            boolean available = crawlAvailability(jsonSku);
            String internalId = crawlInternalId(jsonSku);
            Float price = crawlPrice(jsonSku, available);
            Prices prices = crawlPrices(doc, jsonSku, price);
            String name = crawlName(doc, jsonSku);
            RatingsReviews ratingReviews = crawRating(internalPid);

            // Creating the product
            Product product = new Product();
            product.setUrl(session.getOriginalURL());
            product.setInternalId(internalId);
            product.setInternalPid(internalPid);
            product.setName(name);
            product.setPrice(price);
            product.setPrices(prices);
            product.setAvailable(available);
            product.setCategory1(category1);
            product.setCategory2(category2);
            product.setCategory3(category3);
            product.setPrimaryImage(primaryImage);
            product.setSecondaryImages(secondaryImages);
            product.setDescription(description);
            product.setStock(stock);
            product.setMarketplace(marketplace);
            product.setRatingReviews(ratingReviews);

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document, String url) {
      if (document.select(".productName").first() != null && (url.contains("/p"))) {
         return true;
      }
      return false;
   }


   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = Integer.toString((json.getInt("sku"))).trim();
      }

      return internalId;
   }


   private String crawlInternalPid(Document document) {
      String internalPid = null;
      Element internalPidElement = document.select("#___rc-p-id").first();

      if (internalPidElement != null) {
         internalPid = internalPidElement.attr("value").toString().trim();
      }

      return internalPid;
   }

   private String crawlName(Document document, JSONObject jsonSku) {
      String name = null;
      Element nameElement = document.select(".productName").first();

      String nameVariation = jsonSku.getString("skuname");

      if (nameElement != null) {
         name = nameElement.text().toString().trim();

         if (name.length() > nameVariation.length()) {
            name += " " + nameVariation;
         } else {
            name = nameVariation;
         }
      }

      return name;
   }

   private Float crawlPrice(JSONObject json, boolean available) {
      Float price = null;

      if (json.has("bestPriceFormated") && available) {
         price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      return price;
   }

   /**
    * We use the sku internal id to fetch a page containing the card payment options. URL format:
    * http://www.lebes.com.br/productotherpaymentsystems/skuId
    * 
    * @param document
    * @param skuInformationJson
    * @return
    */
   private Prices crawlPrices(Document document, JSONObject skuInformationJson, Float price) {
      Prices prices = new Prices();

      // bank slip
      Float bankSlipPrice = crawlBankSlipPrice(document, skuInformationJson);
      if (bankSlipPrice != null) {
         prices.setBankTicketPrice(bankSlipPrice);
      } else {
         prices.setBankTicketPrice(price);
      }

      // installments
      String skuId = null;
      if (skuInformationJson.has("sku")) {
         skuId = Integer.toString((skuInformationJson.getInt("sku"))).trim();
         String paymentOptionsURL = "http://www.lebes.com.br/productotherpaymentsystems/" + skuId;
         Request request = RequestBuilder.create().setUrl(paymentOptionsURL).setCookies(cookies).build();
         Document paymentOptionsDocument = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         Elements tableElements = paymentOptionsDocument.select("#divCredito .tbl-payment-system");
         for (Element tableElement : tableElements) {
            Card card = crawlCardFromTableElement(tableElement);
            Map<Integer, Float> installments = crawlInstallmentsFromTable(tableElement);

            prices.insertCardInstallment(card.toString(), installments);
         }

      }

      return prices;
   }

   /**
    * Crawl all the installments numbers and values from a table element. It's the same form as the
    * example on crawlCardFromTableElement method.
    * 
    * @param skuInformationJson
    * @return
    */
   private Map<Integer, Float> crawlInstallmentsFromTable(Element tableElement) {
      Map<Integer, Float> installments = new TreeMap<Integer, Float>();
      Elements lines = tableElement.select("tr");
      for (int i = 1; i < lines.size(); i++) { // first line is the table header
         Element installmentTextElement = lines.get(i).select("td.parcelas").first();
         Element installmentPriceTextElement = lines.get(i).select("td").last();

         if (installmentTextElement != null && installmentPriceTextElement != null) {
            List<String> parsedNumbers = MathUtils.parseNumbers(installmentTextElement.text());
            if (parsedNumbers.size() == 0) { // à vista
               installments.put(1, MathUtils.parseFloatWithComma(installmentPriceTextElement.text()));
            } else {
               installments.put(Integer.parseInt(parsedNumbers.get(0)), MathUtils.parseFloatWithComma(installmentPriceTextElement.text()));
            }
         }
      }

      return installments;
   }

   /**
    *
    * Crawl the card brand from a table html element.
    * 
    * e.g:
    * 
    * Nº de Parcelas Valor de cada parcela Visa à vista R$ 479,90 Visa 2 vezes sem juros R$ 239,95 Visa
    * 3 vezes sem juros R$ 159,96 Visa 4 vezes sem juros R$ 119,97 Visa 5 vezes sem juros R$ 95,98 Visa
    * 6 vezes sem juros R$ 79,98 Visa 7 vezes sem juros R$ 68,55 Visa 8 vezes sem juros R$ 59,98 Visa 9
    * vezes sem juros R$ 53,32 Visa 10 vezes sem juros R$ 47,99 Visa 11 vezes com juros R$ 46,26 Visa
    * 12 vezes com juros R$ 42,61
    *
    * @param table
    * @return
    */
   private Card crawlCardFromTableElement(Element table) {
      Elements lines = table.select("tr");
      for (int i = 1; i < lines.size(); i++) { // the first is the table header
         Element installmentTextElement = lines.get(i).select("td.parcelas").first();
         if (installmentTextElement != null) {
            String installmentText = installmentTextElement.text().toLowerCase();
            if (installmentText.contains(Card.VISA.toString()))
               return Card.VISA;
            if (installmentText.contains(Card.AMEX.toString()))
               return Card.AMEX;
            if (installmentText.contains(Card.DINERS.toString()))
               return Card.DINERS;
            if (installmentText.contains(Card.MASTERCARD.toString()))
               return Card.MASTERCARD;
            if (installmentText.contains(Card.HIPERCARD.toString()))
               return Card.HIPERCARD;
            if (installmentText.contains(Card.ELO.toString()))
               return Card.ELO;
         }
      }
      return Card.UNKNOWN_CARD;
   }

   /**
    * Computes the bank slip price by applying a discount on the base price. The base price is the same
    * that is crawled on crawlPrice method.
    * 
    * For the calculations we round the final number to the lower bound, as observed on the ecommerce.
    * 
    * @param document
    * @param jsonSku
    * @return
    */
   private Float crawlBankSlipPrice(Document document, JSONObject skuInformationJson) {
      Float bankSlipPrice = null;

      if (skuInformationJson.has("bestPriceFormated")) {
         Float basePrice = MathUtils.parseFloatWithComma(skuInformationJson.getString("bestPriceFormated"));
         Float discountPercentage = crawlDiscountPercentage(document);

         // apply the discount on base price
         if (discountPercentage != null) {
            bankSlipPrice = MathUtils.normalizeTwoDecimalPlacesDown(basePrice - (discountPercentage * basePrice));
         }
      }

      return bankSlipPrice;
   }

   /**
    * Look for the discount html element and parses the discount percentage from the element name. In
    * this ecommerce we have elements in this form
    * <p class="flag 5--no-boleto">
    * 5% no boleto
    * </p>
    * where the 5 in the name of the class indicates the percentual value we must apply on the base
    * value. But we must search for the suffix '--no-boleto'.
    * 
    * @return
    */
   private Float crawlDiscountPercentage(Document document) {
      Float discountPercentage = null;
      Element discountElement = document.select(".product__discount p.flag").last();

      if (discountElement != null) {
         String text = discountElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            discountPercentage = Float.parseFloat(text) / 100;

            // cases when html changes
            if (discountPercentage > 1) {
               discountPercentage = 0f;
            }
         }
      }

      return discountPercentage;
   }

   private boolean crawlAvailability(JSONObject json) {

      if (json.has("available")) {
         return json.getBoolean("available");
      }

      return false;
   }

   private Map<String, Float> crawlMarketplace(Document document) {
      return new HashMap<>();
   }

   private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
      return new Marketplace();
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select("#botaoZoom").first();

      if (image != null) {
         String urlImage = image.attr("zoom");

         if (!urlImage.startsWith("http")) {
            urlImage = image.attr("rel");
         }
         primaryImage = urlImage;
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select("#botaoZoom");

      for (int i = 1; i < images.size(); i++) { // starts with index 1, because the first image is the
         // primary image
         Element e = images.get(i);

         if (e.hasAttr("zoom")) {
            String urlImage = e.attr("zoom");

            if (!urlImage.startsWith("http")) {
               urlImage = e.attr("rel");
            }

            secondaryImagesArray.put(urlImage);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private ArrayList<String> crawlCategories(Document document) {
      ArrayList<String> categories = new ArrayList<String>();
      Elements elementCategories = document.select(".bread-crumb > ul li a");

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

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      Element descElement = document.select("#informacoesProduto").first();

      if (descElement != null) {
         description.append(descElement.html());
      }

      Element especificationElement = document.select("#caracteristicas").first();

      if (especificationElement != null) {
         description.append(especificationElement.html());
      }

      Element detalheNagem = document.select(".flixContainer").first();
      String ean = crawlEan(document);

      if (ean != null && detalheNagem != null && !detalheNagem.html().trim().isEmpty()) {
         description.append(CrawlerUtils.crawlDescriptionFromFlixMedia("11947", ean, dataFetcher, session));
      }

      return description.toString();
   }

   private String crawlEan(Document doc) {
      String ean = null;

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "vtex.events.addData(", ");", false, false);

      if (json.has("productEans")) {
         ean = json.getJSONArray("productEans").getString(0);
      }

      return ean;
   }

   /**
    * Get the json array containing information about the skus in this page.
    * 
    * @return
    */
   private JSONArray crawlSkuJsonArray(Document document) {
      JSONObject skuJson = crawlSKUJson(document);
      JSONArray skuJsonArray = null;

      if (skuJson != null) {
         try {
            skuJsonArray = skuJson.getJSONArray("skus");
         } catch (Exception e) {
            skuJsonArray = new JSONArray();
            Logging.printLogWarn(logger, session, CommonMethods.getStackTraceString(e));
         }
      }

      return skuJsonArray;
   }

   /**
    * Get the json containing all skus info. In case we can't find the json on the already loaded html
    * from the sku page, we try to fetch this json from an API.
    * 
    * @param document
    * @return
    */
   private JSONObject crawlSKUJson(Document document) {
      JSONObject skuJson = null;
      Elements scriptTags = document.getElementsByTag("script");

      // first we will try to get the json object in the html
      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith("var skuJson_0 = ")) {
               skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
                     + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);
            }
         }
      }

      // if we couldn't find the json on the html, we will use the API
      if (skuJson == null) {
         Element elementId = document.select("#___rc-p-id").first();
         if (elementId != null) {
            String id = elementId.attr("value").trim();
            String apiURL = "http://www.lebes.com.br/api/catalog_system/pub/products/variations/" + id;
            Request request = RequestBuilder.create().setUrl(apiURL).setCookies(cookies).build();
            skuJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
         }
      }

      return skuJson;
   }

   private RatingsReviews crawRating(String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      String storeKey = "d0b53e21-10fb-4c05-8d9c-10fdafae9edd";

      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, storeKey, this.dataFetcher);
      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, storeKey, dataFetcher);

      Integer totalNumOfEvaluations = yourReviews.getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview totalStarsFromEachValue = yourReviews.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setDate(session.getDate());
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(totalStarsFromEachValue);

      return ratingReviews;
   }
}
