package br.com.lett.crawlernode.crawlers.corecontent.manaus;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ManausSenhormercadomanausCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Senhor Mercado Manaus";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   public ManausSenhormercadomanausCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {

      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {

         String name = CrawlerUtils.scrapStringSimpleInfo(document, "h1.product-name", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(document, "#product-reference", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "#form_comprar", "data-id");
         CategoryCollection categories = CrawlerUtils.crawlCategories(document, ".breadcrumb-item a", true);
         String primaryImage = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".box-img.index-list.active .zoom img", "src");
         String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList("#descricao", "#ficha", ".section-box.properties"));
         boolean available = !document.select(".botao-commerce-img").isEmpty();
         Offers offers = available ? scrapOffers(document) : new Offers();

         products.add(ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build());

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String getDescription(Document document) {
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".single-product-page", "id");
      return internalId != null ? CommonMethods.getLast(internalId.split("-")) : null;

   }

   private boolean isProductPage(Document document) {
      return !document.select(".produto-wrapper.product-detail").isEmpty();
   }

   private Offers scrapOffers(Document document) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);
      List<String> sales = getSales(pricing);


      offers.add(Offer.OfferBuilder.create()
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

   private List<String> getSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);

      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".PrecoPrincipal span", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#produto_preco #precoDe", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setPriceFrom(priceFrom)
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
