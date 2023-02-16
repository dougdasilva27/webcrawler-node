package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilAutoequipCrawler extends Crawler {

   private static final String MAIN_SELLER_NAME = "Auto Equip";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());

   public BrasilAutoequipCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".principal .acoes-produto", "data-produto-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-produto.titulo", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".conteiner-imagem img", Collections.singletonList("src"), "https", "www.autoequip.com.br");
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".thumbs-vertical  #carouselImagem.flexslider .miniaturas.slides li img", Collections.singletonList("src"), "https", "www.autoequip.com.br", primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Collections.singletonList("#descricao"));
         boolean availability = doc.selectFirst(".indisponivel") == null;
         Offers offers = availability ? scrapOffers(doc) : new Offers();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li a", true);
         List<String> eans = crawlEans(doc);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      }

      return products;
   }

   private List<String> crawlEans(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfo(doc, ".codigo-produto [itemprop='sku']", true);
      if (ean != null) {
         eans.add(ean);
      }
      return eans;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".pagina-produto") != null;
   }

   private String crawlInternalPid(Element e) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".product.type-product", "id");
      if (internalId != null && internalId.contains("-")) {
         internalId = internalId.split("-")[1];
      }
      return internalId;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setSales(Collections.singletonList(CrawlerUtils.calculateSales(pricing)))
            .setPricing(pricing)
            .build());
      }
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .acoes-produto .preco-promocional", null, false, '.', this.session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .acoes-produto .preco-venda", null, false, '.', this.session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .setBankSlip(scrapBankSlip(spotlightPrice))
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

   private BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }
}
