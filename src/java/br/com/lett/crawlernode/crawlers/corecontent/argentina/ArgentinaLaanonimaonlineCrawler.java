package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 25/06/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaLaanonimaonlineCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.laanonimaonline.com";

  public ArgentinaLaanonimaonlineCrawler(Session session) {
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
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
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
    return doc.select("#id_item").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("#id_item").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element pidElement = doc.select("#cont_producto > div > div").first();
    if (pidElement != null) {
      internalPid = CommonMethods.getLast(pidElement.ownText().trim().split(" ")).trim();
    }

    return internalPid;
  }

  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.select("h1.titulo_producto").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;

    Element salePriceElement = doc.select(".precio_carrito span").first();
    Element specialPrice = doc.select(".precio.destacado").first();

    if (specialPrice != null) {
      price = MathUtils.parseFloat(specialPrice.ownText());
    } else if (salePriceElement != null) {
      price = MathUtils.parseFloat(salePriceElement.ownText());
    }

    return price;
  }

  private boolean crawlAvailability(Document doc) {
    return !doc.select(".bt_agregar").isEmpty();
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.select("#img_producto > img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = doc.select("#galeria_img div > a");

    for (int i = 1; i < imagesElement.size() - 1; i++) { // first index and last index is the primary image
      String image = imagesElement.get(i).attr("data-zoom-image").trim();
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = doc.select(".barra_navegacion a.link_navegacion");
    for (Element e : elementCategories) {
      categories.add(e.text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDesc = doc.select("#detalle_producto .texto").first();

    if (shortDesc != null) {
      description.append(shortDesc.html());
    }

    Element descriptionElement = doc.select(".container_tabs").first();

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    return description.toString();
  }

  private Double crawlPriceFrom(Document doc) {
    Double price = null;

    Element from = doc.select(".precio.anterior").first();
    if (from != null) {
      price = MathUtils.parseDouble(from.ownText());
    }

    return price;
  }

  /**
   * There is no bankSlip price.
   * 
   * @param doc
   * @param price
   * @return
   */
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

}
