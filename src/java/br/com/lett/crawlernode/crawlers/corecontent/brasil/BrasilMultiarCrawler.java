package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.YourreviewsRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.*;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/*********************************************************************************************************************************************
 * Crawling notes (12/07/2016):
 *
 * 1) For this crawler, we have one url per each sku 2) There is no stock information for skus in
 * this ecommerce by the time this crawler was made 3) There is no marketplace in this ecommerce by
 * the time this crawler was made 4) The sku page identification is done simply looking for an
 * specific html element 5) if the sku is unavailable, it's price is not displayed. 6) There is no
 * internalPid for skus in this ecommerce. The internalPid must be a number that is the same for all
 * the variations of a given sku 7) For the availability we crawl a script in the html. A script
 * that has a variable named skuJson_0. It's been a common script, that contains a jsonObject with
 * certain informations about the sku. It's used only when the information needed is too complicated
 * to be crawled by normal means, or inexistent in other place. Although this json has other
 * informations about the sku, only the availability is crawled this way in this website. 8) All the
 * images are .png 9) We have one method for each type of information for a sku (please carry on
 * with this pattern).
 *
 * Examples: ex1 (available):
 * http://www.multiar.com.br/condicionador-de-ar-split-consul-frio-9-000-btu-h-2001105/p ex2
 * (unavailable):
 * http://www.multiar.com.br/ar-condicionado-split-hi-wall-inverter-daikin-advance-18000-btus-frio-220v-ftk18p5vl-rk18p5vl/p
 *
 **********************************************************************************************************************************************/

