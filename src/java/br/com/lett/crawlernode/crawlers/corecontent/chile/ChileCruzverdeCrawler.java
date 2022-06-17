package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.commons.lang.WordUtils;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChileCruzverdeCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Cruz Verde";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ChileCruzverdeCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   private final int STORE_ID = session.getOptions().optInt("store_id", 1121);

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("sec-fetch-mode", "cors");
      headers.put("origin", "https://cruzverde.cl");
      headers.put("sec-fetch-site", "same-origin");
      headers.put("x-requested-with", "XMLHttpRequest");
      headers.put("accept", "application/json, text/javascript, */*; q=0.01");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.cruzverde.cl/customer-service/login")
         .setSendUserAgent(true)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_ES,
            ProxyCollection.NETNUT_RESIDENTIAL_BR
         ))
         .build();
      Response responseApi = this.dataFetcher.post(session, request);

      int tries = 0;
      while(!responseApi.isSuccess() && tries < 3) {
         tries++;
         responseApi = new JsoupDataFetcher().post(session, request);
      }

      this.cookies.addAll(responseApi.getCookies());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      String internalId = getId();
      JSONObject productList = getProductList(internalId);

      if (productList != null && !productList.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());
         String brand = getBrand(productList);
         String name = getName(productList);
         CategoryCollection categories = getCategory(productList);
         String primaryImage = getPrimaryImage(productList);
         List<String> secondaryImage = getSecondaryImage(productList, primaryImage);
         String description = getDescription(productList);
         int stock = JSONUtils.getValueRecursive(productList, "productData.stock", Integer.class, 0);
         boolean available = stock > 0;
         Offers offers = available ? getOffer(productList) : new Offers();

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

      String regex = "/(\\d+).html";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         id = matcher.group(1);
      }
      return id;
   }


   private JSONObject getProductList(String internalId) {
      JSONObject obj = null;
      String url = "https://api.cruzverde.cl/product-service/products/detail/" + internalId + "?inventoryId=" + STORE_ID;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setCookies(this.cookies)
         .build();

      int tries = 0;
      Response response = this.dataFetcher.get(session, request);

      while(!response.isSuccess() && tries < 3) {
         tries++;
         response = this.dataFetcher.get(session, request);
      }

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
