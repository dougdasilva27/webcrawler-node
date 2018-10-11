package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
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
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gb-main-detail-gallery-grid-img-full img", Arrays.asList("src"), "https:",
          "d3lfzbr90tctqz.cloudfront.net");
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Marketplace marketplace = crawlMarketplace();

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(marketplace).build();

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

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.NARANJA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
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

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.selectFirst(".gb-js-main-image-thumbnail");

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();

      if (!primaryImage.startsWith("http;")) {
        primaryImage = "https:" + primaryImage;
      }

    }
    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = doc.select(".gb-main-detail-gallery-thumbs-list > ul > li > a");

    if (!imagesElement.isEmpty()) {
      for (int i = 1; i < imagesElement.size(); i++) { // first index and last index is the primary image
        String image = imagesElement.get(i).attr("href").trim();

        if (!image.startsWith("http;")) {
          image = "https:" + image;
        }
        secondaryImagesArray.put(image);
      }
    }

    Elements imgElements = doc.select(".gb-main-detail-gallery-grid-list > li > img");

    if (!imgElements.isEmpty()) {
      for (int i = 1; i < imgElements.size(); i++) { // first index and last index is the primary image
        String image = imgElements.get(i).attr("src").trim();

        if (!image.startsWith("http;")) {
          image = "https:" + image;
        }
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
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