public class BrasilMultiarCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.multiar.com.br/";
   private static final String SELLER_NAME = "leveros";

   public BrasilMultiarCrawler(Session session) {
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

         // sku data in json
         JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(jsonSku);
            String description = crawlDescription(internalId);
            String name = crawlName(jsonSku, skuJson);
            Map<String, Float> marketplaceMap = crawlMarketplace(jsonSku);
            Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap, internalId, jsonSku, doc);
            boolean available = marketplaceMap.containsKey(SELLER_NAME);
            Float price = crawlMainPagePrice(marketplaceMap);

            JSONObject jsonProduct = crawlApi(internalId);
            String primaryImage = crawlPrimaryImage(jsonProduct);
            String secondaryImages = crawlSecondaryImages(jsonProduct);

            Prices prices = crawlPrices(internalId, price, jsonSku, doc);
            Integer stock = crawlStock(jsonProduct);
            RatingsReviews ratingReviews = crawRating(internalPid);

            // Creating the product
            Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(marketplace).setRatingReviews(ratingReviews).build();

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

   private boolean isProductPage(Document document) {
      return document.select("#___rc-p-id").first() != null;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = Integer.toString(json.getInt("sku")).trim();
      }

      return internalId;
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

      if (marketplace.containsKey(SELLER_NAME)) {
         price = marketplace.get(SELLER_NAME);
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

   private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap, String internalId, JSONObject jsonSku, Document doc) {
      Marketplace marketplace = new Marketplace();

      for (String seller : marketplaceMap.keySet()) {
         if (!seller.equalsIgnoreCase(SELLER_NAME)) {
            Float price = marketplaceMap.get(seller);

            JSONObject sellerJSON = new JSONObject();
            sellerJSON.put("name", seller);
            sellerJSON.put("price", price);
            sellerJSON.put("prices", crawlPrices(internalId, price, jsonSku, doc).toJSON());

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

   private JSONObject getJsonDescription(String internalId) {
      String url = "https://www.leverosintegra.com.br/map/api/GetItemData/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).build();

      String content = this.dataFetcher.get(session, request).getBody();

      return CrawlerUtils.stringToJson(content);
   }

   private String crawlDescription(String internalId) {

      JSONObject jsonObject = getJsonDescription(internalId);


      return jsonObject.optString("ProductDescription");
   }

   /**
    * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
    * à vista R$ 1.790,00
    *
    * @param internalId
    * @param price
    * @return
    */
   private Prices crawlPrices(String internalId, Float price, JSONObject jsonSku, Document docPrincipal) {
      Prices prices = new Prices();

      if (price != null) {
         String url = "http://www.multiar.com.br/productotherpaymentsystems/" + internalId;
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         prices.setPriceFrom(crawlPriceFrom(jsonSku));

         Elements flags = docPrincipal.select("#product-info .flag");
         Integer discountBoleto = 0;
         Integer cardDiscount = 0;

         for (Element e : flags) {
            String classFlag = e.attr("class");

            if (classFlag.contains("boleto")) {
               String text = e.ownText().replaceAll("[^0-9]", "").trim();

               if (!text.isEmpty()) {
                  discountBoleto = Integer.parseInt(text);
               }
            } else if (classFlag.contains("credito")) {
               String text = e.ownText().replaceAll("[^0-9]", "").trim();

               if (!text.isEmpty()) {
                  cardDiscount = Integer.parseInt(text);
               }
            }
         }

         if (discountBoleto > 0) {
            prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price - (price * (discountBoleto / 100.0))));
         } else {
            prices.setBankTicketPrice(price);
         }

         Elements cardsElements = doc.select("#ddlCartao option");

         if (!cardsElements.isEmpty()) {
            for (Element e : cardsElements) {
               String text = e.text().toLowerCase();

               if (text.contains("visa")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);

               } else if (text.contains("mastercard")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

               } else if (text.contains("diners")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);

               } else if (text.contains("american") || text.contains("amex")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

               } else if (text.contains("hipercard") || text.contains("amex")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

               } else if (text.contains("credicard")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.CREDICARD.toString(), installmentPriceMap);

               } else if (text.contains("elo")) {
                  Map<Integer, Float> installmentPriceMap = getInstallmentsForCard(doc, e.attr("value"), cardDiscount);
                  prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);

               }
            }
         } else {
            Map<Integer, Float> installmentPriceMap = new HashMap<>();

            if (cardDiscount > 0) {
               installmentPriceMap.put(1, MathUtils.normalizeTwoDecimalPlaces(price - (price * (cardDiscount / 100f))));
            } else {
               installmentPriceMap.put(1, price);
            }

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

   private Map<Integer, Float> getInstallmentsForCard(Document doc, String idCard, Integer discount) {
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

               if (discount != null && installment == 1) {
                  value = value - (value * (discount / 100f));
               }
               mapInstallments.put(installment, value);
            }
         }
      }

      return mapInstallments;
   }

   private JSONObject crawlApi(String internalId) {
      String url = "http://www.multiar.com.br/produto/sku/" + internalId;

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(this.dataFetcher.get(session, request).getBody());

      if (jsonArray.length() > 0) {
         return jsonArray.getJSONObject(0);
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

   private RatingsReviews crawRating(String internalPid) {
      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, "d8a04d6d-5542-4f32-ab89-52cc2ef8c643", this.dataFetcher);
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "d8a04d6d-5542-4f32-ab89-52cc2ef8c643", dataFetcher);
      Integer totalNumOfEvaluations = getTotalNumOfRatingsFromYourViews(docRating);
      Double avgRating = yourReviews.getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValue(docRating, internalPid);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   public Integer getTotalNumOfRatingsFromYourViews(Document doc) {
      Integer totalRating = 0;
      Element totalRatingElement = doc.select("span[itemprop=ratingCount]").first();

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }

   public AdvancedRatingReview getTotalStarsFromEachValue(Document doc, String internalPid) {
      YourreviewsRatingCrawler yourReviews = new YourreviewsRatingCrawler(session, cookies, logger, "d8a04d6d-5542-4f32-ab89-52cc2ef8c643", this.dataFetcher);
      Document docRating;
      Integer currentPage = 1;

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      do {
         currentPage++;
         docRating = yourReviews.crawlAllPagesRatingsFromYourViews(internalPid, "d8a04d6d-5542-4f32-ab89-52cc2ef8c643", dataFetcher, currentPage);
         Elements reviews = doc.select(".yv-col-md-9");
         for (Element element : reviews) {
            Elements stars = element.select(".fa-star");

            if (stars.size() == 1) {
               star1++;
            }

            if (stars.size() == 2) {
               star2++;
            }

            if (stars.size() == 3) {
               star3++;
            }

            if (stars.size() == 4) {
               star4++;
            }

            if (stars.size() == 5) {
               star5++;
            }

         }
      } while (hasNextPage(docRating, currentPage));
      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }

   private boolean hasNextPage(Document docRating, Integer currentPage) {
      boolean hasNextPage = false;

      Elements pages = docRating.select(".yv-paging.yv-hasresults:not(:last-child)");

      if (!pages.isEmpty() && !pages.get(pages.size() - 1).text().trim().equals(currentPage.toString())) {
         hasNextPage = true;
      }

      return hasNextPage;
   }

}
