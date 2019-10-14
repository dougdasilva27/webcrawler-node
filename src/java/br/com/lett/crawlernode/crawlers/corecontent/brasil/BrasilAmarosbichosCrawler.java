package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.Marketplace;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilAmarosbichosCrawler extends Crawler {
  
  private static final String HOME_PAGE = "www.petshopamarosbichos.com.br";
  
  public BrasilAmarosbichosCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".cod-sku [itemprop=\"sku\"]", true);
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-container[data-product-id]", "data-product-id");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".container .title-page-product", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product .product-sku-information meta[itemprop=\"price\"]", "content", false, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrump-container > ul > li span[itemprop=\"title\"]", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-sku-image .image-highlight a.main-product img", 
          Arrays.asList("src"), "https:", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-sku-image .image-highlight a.main-product img", 
          Arrays.asList("src"), "https:", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".descriptions"));
      Integer stock = null;
      boolean available = doc.selectFirst(".buybox .compra-wrapper") != null;
      Marketplace marketplace = null;
      Offers offers = null;
      RatingsReviews ratingsReviews = scrapRatingReviews(doc);
          
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
          .setOffers(offers)
          .setRatingReviews(ratingsReviews)
          .build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#frontend-product .product-container") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      
      prices.setBankTicketPrice(
          CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product .product-sku-information [itemprop=\"offers\"] .cash-payment", null, false, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".parcel-price .cash-payment span", doc, true, "x de", "", true);
      
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);      
    }
    
    return prices;
  }
  
  private RatingsReviews scrapRatingReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"ratingCount\"]", "content", 0);
    Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".comments-wrapper [itemprop=\"ratingValue\"]", "", false, '.', session);
    Integer totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"reviewCount\"]", "content", 0);
    AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    
    return ratingReviews;
  }
  
  private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;
    
    Elements reviews = doc.select(".product-comment-list > .customer-comments .stars > .rating-stars");
    
    for(Element review : reviews) {
      if(review.hasAttr("data-score")) {
        Integer val = Integer.parseInt(review.attr("data-score").replaceAll("[^0-9]+", ""));     
        
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
    
    return new AdvancedRatingReview.Builder()
        .totalStar1(star1)
        .totalStar2(star2)
        .totalStar3(star3)
        .totalStar4(star4)
        .totalStar5(star5)
        .build();
  }
}
