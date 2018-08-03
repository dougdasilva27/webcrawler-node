package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
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
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 12/12/2016
 * 
 * 1 - In time this crawler was made, there was no products unnavailable; 2 - Thre is only one sku
 * per page; 3 - In this market has no bank slip payment method 4 - There is no secondary images in
 * this market 5 - There is no informations of installments
 * 
 * @author Gabriel Dornelas
 *
 */

public class BrasilHomerefillCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.homerefill.com.br/";

  public BrasilHomerefillCrawler(Session session) {
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

    if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // InternalId
      String internalId = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid();

      // Name
      String name = crawlName(doc, internalId);

      // Price
      Float price = crawlMainPagePrice(doc);

      // Availability
      boolean available = crawlAvailability(doc);

      // Categories
      CategoryCollection categories = crawlCategories(doc);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages();

      // Description
      String description = crawlDescription();

      // Stock
      Integer stock = null;

      // Marketplace map
      Map<String, Float> marketplaceMap = crawlMarketplace();

      // Marketplace
      Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);

      // Prices
      Prices prices = crawlPrices(price);

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


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    if (url.startsWith(HOME_PAGE + "product")) {
      return true;
    }

    return false;
  }



  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.selectFirst(".molecule-product-featured");

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("data-product-sku").trim();
    }

    return internalId;
  }

  private String crawlInternalPid() {
    return null;
  }

  private String crawlName(Document document, String internalId) {
    String name = null;
    Element nameElement = document.select("h3#product-" + internalId + "__description").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element specialPrice = document.select(".molecule-product-featured__price").first();

    if (specialPrice != null) {
      price = Float.parseFloat(specialPrice.attr("data-product-price"));
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    Element notifyMeElement = document.select(".molecule-product-featured__quantity__plus-button").first();

    if (notifyMeElement != null) {
      return true;
    }

    return false;
  }

  private Map<String, Float> crawlMarketplace() {
    return new HashMap<String, Float>();
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Float> marketplaceMap) {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".molecule-product-featured__figure > img").first();

    if (primaryImageElement != null) {
      String image = primaryImageElement.attr("src");

      if (image != null) {
        primaryImage = image.trim();
      }

    }

    return primaryImage;
  }

  private String crawlSecondaryImages() {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".molecule-breadcrumbs-navigation__breadcrumb a:not(.actived)");

    for (Element e : elementCategories) {
      categories.add(e.text().trim());
    }

    return categories;
  }

  private String crawlDescription() {
    return "";
  }

  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.JCB.toString(), installmentPriceMap);
    }

    return prices;
  }
}
