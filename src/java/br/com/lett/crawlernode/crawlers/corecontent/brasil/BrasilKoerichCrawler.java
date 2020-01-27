package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
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

/************************************************************************************************************************************************************************************
 * Crawling notes (27/06/2019):
 * 
 * Examples: ex1 (available):
 * http://www.koerich.com.br/micro-ondas-panasonic-32-litros-style-nn-st654wruk-branco-3305600/p ex2
 * (unavailable):
 * http://www.koerich.com.br/furadeira-black-decker-de-impacto-3-8--500w-1-velocidade-tm500-b52-laranja-3332500/p
 *
 *
 ************************************************************************************************************************************************************************************/

public class BrasilKoerichCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.koerich.com.br/";

   public BrasilKoerichCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   protected JSONObject fetch() {
      JSONObject api = new JSONObject();

      String[] tokens = session.getOriginalURL().split("\\?")[0].split("/");
      String id = CommonMethods.getLast(tokens);
      String pathName = tokens[tokens.length - 2];

      String apiUrl =
            "https://www.koerich.com.br/ccstoreui/v1/pages/p/" + pathName + "/" + id + "?dataOnly=false&cacheableDataOnly=true&productTypesRequired=true";

      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
      JSONObject response = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data")) {
         api = response.getJSONObject("data");
      }

      return api;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(JSONObject pageJson) throws Exception {
      super.extractInformation(pageJson);
      List<Product> products = new ArrayList<>();

      if (pageJson.has("page")) {
         JSONObject page = pageJson.getJSONObject("page");

         if (page.has("product")) {
            JSONObject json = page.getJSONObject("product");
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            String internalPid = crawlInternalPid(json);
            CategoryCollection categories = new CategoryCollection();
            String primaryImage = crawlPrimaryImage(json);
            String secondaryImages = crawlSecondaryImages(json, primaryImage);

            JSONArray arraySkus = json != null && json.has("childSKUs") ? json.getJSONArray("childSKUs") : new JSONArray();

            for (int i = 0; i < arraySkus.length(); i++) {
               JSONObject jsonSku = arraySkus.getJSONObject(i);
               String internalId = crawlInternalId(jsonSku);
               String name = crawlName(jsonSku);
               Float price = CrawlerUtils.getFloatValueFromJSON(jsonSku, "salePrice");
               Prices prices = crawlPrices(price, jsonSku, pageJson);
               Integer stock = crawlStock(internalId, internalPid);
               boolean available = stock != null && stock > 0;
               String description = crawlDescription(json, jsonSku);
               RatingsReviews ratingReviews = crawlRating(page, internalId);
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
                     .setMarketplace(new Marketplace())
                     .setRatingReviews(ratingReviews)
                     .build();

               products.add(product);
            }
         } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
         }
      }

      return products;

   }

   private RatingsReviews crawlRating(JSONObject page, String internalId) {

      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      JSONObject json = page.getJSONObject("product");
      YourreviewsRatingCrawler yourReviews =
            new YourreviewsRatingCrawler(session, cookies, logger, "d3587a80-eb86-47a6-ac89-8de3f703770c", this.dataFetcher);
      String internalPid = crawlInternalPid(json);

      Document docRating = yourReviews.crawlPageRatingsFromYourViews(internalPid, "d3587a80-eb86-47a6-ac89-8de3f703770c", this.dataFetcher);
      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(docRating, ".yv-col-md-4 span strong", false, 0);
      Double avgRating = getTotalAvgRatingFromYourViews(docRating);
      AdvancedRatingReview advancedRatingReview = yourReviews.getTotalStarsFromEachValue(internalPid);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setInternalId(internalId);

      return ratingReviews;
   }

   private Double getTotalAvgRatingFromYourViews(Document docRating) {
      Double avgRating = 0d;
      Element rating = docRating.selectFirst("meta[itemprop=ratingValue]");

      if (rating != null) {
         avgRating = Double.parseDouble(rating.attr("content"));
      }

      return avgRating;
   }



   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("repositoryId")) {
         internalId = json.get("repositoryId").toString();
      }

      return internalId;
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("id")) {
         internalPid = json.get("id").toString();
      }

      return internalPid;
   }


   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("displayName")) {
         name = json.getString("displayName");
      }

      return name;
   }

   private Integer crawlStock(String internalId, String internalPid) {
      Integer stock = 0;

      String url = "https://www.koerich.com.br/ccstoreui/v1/stockStatus/" + internalPid + "?skuId=" + internalId + "&catalogId=";
      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONObject stockJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (stockJson.has("inStockQuantity") && stockJson.get("inStockQuantity") instanceof Integer) {
         stock = stockJson.getInt("inStockQuantity");
      } else if (stockJson.has("orderableQuantity") && stockJson.get("orderableQuantity") instanceof Integer) {
         stock = stockJson.getInt("orderableQuantity");
      }

      return stock;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("primaryFullImageURL") && !json.get("primaryFullImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryFullImageURL").toString(), "https:", "www.koerich.com.br");
      } else if (json.has("primaryLargeImageURL") && !json.get("primaryLargeImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryLargeImageURL").toString(), "https:", "www.koerich.com.br");
      } else if (json.has("primaryMediumImageURL") && !json.get("primaryMediumImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryMediumImageURL").toString(), "https:", "www.koerich.com.br");
      } else if (json.has("primarySmallImageURL") && !json.get("primarySmallImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primarySmallImageURL").toString(), "https:", "www.koerich.com.br");
      } else if (json.has("primaryThumbImageURL") && !json.get("primaryThumbImageURL").toString().equalsIgnoreCase("null")) {
         primaryImage = CrawlerUtils.completeUrl(json.get("primaryThumbImageURL").toString(), "https:", "www.koerich.com.br");
      }

      return primaryImage;
   }

   /**
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(JSONObject json, String primaryImage) {
      String secondaryImages = null;

      JSONArray secondaryImagesArray = new JSONArray();
      JSONArray images = new JSONArray();

      if (verifyImagesArray(json, "fullImageURLs")) {
         images = json.getJSONArray("fullImageURLs");
      } else if (verifyImagesArray(json, "largeImageURLs")) {
         images = json.getJSONArray("largeImageURLs");
      } else if (verifyImagesArray(json, "mediumImageURLs")) {
         images = json.getJSONArray("mediumImageURLs");
      } else if (verifyImagesArray(json, "smallImageURLs")) {
         images = json.getJSONArray("smallImageURLs");
      } else if (verifyImagesArray(json, "thumbImageURLs")) {
         images = json.getJSONArray("thumbImageURLs");
      }

      for (Object o : images) {
         String image = CrawlerUtils.completeUrl(o.toString(), "https:", "www.koerich.com.br");

         if (!image.equalsIgnoreCase(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private boolean verifyImagesArray(JSONObject json, String key) {
      if (json.has(key) && json.get(key) instanceof JSONArray) {
         JSONArray array = json.getJSONArray(key);

         if (array.length() > 0) {
            return true;
         }
      }

      return false;
   }

   private String crawlDescription(JSONObject json, JSONObject jsonSku) {
      StringBuilder description = new StringBuilder();

      if (json.has("longDescription")) {
         description.append(json.get("longDescription").toString());
      }

      description.append("<table>");
      Set<String> keys = jsonSku.keySet();
      for (String key : keys) {
         if ((key.startsWith("1_") || key.startsWith("x_")) && !key.equalsIgnoreCase("1_informaesAdicionais") && jsonSku.get(key) instanceof String) {
            description.append("<tr>");
            description.append("<td>")
                  .append(CommonMethods.upperCaseFirstCharacter(CommonMethods.splitStringWithUpperCase(key.replace("1_", "").replace("x_", ""))))
                  .append("</td>");

            description.append("<td>").append(jsonSku.get(key)).append("</td>");
            description.append("</tr>");
         }
      }
      description.append("</table>");

      if (jsonSku.has("1_informaesAdicionais")) {
         description.append("<h4> Informações Adicionais: </h4>\n ").append(jsonSku.get("1_informaesAdicionais").toString());
      }

      return description.toString();
   }

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, JSONObject jsonSku, JSONObject pageJson) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         Map<Integer, Float> installmentShopCardPriceMap = new TreeMap<>();
         installmentShopCardPriceMap.put(1, price);

         prices.setPriceFrom(CrawlerUtils.getDoubleValueFromJSON(jsonSku, "listPrice", true, null));
         prices.setBankTicketPrice(price);

         if (pageJson.has("global")) {
            JSONObject global = pageJson.getJSONObject("global");

            if (global.has("site")) {
               JSONObject site = global.getJSONObject("site");

               if (site.has("extensionSiteSettings")) {
                  JSONObject extensionSiteSettings = site.getJSONObject("extensionSiteSettings");

                  if (extensionSiteSettings.has("boletoSettings")) {
                     JSONObject discountSettings = extensionSiteSettings.getJSONObject("boletoSettings");

                     if (discountSettings.has("descontoBoleto")) {
                        String text = discountSettings.get("descontoBoleto").toString().replaceAll("[^0-9]", "").trim();

                        if (!text.isEmpty()) {
                           prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(price - (price * (Integer.parseInt(text) / 100d))));
                        }
                     }
                  }

                  if (extensionSiteSettings.has("installmentSettings")) {
                     JSONObject customSiteSettings = extensionSiteSettings.getJSONObject("installmentSettings");

                     setParcels(customSiteSettings, "maxInstallmentParcel", "minInstallmentValue", installmentPriceMap, price);
                     setParcels(customSiteSettings, "maxInstallmentParcelKoerich", "minInstallmentValueKoerich", installmentShopCardPriceMap, price);
                  }
               }
            }
         }

         prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentShopCardPriceMap);
         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
      }

      return prices;
   }

   private void setParcels(JSONObject customSiteSettings, String keyParcel, String keyValue, Map<Integer, Float> installmentPriceMap, Float price) {
      int maxNumInstallment = CrawlerUtils.getIntegerValueFromJSON(customSiteSettings, keyParcel, 1);
      Double minValuePerInstallment = CrawlerUtils.getDoubleValueFromJSON(customSiteSettings, keyValue, true, false);

      if (minValuePerInstallment != null) {
         for (int i = 1; i <= maxNumInstallment; i++) {
            Float priceInstallment = MathUtils.normalizeTwoDecimalPlaces(price / i);

            if (priceInstallment >= minValuePerInstallment) {
               installmentPriceMap.put(i, priceInstallment);
            } else {
               break;
            }
         }
      }
   }
}
