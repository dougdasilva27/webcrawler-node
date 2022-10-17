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
import br.com.lett.crawlernode.util.MathUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilEspacoprimeCrawler extends Crawler {

   public BrasilEspacoprimeCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.espacoprime.com.br";

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);

      List<Product> products = new ArrayList<>();

      JSONObject productJson = crawlPageJson(doc);

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productJson.optString("idProduct");
         String internalPid = internalId;
         String name = productJson.optString("nameProduct");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".image-show .zoom img", List.of("src"), "https", "images.tcdn.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".image-show .zoom img", List.of("src"), "https", "images.tcdn.com.br", primaryImage);
         CategoryCollection categories = crawlCategories(productJson);
         String description = CrawlerUtils.scrapSimpleDescription(doc, List.of(".page-info-product"));
         int stock = CrawlerUtils.scrapIntegerFromHtml(doc, ".product-colum-right .info-product span:contains(Estoque:) b", true, 0);
         boolean isAvailable = stock > 0;
         Offers offers = isAvailable ? crawlOffers(productJson) : new Offers();
         List<String> eans = List.of(productJson.optString("EAN"));

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private JSONObject crawlPageJson(Document doc) {
      Element dataScript = doc.selectFirst("script:containsData(dataLayer = [)");
      String jsonDataString = dataScript != null ? dataScript.data().substring(13, dataScript.data().length() - 1) : null; // removing "dataLayer = [" and the last "}"
      return CrawlerUtils.stringToJson(jsonDataString);
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#product-container") != null;
   }

   private CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();
      List<String> categoriesString = JSONUtils.jsonArrayToStringList(productJson.optJSONArray("breadcrumbDetails"), "name");
      categories.addAll(categoriesString);

      return categories;
   }

   private Offers crawlOffers(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = scrapSales(pricing);

      if (pricing != null){
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("espacoprime")
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productJson, "priceSell", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(productJson, "price", true);
      String bankSlipPriceString = JSONUtils.getValueRecursive(productJson, "priceSellDetails,0,installment.amount", ",", String.class, null);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      if (spotlightPrice != null){
         CreditCards creditCards = scrapCreditCards(productJson);
         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(bankSlipPriceString != null ? MathUtils.parseDoubleWithDot(bankSlipPriceString) : spotlightPrice)
            .build();

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
      } else {
         return null;
      }
   }

   private CreditCards scrapCreditCards(JSONObject productJson) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      JSONArray priceSellDetails = productJson.optJSONArray("priceSellDetails");
      for (Object o : priceSellDetails) {
         JSONObject installment = (JSONObject) o;
         Integer installmentNumber = JSONUtils.getIntegerValueFromJSON(installment, "installment.months", null);
         Double installmentPrice = JSONUtils.getDoubleValueFromJSON(installment, "installment.amount", true);
         if (installmentNumber != null && installmentPrice != null) {
            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installmentNumber)
               .setInstallmentPrice(installmentPrice)
               .build());
         }
      }

      Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      sales.add(saleDiscount);

      return sales;
   }
}
