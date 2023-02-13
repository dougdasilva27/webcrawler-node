package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class BrasilLojadomecanicoCrawler extends Crawler {
   private static final String HOME_PAGE = "http://www.lojadomecanico.com.br/";
   private static final String SELLER_FULL_NAME = "loja-do-mecanico-brasil";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString());

   public BrasilLojadomecanicoCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setFollowRedirects(true)
         .setProxyservice(List.of(
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();
      return this.dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject jsonIdSkuPrice = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.chaordic_meta=", ";", true, false);
         JSONObject jsonNameDesc = CrawlerUtils.selectJsonFromHtml(doc, ".product-page script[type=\"application/ld+json\"]", "", null, false, false);

         String internalId = jsonIdSkuPrice.has("pid") ? jsonIdSkuPrice.getString("pid") : null;
         String internalPid = jsonIdSkuPrice.has("sku") ? jsonIdSkuPrice.getString("sku") : null;
         String name = scrapName(jsonNameDesc, doc, internalId);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".cateMain_breadCrumbs .breadCrumbNew li span[itemprop=\"name\"]", false);
         String description = scrapDescription(jsonNameDesc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image .img-produto-min li a", Arrays.asList("data-image"), "https:", "www.lojadomecanico.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-image .img-produto-min li a", Arrays.asList("data-image"), "https", "www.lojadomecanico.com.br", primaryImage);
         boolean availableToBuy = doc.selectFirst("#btn-comprar-product") != null;
         Integer stock = scrapStock(doc, availableToBuy);
         RatingsReviews rating = scrapRating(jsonNameDesc);
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setRatingReviews(rating)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-page #product") != null;
   }

   private String scrapName(JSONObject json, Document doc, String internalId) {
      String name = null;
      if (json.has("name") && !json.isNull("name")) {
         name = json.get("name").toString();

         String nameVariation = CrawlerUtils.scrapStringSimpleInfo(doc, "button[data-id=\"" + internalId + "\"][disabled=\"disabled\"]", false);
         if (nameVariation != null) {
            name += " " + nameVariation;
         }
      }

      return name;
   }

   private String scrapDescription(JSONObject json) {
      String description = null;

      if (json.has("description")) {
         description = json.getString("description");

         // Decodificando string
         description = description.replace("&lt;", "<");
         description = description.replace("&gt;", ">");
      }

      return description;
   }

   private Integer scrapStock(Document doc, boolean available) {
      Integer stock = 0;

      if (available) {
         stock = null;
         Element e = doc.selectFirst("#btn-comprar-product");

         if (e != null && e.hasAttr("data-product")) {
            JSONObject json = new JSONObject(e.attr("data-product"));

            if (json.has("maxStock")) {
               stock = json.getInt("maxStock");
            }
         }
      }

      return stock;
   }

   private RatingsReviews scrapRating(JSONObject json) {
      RatingsReviews rating = new RatingsReviews();

      Integer totalNumOfEvaluations = 0;
      Double avgRating = 0.0;

      if (json.has("aggregateRating")) {
         json = json.getJSONObject("aggregateRating");

         totalNumOfEvaluations = CrawlerUtils.getIntegerValueFromJSON(json, "reviewCount", 0);
         avgRating = scrapAvgRating(json);
      }

      rating.setDate(session.getDate());
      rating.setTotalRating(totalNumOfEvaluations);
      rating.setAverageOverallRating(avgRating);
      rating.setTotalWrittenReviews(totalNumOfEvaluations);

      return rating;
   }

   private Double scrapAvgRating(JSONObject json) {
      Double avgRating = JSONUtils.getDoubleValueFromJSON(json, "ratingValue", true);

      if (avgRating == null) {
         avgRating = 0d;
      }

      return avgRating;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[id=product-price]", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.preco-tabela", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }
}
