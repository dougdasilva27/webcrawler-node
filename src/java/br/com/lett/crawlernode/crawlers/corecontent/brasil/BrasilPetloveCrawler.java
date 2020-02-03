package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.prices.Prices;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataLayer.push(", ");", false, false);
      JSONObject productJson = JSONUtils.getJSONValue(json, "info");
      String internalPid = crawlInternalPid(productJson);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);
      Integer stock = null;

      JSONArray arraySkus = JSONUtils.getJSONArrayValue(productJson, "variants");
      
      for(Object obj : arraySkus) {
        if(obj instanceof JSONObject) {
          JSONObject jsonSku = (JSONObject) obj;
  
          String internalId = JSONUtils.getStringValue(jsonSku, "sku");
            String name = crawlName(jsonSku);
            boolean available = (jsonSku.has("in_stock") && jsonSku.get("in_stock") instanceof Boolean) && jsonSku.getBoolean("in_stock");
            Float price = JSONUtils.getFloatValueFromJSON(jsonSku, "price", true);
          String primaryImage = crawlPrimaryImage(jsonSku);
          String secondaryImages = crawlSecondaryImages(jsonSku);
          Prices prices = crawlPrices(price, jsonSku);
  
          // Creating the product
          Product product = ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId)
              .setInternalPid(internalPid)
              .setName(name)
              .setPrice(price)
              .setPrices(prices)
              .setAvailable(available)
              .setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages)
              .setDescription(description)
              .setStock(stock)
              .build();
  
          products.add(product);
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#product") != null;
  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("master") && json.get("master") instanceof JSONObject) {
      JSONObject master = json.getJSONObject("master");

      if (master.has("product_sku") && master.get("product_sku") instanceof String) {
        internalPid = master.getString("product_sku");
      }
    }

    return internalPid;
  }

  private String crawlName(JSONObject jsonSku) {
    StringBuilder name = new StringBuilder();

    if (jsonSku.has("name") && jsonSku.get("name") instanceof String) {
      name.append(jsonSku.getString("name"));

      if (jsonSku.has("label_name") && !jsonSku.isNull("label_name")) {
        name.append(" ");
        name.append(jsonSku.get("label_name"));
      }

      if (jsonSku.has("original_short_name") && jsonSku.get("original_short_name") instanceof String) {
        String shortName = jsonSku.getString("original_short_name");

        if (!name.toString().toLowerCase().contains(shortName.toLowerCase())) {
          name.append(" ");
          name.append(shortName);
        }
      }
    }

    return name.toString().trim();
  }

  private String crawlPrimaryImage(JSONObject skuJson) {
    String primaryImage = null;

    if (skuJson.has("images") && skuJson.get("images") instanceof JSONArray && skuJson.getJSONArray("images").length() > 0) {
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

    if (skuJson.has("images") && skuJson.get("images") instanceof JSONArray && skuJson.getJSONArray("images").length() > 1) {
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

      if (skuJson.has("best_installment") && skuJson.get("best_installment") instanceof JSONObject) {
        JSONObject bestInstallment = skuJson.getJSONObject("best_installment");

        if (bestInstallment.has("count") && bestInstallment.get("count") instanceof Integer 
            && bestInstallment.has("display_amount") && !bestInstallment.isNull("display_amount")) {
          
          Integer installment = bestInstallment.getInt("count");
          Float value = MathUtils.parseFloatWithComma(bestInstallment.get("display_amount").toString());

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
