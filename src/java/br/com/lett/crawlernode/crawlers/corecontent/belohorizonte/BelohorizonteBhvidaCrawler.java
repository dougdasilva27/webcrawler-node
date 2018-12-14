package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class BelohorizonteBhvidaCrawler extends Crawler {

  public BelohorizonteBhvidaCrawler(Session session) {
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
      String internalPid =crawlInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#detalhes-mini > ul > li h1", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".preco strong", false);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      Prices prices = crawlPrices(price, doc);
      
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery img", Arrays.asList("src"), "https:", "www.larebajavirtual.com");

      String secondaryImages =
          CrawlerUtils.scrapSimpleSecondaryImages(doc, ".ad-thumb-list li a img", Arrays.asList("src"), "https:", "www.larebajavirtual.com", primaryImage);


      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
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
    return !doc.select(".det_carrinho").isEmpty();
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element serchedId = doc.selectFirst("#detalhes-mini > ul > li:last-child");
    if(serchedId != null) {
      internalPid = serchedId.ownText();
    }

    return internalPid;
  }
  
  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element serchedId = doc.selectFirst("#frmcarrinho #codigo");
    if(serchedId != null) {
      internalId = serchedId.val().trim();
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

    if (price != null) {
      Map<Integer, Float> installmentPriceMapShop = new HashMap<>();
      installmentPriceMapShop.put(1, price);
      Elements elements = doc.select(".parcelmento ul");
      for (Element element : elements) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, element, true);

        if (!pair.isAnyValueNull()) {
          installmentPriceMapShop.put(pair.getFirst(), pair.getSecond());
        }
      }
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapShop);
    }

    return prices;
  }
}
