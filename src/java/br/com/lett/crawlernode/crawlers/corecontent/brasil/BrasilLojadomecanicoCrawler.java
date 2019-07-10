package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
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
import models.prices.Prices;


public class BrasilLojadomecanicoCrawler extends Crawler {
  private final String HOME_PAGE = "http://www.lojadomecanico.com.br/";

  public BrasilLojadomecanicoCrawler(Session session) {
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

      JSONObject jsonIdSkuPrice = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.chaordic_meta=", ";", true, false);
      JSONObject jsonNameDesc = CrawlerUtils.selectJsonFromHtml(doc, ".container script[type=\"application/ld+json\"]", "", null, false, false);

      String internalId = jsonIdSkuPrice.has("pid") ? jsonIdSkuPrice.getString("pid") : null;
      String internalPid = jsonIdSkuPrice.has("sku") ? jsonIdSkuPrice.getString("sku") : null;
      String name = scrapName(jsonNameDesc, doc, internalId);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".cateMain_breadCrumbs .breadCrumbNew li span[itemprop=\"name\"]", false);
      String description = scrapDescription(jsonNameDesc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image .img-produto-min li a", Arrays.asList("data-image"), "https:",
          "www.lojadomecanico.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image .img-produto-min li a", Arrays.asList("data-image"),
          "https:", "www.lojadomecanico.com.br", primaryImage);
      Float price = CrawlerUtils.getFloatValueFromJSON(jsonIdSkuPrice, "price");
      Prices prices = scrapPrices(doc, price, jsonIdSkuPrice);
      boolean available = doc.selectFirst("#btn-comprar-product") != null;
      Integer stock = scrapStock(doc, available);;

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
    return doc.selectFirst("#product .primary-box") != null;
  }

  private String scrapName(JSONObject json, Document doc, String internalId) {
    String name = null;

    if (json.has("name") && !json.isNull("name")) {
      name = json.get("name").toString();

      String nameVariation = CrawlerUtils.scrapStringSimpleInfo(doc, "button[data-id=\"" + internalId + "\"][disabled=\"disabled\"]", false);
      if (nameVariation != null) {
        name += " " + nameVariation;
      }
    }

    return name;
  }

  private String scrapDescription(JSONObject json) {
    String description = null;

    if (json.has("description")) {
      description = json.getString("description");

      // Decodificando string
      description = description.replace("&lt;", "<");
      description = description.replace("&gt;", ">");
    }

    return description;
  }

  private Prices scrapPrices(Document doc, Float price, JSONObject json) {
    Prices prices = new Prices();
    Elements elements = doc.select(".modal-content .tab-container #pgCartao tbody tr");

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();

      prices.setBankTicketPrice(price);

      if (json.has("product")) {
        JSONObject innerJson = json.getJSONObject("product");

        if (innerJson.has("old_price")) {
          prices.setPriceFrom(innerJson.getDouble("old_price"));
        }
      }

      for (Element e : elements) {
        br.com.lett.crawlernode.util.Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "total", false);
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
    }

    return prices;
  }

  private Integer scrapStock(Document doc, boolean available) {
    Integer stock = 0;

    if (available) {
      stock = null;
      Element e = doc.selectFirst("#btn-comprar-product");

      if (e != null && e.hasAttr("data-product")) {
        JSONObject json = new JSONObject(e.attr("data-product"));

        if (json.has("maxStock")) {
          stock = json.getInt("maxStock");
        }
      }
    }

    return stock;
  }
}
