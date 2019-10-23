package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 23/10/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilAbcdaconstrucaoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.abcdaconstrucao.com.br/";

  public BrasilAbcdaconstrucaoCrawler(Session session) {
    super(session);
  }

  @Override
  protected Object fetch() {
    String id = CommonMethods.getLast(session.getOriginalURL().split("\\?")[0].split("/"));
    String payload = "c=detalhe-produto&id=" + id;

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
    String urlToFetch = "https://www.abcdaconstrucao.com.br/ajax/produtos.ajax.php";

    Request request = RequestBuilder.create()
        .setUrl(urlToFetch)
        .setCookies(cookies)
        .setHeaders(headers)
        .setPayload(payload)
        .build();

    return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
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

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "a.desejo[id]", "id");
      String internalPid = scrapInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.title-product", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbAbc a:not(:first-child)");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#galeriaThumb > a", Arrays.asList("data-zoom-image", "data-image"), "https:",
          "vendas.digitalabc.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#galeriaThumb > a", Arrays.asList("data-zoom-image", "data-image"),
          "https:", "vendas.digitalabc.com.br", primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("body > div > h2.subtitle, body > div > [style]"));

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrices(new Prices())
          .setAvailable(false)
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

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("a.desejo[id]") != null;
  }

  private String scrapInternalPid(Document doc) {
    String internalPid = null;

    String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".box-white-product > p", true);
    if (brand != null) {
      internalPid = CommonMethods.getLast(brand.split(":")).trim();
    }

    return internalPid;
  }
}
