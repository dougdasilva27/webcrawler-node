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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilMadeiramadeiraCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.madeiramadeira.com.br/";
   private static final String MAIN_SELLER = "MadeiraMadeira";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString(),
      Card.AMEX.toString(), Card.DINERS.toString());

   public BrasilMadeiramadeiraCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "data[data-product-id]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info__title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "ul.media-gallery__list li img",
            Collections.singletonList(
               "src"),
            "https",
            "images.madeiramadeira.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "ul.media-gallery__list li img",
            Collections.singletonList("src"), "https", "images.madeiramadeira.com.br",
            primaryImage);

         //  RatingsReviews ratingsReviews = scrapRating(internalId, doc);
         String description = CrawlerUtils.scrapSimpleDescription(doc,
            Collections.singletonList(".tab__content"));

         String availableEl = doc.selectFirst("#product-buy-button") != null ? doc.selectFirst("#product-buy-button").toString() : "";
         Offers offers = availableEl.contains("Comprar")? scrapOffers(doc) : new Offers();

         //identificamos uma mudança de internalId no mm e pedimos que reunifiquem essa loja
         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            //  .setRatingReviews(ratingsReviews)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {

      Offers offers = new Offers();

      JSONArray offersJson = findAndParseJson(doc);

      for(Object e: offersJson){

         JSONObject offer = (JSONObject) e;

         if(offer.optInt("stock") > 1){
            Pricing pricing = scrapPricing(offer);
            List<String> sales = new ArrayList<>();
            JSONObject seller = offer.optJSONObject("seller");
            String sellerName = seller.optString("name");

            offers.add(Offer.OfferBuilder.create()
               .setUseSlugNameAsInternalSellerId(false)
               .setInternalSellerId(seller.optString("id"))
               .setSellerFullName(sellerName)
               .setMainPagePosition(1)
               .setIsBuybox(true)
               .setIsMainRetailer(sellerName != null && sellerName.equalsIgnoreCase(MAIN_SELLER))
               .setSales(sales)
               .setPricing(pricing)
               .build());
         }
      }

      return offers;
   }

   private JSONArray findAndParseJson(Document doc){

      JSONArray offersJson = new JSONArray();
      Elements scriptTags = doc.select("script");

      for(Element e: scriptTags){

         String text = e.html();

         if(text.contains("buybox")){

            int delimiterLength = "JSON.parse('".length();
            int jsonFirstIndex = text.indexOf("JSON.parse('");
            int jsonLastIndex = text.indexOf("');");

            String offersText = text.substring(jsonFirstIndex + delimiterLength, jsonLastIndex);

            offersJson = JSONUtils.stringToJsonArray(offersText);
         }
      }

      return offersJson;
   }

   private Pricing scrapPricing(JSONObject offer) throws MalformedPricingException {
      JSONObject price = offer.optJSONObject("price");

      Double priceFrom = price.optDouble("fake");
      Double spotlightPrice = price.optDouble("inCash");
      BankSlip bankSlipPrice = BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();

      JSONObject installmentsInfo = offer.optJSONObject("installmentToDisplay");

      CreditCards creditCards = scrapCreditCards(installmentsInfo, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlipPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(JSONObject priceInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(priceInfo , spotlightPrice);
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .setFinalPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private Installments scrapInstallments(JSONObject installmentsInfo, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Double value;
      int installmentsNumbers;

      if(installmentsInfo != null){
         value = installmentsInfo.optDouble("value");
         installmentsNumbers = installmentsInfo.optInt("number");

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentsNumbers)
            .setInstallmentPrice(value)
            .setFinalPrice(spotlightPrice)
            .build());

      }
      return installments;
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".section product__header").isEmpty();
   }

   /*
   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "85050", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }

    */
}
