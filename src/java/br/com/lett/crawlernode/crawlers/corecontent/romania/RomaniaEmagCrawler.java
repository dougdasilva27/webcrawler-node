package br.com.lett.crawlernode.crawlers.corecontent.romania;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;

public class RomaniaEmagCrawler extends Crawler {



   private final String HOME_PAGE = "https://www.emag.ro/supermarket/d";
   private static final String SELLER_FULL_NAME = "Emag";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public RomaniaEmagCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      String url = session.getOriginalURL();

      Map<String, String> headers = new HashMap<>();
      headers.put("Cookie",
            "EMAGVISITOR=a%3A1%3A%7Bs%3A7%3A%22user_id%22%3Bi%3A2024580195347975914%3B%7D; site_version_11=not_mobile; EMAG_VIEW=not_mobile; ltuid=1599074195.138-b24ce42a01649d8df35e123caf9ffe88362ef632; EMAGUUID=1598879521-291954679-31430.446; _pdr_internal=GA1.2.5464345008.1599074195; eab290=c; profile_token=pftk_7165403916746472080; loginTooltipShown=1; _gcl_au=1.1.1094787864.1599074447; G_ENABLED_IDPS=google; _scid=c538a4af-60d8-41d8-bf9a-0ee452eeed17; _pin_unauth=dWlkPU0yUTNZV1l3WXpZdE1UZ3pNeTAwTURVMExUZzBPVGd0WkRRNE9HSmtOamt5T0RFeiZycD1abUZzYzJV; __gads=ID=ae9aa331d1556e73:T=1599074460:S=ALNI_MZPen0PNzhKsK9sSchpR8Wtq6bxOw; _sctr=1|1599015600000; EMAGROSESSID=d1c99e13d3eee65cf9365ce0c0f80d2d; eab275=a; eab279=a; eab282=a; eab283=a; sr=1920x1080; vp=1920x1008; _rsv=2; _rscd=1; _rsdc=2; listingDisplayId=2; supermarket_delivery_address=%7B%22name%22%3A%22Bucure%5Cu015fti%22%2C%22id%22%3A4954%2C%22delivery_type%22%3A2%2C%22storage_type%22%3A%7B%221%22%3A%221%22%2C%222%22%3A%221%22%2C%223%22%3A%221%22%7D%2C%22delivery_categories%22%3A%7B%22Fructe+si+Legume%22%3A1%2C%22Lactate%2C+Oua+si+Paine%22%3A1%2C%22Carne%2C+Mezeluri+si+Pes+...%22%3A1%2C%22Produse+congelate%22%3A1%2C%22Alimente+de+baza%2C+cons+...%22%3A1%2C%22Cafea%2C+cereale%2C+dulciu+...%22%3A1%2C%22Bauturi+si+tutun%22%3A1%2C%22Ingrijire+copii%22%3A1%2C%22Intretinerea+casei+si++...%22%3A1%2C%22Ingrijire+personala+%22%3A1%2C%22Vinoteca%22%3A1%2C%22Produse+naturale+si+sa+...%22%3A1%7D%7D; supermarket_delivery_zone=%7B%22id%22%3A4954%2C%22name%22%3A%22Bucure%5Cu015fti%22%7D; campaign_notifications={\"4535\":1}; delivery_locality_id=4958; _uetsid=055bd66b1122354e5eef99173501229f; _uetvid=43ccf5f349725a648f90f16e7ac9221d; _pdr_view_id=1599144227-14804.696-563806517; _dc_gtm_UA-220157-3=1");
      System.err.println(headers);

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setCookies(this.cookies).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      return doc;

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".container .page-title") != null) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#main-container input[name=\"product[]\"]", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-header.has-subtitle-info h1", false);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".mrg-sep-sm", ".container.pad-btm-lg"));
         boolean available = doc.selectFirst(".label.label-out_of_stock") == null;
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnail-wrapper .product-gallery-image", Arrays.asList("href"), "https:", "www.emag.ro/");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnail-wrapper .product-gallery-image", Arrays.asList("href"), "https:", "www.emag.ro/", primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li a");
         RatingsReviews ratingReviews = crawlRating(doc);
         Offers offers = available ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingReviews)
               .setOffers(offers)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".page-skin-inner .product-this-deal");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Double concatPrice(Document doc, String cssSelector1, String cssSelector2) {
      Double price = 0D;

      if (cssSelector1 != null && cssSelector2 != null) {

         String firstDecimalPlace = CrawlerUtils.scrapStringSimpleInfo(doc, cssSelector1, true);
         String secondDecimalPlace = CrawlerUtils.scrapStringSimpleInfo(doc, cssSelector2, false);

         if (firstDecimalPlace != null && secondDecimalPlace != null) {
            String priceConcat = firstDecimalPlace + "," + secondDecimalPlace;
            price = MathUtils.parseDoubleWithComma(priceConcat);
         }
      }
      return price;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = 0D;

      if (doc.selectFirst(".product-highlight .product-old-price s") != null) {
         priceFrom = concatPrice(doc, ".product-highlight .product-old-price s", ".product-highlight .product-old-price s sup");
      } else {
         priceFrom = null;
      }

      Double spotlightPrice = concatPrice(doc, ".product-new-price-offer", ".product-new-price-offer sup");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   private RatingsReviews crawlRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, ".small.semibold.font-size-sm.text-muted", false, 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".review-rating-data", null, false, '.', session);
      AdvancedRatingReview adRating = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(adRating);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".reviews-summary-container .reviews-summary-bars .js-rating-bar:last-child");

      for (Element review : reviews) {

         String stringStarNumber = review.attr("data-value");
         Integer numberOfStars = !stringStarNumber.isEmpty() ? Integer.parseInt(stringStarNumber) : 0;

         String elementVoteNumber = review.text();

         if (elementVoteNumber != null) {

            String vN = elementVoteNumber.replaceAll("[^0-9]", "").trim();
            Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

            switch (numberOfStars) {
               case 5:
                  star5 = numberOfVotes;
                  break;
               case 4:
                  star4 = numberOfVotes;
                  break;
               case 3:
                  star3 = numberOfVotes;
                  break;
               case 2:
                  star2 = numberOfVotes;
                  break;
               case 1:
                  star1 = numberOfVotes;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }


}
