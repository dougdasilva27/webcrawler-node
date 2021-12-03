package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PricesmartCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());
   private static final String MAINSELLER = "Price Smart";


   public PricesmartCrawler(Session session) {
      super(session);
   }


   @Override
   protected Object fetch() {

      String club_id = session.getOptions().optString("club_id");
      String country = session.getOptions().optString("country");

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie", "userPreferences=country=" + country + "&selectedClub=" + club_id + "&lang=es");

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).build();
      String response = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(response);

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".row .product-price-small") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "#itemNumber", false);
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#product-name-item", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".pdp-main-image", Arrays.asList("src"), "http://", "pim-img-psmt1.aeropost.com");
         String secondaryImage = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-thumb img", Arrays.asList("src"), "http://", "pim-img-psmt1.aeropost.com", primaryImage);

         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "#collapseOne .card-body", true);
         Integer stock = null;

         boolean available = doc.selectFirst(".btn-add-to-cart-disabled") == null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(updateUrl(this.session.getOriginalURL()))
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String updateUrl(String url) {
      String regex = "/site/(..)/es";
      String updatedUrl = url;
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(url);

      if (matcher.find()) {
         updatedUrl = url.replace(matcher.group(1),session.getOptions().optString("country"));
      }
      return updatedUrl;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setMainPagePosition(1)
         .setSellerFullName(MAINSELLER)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());


      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      return sales;
   }


   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#product-price", null, false, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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
}
