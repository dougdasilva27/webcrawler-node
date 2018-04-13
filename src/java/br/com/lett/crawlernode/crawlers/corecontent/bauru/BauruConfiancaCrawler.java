package br.com.lett.crawlernode.crawlers.corecontent.bauru;

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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 12/04/2018
 * 
 * @author gabriel
 *
 */
public class BauruConfiancaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.confianca.com.br/";

  public BauruConfiancaCrawler(Session session) {
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

    BasicClientCookie cookie = new BasicClientCookie("bauru", "lojabauru");
    cookie.setDomain("www.confianca.com.br");
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
    return doc.select("input[name=product]").first() != null && doc.select("button[data-target=\"#modal-lista-ingredientes\"]").isEmpty();
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element id = document.select("input[name=product]").first();
    if (id != null) {
      internalId = id.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalPid = null;

    Element id = document.select("meta[itemprop=productID]").first();
    if (id != null) {
      internalPid = id.attr("content");
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".product-name h1").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Element salePriceElement = document.select(".price-box .special-price .price").first();

    if (salePriceElement == null) {
      salePriceElement = document.select(".price-box .regular-price .price").first();
    }

    if (salePriceElement != null) {
      price = MathUtils.parseFloat(salePriceElement.ownText());
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    return document.select("#out-of-stock-sub[style^=\"display:none\"]").first() != null || document.select("#out-of-stock-sub").isEmpty();
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".product-image img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".more-views a[data-zoom-image]");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      secondaryImagesArray.put(imagesElement.get(i).attr("data-zoom-image").trim());
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * At the time this crawler was made, wasn't not found any categories in product page
   * 
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    // CategoryCollection categories = new CategoryCollection();
    // Elements elementCategories = document.select(".breadcrumb a:not(.first) > span");
    // for (Element e : elementCategories) {
    // categories.add(e.ownText().trim());
    // }
    // return categories;

    return new CategoryCollection();
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = document.select(".short-description").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element desc = document.select("#descricao").first();
    if (desc != null) {
      description.append(desc.html());
    }

    Element ingredientes = document.select("#ingredientes").first();
    if (ingredientes != null) {
      description.append(ingredientes.html());
    }

    Element preparo = document.select("#modo-preparo").first();
    if (preparo != null) {
      description.append(preparo.html());
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

      Element priceFrom = doc.select(".price-box .old-price .price").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.ownText()));
      }

      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

}
