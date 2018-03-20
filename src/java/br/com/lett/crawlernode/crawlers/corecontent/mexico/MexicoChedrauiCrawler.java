package br.com.lett.crawlernode.crawlers.corecontent.mexico;

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
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 28/11/2016
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes: 1) In time crawler was made, there no product unnavailable. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito). 4) In this market has some stores, I made the crawler
 * according to the store Coatzacoalcos Crystal which had a greater number of products. The name of
 * the store follows in the url: http://www.chedraui.com.mx/index.php/coatzacoalcoscrystal
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoChedrauiCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.chedraui.com.mx/";

  public MexicoChedrauiCrawler(Session session) {
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
      Prices prices = crawlPrices(price);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace(doc);

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
    return doc.select(".product-details").first() != null;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element internalIdElement = document.select(".name .code").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.text().trim();
    }

    return internalId;
  }

  /**
   * There is no internalPid.
   * 
   * @param document
   * @return
   */
  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element pid = document.select("input[name=productCodePost]").first();

    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".product-details > .name").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    String priceText = null;
    Element salePriceElement = document.select(".price-colour-final-pdp").last();

    if (salePriceElement != null) {
      priceText = salePriceElement.ownText();
      price = Float.parseFloat(priceText.replaceAll("\\$", "").replaceAll(",", ""));
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = false;

    Element outOfStockElement = document.select(".addToCart-btn-pdp").first();
    if (outOfStockElement != null) {
      available = true;
    }

    return available;
  }

  private Marketplace crawlMarketplace(Document document) {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".image-gallery__image .item img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();

      if (!primaryImage.contains("http")) {
        primaryImage = HOME_PAGE + primaryImage;
      }
    }

    return primaryImage;
  }

  /**
   * In the moment this crawler was made, was not fount any secondary image
   * 
   * @param document
   * @param primaryImage
   * @return
   */
  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = document.select(".image-gallery__image .item img");

    if (!images.isEmpty()) {
      images.remove(0);

      for (Element e : images) {
        String image = e.attr("data-zoom-image");

        if (!image.contains("http")) {
          image = HOME_PAGE + image;
        }

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

    Elements elementCategories = document.select(".breadcrumb li a");
    for (int i = 3; i < elementCategories.size(); i++) { // start with index 3 because the first 3
                                                         // items are no categories
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Elements descriptionElements = document.select("#productTabs > div:not(#tabreview)");

    for (Element e : descriptionElements) {
      if (e.select(".tab-review").first() == null) {
        description.append(e.html());
      }
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * There is no card payment options, other than cash price. So for installments, we will have only
   * one installment for each card brand, and it will be equals to the price crawled on the sku main
   * page.
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

}
