package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.*;

import br.com.lett.crawlernode.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import models.Marketplace;
import models.prices.Prices;

import javax.swing.plaf.synth.SynthOptionPaneUI;

public class ColombiaMerqueoCrawler extends Crawler {

   public ColombiaMerqueoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject apiJson = scrapApiJson(session.getOriginalURL());
      JSONObject data = new JSONObject();

      if (apiJson.has("data") && !apiJson.isNull("data")) {
         data = apiJson.getJSONObject("data");
      }

      if (isProductPage(data)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(data);
         String name = crawlName(data);
         Float price = crawlPrice(data);
         boolean available = crawlAvailable(data);

         CategoryCollection categories = crawlCategories(data);
         Prices prices = crawlPrices(price);
         String primaryImage = crawlPrimaryImage(data);
         String secondaryImages = crawlSecondaryImage(data);
         String description = crawlDescription(data);
         Integer stock = crawlStock(data);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
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
            .setStock(stock)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Integer crawlStock(JSONObject data) {
      Integer stock = null;

      if (data.has("quantity")) {
         stock = MathUtils.parseInt(data.get("quantity").toString());
      }

      return stock;
   }

   private CategoryCollection crawlCategories(JSONObject data) {
      CategoryCollection categories = new CategoryCollection();
      JSONObject shelf = new JSONObject();

      if (data.has("shelf") && !data.isNull("shelf")) {
         shelf = data.getJSONObject("shelf");
         if (shelf.has("name") && !shelf.isNull("name")) {
            categories.add(shelf.getString("name"));
         }
      }

      if (data.has("department") && !data.isNull("department")) {
         shelf = data.getJSONObject("department");
         if (shelf.has("name") && !shelf.isNull("name")) {
            categories.add(shelf.getString("name"));
         }
      }

      return categories;
   }

   private String crawlDescription(JSONObject data) {
      String description = null;

      if (data.has("description") && !data.isNull("description")) {
         description = data.getString("description");
      }

      return description;
   }

   private String crawlPrimaryImage(JSONObject data) {
      String primaryImage = null;

      JSONArray jsonArrImg = JSONUtils.getJSONArrayValue(data, "images");
      if (jsonArrImg.length() > 0) {
         primaryImage = getLargestImage(jsonArrImg.get(0) instanceof JSONObject ? jsonArrImg.getJSONObject(0) : new JSONObject());
      } else {
         primaryImage = getLargestImage(data);
      }

      return primaryImage;
   }

   private String crawlSecondaryImage(JSONObject data) {
      String secondaryImagesString = null;
      JSONArray jsonArrImg = JSONUtils.getJSONArrayValue(data, "images");

      JSONArray secondaryImages = new JSONArray();

      for (int i = 1; i < jsonArrImg.length(); i++) {
         JSONObject jsonObjImg = jsonArrImg.get(i) instanceof JSONObject ? jsonArrImg.getJSONObject(i) : new JSONObject();
         secondaryImages.put(getLargestImage(jsonObjImg));
      }

      if (secondaryImages.length() > 0) {
         secondaryImagesString = secondaryImages.toString();
      }

      return secondaryImagesString;
   }

   private String getLargestImage(JSONObject jsonObjImg) {
      String image = null;

      if (jsonObjImg.has("imageLargeUrl") && !jsonObjImg.isNull("imageLargeUrl")) {
         image = jsonObjImg.getString("imageLargeUrl");

      } else if (jsonObjImg.has("imageMediumUrl") && !jsonObjImg.isNull("imageMediumUrl")) {
         image = jsonObjImg.getString("imageMediumUrl");

      } else if (jsonObjImg.has("imageSmallUrl") && !jsonObjImg.isNull("imageSmallUrl")) {
         image = jsonObjImg.getString("imageSmallUrl");
      }
      return image;
   }

   private boolean crawlAvailable(JSONObject data) {
      boolean availability = false;

      if (data.has("availability") && !data.isNull("availability")) {
         availability = data.getBoolean("availability");
      }

      return availability;
   }

   private Float crawlPrice(JSONObject data) {
      Float price = null;
      if (data.has("specialPrice") && !data.isNull("specialPrice")) {
         price = CrawlerUtils.getFloatValueFromJSON(data, "specialPrice");
      } else if (data.has("price") && !data.isNull("price")) {
         price = CrawlerUtils.getFloatValueFromJSON(data, "price");
      }

      return price;
   }

   private String crawlName(JSONObject data) {
      StringBuilder name = new StringBuilder();

      if (data.has("name") && !data.isNull("name")) {
         name.append(data.getString("name") + " ");
      }
      if (data.has("quantity") && !data.isNull("quantity")) {
         name.append(data.getString("quantity") + " ");
      }
      if (data.has("unit") && !data.isNull("unit")) {
         name.append(data.getString("unit"));
      }

      return name.toString();
   }

   private JSONObject scrapApiJson(String originalURL) {
      List<String> slugs = scrapSlugs(originalURL);

      StringBuilder apiUrl = new StringBuilder();
      apiUrl.append("https://merqueo.com/api/2.0/stores/63/find?");

      if(slugs.size() == 3) {
         apiUrl.append("department_slug=").append(slugs.get(0));
         apiUrl.append("&shelf_slug=").append(slugs.get(1));
         apiUrl.append("&product_slug=").append(slugs.get(2));
      } else {
         apiUrl.append("department_slug=").append(slugs.get(1));
         apiUrl.append("&shelf_slug=").append(slugs.get(2));
         apiUrl.append("&product_slug=").append(slugs.get(3));
      }

      apiUrl.append("&limit=7&zoneId=32&adq=1");

      Request request = RequestBuilder
         .create()
         .setUrl(apiUrl.toString())
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
   }

   /*
    * Url exemple:
    * https://merqueo.com/bogota/aseo-del-hogar/detergentes/ariel-concentrado-doble-poder-detergente-la
    * -quido-2-lt
    */
   private List<String> scrapSlugs(String originalURL) {
      List<String> slugs = new ArrayList<>();
      String slugString = CommonMethods.getLast(originalURL.split("bogota/"));
      String[] slug = slugString.contains("/")?slugString.split("/"):null;

      if(slug != null) {
         Collections.addAll(slugs, slug);
      }
      return slugs;
   }

   private boolean isProductPage(JSONObject data) {
      return data.has("id");
   }

   private String crawlInternalId(JSONObject data) {
      String internalId = null;

      if (data.has("id") && !data.isNull("id")) {
         internalId = data.get("id").toString();
      }

      return internalId;
   }

   /**
    * In the time when this crawler was made, this market hasn't installments informations
    *
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
         installmentPriceMapShop.put(1, price);
         prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMapShop);
      }

      return prices;
   }
}
