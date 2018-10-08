package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaRibeiroCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.belezanaweb.com.br/";

  public ArgentinaRibeiroCrawler(Session session) {
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
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product_name", true);
      Prices prices = crawlPrices(doc);
      Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);

      boolean available = !doc.select("#tableComprarButtom .atg_store_productAvailability").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#atg_store_breadcrumbs_mod li:not(:first-child) > a", false);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".atg_store_productImage #imgAux > a", Arrays.asList("data-zoom-image", "src"),
          "https:", "minicuotas.ribeiro.com.ar");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".atg_store_productImage #imgAux > a",
          Arrays.asList("data-zoom-image", "data-image"), "https:", "minicuotas.ribeiro.com.ar", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-description", ".product-characteristics"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-sku").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Elements scripts = doc.select("script[type=\"text/javascript\"]");
    for (Element e : scripts) {
      String script = e.html().replace(" ", "").toLowerCase();

      if (script.contains("varproductid=")) {
        internalId = CrawlerUtils.extractSpecificStringFromScript(script, "varproductid=", ";", false).replace("\"", "").trim();
        break;
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
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();

    Map<Integer, Float> installmentPriceMap = new TreeMap<>();

    Element priceFrom = doc.selectFirst("div[itemprop=offers] .precio_big_indivGris");
    if (priceFrom != null) {
      prices.setPriceFrom(MathUtils.parseDoubleWithDot(priceFrom.ownText()));
    }

    Elements parcels = doc.select("#planLista li");
    for (Element e : parcels) {
      Element installment = e.selectFirst("input[id^=cantCuotas]");
      Element installmentValue = e.selectFirst("input[id^=pxcuota]");

      if (installment != null && installmentValue != null) {
        String textInstallment = installment.val().replaceAll("[^0-9]", "").trim();
        String textInstallmentValue = installmentValue.val().replaceAll("[^0-9.]", "").trim();

        if (!textInstallment.isEmpty() && !textInstallmentValue.isEmpty()) {
          installmentPriceMap.put(Integer.parseInt(textInstallment), Float.parseFloat(textInstallmentValue));
        }
      }
    }

    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

    return prices;
  }

}
