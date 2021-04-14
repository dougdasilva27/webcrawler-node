package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedUrlException;
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
import java.util.regex.Pattern;

public abstract class SupermarketCrawler extends Crawler {
   private static final String SELLER_FULL_NAME = "Super Market";
   private final String store = getStore();
   private final Pattern urlPattern = Pattern.compile("^https:\\/\\/app[.]bigdatawifi[.]com[.]br[\\/]" + store + ".*");

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString());

   public SupermarketCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);

   }

   public String getStore() {
      return store;
   }

   @Override
   protected Object fetch() {

      if (urlPattern.matcher(session.getOriginalURL()).matches()) {

         return super.fetch();

      } else {
         throw new MalformedUrlException("URL não corresponde ao market");
      }

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".quantidade", "data-id");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-detalhe", true);
         //Site hasn't categories
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".foto.horizontal img", Arrays.asList("src"), "https", "app.bigdatawifi.com.br");
         //Site hasn't secondary images
         String description = name;
         boolean available = !doc.select(".add-mais-itens-detalhe").isEmpty(); //I didn't find any product unavailable to test
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
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
      return doc.selectFirst(".produto-container.detalhe") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //Site hasn't any sale

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
      Double spotlightPrice = getSpotlightPrice(doc);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-original", null, true, '.', session);
      Double priceFrom = price != null && !price.equals(spotlightPrice) && price > spotlightPrice ? price : null;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private Double getSpotlightPrice(Document doc) {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-promocional", null, true, '.', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-original", null, true, '.', session);

      }

      return spotlightPrice;
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

   //Site hasn't rating

}
