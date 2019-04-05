package br.com.lett.crawlernode.crawlers.corecontent.peru;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
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
 * Date: 21/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class PeruGrouponCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.groupon.com.pe/";

  public PeruGrouponCrawler(Session session) {
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

    // Replace "S/." exists because char "." is considered for parse float
    doc = Jsoup.parse(doc.toString().replace("S/.", ""));

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input.unified_discount_id", "value");
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".deal-description"));
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-list .breadcrumb-item:not(:first-child) > a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#ul-gallery-image li > img, #gallery > img",
          Arrays.asList("data-zoom-image", "data-zoom", "data-original", "src"), "https:", "img.stpu.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#ul-gallery-image li > img",
          Arrays.asList("data-zoom-image", "data-zoom", "data-original", "src"), "https:", "img.stpu.com.br", primaryImage);

      Elements variants = doc.select("#deal-info .list-fraction .buying-option[data-end]");
      if (!variants.isEmpty()) {

        for (Element variant : variants) {
          String internalId = crawlVariantId(variant);
          String name = CrawlerUtils.scrapStringSimpleInfo(variant, ".infoFraction .titleOption", true);
          Float price = CrawlerUtils.scrapSimplePriceFloat(variant, ".action-group .priceFraction .valueFraction", false);
          Prices prices = crawlPrices(price, doc);
          boolean available = !variant.select(".btnFraction .select-option:not(.closed)").isEmpty();

          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(new Marketplace()).build();

          products.add(product);
        }

      } else {
        String internalId = internalPid;
        String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=name]", false);
        Float price =
            CrawlerUtils.scrapSimplePriceFloat(doc, ".discount-price-text:not(.old-price) .js-deal-price, .buy-buttons .price-label", false);
        Prices prices = crawlPrices(price, doc);
        boolean available = price != null;

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
    return doc.selectFirst("input.unified_discount_id") != null;
  }

  private String crawlVariantId(Element e) {
    String internalId = null;

    String text = CrawlerUtils.scrapStringSimpleInfo(e, ".infoFraction .option-description", true).toLowerCase();
    if (text.contains("sku") && text.contains(")")) {
      int x = text.indexOf("sku") + 3;
      int y = text.indexOf(')', x);

      internalId = CommonMethods.getLast(text.substring(x, y).split(":")).trim();
    }

    return internalId;
  }

  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc,
          ".discount-price-text.old-price .js-deal-price, .fullPrice .js-deal-fullprice, .price-box .topbar-old-price", false));

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
