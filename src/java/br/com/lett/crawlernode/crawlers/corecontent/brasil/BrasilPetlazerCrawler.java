package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPetlazerCrawler extends Crawler {

   private static final String HOME_PAGE = "petlazer.com.br";
   private static final String SELLER_FULL_NAME = "Pet Lazer";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilPetlazerCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = scrapInternalId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#produto #h1", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".descricao_caminho a", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#produto .img-responsive", Arrays.asList("src"), "https", HOME_PAGE);
         String secondaryImages = null;
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".descricao_div_conteudo:not(col-lg-3)"));

         Offers offers = scrapOffers(doc);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
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
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#produto") != null;
   }

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      String saleStr = CrawlerUtils.scrapStringSimpleInfo(doc, "#produto div div[style] center b[style]", false);

      if (saleStr != null && !saleStr.isEmpty()) {
         sales.add(saleStr);
      }

      return sales;
   }


   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      boolean available = doc.selectFirst(".bt_carrinho") != null;

      if (!available) {
         return offers;
      }

      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);


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

      Elements prices = doc.select("div[id=produto] div font b");

      Double priceFrom = null;

      if (prices.size() == 4) {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(prices.remove(0), null, null, false, ',', session);
      }

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(prices.first(), null, null, false, ',', session);
      Double bankSlipPrice = spotlightPrice;

      if (prices.size() == 3) {
         bankSlipPrice = CrawlerUtils.scrapDoublePriceFromHtml(prices.get(2), null, null, false, ',', session);
      }

      CreditCards creditCards = scrapCreditCards(prices, spotlightPrice);

      BankSlip bankSlip = BankSlipBuilder.create()
         .setFinalPrice(bankSlipPrice)
         .build();

      return PricingBuilder.create()
         .setPriceFrom(null)
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Elements prices, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(prices, spotlightPrice);

      if(installments.getInstallments().isEmpty()) {
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

   private Installments scrapInstallments(Elements prices, Double spotlightPrice) throws MalformedPricingException {

      Installments installments = new Installments();

      Element installmentEl = null;

      if (prices.size() == 3 && prices.get(1) != null) {
         installmentEl = prices.get(1);
      }

      if (installmentEl != null) {
         Pair<Integer, Float> installmentPair = CrawlerUtils.crawlSimpleInstallment(null, installmentEl, true, "x de");

         if (!installmentPair.isAnyValueNull()) {

            if (installmentPair.getFirst() > 1) {
               installments.add(InstallmentBuilder.create()
                  .setInstallmentNumber(1)
                  .setInstallmentPrice(spotlightPrice)
                  .build());
            }

            installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(installmentPair.getFirst())
               .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(installmentPair.getSecond().doubleValue()))
               .build());
         }
      }

      if (!installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      return installments;
   }

   private String scrapInternalId (Document doc) {

      String internalId = null;

      Element button = doc.selectFirst("#produto button");

      if (button == null) {
         return internalId;
      }

      if (button.hasAttr("onclick")) {
         String att = button.attr("onclick");

         if (!att.isEmpty()) {

            String search = "carrinho/";

            int firstIndex = att.lastIndexOf(search);
            int lastIndex = att.lastIndexOf("/");

            if (firstIndex > 0 && lastIndex > firstIndex + search.length()) {
               internalId = att.substring(firstIndex + search.length(), lastIndex);
            }
         }
      } else if (button.hasAttr("href")) {
         String att = button.attr("href");

         if (!att.isEmpty()) {
            String search = "produto=";

            int firstIndex = att.lastIndexOf(search);
            int lastIndex = att.indexOf("&", firstIndex+search.length());

            if (firstIndex > 0 && lastIndex > firstIndex + search.length()) {
               internalId = att.substring(firstIndex + search.length(), lastIndex);
            }
         }
      }

      return internalId;
   }
}
