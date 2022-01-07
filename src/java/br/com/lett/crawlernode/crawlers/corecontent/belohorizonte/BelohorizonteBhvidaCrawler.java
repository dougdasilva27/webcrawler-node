package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BelohorizonteBhvidaCrawler extends Crawler {

   public BelohorizonteBhvidaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script[type=application/ld+json]", null, null, false, false);

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#produtoID", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#produtoCodigo", "value");
         String name = json.optString("name");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "div.bread-crumb a", true);
         String description = crawlDescription(doc);
         String primaryImage = json.optString("image");
         List<String> eans = crawlEans(doc);
         boolean available = doc.selectFirst(".btn-addcart-product-detail") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private List<String> crawlEans(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto-det-atributos td:contains(EAN) ~ td", true);

      if(ean != null) {
         eans.add(ean);
      }
      return eans;
   }

   private String crawlDescription(Document doc) {
      List<String> selectors = new ArrayList<>();
      selectors.add("p.det-previa");
      selectors.add("div.produto-det-atributos");
      selectors.add("section.produtos-det-descricao div.container div.t-left");

      return CrawlerUtils.scrapSimpleDescription(doc, selectors);
   }

   private boolean isProductPage(Element doc) {
      return !doc.select(".produtos-det-info").isEmpty();
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("bh vida belo horizonte")
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.det-preco > strong > small > b", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.det-preco > strong", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

      Element priceElement = doc.selectFirst("div.det-preco");

      if(priceElement.html().contains("no cart")){
         Double cardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.det-preco > strong > span > b", null, true, ',', session);
         Integer cardInstallments = 1;

         if(cardPrice == null) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".desconto",priceElement , false, "x", "", true, ',');
            if(!pair.isAnyValueNull()) {
               cardPrice = pair.getSecond().doubleValue();
               cardInstallments = pair.getFirst();
            } else {
               cardPrice = spotlightPrice;
            }
         }

         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(cardInstallments)
            .setInstallmentPrice(cardPrice)
            .build());
      }else{
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

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
