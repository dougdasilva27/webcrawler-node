package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class MexicoCreatorstudioCrawler extends Crawler {

   private static final String SELLER_NAME = "Creator Studio";

   public MexicoCreatorstudioCrawler(Session session) {
      super(session);
   }

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".page-content.page-content--product") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = removeSpacesId(CrawlerUtils.scrapStringSimpleInfo(doc, ".product-single__sku", true));
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".h2.product-single__title", false);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product__thumb .lazyloaded", Arrays.asList("src"), "https", "", null);
         String primaryImage = secondaryImages != null && !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a", true);
         String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList(".product-single__description-full.rte"));
         boolean available = !doc.select(".btn.btn--full.add-to-cart").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();

         products.add(product);

         JSONArray jsonArrayVariation = CrawlerUtils.selectJsonArrayFromHtml(doc, "div[id^=VariantsJson-]", null, "", false, false);
         if (jsonArrayVariation != null && !jsonArrayVariation.isEmpty()) {
            for (Object o : jsonArrayVariation) {
               JSONObject jsonVariation = (JSONObject) o;
               internalId = removeSpacesId(jsonVariation.optString("sku"));
               if (checkIfProductAlreadyAdded(products, internalId)) {
                  continue;
               }

               products.add(crawlVariations(jsonVariation, description));
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean checkIfProductAlreadyAdded(List<Product> productList, String internalId) {
      boolean hasProduct = false;
      for (Product product : productList) {
         if (product.getInternalId().equals(internalId)) {
            hasProduct = true;
            break;
         }
      }

      return hasProduct;
   }

   private String removeSpacesId(String id) {
      if (id != null) {
         return id.replace(" ", "-");
      }

      return null;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-single__sku", "id");
      if (internalPid != null) {
         return internalPid.replace("Sku-", "");
      }

      return null;
   }

   private Product crawlVariations(JSONObject jsonVariation, String description) throws MalformedProductException, MalformedPricingException, OfferException {
      String internalId = jsonVariation.optString("sku");
      String internalPid = jsonVariation.optString("id");
      String name = jsonVariation.optString("name");
      String primaryImage = JSONUtils.getValueRecursive(jsonVariation, "featured_media.preview_image.src", String.class);
      Offers offers = scrapOffers(jsonVariation); //all variations has status "sobre pedido" so we are capture as available to show price

      // Creating the product
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setOffers(offers)
         .setPrimaryImage(primaryImage)
         .setDescription(description)
         .build();

      return product;

   }

   private Offers scrapOffers(Object o) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing;
      if (o instanceof Document) {
         pricing = scrapPricing((Document) o);
      } else {
         pricing = scrapPricingJson((JSONObject) o);
      }

      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product__price", null, true, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(null)
         .setCreditCards(creditCards)
         .build();
   }

   private Pricing scrapPricingJson(JSONObject jsonObject) throws MalformedPricingException {
      Integer intPrice = jsonObject.optInt("price", 0);
      Double spotlightPrice = (double) Math.round((intPrice / 100));

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
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
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
}
