package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.pricing.*;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilAgrosoloCrawler extends Crawler {

   public BrasilAgrosoloCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=application/ld+json]", null, null, false, false);

      if (isProductPage(doc) && !json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = scrapInternalPid();
         String name = json.optString("name");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div.bread > ol > li > a > span", true);
         List<String> images = scrapImages(json);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         String description = json.optString("description");

         JSONArray variants = json.optJSONArray("offers");

         for (Object variant : variants) {
            if (variant instanceof JSONObject) {
               JSONObject variantJson = (JSONObject) variant;
               String internalId = variantJson.optString("sku");
               List<String> eans = Collections.singletonList(variantJson.optString("gtin14"));
               boolean available = variantJson.optString("availability").contains("InStock");
               Offers offers = available ? scrapOffers(doc, variantJson) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setEans(eans)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.content.produto") != null;
   }

   private String scrapInternalPid(){
      String internalPid = "";
      String[] splitUrl = session.getOriginalURL().split("-");

      if(splitUrl.length > 0){
         return CommonMethods.getLast(splitUrl);
      }

      return internalPid;
   }

   private List<String> scrapImages(JSONObject json) {
      List<String> imgs = new ArrayList<>();
      JSONArray arrImgs = json.optJSONArray("image");

      for (Object img : arrImgs) {
         imgs.add(img.toString());
      }

      return imgs;
   }

   private Offers scrapOffers(Document doc, JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("Agrosolo Brasil")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc, JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "price", true);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.fbits-preco .precoDe", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(doc);
      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc);

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsEl = doc.select("div.details-content > p");

      for(Element el : installmentsEl){
         Elements results = el.select("b");

         if(results.size() == 2){
            int installmentNumber = Integer.parseInt(results.get(0).html());
            Double installmentPrice = MathUtils.parseDoubleWithComma(results.get(1).html());

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installmentNumber)
               .setInstallmentPrice(installmentPrice)
               .build());
         }
      }

      return installments;
   }


}
