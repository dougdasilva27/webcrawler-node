package br.com.lett.crawlernode.crawlers.corecontent.portugal;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PortugalAuchanCrawler extends Crawler {


   private static final String HOME_PAGE = "https://www.auchan.pt/Frontoffice/";
   private static final String SELLER_FULL_NAME = "Auchan";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public PortugalAuchanCrawler(Session session) {
      super(session);
   }


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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".info input[name=\"Id\"]", "value");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail .relative", false);
         boolean available = doc.selectFirst("button[data-gtmevent=\"ev_add_to_cart\"]") != null;
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".item.type-image a img", Arrays.asList("src"), "http://", HOME_PAGE);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#productDetail .row .col-md-12"));
         Offers offers = available ? scrapOffer(doc) : new Offers();
         RatingsReviews reviews = scarpRatingsReviews(doc);
         // Creating the product

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setRatingReviews(reviews)
            .setOffers(offers)
            .build();
         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst("#conteudo .product-detail") != null;
   }

   private RatingsReviews scarpRatingsReviews(Document doc) {
      //this only information can be scraper for now
      RatingsReviews reviews = new RatingsReviews();
      double averageRating = 0;
      int totalReviews = 0;

      Element element = doc.selectFirst("div.col-md-11 div.row script[ type=text/javascript]");

      List<DataNode> dataNodes = element != null ? element.dataNodes() : null;
      String scriptStar = dataNodes.isEmpty() ? "" : dataNodes.get(0).getWholeData();
      String valueLocate = "var ratingStar =";

      if (scriptStar.contains(valueLocate)) {
         int indexStart = scriptStar.indexOf(valueLocate) + valueLocate.length();
         int indexEnd = scriptStar.indexOf(scriptStar.substring(indexStart,scriptStar.indexOf(";",indexStart))) + valueLocate.length();
         String value = scriptStar.substring(indexStart,indexEnd ).replaceAll("[^0-9]", "");
         averageRating = MathUtils.parseDoubleWithDot(value);
      }
      totalReviews = CrawlerUtils.scrapIntegerFromHtml(doc, "#numberOfVotes", false, 0);

      reviews.setTotalRating(totalReviews);
      reviews.setAverageOverallRating(averageRating);

      return reviews;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

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
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".item-old-price", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".item-price", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
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

}
