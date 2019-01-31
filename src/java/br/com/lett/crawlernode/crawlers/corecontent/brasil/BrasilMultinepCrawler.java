package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 12/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMultinepCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.multinep.com.br/";

  public BrasilMultinepCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "#variacaoPreco", true);
      Prices prices = crawlPrices(price, doc);
      boolean available = doc.select(".botao-nao_indisponivel").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb .breadcrumb-item:not(:first-child) > a");
      String primaryImage =
          CrawlerUtils.scrapSimplePrimaryImage(doc, ".produto-imagem a[href]", Arrays.asList("href"), "https:", "images.tcdn.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".produto-imagem-miniaturas a", Arrays.asList("href"), "https:",
          "images.tcdn.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".description"));
      String ean = crawlEan(doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setEan(ean).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-name").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    JSONArray array = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "dataLayer=", null, true, true);
    if (array.length() > 0) {
      JSONObject json = array.getJSONObject(0);

      if (json.has("idProduct")) {
        internalId = json.get("idProduct").toString();
      }
    }

    return internalId;
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
      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, "#precoDe", false));
      prices.setBankTicketPrice(price);
    }

    return prices;
  }

  private String crawlEan(Document doc) {
    String ean = null;
    JSONArray arr = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "dataLayer=", null, true, false);

    if (arr.length() > 0) {
      JSONObject obj = arr.getJSONObject(0);

      if (obj.has("EAN")) {
        ean = obj.getString("EAN");
      }
    }

    return ean;
  }
}
