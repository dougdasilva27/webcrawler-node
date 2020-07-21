package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilUninaturalCrawler extends Crawler {
  
  public BrasilUninaturalCrawler(Session session) {
    super(session);
  }

  private static final String SELLER_FULL_NAME = "Uni Natural";
  protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
        Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

  private Double scrapOldPriceFromHTML(Document doc) {
    Double priceFrom = 0D;

    Elements scripts = doc.select(".ajustaLista script");
    for (Element e : scripts) {
      String scriptHTML = e.html();
      if (scriptHTML != null && scriptHTML.contains("var aProductOnlyOneGrid=[]")) {
        String offersString = CrawlerUtils.extractSpecificStringFromScript(scriptHTML, "priceOri:", false, ", priceNum:", false);
        priceFrom = offersString != null ? MathUtils.parseDoubleWithDot(offersString) : null;
      }
    }
    return priceFrom;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (doc.selectFirst(".FCGridMain") != null) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", null, null, false, false);
      JSONObject jsonOffers = JSONUtils.getJSONValue(jsonInfo, "offers");

      Double oldPrice = scrapOldPriceFromHTML(doc);


      String internalId = jsonInfo.optString("sku");
      String name = jsonInfo.optString("name");
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".FCCodProdGrid .EstPathCat ul li span", true);
      String primaryImage = jsonInfo.optString("image");
      String description = jsonInfo.optString("description");

      String availability = jsonOffers.optString("availability");
      boolean available = availability.equals("http://schema.org/InStock");
      Offers offers = available ? scrapOffer(jsonOffers, oldPrice) : new Offers();
      RatingsReviews ratingReviews = scrapRatingReviews(jsonInfo);

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
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingReviews)
            .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  private Offers scrapOffer(JSONObject jsonOffers, Double oldPrice) throws OfferException, MalformedPricingException {
    Offers offers = new Offers();
    Pricing pricing = scrapPricing(jsonOffers, oldPrice);
    List<String> sales = new ArrayList<>();

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

  private Pricing scrapPricing(JSONObject jsonOffers, Double oldPrice) throws MalformedPricingException {
    Double spotlightPrice = jsonOffers.optDouble("price");
    CreditCards creditCards = scrapCreditCards(spotlightPrice);
    BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

    return Pricing.PricingBuilder.create()
          .setPriceFrom(oldPrice)
          .setSpotlightPrice(spotlightPrice)
          .setCreditCards(creditCards)
          .setBankSlip(bankSlip)
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



  private RatingsReviews scrapRatingReviews(JSONObject jsonInfo) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());

    JSONObject aggregateRating = JSONUtils.getJSONValue(jsonInfo, "aggregateRating");

    if (aggregateRating != null && !aggregateRating.isEmpty()) {

      Integer totalNumOfEvaluations = aggregateRating.optInt("reviewCount");
      Double avgRating = aggregateRating.optDouble("ratingValue");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(jsonInfo);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    }

    return ratingReviews;
  }

  private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject jsonInfo) {
    int star1 = 0;
    int star2 = 0;
    int star3 = 0;
    int star4 = 0;
    int star5 = 0;

    JSONArray reviews = JSONUtils.getJSONArrayValue(jsonInfo, "review");

    if (reviews != null && !reviews.isEmpty()) {

      for (Object review : reviews) {

        JSONObject reviewbyclient = (JSONObject) review;
        JSONObject reviewRating = JSONUtils.getJSONValue(reviewbyclient, "reviewRating");

        switch (reviewRating.optInt("ratingValue")) {
          case 5:
            star5 += 1;
            break;
          case 4:
            star4 += 1;
            break;
          case 3:
            star3 += 1;
            break;
          case 2:
            star2 += 1;
            break;
          case 1:
            star1 += 1;
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

