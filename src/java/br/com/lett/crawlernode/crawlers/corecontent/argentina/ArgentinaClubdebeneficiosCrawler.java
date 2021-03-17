package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class ArgentinaClubdebeneficiosCrawler extends Crawler {

   public ArgentinaClubdebeneficiosCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   private static final String SELLER_FULL_NAME = "ClubDeBeneficios";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://clubdebeneficios.com/";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   //cannot find out of stock products when this crawler was made (15/03/2021)
   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=name]", false);
         CategoryCollection categories = scrapCategories(doc);
         List<String> secondaryImages = scrapImages(doc);
         String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "div[itemprop=short_description]", false);
         Offers offers = scrapOffers(doc, internalId);

         // Stuff that were not on site when crawler was made
         Integer stock = null;
         List<String> eans = null;
         RatingsReviews ratingsReviews = null;

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setRatingReviews(ratingsReviews)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-info-main") != null;
   }

   private CategoryCollection scrapCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();

      String categoriesString = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[id=txtCategoriesPath]", "value");

      if (categoriesString != null) {
         String[] splittedCategories = categoriesString.split(",");
         if (splittedCategories.length > 0) {
            String[] result = splittedCategories[0].split("/");
            if (result.length > 0) {
               categories.addAll(Arrays.asList(result));
            }
         }
      }

      return categories;
   }

   private List<String> scrapImages(Document doc) {
      List<String> imagesList = new ArrayList<>();

      Elements imagesElement = doc.select("script[type=text/x-magento-init]");

      if (!imagesElement.isEmpty()) {
         JSONObject imagesJson = getDataJsonFromHtml(imagesElement, "mage/gallery/gallery");
         JSONArray imagesArray = JSONUtils.getValueRecursive(imagesJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class);

         if (imagesArray != null) {
            for (Object e : imagesArray) {
               if (e instanceof JSONObject) {
                  JSONObject item = (JSONObject) e;
                  imagesList.add(item.optString("full"));
               }
            }
         }
      }

      return imagesList;
   }

   private Offers scrapOffers(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc, internalId);

      sales.add(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      BankSlip bankSlip = new BankSlip();
      CreditCards creditCards = new CreditCards();
      Double priceFrom = null;
      Double spotlightPrice = null;

      Elements jsonElement = doc.select("script[type=text/x-magento-init]");

      if (!jsonElement.isEmpty()) {
         JSONObject pricesJson = getDataJsonFromHtml(jsonElement, "max_price");

         if(pricesJson != null){
            JSONObject priceInfoJson = JSONUtils.getValueRecursive(pricesJson, "*.Magento_Catalog/js/product/view/provider.data.items." + internalId + ".price_info", JSONObject.class);

            if(priceInfoJson != null){
               spotlightPrice = JSONUtils.getDoubleValueFromJSON(priceInfoJson, "max_price", false);
               priceFrom = JSONUtils.getDoubleValueFromJSON(priceInfoJson, "max_regular_price", false);

               if(priceFrom.equals(spotlightPrice)){
                  priceFrom = null;
               }

               bankSlip = BankSlip.BankSlipBuilder.create()
                  .setFinalPrice(spotlightPrice)
                  .build();
               creditCards = scrapCreditcards(spotlightPrice);
            }
         }
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditcards(Double installmentPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setFinalPrice(installmentPrice)
         .setInstallmentNumber(1)
         .setInstallmentPrice(installmentPrice)
         .build());

      for (String flag : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(flag)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   private JSONObject getDataJsonFromHtml(Elements imagesElement, String keyword) {
      JSONObject json = new JSONObject();

      for (Element el : imagesElement) {
         if (el.html().contains(keyword)) {
            json = JSONUtils.stringToJson(el.html());
            break;
         }
      }

      return json;
   }

}
