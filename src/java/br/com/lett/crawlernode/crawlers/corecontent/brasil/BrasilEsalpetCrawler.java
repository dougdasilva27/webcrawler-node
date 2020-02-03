package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilEsalpetCrawler extends Crawler {
  
  private static final String IMAGE_HOST = "assets.xtechcommerce.com";
  
  public BrasilEsalpetCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".prod-action [name=id]", "value");
      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "var productvariants_settings_" + internalId + " = ", ";", false, false);
      
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=product_sku]", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=name]", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".product_price", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li:not(:last-child)", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".prod-image #zoom", Arrays.asList("data-zoom-image", "src"), "https", IMAGE_HOST);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".prod-image-thumbs > a", Arrays.asList("data-zoom-image", "data-image"), "https", IMAGE_HOST, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".prod-excerpt", ".prod-description"));
      Integer stock = json.has("overall_quantity") && json.get("overall_quantity") instanceof Integer ? json.getInt("overall_quantity") : 0;
      boolean available = stock > 0 || ((json.has("allow_os_purchase") && json.get("allow_os_purchase") instanceof Boolean) && json.getBoolean("allow_os_purchase"));
      RatingsReviews ratingsReviews = scrapRating(internalId, doc);
          
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
          .setRatingReviews(ratingsReviews)
          .build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#product") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      List<Card> cards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.DINERS, Card.ELO, Card.AMEX);
      for(Card card : cards) {
        prices.insertCardInstallment(card.toString(), installmentPriceMap);
      }    
    }
    
    return prices;
  }
  
  private RatingsReviews scrapRating(String internalId, Document doc) {
    TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "109465", logger);
    return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
  }
}
