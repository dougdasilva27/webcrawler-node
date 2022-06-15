package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public abstract class RappiCrawler extends Crawler {

   public RappiCrawler(Session session) {
      super(session);
      this.config.setFetcher(FetchMode.APACHE);
   }

   protected String getStoreId() {
      return session.getOptions().optString("storeId");
   }

   private String getCurrentLocation() {
      return session.getOptions().optString("currentLocation");
   }

   abstract protected String getHomeDomain();

   abstract protected String getImagePrefix();

   abstract protected String getUrlPrefix();

   abstract protected String getHomeCountry();

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("currentLocation", getCurrentLocation());
      cookie.setDomain(".www." + getHomeDomain());
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   protected JSONObject fetchProduct(Document doc, String storeId) {
      JSONObject productsInfo = new JSONObject();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, false, false);

      JSONArray stores = JSONUtils.getValueRecursive(pageJson, "props.pageProps.master_product_detail_response.data.components.0.resource.product.stores", JSONArray.class, new JSONArray());

      for (int i = 0; i < stores.length(); i++) {
         JSONObject store = stores.optJSONObject(i);
            if (store != null && storeId.equals(store.optString("store_id"))) {
               productsInfo = store.optJSONObject("product");
               break;
            }
      }

      return productsInfo;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      String storeId = getStoreId();

      JSONObject productJson = fetchProduct(doc, storeId);


      if (isProductPage(productJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = productJson.optString("master_product_id");
         String internalId = productJson.optString("product_id");
         String description = productJson.optString("description");
         JSONArray images = productJson.optJSONArray("images");
         String primaryImage = images.length() > 0 ? CrawlerUtils.completeUrl(images.optString(0), "https", getImagePrefix()) : null;
         List<String> secondaryImages = crawlSecondaryImages(images);
         CategoryCollection categories = crawlCategories(productJson);
         String name = productJson.optString("name");
         List<String> eans = List.of(productJson.optString("ean"));
         boolean available = productJson.optBoolean("in_stock");
         Offers offers = available ? scrapOffers(productJson) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   public Offers scrapOffers(JSONObject productJson) {
      Offers offers = new Offers();

      try {
         Pricing pricing = scrapPricing(productJson);
         List<String> sales = scrapSales(pricing);

         Offer offer = new OfferBuilder().setSellerFullName("Rappi")
            .setInternalSellerId(productJson.optString("store_type", null))
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setPricing(pricing)
            .setIsMainRetailer(true)
            .setSales(sales)
            .build();

         offers.add(offer);
      } catch (Exception e) {
         Logging.printLogWarn(logger, session, "offers error: " + session.getOriginalURL());
      }

      return offers;
   }

   public static Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double priceFrom = productJson.optDouble("real_price", 0D);
      Double price = productJson.optDouble("balance_price", 0D);
      if (price == 0D || price.equals(priceFrom)) {
         price = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(price);

      return PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   public static CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<Card> cards = Sets.newHashSet(
         Card.VISA,
         Card.MASTERCARD,
         Card.DINERS,
         Card.AMEX,
         Card.ELO,
         Card.SHOP_CARD
      );

      for (Card card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.toString())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public static List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (pricing.getPriceFrom() != null && pricing.getPriceFrom() > pricing.getSpotlightPrice()) {
         BigDecimal big = BigDecimal.valueOf(pricing.getPriceFrom() / pricing.getSpotlightPrice() - 1);
         String rounded = big.setScale(2, BigDecimal.ROUND_DOWN).toString();
         sales.add('-' + rounded.replace("0.", "") + '%');
      }

      return sales;
   }

   protected boolean isProductPage(JSONObject productJson) {
      return productJson.length() > 0;
   }

   protected List<String> crawlSecondaryImages(JSONArray images) {
      ArrayList<String> resultImages = new ArrayList<>();

      if (images.length() > 1) {
         for (int i = 1; i < images.length(); i++) {
            String imagePath = CrawlerUtils.completeUrl(images.optString(i), "https", getImagePrefix());
            if (imagePath != null) {
               resultImages.add(imagePath);
            }
         }
      }

      return resultImages;
   }

   protected CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();

      String category = JSONUtils.getStringValue(productJson, "category_name");

      if (!category.isEmpty()) {
         categories.add(category);
      }

      return categories;
   }
}
