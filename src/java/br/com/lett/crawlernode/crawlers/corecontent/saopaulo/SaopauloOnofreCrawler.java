package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Date: 25/11/2016 1) Only one sku per page.
 * 
 * Price crawling notes: 1) For this market was not found product unnavailable 2) Page of product
 * disappear when javascripit is off, so is accessed this api:
 * "https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc="+id 3) InternalId of product
 * is in url and a json, but to fetch api is required internalId, so it is crawl in url 4) Has no
 * bank ticket in this market 5) Has no internalPid in this market 6) IN api when have a json,
 * sometimes has duplicates keys, so is used GSON from google.
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloOnofreCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.onofre.com.br";
  private static final String HOME_PAGE_HTTP = "http://www.onofre.com.br/";

  public SaopauloOnofreCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_HTTP));
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
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();

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
    return doc.select("#skuId").first() != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element elementInternalPid = doc.select("#skuId").first();
    if (elementInternalPid != null) {
      internalId = elementInternalPid.text();
    }

    return internalId;
  }

  /**
   * There is no internalPid.
   * 
   * @param document
   * @return
   */
  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element id = doc.select("#product-code").first();

    if (id != null) {
      String text = id.text();

      if (text.contains(":")) {
        internalPid = CommonMethods.getLast(text.split(":")).trim();
      } else {
        internalPid = text.trim();
      }
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".product-information__title").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element elementPrice = document.select(".price-box__value").first();

    if (elementPrice != null) {
      price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = false;
    Element elementAvailable = document.select(".product-information__stock").first();
    if (elementAvailable != null) {
      String text = elementAvailable.text().trim();
      available = text.equalsIgnoreCase("em estoque");
    }

    return available;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element elementPrimaryImage = document.select(".product-gallery__main img").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("src").trim();
    }

    if (primaryImage != null && !primaryImage.startsWith("http")) {
      primaryImage = HOME_PAGE + primaryImage;
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();
    Elements elementSecondaryImages = document.select(".product-gallery__thumbs-list a");

    for (Element e : elementSecondaryImages) {
      String image = e.attr("data-image").trim();

      if (!image.startsWith("http")) {
        image = HOME_PAGE + image;
      }

      if (!image.equals(primaryImage)) {
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
    Elements elementCategories = document.select("#breadcrumbs-data a");

    for (int i = 1; i < elementCategories.size(); i++) { // first index is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".product-tabs__content").first();
    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element elementWarning = doc.select(".product-disclaimer").first();
    if (elementWarning != null) {
      description.append(elementWarning.html());
    }

    return description.toString();
  }


  /**
   * In product page has only one price, but in footer has informations of payment methods
   *
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      Element priceFrom = doc.select(".price-box__old strike").first();
      if (priceFrom != null) {
        Float priceOld = MathUtils.parseFloat(priceFrom.text());

        if (priceOld != null && !priceOld.equals(price)) {
          prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
        }
      }

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

}
