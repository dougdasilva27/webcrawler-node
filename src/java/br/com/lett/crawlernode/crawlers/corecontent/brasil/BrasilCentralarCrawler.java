package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
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
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;


public class BrasilCentralarCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.centralar.com.br/";

   public BrasilCentralarCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   protected JSONObject fetch() {
      JSONObject api = new JSONObject();

      String url = session.getOriginalURL();

      if (url.contains("/produto/")) {
         String slugName = url.split("/produto/")[1].split("/")[0].replace(".html", "");
         String apiUrl = "http://api-services.centralar.com.br/mds/rest/product/v1?productSlug=" + slugName;

         Map<String, String> headers = new HashMap<>();
         headers.put("Accept", "application/json, text/plain, */*");
         headers.put("Authorization", "123456");
         headers.put("Referer", url);

         Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).setHeaders(headers).build();
         String content = new FetcherDataFetcher().get(session, request).getBody();

         if (content == null || content.isEmpty()) {
            content = this.dataFetcher.get(session, request).getBody();
         }

         api = CrawlerUtils.stringToJson(content);
      }

      return api;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("code")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(json);
         String name = crawlName(json);
         boolean available = crawlAvailability(json);
         Float price = available ? crawlPrice(json) : null;
         Prices prices = crawlPrices(price, json);
         CategoryCollection categories = crawlCategories(json);
         String primaryImage = crawlPrimaryImage(json);
         String secondaryImages = crawlSecondaryImages(json);
         String description = crawlDescription(json);
         RatingsReviews ratingReviews = crawRating(internalId);
         // Creating the product
         Product product = ProductBuilder
               .create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(null)
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
               .setStock(null)
               .setMarketplace(new Marketplace())
               .setRatingReviews(ratingReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("code")) {
         internalId = json.getString("code");
      }

      return internalId;
   }

   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("name")) {
         name = json.getString("name");
      }

      return name;
   }

   private boolean crawlAvailability(JSONObject json) {
      if (json.has("prices")) {
         JSONObject prices = json.getJSONObject("prices");

         return prices.has("isAvailable") && prices.getBoolean("isAvailable");
      }

      return false;
   }

   private Float crawlPrice(JSONObject json) {
      Float price = null;

      if (json.has("prices")) {
         JSONObject prices = json.getJSONObject("prices");

         if (prices.has("priceToText")) {
            price = MathUtils.parseFloatWithComma(prices.getString("priceToText"));
         }
      }

      return price;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("images")) {
         JSONArray images = json.getJSONArray("images");

         if (images.length() > 0) {
            JSONObject imageJson = images.getJSONObject(0);

            if (imageJson.has("url")) {
               primaryImage = imageJson.getString("url");
            }
         }
      }

      return primaryImage;
   }

   /**
    * @param doc
    * @return
    */
   private String crawlSecondaryImages(JSONObject json) {
      String secondaryImages = null;

      if (json.has("images")) {
         JSONArray images = json.getJSONArray("images");
         JSONArray secondaryImagesArray = new JSONArray();

         for (Object o : images) {
            JSONObject imageJson = (JSONObject) o;

            if (imageJson.has("url")) {
               secondaryImagesArray.put(imageJson.getString("url"));
            }
         }

         if (secondaryImagesArray.length() > 0) {
            secondaryImages = secondaryImagesArray.toString();
         }
      }

      return secondaryImages;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();

      if (json.has("category")) {
         categories.add(json.getString("category"));
      }

      if (json.has("btus")) {
         String text = json.getString("btus");

         if (!text.equals("1")) {
            categories.add(text + "BTUS");
         }
      }

      return categories;
   }

   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("productInformations") && json.get("productInformations") instanceof JSONArray) {
         JSONArray productInformations = json.getJSONArray("productInformations");

         for (Object o : productInformations) {
            JSONObject descJson = (JSONObject) o;

            if (descJson.has("title")) {
               description.append("<h3>").append(descJson.get("title").toString()).append("</h3>");
            }

            if (descJson.has("value")) {
               description.append(descJson.get("value").toString());
            }
         }
      }

      if (json.has("specs") && json.get("specs") instanceof JSONArray) {
         JSONArray specs = json.getJSONArray("specs");

         if (specs.length() > 0) {
            description.append("<table>");

            for (Object o : specs) {
               JSONObject specJson = (JSONObject) o;
               description.append("<tr>");

               if (specJson.has("title")) {
                  description.append("<td>").append(specJson.get("title")).append("</td>");
               }

               if (specJson.has("value")) {
                  description.append("<td>").append(specJson.get("value")).append("</td>");
               }

               description.append("</tr>");
            }

            description.append("</table>");
         }
      }

      if (json.has("dimensionImages")) {
         JSONArray dimensionImages = json.getJSONArray("dimensionImages");
         for (Object o : dimensionImages) {
            JSONObject dimension = (JSONObject) o;

            if (dimension.has("url")) {
               Request request = RequestBuilder.create().setUrl(dimension.get("url").toString()).setCookies(cookies).build();
               description.append(this.dataFetcher.get(session, request).getBody());
            }
         }
      }

      return description.toString();
   }

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, JSONObject jsonSku) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();
         installmentPriceMap.put(1, price);

         if (jsonSku.has("prices")) {
            JSONObject pricesJson = jsonSku.getJSONObject("prices");

            if (pricesJson.has("priceBilletText")) {
               prices.setBankTicketPrice(MathUtils.parseDoubleWithComma(pricesJson.get("priceBilletText").toString()));
            }

            if (pricesJson.has("priceFromText")) {
               prices.setPriceFrom(MathUtils.parseDoubleWithComma(pricesJson.get("priceFromText").toString()));
            }
         }

         if (jsonSku.has("installmentsTexts")) {
            JSONArray installments = jsonSku.getJSONArray("installmentsTexts");

            for (int i = 0; i < installments.length(); i++) {
               String text = installments.get(i).toString().toLowerCase();

               if (text.contains("x")) {
                  int x = text.indexOf('x');

                  String installment = text.substring(0, x).replaceAll("[^0-9]", "").trim();
                  Float value = MathUtils.parseFloatWithComma(text.substring(x));

                  if (!installment.isEmpty() && value != null) {
                     installmentPriceMap.put(Integer.parseInt(installment), value);
                  }
               }
            }
         }

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      return prices;
   }

   private RatingsReviews crawRating(String internalId) {

      String url = session.getOriginalURL();
      Request request = RequestBuilder.create().setCookies(cookies).mustSendContentEncoding(false).setUrl(url).build();
      String response = this.dataFetcher.get(session, request).getBody();
      Document document = Jsoup.parse(response);

      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "79461", logger);
      return trustVox.extractRatingAndReviews(internalId, document, dataFetcher);

   }

}
