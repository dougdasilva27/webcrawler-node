package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;

import java.util.*;

import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Date: 25/11/2016 1) Only one sku per page.
 * 
 * Price crawling notes: 1) For this market was not found product unnavailable 2) Page of product
 * disappear when javascripit is off, so is accessed this api:
 * "https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc="+id 3) InternalId of product
 * is in url and a json, but to fetch api is required internalId, so it is crawl in url 4) Has no
 * bank ticket in this market 5) Has no internalPid in this market 6) IN api when have a json,
 * sometimes has duplicates keys, so is used GSON from google.
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloOnofreCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.onofre.com.br";
   private static final String HOME_PAGE_HTTP = "http://www.onofre.com.br/";

   private static final List<String> cards = Arrays.asList(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   public SaopauloOnofreCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);

         JSONArray skus = JSONUtils.getJSONArrayValue(jsonInfo, "sku");

         String name = crawlName(doc, jsonInfo);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home):not(.product) a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-short-description", "#details"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img#image-main", Arrays.asList("data-zoom-image", "src"), "https", "img.onofre.com.br");
         List<String> secondaryImages = scrapSecondaryImages(doc, primaryImage);
         String ean = JSONUtils.getStringValue(jsonInfo, "gtin13");
         List<String> eans = ean != null ? Collections.singletonList(ean) : null;

         boolean available = crawlAvailability(jsonInfo);
         Offers offers = available ? scrapOffers(doc) : new Offers();

         for (Object obj : skus) {
            if (obj instanceof String) {
               String internalId = (String) obj;
               String internalPid = internalId;
               RatingsReviews ratingReviews = crawlRating(doc, internalId, primaryImage);

               // Creating the product
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
                     .setEans(eans)
                     .setRatingReviews(ratingReviews)
                     .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private List<String> scrapSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();

      Elements images = doc.select(".product-image-gallery img:not(#image-main)");
      for (Element e : images) {
         String image = CrawlerUtils.sanitizeUrl(e, Arrays.asList("data-zoom-image", "src"), "https", "img.onofre.com.br");

         if ((primaryImage == null || !primaryImage.split("\\?")[0].equals(image.split("\\?")[0])) && image != null) {
            secondaryImages.add(image);
         }
      }

      return secondaryImages;
   }

   private String crawlName(Document doc, JSONObject jsonInfo) {
      String name = JSONUtils.getStringValue(jsonInfo, "name");

      Element el = doc.selectFirst(".product-view .product-info .marca.show-hover");
      if (el != null) {
         name = name + " " + el.text();
      }

      Element ele = doc.selectFirst(".product-view .product-info .quantidade.show-hover");
      if (ele != null) {
         name = name + " " + ele.text();
      }

      return name;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-view") != null;
   }

   private boolean crawlAvailability(JSONObject json) {
      boolean availability = false;

      if (json.has("offers") && !json.isNull("offers")) {
         JSONObject value = json.optJSONObject("offers");
         if (value.has("availability")) {
            availability = value.optString("availability").contains("InStock");
         }
      }

      return availability;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("onofre sao paulo")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-shop .price-box .special-price .price", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-shop .price-box .old-price .price", null, true, ',', session);

      if(spotlightPrice == null){
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.product-shop .price-box .price", null, true, ',', session);
      } else if(spotlightPrice.equals(priceFrom)){
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
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

   private RatingsReviews crawlRating(Document doc, String internalId, String primaryImage) {
      TrustvoxRatingCrawler rating = new TrustvoxRatingCrawler(session, "109192", logger);
      rating.setPrimaryImage(primaryImage);

      return rating.extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
