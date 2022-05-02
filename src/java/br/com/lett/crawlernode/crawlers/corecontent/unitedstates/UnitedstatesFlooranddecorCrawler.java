package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Cookie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UnitedstatesFlooranddecorCrawler extends Crawler {

   protected String storeId = getStoreId();
   private static final Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());
   private static final String MAIN_SELLER_NAME = "floor and decor";
   private static final String HOME_PAGE = "https://www.flooranddecor.com/";

   public UnitedstatesFlooranddecorCrawler(Session session) {
      super(session);
   }

   protected String getStoreId() {
      return session.getOptions().optString("StoreID");
   }

   @Override
   protected Object fetch() {
      Document doc = null;
      try {
         int attempts = 0;
         do {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.NETNUT_RESIDENTIAL_US_HAPROXY, session, this.cookiesWD, HOME_PAGE);
            webdriver.waitLoad(30000);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());
            webdriver.terminate();

         } while (doc != null && doc.select(".b-pdp_details").isEmpty() && attempts++ < 2);

      } catch (Exception e) {
         Logging.printLogInfo(logger, session, CommonMethods.getStackTrace(e));
         webdriver.terminate();
      }

      return doc;
   }


   @Override
   public void handleCookiesBeforeFetch() {
      Cookie cookie = new Cookie.Builder("StoreID", storeId)
         .domain("www.flooranddecor.com")
         .path("/")
         .isHttpOnly(true)
         .isSecure(false)
         .build();
      this.cookiesWD.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (!doc.select(".b-pdp_details").isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type='application/ld+json']", "", null, false, true);
         String internalPid = json.optString("sku");

         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".l-plp-breadcrumbs .b-breadcrumbs-item", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".b-pdp_specifications-content"));

         Elements variations = doc.select(".b-pdp_details .b-pdp_details-variation a");
         if (variations.isEmpty()) {
            variations = doc.select(".b-pdp_details");
         }

         for (Element variation : variations) {
            if (variations.size() != 1) {
               String productUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(variation, null, "href");
               json = fetchNextVariationJson(productUrl);
            }
            String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".b-pdp_details-element_value", true);
            String name = json.optString("name");
            List<String> images = CrawlerUtils.scrapImagesListFromJSONArray(json.optJSONArray("image"), null, null, "https", "i8.amplience.net", session);
            String primaryImage = !images.isEmpty() ? images.remove(0) : null;
            boolean available = JSONUtils.getValueRecursive(json, "offers.availability", String.class).contains("InStock");
            Offers offers = available ? scrapOffers(json) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setOffers(offers)
               .build();
            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   protected JSONObject fetchNextVariationJson(String productUrl) {
      Request request = Request.RequestBuilder.create()
         .setUrl(productUrl)
         .build();

      Response response = this.dataFetcher.get(session, request);
      Document doc = Jsoup.parse(response.getBody());
      return CrawlerUtils.selectJsonFromHtml(doc, "script[type='application/ld+json']", "", null, false, true);
   }


   protected Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      String priceStr = JSONUtils.getValueRecursive(json, "offers.price", String.class);
      Double spotlightPrice = MathUtils.parseDoubleWithDot(priceStr);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
