package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ChileRappijumboCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.rappi.cl/";
   private static final String DETAILS_API_URL = "https://services.rappi.cl/api/cpgs-graphql-domain/";
   private static final String IMAGES_DOMAIN = "images.rappi.cl/products";
   private static final String STORE = "76";

   public ChileRappijumboCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      return new Document("");
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      String productUrl = session.getOriginalURL();
      JSONObject jsonSku = crawlProductInformatioFromApi(productUrl, STORE);

      if (isProductPage(jsonSku)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         CategoryCollection categories = crawlCategories(jsonSku);
         String description = crawlDescription(jsonSku);
         boolean available = crawlAvailability(jsonSku);
         Float price = available ? CrawlerUtils.getFloatValueFromJSON(jsonSku, "price") : null;
         Double priceFrom = available ? CrawlerUtils.getDoubleValueFromJSON(jsonSku, "real_price", true, false) : null;
         String primaryImage = crawlPrimaryImage(jsonSku);
         String secondaryImages = crawlSecondaryImages(jsonSku, primaryImage);
         String ean = JSONUtils.getStringValue(jsonSku, "ean");
         List<String> eans = ean != null && !ean.isEmpty() ? Arrays.asList(ean) : null;
         String name = crawlName(jsonSku);

         Prices prices = crawlPrices(price, priceFrom);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(productUrl)
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
               .setEans(eans)
               .setMarketplace(new Marketplace())
               .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(JSONObject json) {
      return json.length() > 0;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("id")) {
         internalId = json.get("id").toString();
      }

      return internalId;
   }


   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("product_id")) {
         internalPid = json.getString("product_id");
      }

      return internalPid;
   }

   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("name")) {
         name = json.getString("name");
      }

      return name;
   }

   private boolean crawlAvailability(JSONObject json) {
      return json.has("is_available") && json.getBoolean("is_available");
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("image")) {
         primaryImage = "https://images.rappi.cl/products/" + json.get("image");
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(JSONObject json, String primaryImage) {
      JSONArray imagesArray = new JSONArray();

      if (json.has("store_id") && !json.isNull("store_id") && json.has("product_id") && !json.isNull("product_id")) {
         JSONArray jsonImagesArray = crawlProductImagesFromApi(json.get("product_id").toString(), json.get("store_id").toString());

         for (Object obj : jsonImagesArray) {
            if (obj instanceof JSONObject) {
               JSONObject imageObj = (JSONObject) obj;

               if (imageObj.has("name") && imageObj.get("name") instanceof String) {
                  String secondaryImage = CrawlerUtils.completeUrl(imageObj.getString("name"), "https", IMAGES_DOMAIN);

                  if (!secondaryImage.equals(primaryImage)) {
                     imagesArray.put(CrawlerUtils.completeUrl(imageObj.getString("name"), "https", IMAGES_DOMAIN));
                  }
               }
            }
         }
      }

      return imagesArray.toString();
   }

   private JSONArray crawlProductImagesFromApi(String productId, String storeId) {
      JSONArray productImages = new JSONArray();

      if (productId != null && storeId != null) {
         Map<String, String> headers = new HashMap<>();
         headers.put("Content-Type", "application/json");

         try {
            long productIdNumber = Long.parseLong(productId);
            long storeIdNumber = Long.parseLong(storeId);

            JSONObject payload = new JSONObject();
            payload.put("operationName", "fetchProductDetails");
            payload.put("variables", new JSONObject().put("productId", productIdNumber).put("storeId", storeIdNumber));
            payload.put("query", "query fetchProductDetails($productId: Int!, $storeId: Int!) " +
                  "{\n productDetail(productId: $productId, storeId: $storeId) {\n longDescription\n " +
                  "images {\n name\n }\n toppings {\n id\n description\n toppingTypeId\n " +
                  "minToppingsForCategories\n maxToppingsForCategories\n index\n presentationTypeId\n " +
                  "topping {\n id\n description\n toppingCategoryId\n price\n maxLimit\n index\n }\n " +
                  "}\n nutritionFactsImg {\n name\n }\n additionalInformation {\n attribute\n value\n " +
                  "}\n ingredients {\n name\n }\n }\n}\n");

            Request request = RequestBuilder.create()
                  .setUrl(DETAILS_API_URL)
                  .setHeaders(headers)
                  .mustSendContentEncoding(false)
                  .setPayload(payload.toString())
                  .build();

            String page = this.dataFetcher.post(session, request).getBody();

            if (page.startsWith("{") && page.endsWith("}")) {
               JSONObject apiResponse = new JSONObject(page);

               if (apiResponse.has("data") && apiResponse.get("data") instanceof JSONObject) {
                  apiResponse = apiResponse.getJSONObject("data");

                  if (apiResponse.has("productDetail") && apiResponse.get("productDetail") instanceof JSONObject) {
                     apiResponse = apiResponse.getJSONObject("productDetail");

                     if (apiResponse.has("images") && apiResponse.get("images") instanceof JSONArray) {
                        productImages = apiResponse.getJSONArray("images");
                     }
                  }
               }
            }
         } catch (Exception e) {
            Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
         }
      }


      return productImages;
   }


   private CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("categories")) {
         JSONArray shelfList = json.getJSONArray("categories");

         for (Object o : shelfList) {
            JSONObject cat = (JSONObject) o;

            if (cat.has("category_name")) {
               categories.add(cat.getString("category_name"));
            }
         }
      }

      return categories;
   }


   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("description") && json.get("description") instanceof String) {
         String desc = json.getString("description");

         // Because of this link: https://www.rappi.com.co/search?store_type=hiper&query=900187
         if (desc.replace(" ", "").contains("-PLU")) {
            String descFinal = desc.replace(CommonMethods.getLast(desc.split("-")), "").trim();
            description.append(descFinal.substring(0, descFinal.length() - 2).trim());
         } else {
            description.append(desc);
         }
      }


      return description.toString();
   }

   /**
    * In this site has no information of installments
    * 
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Double priceFrom) {
      Prices p = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();
         installmentPriceMap.put(1, price);

         p.setPriceFrom(priceFrom);
         p.setBankTicketPrice(price);

         p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
         p.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);

      }

      return p;
   }

   /**
    * - Get the json of api, this api has all info of product - Spected url like this
    * https://www.rappi.com.br/product/900020401_2092884955?store_type=carrefour
    * 
    * @return
    */

   private JSONObject crawlProductInformatioFromApi(String productUrl, String storeId) {
      JSONObject productsInfo = new JSONObject();
      String productId = null;

      if (productUrl.contains("_")) {
         String ids = productUrl.split("\\?")[0];
         productId = CommonMethods.getLast(ids.split("_")).replaceAll("[^0-9]", "");
      }

      if (productId != null && storeId != null) {
         Map<String, String> headers = new HashMap<>();

         String url = "https://services.rappi.cl/windu/products/store/" + storeId + "/product/" + productId + "?include_stock_out=true";
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).mustSendContentEncoding(false).build();

         String page = this.dataFetcher.get(session, request).getBody();
         if (page.startsWith("{") && page.endsWith("}")) {
            try {
               JSONObject apiResponse = new JSONObject(page);

               if (apiResponse.has("product") && apiResponse.get("product") instanceof JSONObject) {
                  productsInfo = apiResponse.getJSONObject("product");
               }

            } catch (Exception e) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
            }
         }
      }

      return productsInfo;
   }

}
