package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

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

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".center > #hdnProdutoId", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.prodTitle", true);
         List<String> secondaryImages = crawlImagesArray(doc);
         String primaryImage = !secondaryImages.isEmpty() ? secondaryImages.remove(0) : null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "[itemprop=\"itemListElement\"] a span", true);
         String description = crawlDescription(doc);

         boolean isAvailable = doc.selectFirst("a.comprarProduto") != null;
         Offers offers = isAvailable ? crawlOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
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

   private String crawlDescription(Document doc) {
      JSONObject jsonDataString = CrawlerUtils.selectJsonFromHtml(doc, "script[type='application/ld+json']", null, " ", false, false);
      if (!jsonDataString.isEmpty()) {
         return jsonDataString.optString("description");
      }
      return null;
   }

   private List<String> crawlImagesArray(Document doc) {
      List<String> cleanImages = new ArrayList<>();
      List<String> arrayImages = CrawlerUtils.scrapSecondaryImages(doc, "a.elevatezoom-gallery img", List.of("src"), "https", "espacoprime.fbitsstatic.net", null);
      for (String img : arrayImages) {
         img = img.split("\\?")[0];
         cleanImages.add(img);
      }

      return cleanImages;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".detalhe-produto") != null;
   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if (pricing != null) {
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco div.precoPor", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco div.precoDe", null, true, ',', session);

      Double bankSlipPriceString = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pag-produto-boleto .details-content b", null, true, ',', session);

      if (spotlightPrice != null) {
         CreditCards creditCards = scrapCreditCards(doc);
         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(bankSlipPriceString)
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

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Integer installmentNumber = null;

      String installmentString = CrawlerUtils.scrapStringSimpleInfo(doc, ".colunaProduto .fbits-quantidadeParcelas", true);
      if (installmentString != null) {
         installmentNumber = Integer.parseInt(installmentString);
      }
      Double installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".colunaProduto .fbits-parcela", null, true, ',', session);

      if (installmentNumber != null && installmentPrice != null) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentPrice)
            .build());
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
