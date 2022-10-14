package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilBebidasStoreCrawler extends Crawler {
   public BrasilBebidasStoreCrawler(Session session) {
      super(session);
   }

   private String HOME_PAGE = "https://www.bebidastore.com.br/";
   private String SELLER_NAME = "BebidasStore";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<Product>();
      Element product = doc.selectFirst(".produto-wrapper.product-detail");
      if (product != null) {
         List<String> categories = getCategories(doc);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "#form_comprar", "data-id");
         String primaryImg =  CrawlerUtils.scrapSimplePrimaryImage(product,".box-img.index-list.active .zoom .swiper-lazy",List.of("data-src"),"","");
         List<String> secondaryImg = getSecondaryImages(doc,primaryImg);
         String description = CrawlerUtils.scrapStringSimpleInfo(product, ".section-box.description", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", true);
         Boolean isAvailable = checkIsAvailable(CrawlerUtils.scrapStringSimpleInfo(product, ".botao-commerce.botao-nao_indisponivel", true));
         Offers offers = isAvailable ? scrapOffers(product) : new Offers();
         Product newProduct = ProductBuilder.create()
            .setInternalId(internalId)
            .setUrl(this.session.getOriginalURL())
            .setCategories(categories)
            .setOffers(offers)
            .setName(name)
            .setPrimaryImage(primaryImg)
            .setSecondaryImages(secondaryImg)
            .setDescription(description)
            .build();
         products.add(newProduct);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private Boolean checkIsAvailable(String product) {
      return product == null;
   }
   private List<String> getCategories(Document doc) {
      List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item", true);
      if (categories != null && categories.size() > 0) {
         categories.remove(categories.size() - 1);
         return categories.size() > 0 ? categories : null;
      }
      return null;
   }
   private List<String> getSecondaryImages(Document doc, String primaryImage) {
      List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".nav-images .list.swiper-container .swiper-wrapper .item.swiper-slide .box-img.index-list .swiper-lazy", Arrays.asList("data-src"), "", "", primaryImage);
      if(images!=null && images.size()>0) {
         images.remove(0);
         images = images.size() > 0 ? images : null;
         return images;
      }
      return null;
   }
   private Offers scrapOffers(Element data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      String priceString = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#preco_atual", "value");
      Double price = Double.parseDouble(priceString);
      Element installments = doc.selectFirst("#info_preco");
      CreditCards creditCards = scrapCreditCards(price, installments);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(price)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice, Element elementInstallments) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Integer installmentNumber = 1;
      Double installmentPrice = spotlightPrice;
      if (elementInstallments != null) {
         Integer installmentsInteger = CrawlerUtils.scrapIntegerFromHtml(elementInstallments, ".preco-parc2 strong", true, 1);
         Double p = CrawlerUtils.scrapDoublePriceFromHtml(elementInstallments, ".txt-cadaparcelas .preco-parc2 span", null, false, ',', session);
         installmentPrice = p != null ? p : installmentPrice;
         installmentNumber = installmentsInteger != null ? installmentsInteger : installmentNumber;
      }
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(installmentNumber)
         .setInstallmentPrice(installmentPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.DINERS.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.HIPERCARD.toString());

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
