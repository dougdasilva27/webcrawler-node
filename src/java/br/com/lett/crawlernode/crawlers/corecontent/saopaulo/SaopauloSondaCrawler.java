package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloSondaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.sondadelivery.com.br/";

  public SaopauloSondaCrawler(Session session) {
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

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(session.getOriginalURL());
      String internalPid = crawlInternalPid();
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

  private boolean isProductPage(String url) {
    return url.contains("/delivery/produto/");
  }

  private String crawlInternalId(String url) {
    return Integer.toString(Integer.parseInt(this.session.getOriginalURL().split("/")[this.session.getOriginalURL().split("/").length - 1]));
  }

  private String crawlInternalPid() {
    return null;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h3.product--title_in").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Element salePriceElement = document.select(".price-full .price strong").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.ownText());
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = true;

    Element outOfStockElement = document.select("strong.noestoque").first();
    if (outOfStockElement != null) {
      available = false;
    }

    return available;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".owl-carousel .item").first();

    if (primaryImageElement != null && !primaryImageElement.attr("data-zoom-image").isEmpty()) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();
    } else if (primaryImageElement != null) {
      Element e = primaryImageElement.select(" > a > img").first();

      if (e != null) {
        primaryImage = e.attr("src");
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".owl-carousel .item");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("data-zoom-image").trim();

      if (!image.isEmpty()) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select(".breadcrumb li > a");
    for (int i = 0; i < elementCategories.size(); i++) {
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element descriptionElement = document.select(".product-details").first();

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * Some cases has this: 6 x $259.83
   * 
   * Only card that was found in this market was the market's own
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

      Element sondaElement = doc.select(".cartao-sonda .price-full .price strong span").first();

      if (sondaElement != null) {
        Float sondaPrice = MathUtils.parseFloatWithComma(sondaElement.ownText());

        Map<Integer, Float> installmentPriceMapSonda = new TreeMap<>();
        installmentPriceMapSonda.put(1, sondaPrice);

        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMapSonda);
      } else {
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      }

      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }
}
