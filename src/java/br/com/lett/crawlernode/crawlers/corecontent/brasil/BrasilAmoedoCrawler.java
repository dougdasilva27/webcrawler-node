package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

/************************************************************************************************************************************************************************************
 * Crawling notes (23/08/2016):
 * 
 * 1) For this crawler, we have one url per each sku. There is no page is more than one sku in it.
 * 
 * 2) There is no stock information for skus in this ecommerce by the time this crawler was made.
 * 
 * 3) There is no marketplace in this ecommerce by the time this crawler was made.
 * 
 * 4) The sku page identification is done simply looking for an specific html element.
 * 
 * 5) In this market was not found a product with status unnavailable.
 * 
 * 6) There is internalPid for skus in this ecommerce.
 * 
 * 7) The primary image is the first image in the secondary images selector.
 * 
 * 8) Categories in this market appear only with cookies.
 * 
 * Examples: ex1 (available):
 * http://www.amoedo.com.br/ar-condicionado-split-piso-teto-komeco-60btus-kop60fc ex2 (unavailable):
 * For this market, was not found product unnavailable
 *
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class BrasilAmoedoCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.amoedo.com.br/";

  public BrasilAmoedoCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      /*
       * *********************************** crawling data of only one product *
       *************************************/

      // InternalId
      String internalId = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Name
      String name = crawlName(doc);

      // Price
      Float price = crawlPrice(doc);

      // Prices
      Prices prices = crawlPrices(doc, price);

      // Availability
      boolean available = !doc.select(".stock:not(.unavailable)").isEmpty();

      // Categories
      CategoryCollection categories = new CategoryCollection();

      JSONArray images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);

      // Primary image
      String primaryImage = crawlPrimaryImage(images);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(images);

      // Description
      String description = crawlDescription(doc);

      // Marketplace
      Marketplace marketplace = crawlMarketplace();

      // Creating the product

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(1)).setCategory2(categories.getCategory(2))
          .setCategory3(categories.getCategory(3)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    if (document.select(".catalog-product-view").first() != null)
      return true;
    return false;
  }


  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;
    Element internalIdElement = document.selectFirst("input[name=product]");

    if (internalIdElement != null) {
      internalId = internalIdElement.val().trim();
    }

    return internalId;
  }

  private String crawlInternalPid(Document document) {
    String internalId = null;
    Element internalIdElement = document.selectFirst(".product.attribute.sku .value");

    if (internalIdElement != null) {
      internalId = internalIdElement.ownText().trim();
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.selectFirst(".page-title > span");

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document doc) {
    Float price = null;

    Element priceElement = doc.selectFirst(".special-price .price");

    if (priceElement == null) {
      priceElement = doc.selectFirst(".price");
    }

    if (priceElement != null) {
      price = MathUtils.parseFloatWithComma(priceElement.ownText());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

  private String crawlPrimaryImage(JSONArray images) {
    String primaryImage = null;

    if (images.length() > 0) {
      primaryImage = images.getString(0);
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONArray images) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (images.length() > 1) {
      images.remove(0);
      secondaryImagesArray = images;
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();// É pra ser tostring mesmo? Não era um array?
    }

    return secondaryImages;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();

    Element overviewElement = document.selectFirst(".product.attribute.overview > .value");
    Element descriptionElement = document.selectFirst(".product.attribute.description > .value");

    if (overviewElement != null) {
      description.append(overviewElement.html());
    }

    if (descriptionElement != null)
      description.append(descriptionElement.html());

    return description.toString();
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      prices.setBankTicketPrice(price);

      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    prices.setPriceFrom(crawlPriceFrom(doc));

    return prices;
  }

  private Double crawlPriceFrom(Document doc) {
    Double priceFrom = null;

    Element priceFromElement = doc.selectFirst(".old-price .price");
    if (priceFromElement != null) {
      priceFrom = MathUtils.parseDoubleWithComma(priceFromElement.text());
    }

    return priceFrom;
  }

}
