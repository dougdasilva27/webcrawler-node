package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilDrogariasoaresCrawler extends Crawler {
   public BrasilDrogariasoaresCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.drogariasoares.com.br/";
   private Set<String> CARDS = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(), Card.AMEX.toString());

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#adicionar-lista[data-compra]", "data-compra");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "#side-prod h4 small", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#side-prod h1, #side-prod h2", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li.show-for-large-up:not(.current) a");
         List<String> images = scrapImages(doc);
         String primaryImage = null;
         if (!images.isEmpty()) {
            primaryImage = images.remove(0);
         }
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#corpo-detalhe .prod-descr .prod-content-left"));
         boolean available = !doc.select(".produto-disponivel.show").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#adicionar-lista[data-compra]") != null;
   }

   private List<String> scrapImages(Document doc) {
      List<String> rawImages = CrawlerUtils.scrapSecondaryImages(doc, "#gallery img", List.of("src"), "https", "drogariasoares.com.br", null);
      return rawImages.stream().map(image -> image.replace("/Mini/", "/Super/")).collect(Collectors.toList());
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Drogaria Soares Brasil")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produto-disponivel .preco-por strong", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-det .preco-de", null, true, ',', session);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, CARDS);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }
}
