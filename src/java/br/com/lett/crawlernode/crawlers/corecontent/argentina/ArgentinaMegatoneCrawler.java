package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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

/**
 * Date: 10/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaMegatoneCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.megatone.net/";

  public ArgentinaMegatoneCrawler(Session session) {
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

      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "#MainContent_lblCodProducto", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".mT15 h1", false);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "#divPagosTable .mT10:not(.mB10) > span:not(.tdTachado):not(.cajaAhorroAzul)", true);

      if (price == null) {
        price = CrawlerUtils.scrapSimplePriceFloat(doc, "#divPagosTable .AjusteF50", true);
      }

      Prices prices = crawlPrices(doc, price);
      boolean available = !doc.select(".divBtnComprar > a").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".divTitulos a.nB");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#fotoprincipal img#imgNormal", Arrays.asList("data-zoom-image", "src"),
          "https:", "www.megatone.net");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#gal1 a", Arrays.asList("data-zoom-image", "data-image"), "https:",
          "www.megatone.net", primaryImage);
      String description = crawlDescription(doc, internalId);

      String ean = crawlEan(doc);
      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("#MainContent_lblCodProducto").isEmpty();
  }

  private String crawlDescription(Document doc, String internalId) {
    StringBuilder str = new StringBuilder();

    str.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#MainContent_prodInfo #DivDescripcion")));

    String url = "https://www.megatone.net/Producto.aspx/ObtenerHTMLEspecificacionesProductos";
    String payload = new JSONObject().put("CodProducto", internalId).toString();

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json; charset=UTF-8");

    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).build();
    JSONObject d = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

    if (d.has("d")) {
      str.append(Jsoup.parse(d.get("d").toString()));
    }

    return str.toString();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, "#divPagosTable .mT10:not(.mB10) > span.tdTachado", true));

      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment("#divPagosTable .cE41F13", doc, false);
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }

      Pair<Integer, Float> installment2 = CrawlerUtils.crawlSimpleInstallment("#divPagosTable .cE41F13.mT10", doc, true, "pago:");
      if (!installment2.isAnyValueNull()) {
        installmentPriceMap.put(installment2.getFirst(), installment2.getSecond());
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }
    return prices;
  }

  private String crawlEan(Document doc) {
    String ean = null;
    Element e = doc.selectFirst("script[data-flix-ean]");

    if (e != null) {
      String aux = e.attr("data-flix-ean");

      if (!aux.isEmpty()) {
        ean = aux;
      }
    }

    return ean;
  }
}
