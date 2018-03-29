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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilPetloveCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.petlove.com.br/";

  public BrasilPetloveCrawler(Session session) {
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
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer.push(", ");", false);
      JSONObject productJson = json.has("info") ? json.getJSONObject("info") : new JSONObject();
      String internalPid = crawlInternalPid(productJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);
      Integer stock = null;

      JSONArray arraySkus = productJson.has("variants") ? productJson.getJSONArray("variants") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(jsonSku);
        boolean available = crawlAvailability(jsonSku);
        Float price = crawlMainPagePrice(jsonSku, available);
        String primaryImage = crawlPrimaryImage(jsonSku);
        String secondaryImages = crawlSecondaryImages(jsonSku);
        Prices prices = crawlPrices(price, jsonSku);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(new Marketplace()).build();

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

  private boolean isProductPage(Document doc) {
    return doc.select("#product").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku").trim();
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("master")) {
      JSONObject master = json.getJSONObject("master");

      if (master.has("product_sku")) {
        internalPid = master.getString("product_sku");
      }
    }

    return internalPid;
  }

  private String crawlName(JSONObject jsonSku) {
    StringBuilder name = new StringBuilder();

    if (jsonSku.has("full_name")) {
      name.append(jsonSku.getString("full_name"));
    }

    return name.toString();
  }

  private Float crawlMainPagePrice(JSONObject json, boolean available) {
    Float price = null;

    if (json.has("price") && available) {
      price = Float.parseFloat(json.getString("price"));
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    if (json.has("in_stock")) {
      return json.getBoolean("in_stock");
    }
    return false;
  }

  private String crawlPrimaryImage(JSONObject skuJson) {
    String primaryImage = null;

    if (skuJson.has("images") && skuJson.getJSONArray("images").length() > 0) {
      JSONObject image = skuJson.getJSONArray("images").getJSONObject(0);

      if (image.has("fullhd_url") && image.get("fullhd_url").toString().startsWith("http")) {
        primaryImage = image.get("fullhd_url").toString();
      } else if (image.has("large_url") && image.get("large_url").toString().startsWith("http")) {
        primaryImage = image.get("large_url").toString();
      } else if (image.has("small_url") && image.get("small_url").toString().startsWith("http")) {
        primaryImage = image.get("small_url").toString();
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject skuJson) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (skuJson.has("images") && skuJson.getJSONArray("images").length() > 1) {
      JSONArray images = skuJson.getJSONArray("images");

      for (int i = 1; i < images.length(); i++) { // starts with index 1, because the first image is
                                                  // the primary image
        JSONObject image = images.getJSONObject(i);

        if (image.has("fullhd_url") && image.get("fullhd_url").toString().startsWith("http")) {
          secondaryImagesArray.put(image.get("fullhd_url").toString());
        } else if (image.has("large_url") && image.get("large_url").toString().startsWith("http")) {
          secondaryImagesArray.put(image.get("large_url").toString());
        } else if (image.has("small_url") && image.get("small_url").toString().startsWith("http")) {
          secondaryImagesArray.put(image.get("small_url").toString());
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".list-category li a > span[itemprop]");

    for (Element e : elementCategories) {
      String cat = e.ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }


  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementShortdescription = doc.select(".product-resume").first();

    if (elementShortdescription != null) {
      description.append(elementShortdescription.html());
    }

    Element elementDescription = doc.select(".tab-content").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  /**
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, JSONObject skuJson) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);

      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);

      if (skuJson.has("best_installment")) {
        JSONObject bestInstallment = skuJson.getJSONObject("best_installment");

        if (bestInstallment.has("count") && bestInstallment.has("display_amount")) {
          Integer installment = bestInstallment.getInt("count");
          Float value = MathUtils.parseFloat(bestInstallment.getString("display_amount"));

          if (value != null) {
            mapInstallments.put(installment, value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.NARANJA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.NATIVA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);

    }

    return prices;
  }
}
