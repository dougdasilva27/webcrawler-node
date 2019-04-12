package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.prices.Prices;

public class BrasilLettmagentoCrawler extends Crawler {

  private static final String HOME_PAGE = "http://3.91.55.126/";


  public BrasilLettmagentoCrawler(Session session) {
    super(session);
  }
  
  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }
  
  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=\"product\"]", "value");
      String internalPid = null;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name h1", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li[class]", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-image#image-main", Arrays.asList("src"), "https", "3.91.55.126");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".gallery-image", Arrays.asList("src"), "https", "3.91.55.126", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-shop .short-description .std"));
      Integer stock = 40;
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".price-info .price-box .price", null, true, ',', session);
      Prices prices = null;
      boolean available = doc.selectFirst(".add-to-box .add-to-cart-buttons .btn-cart") != null;
  
      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).build();
  
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("input[name=\"product\"]") != null;
  }
}
