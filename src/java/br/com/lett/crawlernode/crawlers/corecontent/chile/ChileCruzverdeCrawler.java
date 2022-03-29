package br.com.lett.crawlernode.crawlers.corecontent.chile;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChileCruzverdeCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Cruz Verde";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ChileCruzverdeCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://api.cruzverde.cl/customer-service/login")
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build();
      Response responseApi = dataFetcher.post(session, request);
      String cookie = responseApi.getHeaders().toString();

      String finalCookie = null;
      String regex = "sid=(.*); Path";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(cookie);

      if (matcher.find()) {
         finalCookie = matcher.group(1);
      }

      BasicClientCookie sidCookie = new BasicClientCookie("connect.sid", finalCookie);
      sidCookie.setDomain("api.cruzverde.cl");
      sidCookie.setPath("/");
      sidCookie.setValue(finalCookie);
      sidCookie.setSecure(true);
      sidCookie.setAttribute("HttpOnly", "true");
      this.cookies.add(sidCookie);

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
         boolean available = getAvaliability(productList);
         Offers offers = available ? getOffer(productList) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(brand + " " + name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      }

      return products;

   }

   private CategoryCollection getCategory(JSONObject productList) {
      CategoryCollection categories = new CategoryCollection();
      Object objCategory = productList.query("/productData/category");
      categories.add(objCategory.toString());
      return categories;
   }

   private boolean getAvaliability(JSONObject productList) {
      Object objDescription = productList.query("/productData/stock");
      return objDescription != null;
   }

   private String getDescription(JSONObject productList) {
      Object objDescription = productList.optQuery("/productData/pageDescription");
      if (objDescription == null){
         return null;
      }
      return objDescription.toString();
   }

   private List<String> getSecondaryImage(JSONObject productList, String primaryImage) {
      List<String> imgFormat = new ArrayList<>();
      JSONArray arrayImg = (JSONArray) productList.optQuery("/productData/imageGroups/0/images");
      if (arrayImg == null){
         return null;
      }
      for (Object obj : arrayImg){
         JSONObject objJson = (JSONObject) obj;
         String urlImg = objJson.optString("link");
         if (!urlImg.equals(primaryImage)){
            imgFormat.add(urlImg);
         }
      }
      return imgFormat;
   }

   private String getPrimaryImage(JSONObject productList) {
      Object objImg = productList.optQuery("/productData/imageGroups/0/images/0/link");
      return objImg.toString();
   }

   private String getName(JSONObject productList) {
      Object objName = productList.optQuery("/productData/name");
      return objName.toString();
   }

   private String getBrand(JSONObject productList) {
      Object objBrand = productList.optQuery("/productData/brand");
      return objBrand.toString();
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
      int storeId = 1121;
      String url = "https://api.cruzverde.cl/product-service/products/detail/" + internalId + "?inventoryId=" + storeId;
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .mustSendContentEncoding(true)
         .setCookies(this.cookies)
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY, ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY, ProxyCollection.BUY_HAPROXY))
         .build();
      Response response = this.dataFetcher.get(session, request);
      if (response != null) {
         obj = CrawlerUtils.stringToJson(response.getBody());
      }

      return obj;
   }

   private Offers getOffer(JSONObject productList) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = getPrice(productList);
      List<String> sales = new ArrayList<>();

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
      NumberFormat myFormat = NumberFormat.getInstance();
      Double price = Double.parseDouble(myFormat.format((productList.optQuery("/productData/price"))));
      Object holder = productList.optQuery("/productData/prices/price-sale-cl");
      if (holder == null) {
         holder = productList.optQuery("/productData/price");
      }

      Double priceFrom = Double.parseDouble(myFormat.format((holder)));

      CreditCards creditCards = getCreditCards(price);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();


   }

   private CreditCards getCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(price)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
}
