package br.com.lett.crawlernode.crawlers.corecontent.chile;

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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.*;

public class ChileFerretekCrawler extends Crawler {

   private static final String HOME_PAGE = "https://herramientas.cl/";
   private static final String SELLER_FULL_NAME = "Ferretek";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString(), Card.DINERS.toString());

   public ChileFerretekCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if(isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"meta[itemprop=sku]", "content");
         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".infoProducto > span.codigo", true).replace("Ref: ", "");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".info-product > .infoProducto > h2", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imgPrincipal > a > img", Collections.singletonList("src"), "https", "herramientas.cl");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".thumbsGaleriaFicha > a", Collections.singletonList("href"), "https", "herramientas.cl", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li:not(:last-child)", true);
         String description = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"meta[property*=description]", "content");
         boolean availableToBuy = checkIfIsAvailable(doc);

         Offers offers = availableToBuy ? scrapOffer(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean checkIfIsAvailable(Document doc) {
     return doc.select(".stock > .no-stock-ficha").isEmpty();
   }

   private Offers scrapOffer(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".content_precio > .precioOfertaFicha", null, true, ',', session);
      String priceFromStr = CrawlerUtils.scrapStringSimpleInfo(doc, ".content_precio > .precioNormal", true);
      Double priceFrom = null;
      if (priceFromStr != null) {
         priceFromStr = priceFromStr.substring(priceFromStr.indexOf("$") + 1);
         priceFrom = MathUtils.parseDoubleWithComma(priceFromStr);
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(doc);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Double cardPrice;
      Integer installmentsNumber = 1;

      if(hasInstallments(doc)) {
         cardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".content_precio > .precioOfertaFicha", null, true, ',', session);
         Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment("span.cuotas", doc, false, "$", ", sin", true, ',');
         if (!installment.isAnyValueNull()) {
            installmentsNumber = installment.getFirst();
            cardPrice = Double.valueOf(installment.getSecond());
         }
      } else {
         cardPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".content_precio > .precioOfertaFicha", null, true, ',', session);
      }

      Installments installments = new Installments();
      if(installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentsNumber)
            .setInstallmentPrice(cardPrice)
            .build());
      }

      for(String card: cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private boolean hasInstallments(Document doc) {
      return !doc.select("span.cuotas").isEmpty();
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".info-product > .infoProducto").isEmpty();
   }
}
