package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilAnimalshowstoreCrawler extends Crawler {
  
  private static final String HOME_PAGE = "www.animalshowstore.com.br";
  
  public BrasilAnimalshowstoreCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
   
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".product script", "dataLayer.push(", ");", false, true);
      
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "head [name=\"itemId\"]", "content");
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#info-product #productInternalCode", "content");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#info-product .brandNameProduct", true) + " " +
          CrawlerUtils.scrapStringSimpleInfo(doc, "#info-product .name", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price-to", null, false, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadcrumbs > span:not(:last-child) > a > span", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".images .photo #Zoom1", Arrays.asList("href"), "https:", HOME_PAGE);
      String secondaryImages = scrapSecondaryImages(doc, primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#description", "#panCaracteristica"));
      Integer stock = null;
      boolean available = scrapAvailability(json);
      Marketplace marketplace = null;
      String ean = json.has("RKProductEan13") && !json.isNull("RKProductEan13") ? json.get("RKProductEan13").toString() : null;
      RatingsReviews ratingsReviews = scrapRatingReviews(doc);

      List<String> eans = new ArrayList<>();
      eans.add(ean);
      
      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setStock(stock)
          .setMarketplace(marketplace)
          .setEans(eans)
          .setRatingReviews(ratingsReviews)
          .build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product") != null;
  }
  
  private String scrapSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    
    Elements images = doc.select(".images > ul > li:not([style=\"display:none\"])");    
    for(int i = 1; i <= images.size(); i++) {
      secondaryImagesArray.put(primaryImage.replace("/Ampliada/", "/Ampliada" + i + "/"));
    }
    
    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }
    
    return secondaryImages;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if (price != null) {
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-from", null, false, ',', session));
      prices.setBankTicketPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-avista", null, false, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("#lblParcelamento", doc, false, "x de", "juros", false, ',');
      
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }
      
      pair = CrawlerUtils.crawlSimpleInstallment("#lblParcelamento #lblOutroParc", doc, false, "x de", "juros", false, ',');
      
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }
      
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private boolean scrapAvailability(JSONObject json) {
    boolean available = false;
    String aval = JSONUtils.getStringValue(json, "RKProductAvailable");
    
    if(aval != null && aval.equals("1")) {
      available = true;
    }

    return available;
  }
  
  private RatingsReviews scrapRatingReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtml(doc, "#reviewaggregate .count", null, null, false, false, 0);    
    Integer totalWrittenReviews = doc.select("#opinion #panOpiniao").size();
    AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
    Double avgRating = computeAvgRating(advancedRatingReview);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    
    return ratingReviews;
  }
  
  private Double computeAvgRating(AdvancedRatingReview advancedRatingReview) {
    
    int total5 = advancedRatingReview.getTotalStar5();
    int total4 = advancedRatingReview.getTotalStar4();
    int total3 = advancedRatingReview.getTotalStar3();
    int total2 = advancedRatingReview.getTotalStar2();
    int total1 = advancedRatingReview.getTotalStar1();
    
    int totalTotal = total5 + total4 + total3 + total2 + total1;
    
    if(totalTotal == 0) {
      return 0.0;
    }
    
    return (total5 * 5.0 + total4 * 4.0 + total3 * 3.0 + total2 * 2.0 + total1 * 1.0) / totalTotal;
  }
  
  private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;
    
    Elements reviews = doc.select("#opinion #panOpiniao .rating [itemprop=ratingValue]");
    
    for(Element review : reviews) {
      if(review.hasAttr("content")) {
        String content = review.attr("content").replaceAll("[^0-9]+", "");
        
        if(!content.isEmpty()) {
          Integer val = Integer.parseInt(content);     
          
          switch(val) {
            case 1: star1 += 1; 
            break;
            case 2: star2 += 1; 
            break;
            case 3: star3 += 1; 
            break;
            case 4: star4 += 1; 
            break;
            case 5: star5 += 1; 
            break;
          }
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
