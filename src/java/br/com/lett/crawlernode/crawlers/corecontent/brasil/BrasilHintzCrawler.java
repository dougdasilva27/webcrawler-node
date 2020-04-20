package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilHintzCrawler extends Crawler {

   private static final String HOME_PAGE = "loja.hintz.ind.br";
   private static final String SELLER_FULL_NAME = "Hintz";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilHintzCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject json = scrapArray(doc);

         String internalId = scrapInternalId(doc);
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "head title", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#galeria__principal a", Arrays.asList("href"), "https",
               HOME_PAGE);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#galeria__principal a",
               Arrays.asList("href"), "https:", HOME_PAGE, primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".row .produto__texto"));
         Offers offers = scrapOffers(json);

         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setOffers(offers)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#produto .container") != null;
   }

   private String scrapInternalId(Document doc) {
      String internalId = null;

      Element scripts = doc.selectFirst("body script[type=\"text/javascript\"]");

      String script = scripts.html().replace(" ", "").toLowerCase();
      if (script.contains("itemdata.id=")) {
         internalId = CrawlerUtils.extractSpecificStringFromScript(script, "itemdata.id=", false, ";", false).trim();
      }
      return internalId;

   }

   private JSONObject scrapArray(Document doc) {
      JSONObject json = new JSONObject();

      Element scripts = doc.selectFirst("body script[type=\"text/javascript\"]");
      String script = scripts.html().replace(" ", "").toLowerCase();

      if (script.contains("itemdata.variacoes=")) {
         String jsonString = CrawlerUtils.extractSpecificStringFromScript(script, "itemdata.variacoes=[", false, "];", false).trim();

         json = JSONUtils.stringToJson(jsonString);
      }

      return json;
   }

   private Offers scrapOffers(JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
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

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(json, "preco_listagem", true);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "preco_venda", true);
      CreditCards creditCards = scrapCreditCards(json, spotlightPrice);
      BankSlip bankSlip = BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();

   }

   private CreditCards scrapCreditCards(JSONObject json, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(json);
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

   public Installments scrapInstallments(JSONObject json) throws MalformedPricingException {
      Installments installments = new Installments();

      Integer instalment = JSONUtils.getIntegerValueFromJSON(json, "quantidade_parcelas_sem_juros", null);
      Double value = JSONUtils.getDoubleValueFromJSON(json, "preco_venda_parcelado", true);

      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(instalment)
            .setInstallmentPrice(value)
            .build());

      return installments;
   }

}
