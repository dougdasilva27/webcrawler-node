package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import models.Marketplace;
import models.prices.Prices;

public class ColombiaLarebajaCrawler extends Crawler{

  public ColombiaLarebajaCrawler(Session session) {
    super(session);
    // TODO Auto-generated constructor stub
  }
  

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".descripciones h1", true);
      
      Float priceUnique = CrawlerUtils.scrapSimplePriceFloat(doc, ".pricened", false);
      Float price = priceUnique == null ? 
          CrawlerUtils.scrapSimplePriceFloat(doc, "div .fraccionado_columns td[valign=bottom]:not(.container_gray_fracc) .ahora", false) : priceUnique;
      
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      Prices prices = crawlPrices(price, doc);
      
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery img", Arrays.asList("src"), "https:", "www.larebajavirtual.com");

      String secondaryImages =
          CrawlerUtils.scrapSimpleSecondaryImages(doc, ".ad-thumb-list li a img", Arrays.asList("src"), "https:", "www.larebajavirtual.com", primaryImage);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(null)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {    
    return !doc.select(".product_detail").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    Element serchedId = doc.selectFirst(".control_cant_detalle input[data-producto]");

    if(serchedId == null) {
      serchedId = doc.selectFirst(".detPproduct input[data-producto]");
    }

    if(serchedId != null) {
      internalId = serchedId.attr("data-producto").trim(); 
    }
    
    return internalId;
  }
  
  private boolean crawlAvailability(Document doc) {    
    return doc.select("btn btn-primary btn-block") != null;
  }

  public static CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb li + li");

    for (Element e : elementCategories) {
      categories.add(e.text().replace(">", "").trim());
    }

    Element lastCategory = document.selectFirst(".breadcrumb active");
    if (lastCategory != null) {
      categories.add(lastCategory.ownText().trim());
    }
    
    return categories;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
 
  
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
    Element fractioned = doc.selectFirst("div .fraccionado_columns");
    
    if (price != null) {
      installmentPriceMapShop.put(1, price);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);
      if(fractioned != null) {
        prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(fractioned, "td[valign=bottom]:not(.container_gray_fracc) .strike", false));
        
      } else {        
        prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, "[valing=middle] .strike2", false));
      }

    }

    return prices;
  }


}
