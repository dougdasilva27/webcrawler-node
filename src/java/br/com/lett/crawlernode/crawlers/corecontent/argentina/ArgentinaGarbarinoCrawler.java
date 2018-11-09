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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class ArgentinaGarbarinoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.garbarino.com/";

  public ArgentinaGarbarinoCrawler(Session session) {
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
      String internalPid = null;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".gb-breadcrumb > ul > li:not(:first-child) > a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gb-js-main-image-thumbnail > a", Arrays.asList("href", "data-standart"),
          "https:", "d34zlyc2cp9zm7.cloudfront.net");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".gb-js-main-image-thumbnail > a", Arrays.asList("href", "data-standart"),
          "https:", "d34zlyc2cp9zm7.cloudfront.net", primaryImage);
      String description = crawlDescription(doc);
      Marketplace marketplace = crawlMarketplace();

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage.replace(".webp_1000", ".jpg"))
          .setSecondaryImages(secondaryImages.replace(".webp_1000", ".jpg")).setDescription(description).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select(".title-product").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.selectFirst(".gb--gray");
    if (internalIdElement != null) {
      internalId = internalIdElement.attr("data-product-id");
    }

    return internalId;
  }

  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.selectFirst(".title-product > h1");

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }
    return name;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;

    Element specialPrice = doc.selectFirst("#final-price");

    if (specialPrice != null) {
      price = MathUtils.parseFloatWithComma(specialPrice.ownText());
    }
    return price;
  }

  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setPriceFrom(crawlPriceFrom(doc));

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Elements installments =
          doc.select(".payments .payment-method:first-child .gb-detail-fees-detail[data-payment-id=1] .gb-detail-fees-table-body ul li:first-child");
      for (Element e : installments) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true, "de");

        if (!pair.isAnyValueNull()) {
          installmentPriceMap.put(pair.getFirst(), pair.getSecond());
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }

    return prices;
  }

  private Double crawlPriceFrom(Document doc) {
    Double price = null;

    Element from = doc.selectFirst(".value-note > del");
    if (from != null) {
      price = MathUtils.parseDoubleWithComma(from.ownText());
    }
    return price;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = false;

    Element outOfStockElement = document.selectFirst(".gb-button");
    if (outOfStockElement != null) {
      available = true;
    }

    return available;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element techElement = doc.selectFirst(".gb-tech-spec");

    if (techElement != null) {
      description.append(techElement.html());
    }

    Element descriptionElement = doc.selectFirst("gb-description");

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    return description.toString();
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

}
