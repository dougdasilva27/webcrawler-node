package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class BrasilIkesakiCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.ikesaki.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "ikesaki";

   public BrasilIkesakiCrawler(Session session) {
      super(session);
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
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         String internalPid = crawlInternalPid(skuJson);
         CategoryCollection categories = crawlCategories(doc);
         String description = crawlDescription(doc);

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         // ean data in html
         JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(jsonSku);
            String name = crawlName(jsonSku, skuJson);
            Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);
            Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId, jsonSku);
            boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
            Float price = crawlMainPagePrice(marketplaceMap);

            JSONObject jsonProduct = crawlApi(internalId);
            String primaryImage = crawlPrimaryImage(jsonProduct);
            String secondaryImages = crawlSecondaryImages(jsonProduct);
            Prices prices = crawlPrices(internalId, price, jsonSku);
            Integer stock = crawlStock(jsonProduct);
            String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;
            RatingsReviews ratingReviews = crawlRating(internalPid);

            List<String> eans = new ArrayList<>();
            eans.add(ean);

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setPrice(price)
                  .setPrices(prices)
                  .setAvailable(available)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setStock(stock)
                  .setMarketplace(marketplace)
                  .setRatingReviews(ratingReviews)
                  .setEans(eans)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /***********
    * Product page identification *
    ***********/

   private boolean isProductPage(Document document) {
      return document.select(".produto").first() != null;
   }

   /*******
    * General methods *
    *******/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = Integer.toString(json.getInt("sku")).trim();
      }

      return internalId;
   }

   private RatingsReviews crawlRating(String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
      Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(getTotalAvgRating(docRating, totalNumOfEvaluations));

      return ratingReviews;
   }

   private Document crawlApiRatings(String url, String internalPid) {
      String[] tokens = url.split("/");
      String productLinkId = tokens[tokens.length - 2];
      String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");

      Request request =
            RequestBuilder.create().setUrl("https://www.ikesaki.com.br/userreview").setCookies(cookies).setHeaders(headers).setPayload(payload).build();
      return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
   }

   private Integer getTotalNumOfRatings(Document doc) {
      return doc.select("div.rating-wrapper").size();
   }

   private Double getTotalAvgRating(Document doc, Integer totalNumOfEvaluations) {
      Double avgRating = 0.0;
      Elements rating = doc.select("div.rating-wrapper");
      if (totalNumOfEvaluations != null && totalNumOfEvaluations != 0) {
         Double total = 0.0;

         for (Element e : rating) {
            Element star = e.selectFirst("div.rating-wrapper div");
            if (star != null) {
               if (star.hasClass("a50")) {
                  total += 5;
               } else if (star.hasClass("a40")) {
                  total += 4;
               } else if (star.hasClass("a30")) {
                  total += 3;
               } else if (star.hasClass("a20")) {
                  total += 2;
               } else if (star.hasClass("a10")) {
                  total += 1;
               }
            }
         }

         avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalNumOfEvaluations);
      }
      return avgRating;
   }

   private String crawlInternalPid(JSONObject skuJson) {
      String internalPid = null;

      if (skuJson.has("productId")) {
         internalPid = skuJson.get("productId").toString();
      }

      return internalPid;
   }

   private String crawlName(JSONObject jsonSku, JSONObject skuJson) {
      String name = null;

      String nameVariation = jsonSku.getString("skuname");

      if (skuJson.has("name")) {
         name = skuJson.getString("name");

         if (name.length() > nameVariation.length()) {
            name += " " + nameVariation;
         } else {
            name = nameVariation;
         }
      }

      return name;
   }

   /**
    * Price "de"
    * 
    * @param jsonSku
    * @return
    */
   private Double crawlPriceFrom(JSONObject jsonSku) {
      Double priceFrom = null;

      if (jsonSku.has("listPriceFormated")) {
         Float price = MathUtils.parseFloatWithComma(jsonSku.get("listPriceFormated").toString());
         priceFrom = MathUtils.normalizeTwoDecimalPlaces(price.doubleValue());
      }

      return priceFrom;
   }

   private Float crawlMainPagePrice(Map<String, Float> marketplace) {
      Float price = null;

      if (marketplace.containsKey(MAIN_SELLER_NAME_LOWER)) {
         price = marketplace.get(MAIN_SELLER_NAME_LOWER);
      }

      return price;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("Images")) {
         JSONArray jsonArrayImages = json.getJSONArray("Images");

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            if (jsonImage.has("IsMain") && jsonImage.getBoolean("IsMain") && jsonImage.has("Path")) {
               primaryImage = changeImageSizeOnURL(jsonImage.getString("Path"));
               break;
            }
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONObject apiInfo) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (apiInfo.has("Images")) {
         JSONArray jsonArrayImages = apiInfo.getJSONArray("Images");

         for (int i = 0; i < jsonArrayImages.length(); i++) {
            JSONArray arrayImage = jsonArrayImages.getJSONArray(i);
            JSONObject jsonImage = arrayImage.getJSONObject(0);

            // jump primary image
            if (jsonImage.has("IsMain") && jsonImage.getBoolean("IsMain")) {
               continue;
            }

            if (jsonImage.has("Path")) {
               String urlImage = changeImageSizeOnURL(jsonImage.getString("Path"));
               secondaryImagesArray.put(urlImage);
            }

         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * Get the image url and change it size
    * 
    * @param url
    * @return
    */
   private String changeImageSizeOnURL(String url) {
      String[] tokens = url.trim().split("/");
      String dimensionImage = tokens[tokens.length - 2]; // to get dimension image and the image id

      String[] tokens2 = dimensionImage.split("-"); // to get the image-id
      String dimensionImageFinal = tokens2[0] + "-1000-1000";

      return url.replace(dimensionImage, dimensionImageFinal); // The image size is changed
   }

   private Map<String, Float> crawlMarketplace(JSONObject json) {
      Map<String, Float> marketplace = new HashMap<>();

      if (json.has("seller")) {
         String nameSeller = json.getString("seller").toLowerCase().trim();

         if (json.has("bestPriceFormated") && json.has("available") && json.getBoolean("available")) {
            Float price = MathUtils.parseFloatWithComma(json.getString("bestPriceFormated"));
            marketplace.put(nameSeller, price);
         }
      }

      return marketplace;
   }

   private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId, JSONObject jsonSku) {
      Marketplace marketplace = new Marketplace();

      for (String seller : marketplaceMap.keySet()) {
         if (!seller.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER)) {
            Float price = marketplaceMap.get(seller);

            JSONObject sellerJSON = new JSONObject();
            sellerJSON.put("name", seller);
            sellerJSON.put("price", price);
            sellerJSON.put("prices", crawlPrices(internalId, price, jsonSku).toJSON());

            try {
               Seller s = new Seller(sellerJSON);
               marketplace.add(s);
            } catch (Exception e) {
               Logging.printLogError(logger, session, Util.getStackTraceString(e));
            }
         }
      }

      return marketplace;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".bread-crumb li > a");

      for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element shortDescription = doc.selectFirst(".product_description");
      if (shortDescription != null) {
         description.append(shortDescription.html());
      }

      description.append(CrawlerUtils.scrapStandoutDescription("ikesaki", session, cookies, dataFetcher));

      return description.toString();
   }

   /**
    * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
    * à vista R$ 1.790,00
    * 
    * @param internalId
    * @param price
    * @return
    */
   private Prices crawlPrices(String internalId, Float price, JSONObject jsonSku) {
      Prices prices = new Prices();

      if (price != null) {
         String url = "https://www.ikesaki.com.br/productotherpaymentsystems/" + internalId;
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         prices.setPriceFrom(crawlPriceFrom(jsonSku));

         Element bank = doc.select("#ltlPrecoWrapper em").first();
         if (bank != null) {
            prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bank.text()));
         } else {
            prices.setBankTicketPrice(price);
         }

         Elements cardsElements = doc.select("#ddlCartao option");

         if (!cardsElements.isEmpty()) {
            for (Element e : cardsElements) {
               String text = e.text().toLowerCase();

               if (text.contains("visa")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

               } else if (text.contains("mastercard")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

               } else if (text.contains("diners")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

               } else if (text.contains("american") || text.contains("amex")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

               } else if (text.contains("hipercard") || text.contains("amex")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

               } else if (text.contains("credicard")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

               } else if (text.contains("elo")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"));
                  prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

               }
            }
         } else {
            Map<Integer, Float> installmentPriceMap = new HashMap<>();
            installmentPriceMap.put(1, price);

            prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
            prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
            prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
            prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
            prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
            prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         }
      }

      return prices;
   }

   private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard) {
      Map<Integer, Float> mapInstallments = new HashMap<>();

      Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
      for (Element i : installmentsCard) {
         Element installmentElement = i.select("td.parcelas").first();

         if (installmentElement != null) {
            String textInstallment = installmentElement.text().toLowerCase();
            Integer installment;

            if (textInstallment.contains("vista")) {
               installment = 1;
            } else {
               installment = Integer.parseInt(textInstallment.replaceAll("[^0-9]", "").trim());
            }

            Element valueElement = i.select("td:not(.parcelas)").first();

            if (valueElement != null) {
               Float value = Float.parseFloat(valueElement.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

               mapInstallments.put(installment, value);
            }
         }
      }

      return mapInstallments;
   }

   private JSONObject crawlApi(String internalId) {
      String url = "https://www.ikesaki.com.br/produto/sku/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray array = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (array.length() > 0) {
         return array.getJSONObject(0);
      }

      return new JSONObject();
   }


   private Integer crawlStock(JSONObject jsonProduct) {
      Integer stock = null;

      if (jsonProduct.has("SkuSellersInformation")) {
         JSONObject sku = jsonProduct.getJSONArray("SkuSellersInformation").getJSONObject(0);

         if (sku.has("AvailableQuantity")) {
            stock = sku.getInt("AvailableQuantity");
         }
      }

      return stock;
   }
}