package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrasilPolskastoreCrawler extends Crawler {

   private static final String IMAGES_HOST = "polskastore.com.br";
   private static final String SELLER_NAME = "Polska Store";
   private static final List<String> cards = Arrays.asList(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public BrasilPolskastoreCrawler(Session session) {
      super(session);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, "#product-container script", null, null, false, false);

      if (isProductPage(doc) && jsonObject != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#form_comprar", "data-id");
         String internalPid = internalId;
         String description = jsonObject.optString("description");
         String primaryImage = jsonObject.optString("image");
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item:not(last-child)", true);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".swiper-slide .box-img img:not(first-child)", Arrays.asList("src"),
            "https", IMAGES_HOST, primaryImage);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);
         boolean isAvailable = doc.select(".botao-nao_indisponivel").isEmpty();
         Offers offers = isAvailable ? scrapOffers(doc) : new Offers();

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

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-colum-right").isEmpty();
   }


   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(Collections.singletonList(CrawlerUtils.calculateSales(pricing)))
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PrecoPrincipal span", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precode", null, true, ',', session);
      Double bankslip = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#preco .preco-avista.precoAvista span span", null, true, ',', session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(bankslip)
            .build())
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
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

      return creditCards;

   }

}
