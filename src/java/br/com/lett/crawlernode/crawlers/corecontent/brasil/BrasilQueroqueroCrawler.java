package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BrasilQueroqueroCrawler extends VTEXNewScraper {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   public BrasilQueroqueroCrawler(@NotNull Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      String vtex_segment = (String) session.getOptions().optQuery("/cookies/vtex_segment");
      BasicClientCookie cookie = new BasicClientCookie("vtex_segment", vtex_segment);
      cookie.setDomain(getHomePage().replace("https://", "").replace("/", ""));
      cookie.setPath("/");
      this.cookies.add(cookie);

   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      return session.getOptions().optJSONArray("sellers").toList().stream().map(Object::toString).collect(Collectors.toList());
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }


   @Override
   protected Pricing scrapPricing(Document doc, String internalId, JSONObject comertial, JSONObject discountsJson, JSONObject jsonSku) throws MalformedPricingException {
      Double principalPrice = comertial.optDouble("Price");
      Double priceFrom = comertial.optDouble("ListPrice");

      Double spotlightPrice = scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      if (spotlightPrice != null && spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
      }

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   protected CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
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

      String cardBrand = "Quero-Quero";

      Integer numberInstallment = CrawlerUtils.scrapIntegerFromHtml(doc, ".vtex-product-price-1-x-installmentsNumber--VerdeCardName", true, 0);

      Installments installmentsShopCard = new Installments();

      Double priceInstallment = getPriceInstallment(doc);
      installmentsShopCard.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(numberInstallment)
         .setInstallmentPrice(priceInstallment)
         .build());

      creditCards.add(CreditCard.CreditCardBuilder.create()
         .setBrand(cardBrand)
         .setInstallments(installmentsShopCard)
         .setIsShopCard(true)
         .build());

      return creditCards;
   }

   private Double getPriceInstallment(Document doc) {
      Double priceComplete = null;
      String priceNumber = CrawlerUtils.scrapStringSimpleInfo(doc, ".vtex-product-price-1-x-currencyInteger.vtex-product-price-1-x-currencyInteger--VerdeCardName", true);
      String priceDecimal = CrawlerUtils.scrapStringSimpleInfo(doc, ".vtex-product-price-1-x-currencyFraction.vtex-product-price-1-x-currencyFraction--VerdeCardName", true);
     if (priceNumber != null && priceDecimal != null){
        priceComplete = MathUtils.parseDoubleWithComma(priceNumber + "," + priceDecimal);;

     }
      return priceComplete;

   }


}
