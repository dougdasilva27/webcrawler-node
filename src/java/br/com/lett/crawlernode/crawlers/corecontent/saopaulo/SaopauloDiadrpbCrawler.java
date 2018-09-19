package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 12/04/2018
 * 
 * @author gabriel
 *
 */
public class SaopauloDiadrpbCrawler extends Crawler {

  private static final String HOME_PAGE = "https://delivery.dia.com.br/";

  public SaopauloDiadrpbCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("dia_zv_zipcode",
        "04530000%3DRua%20Doutor%20Renato%20Paes%20de%20Barros%2C%20Itaim%20Bibi%20-%20S%C3%A3o%20Paulo%20%2F%20SP");
    cookie.setDomain(".dia.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc) && price != null;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select(".product-reference").first() != null;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element id = document.select(".product-reference").first();
    if (id != null) {
      internalId = id.ownText().trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    Element id = document.select("input[name=product-id]").first();
    if (id != null) {
      internalPid = id.val();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.nameProduct").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Element salePriceElement = document.select(".price-line .withStock .bestPrice .val").first();
    if (salePriceElement != null) {
      price = Float.parseFloat(salePriceElement.attr("content"));
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    return document.select(".priceProduct.withStock").first() != null;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select("#product-showcase-image img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();

      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".thumb-item a");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("data-zoom-image").trim();

      if (!image.startsWith("http")) {
        image = "https:" + image;
      }
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select(".breadcrumb a:not(.first) > span");
    for (Element e : elementCategories) {
      categories.add(e.ownText().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element shortDescription = document.select("#product-description").first();

    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element desc = document.select("#product-specifications").first();

    if (desc != null) {
      description.append(desc.html());
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

}
