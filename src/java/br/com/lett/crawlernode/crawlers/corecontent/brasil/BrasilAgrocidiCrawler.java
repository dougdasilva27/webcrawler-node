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
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilAgrocidiCrawler extends Crawler {

  public BrasilAgrocidiCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    CommonMethods.saveDataToAFile(doc, Test.pathWrite + "e.html");
    List<Product> products = new ArrayList<>();
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".principal div[data-produto-id]", "data-produto-id");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=sku]", false);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".principal .nome-produto", false);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".principal .preco-produto .preco-promocional", null, false, ',', session);
      Prices prices = crawlPrices(doc, price);
      boolean available = !doc.select(".principal .disponivel").isEmpty();;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li a strong");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fotorama img", Arrays.asList("src"), "https:", "www.terabyteshop.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fotorama img", Arrays.asList("src"), "https:",
          "www.terabyteshop.com.br", primaryImage);
      String description = crawlDescription(doc);

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
          .setMarketplace(new Marketplace())
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlDescription(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

  private Prices crawlPrices(Document doc, Float price) {
    // TODO Auto-generated method stub
    return null;
  }

  private Float crawlPrice(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

  private String crawlInternalPid(Document doc) {
    // TODO Auto-generated method stub
    return null;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".secao-principal .produto").isEmpty();
  }

}
