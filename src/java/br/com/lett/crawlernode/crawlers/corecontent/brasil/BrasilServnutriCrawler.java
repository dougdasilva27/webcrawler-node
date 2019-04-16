package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilServnutriCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.servnutri.com.br/";

  public BrasilServnutriCrawler(Session session) {
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

      String internalId = getIdFromUrl(doc, ".product-information button");
      String internalPid = crawlAttrString(doc, ".product-information button", "data-product_sku");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-information h2", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".product-information .woocommerce-Price-amount", true);
      Prices prices = crawlPrices(doc, price);
      boolean available = checkAvaliability(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product-information p a[href]");
      String primaryImage =
          CrawlerUtils.scrapSimplePrimaryImage(doc, ".view-product img", Arrays.asList("src", "srcset"), "http//", "www.servnutri.com.br");
      String secondaryImages = null; // Didnt have secondary images when it was made
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".single-blog-post", ".shop_attributes"));

      List<String> variations = getVariations(doc);

      if (!variations.isEmpty()) {
        for (String s : variations) {
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId + "-" + s).setInternalPid(internalPid)
              .setName(name + " - " + s.toUpperCase()).setPrice(price).setPrices(prices).setAvailable(available)
              .setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(new Marketplace())
              .build();

          products.add(product);
        }
      } else {
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-details").isEmpty();
  }

  private List<String> getVariations(Document doc) {
    Elements variations = doc.select(".product-information #pa_sabor option");
    List<String> vals = new ArrayList<>();

    if (!variations.isEmpty()) {
      variations.remove(0);

      for (Element e : variations) {
        vals.add(e.text().toLowerCase().trim());
      }
    }

    return vals;
  }

  /**
   * Get Attr text from element selected.
   * 
   * @param doc
   * @param selector
   * @param attr
   * @return
   */
  private String crawlAttrString(Document doc, String selector, String attr) {
    String internalId = null;
    Element infoElement = doc.selectFirst(selector);

    if (infoElement != null) {
      internalId = infoElement.attr(attr);

      if (internalId.isEmpty())
        return null;
    }

    return internalId;
  }

  private static String getIdFromUrl(Document doc, String selector) {
    String internalId = null;
    Element infoElement = doc.selectFirst(selector);

    if (infoElement != null) {
      String aux = infoElement.attr("onclick");
      String search = "add-to-cart=";

      if (!aux.contains(search)) {
        search = "post_id_loja=";
      }

      if (aux.contains(search)) {
        int x = aux.indexOf(search) + search.length();
        String finalIndex = "'";

        if (aux.endsWith(finalIndex)) {
          int y = aux.indexOf(finalIndex, x);

          internalId = aux.substring(x, y).trim();
        } else {
          internalId = aux.substring(x).trim();
        }
      }
    }

    return internalId;
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);
    }

    return prices;
  }

  private boolean checkAvaliability(Document doc) {
    return doc.selectFirst(".product-information .btn-fefault.cart .fa-shopping-cart") != null;
  }
}
