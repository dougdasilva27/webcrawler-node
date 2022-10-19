package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilAgrolineCrawler extends Crawler {
   private static String SELLER_NAME = "Agroline";

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString(),
      Card.HIPERCARD.toString());

   public BrasilAgrolineCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".novaColunaEstoque > .fbits-produto-nome.prodTitle", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".content.produto > div > #hdnProdutoVarianteId", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "[property=\"product:retailer_item_id\"]", "content");
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "[property=\"og:image\"]", "content");
         List<String> secondaryImages = getSecondaryImages(document);
         String description = CrawlerUtils.scrapElementsDescription(document, List.of(".infoProd > div > p"));
         List<String> categories = CrawlerUtils.crawlCategories(document, ".fbits-breadcrumb");
         String available = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".blocoQuantidadeComprar .avisoIndisponivel ", "style");
         Offers offers = available != null && !available.isEmpty() ? scrapOffers(document) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(productName)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .setOffers(offers)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".blocoEstoquePrincipal") != null;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select(".elevatezoom-gallery");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("data-image"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pagamentoPix .fbits-parcela", null, false, ',', session);
      Double bankSlipPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pagamentoBoleto .fbits-parcela", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(bankSlipPrice, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".blocoCartaoCredito .precoPor", null, false, ',', session);
      if (price == null) {
         price = spotlightPrice;
      }

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Integer installmentNumber = CrawlerUtils.scrapIntegerFromHtml(doc, ".blocoCartaoCredito .fbits-parcelamento-ultima-parcela.precoParcela .fbits-quantidadeParcelas", true, 0);
      Double installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".blocoCartaoCredito .fbits-parcelamento-ultima-parcela.precoParcela .fbits-parcela", null, false, ',', session);

      if (installmentNumber != 0 && installmentPrice == null) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber)
            .setInstallmentPrice(installmentPrice)
            .build());
      }

      return installments;
   }

}
