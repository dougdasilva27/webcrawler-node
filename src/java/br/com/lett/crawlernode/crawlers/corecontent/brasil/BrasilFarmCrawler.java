package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 19/12/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilFarmCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.farmrio.com.br/";

  public BrasilFarmCrawler(Session session) {
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      JSONObject productJson = crawlProductJson(doc);

      String internalPid = crawlInternalPid(doc);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);
      Float price = crawlMainPagePrice(productJson);
      Prices prices = crawlPrices(price, doc);

      JSONArray arraySkus =
          productJson.has("skus") ? productJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject skuJson = arraySkus.getJSONObject(i);

        Integer stock = crawlStock(skuJson);
        boolean available = stock != null && stock > 0 && skuJson.has("status")
            && skuJson.getString("status").equals("available");
        String internalId = crawlInternalId(skuJson);
        String primaryImage = crawlPrimaryImage(doc);
        String name = crawlName(doc, skuJson);
        String secondaryImages = crawlSecondaryImages(doc);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
            .setDescription(description).setStock(stock).setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select(".product-name h3").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private JSONObject crawlProductJson(Document doc) {
    JSONObject productJson = new JSONObject();
    JSONObject skusInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=text/javascript]",
        "window.chaordic_meta=", ";");

    if (skusInfo.has("product")) {
      productJson = skusInfo.getJSONObject("product");
    }

    return productJson;
  }

  private Integer crawlStock(JSONObject skuJson) {
    Integer stock = null;

    if (skuJson.has("stock")) {
      stock = skuJson.getInt("stock");
    }

    return stock;
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("sku")) {
      internalId = skuJson.getString("sku");
    }

    return internalId;
  }


  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select(".product-ref p").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.ownText().replaceAll("[^0-9]", "").trim();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();
    Element nameElement = document.select(".product-name h3").first();

    if (nameElement != null) {
      name.append(nameElement.text().trim());

      if (skuJson.has("spec")) {
        JSONObject spec = skuJson.getJSONObject("spec");

        Iterator<?> i = spec.keys();

        while (i.hasNext()) {
          name.append(" " + i.next());
        }
      }

    }

    return name.toString();
  }

  private Float crawlMainPagePrice(JSONObject json) {
    Float price = null;

    if (json.has("price")) {
      price = MathCommonsMethods
          .normalizeTwoDecimalPlaces(((Double) json.getDouble("price")).floatValue());
    }

    return price;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select("#product-slider > div > img").first();

    if (image != null) {
      primaryImage = image.attr("data-zoom").trim();

      if (!primaryImage.startsWith("http")) {
        primaryImage = "http://images2.euquerofarm.com.br" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imageThumbs = doc.select("#product-slider > div > img");

    for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image
                                                   // is the primary image
      String url = imageThumbs.get(i).attr("data-zoom");

      if (!url.startsWith("http")) {
        url = "http://images2.euquerofarm.com.br" + url;
      }

      secondaryImagesArray.put(url);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb a[href]:not([rel])");

    for (Element e : elementCategories) {
      String text = e.text().trim();

      if (!text.equals("categorias")) {
        categories.add(e.text().trim());
      }
    }

    return categories;
  }


  private String crawlDescription(Document document) {
    String description = "";

    Element descElement = document.select(".product-list").first();
    if (descElement != null) {
      description = description + descElement.html();
    }

    return description;
  }

  /**
   * There is no bank slip payment method Has no informations of installments
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);

      Elements installmentElement = doc.select(".price-parcelado > span:not([class])");

      if (installmentElement.size() > 1) {
        String installment = installmentElement.get(0).ownText().replaceAll("[^0-9]", "").trim();
        Float value = MathCommonsMethods.parseFloat(installmentElement.get(1).ownText());

        if (!installment.isEmpty() && value != null) {
          mapInstallments.put(Integer.parseInt(installment), value);
        }
      }

      prices.setPriceFrom(crawlPriceFrom(doc));

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);

    }

    return prices;
  }

  /**
   * Price "de"
   * 
   * @param jsonSku
   * @return
   */
  private Double crawlPriceFrom(Document doc) {
    Double priceFrom = null;

    Element e = doc.select(".price-old span").first();
    if (e != null) {
      Float price = MathCommonsMethods.parseFloat(e.ownText());
      priceFrom = MathCommonsMethods.normalizeTwoDecimalPlaces(price.doubleValue());
    }

    return priceFrom;
  }
}
