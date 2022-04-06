package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.util.*;

public class UnitedStatesTheHomeDepotCrawler extends Crawler {
   private static final List<String> cards = Arrays.asList(Card.MASTERCARD.toString());
   private static final String SELLER_NAME = "The Home Depot";
   private String CURRENT_URL = null;
   private Integer VARIATION = 1;
   private static final String HOME_PAGE = "https://www.homedepot.com/";

   public UnitedStatesTheHomeDepotCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);   }

   @Override
   public void handleCookiesBeforeFetch() {
      String localizer = session.getOptions().optString("localizer");

      Cookie cookie = new Cookie.Builder("THD_LOCALIZER", localizer)
         .domain(".homedepot.com")
         .path("/")
         .isHttpOnly(false)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);
   }

   @Override
   protected Document fetch() {
      Document doc = null;

      try {
         if (this.VARIATION == 1) {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, session, this.cookiesWD, this.HOME_PAGE);
            webdriver.waitForElement(".super-sku__inline-attribute-wrapper", 120);
            this.CURRENT_URL = this.session.getOriginalURL();
         } else {
            webdriver.waitForElement(".super-sku__inline-attribute-wrapper", 120);

            WebElement variationButton = webdriver.driver.findElement(
               By.cssSelector(".super-sku__inline-attribute-wrapper div:nth-child(" + this.VARIATION + ") button"));

            webdriver.clickOnElementViaJavascript(variationButton);
            webdriver.waitLoad(70000);

            this.CURRENT_URL = webdriver.getCurURL();
         }
         doc = Jsoup.parse(webdriver.getCurrentPageSource());

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();
      }
      return doc;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc != null) {
         Elements variations = doc.select(".super-sku__inline-attribute-wrapper div button");

         for (Element e : variations) {
            Document variationHtml = scrapVariation(doc);
            JSONObject productJson = scrapJsonFromHtml(variationHtml);

            if (productJson != null) {
               Logging.printLogDebug(logger, session, "Product page identified: " + this.CURRENT_URL);

               String internalId = productJson.optString("productID");
               String name = productJson.optString("name");
               String description = productJson.optString("description");
               JSONArray images = productJson.optJSONArray("image");
               String primaryImage = scrapFirstImage(images);
               List<String> secondaryImages = scrapSecondaryImages(images);
               //            CategoryCollection categories = getCategories(doc, "[name=keywords]", "content");
               boolean available = scrapAvailability(productJson);

               Offers offers = available ? scrapOffers(productJson) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(this.CURRENT_URL)
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setName(name)
                  .setOffers(offers)
                  //.setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapFirstImage(JSONArray imagesArray) {
      if (imagesArray != null && imagesArray.length() > 1) {
         Object image = imagesArray.get(0);
         return image.toString();
      }
      return null;
   }

   private List<String> scrapSecondaryImages(JSONArray imagesArray) {
      List<String> secondaryImages = new ArrayList<>();

      if (imagesArray != null && imagesArray.length() > 2) {
         for (int i = 1; i < imagesArray.length(); i++) {
            secondaryImages.add(imagesArray.get(i).toString());
         }
      }

      return secondaryImages;
   }

   private Boolean scrapAvailability(JSONObject productJson) {
      String availability  = JSONUtils.getValueRecursive(productJson,"offers.availability", String.class);

      if (availability != null && availability.equals("https://schema.org/InStock")) {
         return true;
      }

      return false;
   }

   private Offers scrapOffers(JSONObject productJson) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
//      Double priceFrom = null;
      Integer priceInt = JSONUtils.getValueRecursive(productJson, "offers.price", Integer.class);
      Double spotlightPrice = Double.valueOf(priceInt);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(null)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      Integer installmentNumber = 1;
      Double finalPrice = spotlightPrice;
      Double installmentPrice = spotlightPrice;

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(installmentPrice)
         .setFinalPrice(finalPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Document scrapVariation(Document doc) {
      if (this.VARIATION != 1) {
            doc = fetch();
      }
      this.VARIATION++;
      return doc;
   }

   JSONObject scrapJsonFromHtml (Document doc) {
      JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[data-th=\"server\"]", null, null, false, false);

      if (productJson == null) {
         return null;
      }

      if (productJson.isEmpty()) {
         productJson = CrawlerUtils.selectJsonFromHtml(doc, "script[data-th=\"client\"]", null, null, false, false);
      }

      return productJson;
   }
}
