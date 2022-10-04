package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPetCenterFioriCrawler extends Crawler {

   public BrasilPetCenterFioriCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.petcenterfiore.com.br/";
   private static final String SELLER_NAME = "PetCenterFiore";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Element product = doc.selectFirst(".js-has-new-shipping.js-product-detail.js-product-container.js-shipping-calculator-container");
      if (product != null) {
         String dataVariants = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".js-has-new-shipping.js-product-detail.js-product-container.js-shipping-calculator-container", "data-variants");
         JSONObject dataJson = getDataJson(dataVariants);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".js-product-form input", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".js-product-name.h2.h1-md", false);
         String primaryImage = getUrlImage(dataJson.optString("image_url", ""));
         String description = CrawlerUtils.scrapStringSimpleInfo(product, ".product-description.user-content", false);
         Offers offers = checkAvailability(doc, dataJson.optBoolean("available")) ? scrapOffers(doc) : new Offers();
         List<String> secondaryImages = getSecondaryImages(doc, primaryImage);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs .crumb", true);
         Product newProduct = ProductBuilder.create()
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setUrl(session.getOriginalURL())
            .setCategories(categories)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(newProduct);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private JSONObject getDataJson(String data) {
      if (data != null) {
         return CrawlerUtils.stringToJSONObject(data.replace("[", "").replace("]", ""));
      }
      return new JSONObject();
   }

   private String getUrlImage(String image) {
      if (!image.isEmpty() && image != null) {
         return "https:" + image;
      }
      return null;
   }

   private Offers scrapOffers(Element data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".js-price-display.text-primary", null, true,
         ',', session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".js-compare-price-display.price-compare.font-weight-normal", null, true, ',',
         session);
      if (price != null && spotlightPrice != null && price.equals(spotlightPrice)) {
         price = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(price)
         .setBankSlip(bankSlip)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AMEX.toString(), Card.DINERS.toString(), Card.AURA.toString(),
         Card.ELO.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(), Card.DISCOVER.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private boolean checkAvailability(Document doc, Boolean available) {
      if (available != null && available) {
         String price = CrawlerUtils.scrapStringSimpleInfo(doc, ".js-price-display.text-primary", false);
         return price != null && !price.isEmpty();
      }
      return false;
   }

   private List<String> getSecondaryImages(Document doc, String imgUrl) {
      List<String> imgs = CrawlerUtils.scrapSecondaryImages(doc, ".col-2.d-none.d-md-block a img", Arrays.asList("data-srcset"), "https", "", imgUrl);
      List<String> returnImgs = new ArrayList<String>();
      if (imgs != null && !imgs.isEmpty()) {
         for (Integer i = 1; i < imgs.size(); i++) {
            returnImgs.add(getImage(imgs.get(i)));
         }
      }
      return returnImgs;
   }

   private String getImage(String values) {
      String imgs[] = values.split(",");
      Integer ult = imgs.length - 1;
      String pathImg[] = imgs[ult].split(" ");
      if (pathImg[1].contains("https://")) {
         return pathImg[1];
      }
      return "https:" + pathImg[1];
   }
}
