package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class SaopauloDrogaraiaCrawler extends Crawler {


   private static final String SELLER_NAME_LOWER = "Droga Raia";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public SaopauloDrogaraiaCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      String HOME_PAGE = "http://www.drogaraia.com.br/";
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".product-view") != null) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".price-info .live_price", "data-product-sku");
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".add-to-cart-buttons .live_stock", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1 span", false);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li:not(.home):not(.product) a");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description"));
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img", Arrays.asList("data-zoom-image"), "https://", "www.drogaraia.com.br/");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-image-gallery img", Arrays.asList("data-zoom-image"), "https://", "www.drogaraia.com.br/", primaryImage);
         String ean = scrapEan(doc);
         RatingsReviews ratingReviews = crawRating(doc, internalId);
         boolean available = doc.selectFirst(".product-shop.boxPBM .add-to-cart") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         List<String> eans = new ArrayList<>();
         eans.add(ean);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapEan(Element e) {
      String ean = null;
      Elements trElements = e.select(".farmaBox .data-table tr");

      if (trElements != null && !trElements.isEmpty()) {
         for (Element tr : trElements) {
            if (tr.text().contains("EAN")) {
               Element td = tr.selectFirst("td");
               ean = td != null ? td.text().trim() : null;
            }
         }
      }

      return ean;
   }

   private List<String> scrapSales(Pricing pricing, Document doc) {
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      if ( sale != null) {
         sales.add(sale);
      }

      sales.add(scrapPromotion(doc));

      return sales;
   }

   private String scrapPromotion(Document doc){
      StringBuilder stringBuilder = new StringBuilder();
      String qty = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_label .qty", true);
      String price = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_label .price span", false);

      if (qty != null && price != null){
         stringBuilder.append(qty + " ");
         stringBuilder.append(price);
      }

      return stringBuilder.toString();
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing, doc);

      String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".sold-and-delivered a", false);
      boolean isMainSeller = sellerName != null && sellerName.equals(SELLER_NAME_LOWER);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(isMainSeller)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .old-price .price .cifra", null, false, ',', session );
      Double spotlightPrice = getPrice(doc);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private Double getPrice(Document doc){
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .special-price .price span:nth-child(2)", null, false, ',', session );
      if (spotlightPrice == null){
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-info .regular-price span:nth-child(2) ", null, true, ',', session );
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


   private RatingsReviews crawRating(Document doc, String internalId) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "71450", logger);
      RatingsReviews ratingsReviews = trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
      if(ratingsReviews.getTotalReviews() == 0){
         ratingsReviews = scrapAlternativeRating(internalId);
      }

      return ratingsReviews;
   }

   private String alternativeRatingFetch(String internalId){

      StringBuilder apiRating = new StringBuilder();

      apiRating.append("https://trustvox.com.br/widget/shelf/v2/products_rates?codes[]=")
         .append(internalId)
         .append("&store_id=71450&callback=_tsRatesReady");

      Request request = RequestBuilder.create().setUrl(apiRating.toString()).build();

      return this.dataFetcher.get(session, request).getBody();
   }

   private RatingsReviews scrapAlternativeRating(String internalId){

      RatingsReviews ratingsReviews = new RatingsReviews();

      String ratingResponse = alternativeRatingFetch(internalId);

      // Split in parentheses
      String[] responseSplit = ratingResponse.split("\\s*[()]\\s*");

      JSONObject rating;

      if(responseSplit.length > 1){
         String ratingFormatted = responseSplit[1];
         rating = CrawlerUtils.stringToJson(ratingFormatted);

         JSONArray productRateArray = rating.optJSONArray("products_rates");

         int totalReviews = ((JSONObject) productRateArray.get(0)).optInt("count");

         double avgReviews = ((JSONObject) productRateArray.get(0)).optDouble("average");

         ratingsReviews.setTotalRating(totalReviews);
         ratingsReviews.setTotalWrittenReviews(totalReviews);
         ratingsReviews.setAverageOverallRating(avgReviews);
      }
      return ratingsReviews;
   }
}

