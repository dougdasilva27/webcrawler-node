package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BrasilCasadofazendeiroCrawler extends Crawler {
  
  private static final String HOME_PAGE = "https://www.casafazendeiro.com.br/";
  
  public BrasilCasadofazendeiroCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = scrapProductId(doc);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info > ul > li > span", false);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#content h1", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#content .price", null, false, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li:not(:last-child) > a", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".thumbnails > li > .thumbnail", Arrays.asList("href"), "https:", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".thumbnails > li > .thumbnail", Arrays.asList("href"), "https:", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("div#tab-description"));
      Integer stock = null;
      boolean available = doc.selectFirst("#product #button-cart") != null;
      Marketplace marketplace = null;
          
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
          .build();
      
      products.add(product);
        
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("[class*=\"product-product\"]") != null;
  }
  
  private String scrapProductId(Document doc) {
    String productId = null;
    Element productIdElement = doc.selectFirst("[class*=\"product-product\"]");
    
    if(productIdElement != null) {
      for(String classe : productIdElement.classNames()) {
        if(classe.startsWith("product-product-")) {
          productId = classe.substring("product-product-".length());
        }
      }
    }
    
    return productId;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(CrawlerUtils.scrapDoublePriceFromHtml(doc, "#content .avista", null, false, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(
          "#content .microdata ul li .small", doc, false, "x", "", true);
      
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }
      
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }
    
    return prices;
  }
}
