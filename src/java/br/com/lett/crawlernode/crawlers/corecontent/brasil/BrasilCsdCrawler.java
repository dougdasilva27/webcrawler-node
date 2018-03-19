package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
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
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * 
 * @author gabriel date: 2018-03-15
 */
public class BrasilCsdCrawler extends Crawler {

  public BrasilCsdCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.sitemercado.com.br/supermercadoscidadecancao/maringa-loja-brasil-01-zona-05-avenida-brasil";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = null;
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      String name = crawlName(doc);
      Float price = crawlMainPagePrice(doc);
      boolean available = price != null; // at the time this crawler was made, no unavailable products were found
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
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

  private boolean isProductPage(Document doc) {
    return doc.select(".single-prod").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;

    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = new JSONObject();

    for (Element tag : scriptTags) {
      String scrpit = tag.html().trim();
      if (scrpit.startsWith("window.google_tag_params = ")) {
        String finalJson = scrpit.replaceAll("window.google_tag_params = ", "").replace(";", "").trim();

        skuJson = new JSONObject(finalJson);

      }

    }

    if (skuJson.has("ecomm_prodid")) {
      internalId = skuJson.getString("ecomm_prodid");
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".single-prod h1").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element mainPagePriceElement = document.select(".price > span").first();

    if (mainPagePriceElement != null) {
      mainPagePriceElement.select("small").remove();
      price = MathCommonsMethods.parseFloat(mainPagePriceElement.text());
    }

    return price;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".photo-prd-modal > img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src");

      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }

    return primaryImage;
  }

  // at the time this crawler was made, no secondary images was found
  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb > a");

    for (Element e : elementCategories) {
      String cat = e.text().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element descriptionElement = document.select(".single-prod .condition").first();

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }
    return description.toString();
  }

  /**
   * Nesse market os produtos que verifiquei não tinham desconto ou juros nos preços na página
   * principal
   * 
   * @param price
   * @param doc
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DISCOVER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);

    }

    return prices;
  }
}
