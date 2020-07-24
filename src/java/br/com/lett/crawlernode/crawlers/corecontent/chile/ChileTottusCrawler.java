package br.com.lett.crawlernode.crawlers.corecontent.chile;

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
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Date: 03/12/2018
 *
 * @author Gabriel Dornelas
 */
public class ChileTottusCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.tottus.cl/";
   private static final String SELLER_FULL_NAME = "Tottus";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public ChileTottusCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         JSONObject jsonFromHTML = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);
         JSONObject offersJson = jsonFromHTML.optJSONObject("offers");

         String internalId = jsonFromHTML.optString("sku");
         String internalPid = internalId;
         String name = scrapName(doc);
         boolean available = offersJson.opt("availability").equals("https://schema.org/InStock");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".Breadcrumbs .link.small");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-gallery-image", Arrays.asList("src"), "http://",
            "www.tottus.cl");
         String secondaryImages = scrapSecondaryImages(jsonFromHTML);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".react-tabs__tab-panel tbody tr"));
         Offers offers = available ? scrapOffer(doc) : new Offers();

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
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;

   }


   private boolean isProductPage(Document doc) {
      return !doc.select("#container .Product").isEmpty();
   }

   private String scrapName(Document doc) {
      String mainTitle = CrawlerUtils.scrapStringSimpleInfo(doc, ".column-right-content .title", true);
      String subTitle = CrawlerUtils.scrapStringSimpleInfo(doc, ".column-right-content .brand", true);
      return mainTitle + " " + subTitle;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(OfferBuilder.create()
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price.medium.currentPrice", null, false, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);


      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();


   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private String scrapSecondaryImages(JSONObject jsonFromHTML) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = JSONUtils.getJSONArrayValue(jsonFromHTML, "image");
      List<String> listOfUrls = new ArrayList<>();
      if (secondaryImagesArray != null) {
         for (Object e : secondaryImagesArray) {
            if (e instanceof String) {
               String images = (String) e;
               listOfUrls.add(images);
            }
         }

         if (listOfUrls.size() > 0) {
            secondaryImages = secondaryImagesArray.toString();
         }
      }
      return secondaryImages;
   }

}
