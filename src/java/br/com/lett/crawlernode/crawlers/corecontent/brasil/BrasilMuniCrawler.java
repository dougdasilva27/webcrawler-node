package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.commons.lang.WordUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilMuniCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Muni";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());
   public BrasilMuniCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }
   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
      headers.put("origin", "https://cruzverde.cl");
      headers.put("authority", "api.cruzverde.cl");
      headers.put("referer", "https://cruzverde.cl/");
      headers.put("accept", "application/json, text/plain, */*");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");

      return headers;
   }
   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      String internalId = getId();
      JSONObject productData = getProductList(internalId);

      if (productData != null && !productData.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
         String brand = getBrand(productData);
         String name = getName(productData);
         CategoryCollection categories = getCategory(productData);
         String primaryImage = getPrimaryImage(productData);
         List<String> secondaryImage = getSecondaryImage(productData, primaryImage);
         String description = getDescription(productData);
         int stock = JSONUtils.getValueRecursive(productData, "productData.stock", Integer.class, 0);
         boolean available = stock > 0;
         Offers offers = available ? getOffer(productData) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(brand + " " + name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }
   private CategoryCollection getCategory(JSONObject productList) {
      CategoryCollection categories = new CategoryCollection();
      Object objCategory = productList.optQuery("/productData/category");
      if (objCategory instanceof String && !((String) objCategory).isEmpty()) {
         String category = WordUtils.capitalize(objCategory.toString().replace("-", " "));
         categories.add(category);
      }
      return categories;
   }

   private String getDescription(JSONObject productList) {
      StringBuilder description = new StringBuilder();

      Object objDescription = productList.optQuery("/productData/pageDescription");
      if (objDescription instanceof String && !objDescription.toString().isEmpty()) {
         description.append(objDescription.toString()).append("<br>");
      }

      JSONArray tabs = JSONUtils.getValueRecursive(productList, "productData.tabs", JSONArray.class, new JSONArray());

      for (Object tab : tabs) {
         JSONObject tabObj = (JSONObject) tab;
         String title = tabObj.optString("title");
         String content = tabObj.optString("content");
         if (title != null && !title.isEmpty() && content != null && !content.isEmpty()) {
            description.append("<h2>").append(title).append("</h2>").append("<p>").append(content).append("</p>");
         }
      }

      return description.toString();
   }

   private List<String> getSecondaryImage(JSONObject productList, String primaryImage) {
      List<String> imgFormat = new ArrayList<>();
      JSONArray arrayImg = (JSONArray) productList.optQuery("/productData/imageGroups/0/images");
      if (arrayImg == null) {
         return null;
      }
      for (Object obj : arrayImg) {
         if (obj instanceof JSONObject) {
            JSONObject objJson = (JSONObject) obj;
            String urlImg = objJson.optString("link");
            if (!urlImg.equals(primaryImage)) {
               imgFormat.add(urlImg);
            }
         }
      }
      return imgFormat;
   }

   private String getPrimaryImage(JSONObject productList) {
      String primaryImg = "";
      Object objImg = productList.optQuery("/productData/imageGroups/0/images/0/link");
      if (objImg != null) {
         primaryImg = objImg.toString();
      }
      return primaryImg;
   }

   private String getName(JSONObject productList) {
      String name = "";
      Object objName = productList.optQuery("/productData/name");
      if (objName != null) {
         name = objName.toString();
      }
      return name;
   }

   private String getBrand(JSONObject productList) {
      String brand = "";
      Object objBrand = productList.optQuery("/productData/brand");
      if (objBrand != null) {
         brand = objBrand.toString();
      }
      return brand;
   }

   private String getId() {
      String id = null;
      String regex = "mp/(.*)/";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }


   private JSONObject getProductList(String internalId) {
      JSONObject obj = null;
      String url = "https://api.cruzverde.cl/product-service/products/detail/" + internalId + "?inventoryId=";
      Map<String, String> headers = getHeaders();
      headers.put("cookie", CommonMethods.cookiesToString(this.cookies));

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setHeaders(headers)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);
      ;

      return CrawlerUtils.stringToJson(response.getBody());
   }

   private Offers getOffer(JSONObject productList) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = getPrice(productList);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing getPrice(JSONObject productList) throws MalformedPricingException {
      Object priceObj = productList.optQuery("/productData/prices");

      if (priceObj instanceof JSONObject) {
         JSONObject prices = (JSONObject) priceObj;
         Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(prices, "price-sale-cl", false);
         Double priceFrom = JSONUtils.getDoubleValueFromJSON(prices, "price-list-cl", false);

         if (spotlightPrice == null && priceFrom != null) {
            spotlightPrice = priceFrom;
         }

         if (Objects.equals(priceFrom, spotlightPrice)) {
            priceFrom = null;
         }

         CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();
      }

      throw new MalformedPricingException("No price found");
   }
}
