package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class UnitedstatesWholefoodsnewyorkCrawler extends Crawler {

  private static final String STORE = "10162";
  private static final String HOME_PAGE = "https://www.mercadoni.com.co/";

  public UnitedstatesWholefoodsnewyorkCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    return crawlProductInformatioFromApi(session.getOriginalURL());
  }

  @Override
  public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
    super.extractInformation(jsonSku);
    List<Product> products = new ArrayList<>();

    if (isProductPage(jsonSku)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      JSONObject store = jsonSku.has("store") ? jsonSku.getJSONObject("store") : new JSONObject();

      String internalId = crawlInternalId(jsonSku);
      CategoryCollection categories = crawlCategories(jsonSku);
      String description = crawlDescription(jsonSku);
      boolean available = crawlAvailability(store);
      Float price = available ? CrawlerUtils.getFloatValueFromJSON(jsonSku, "price") : null;
      Double priceFrom = available ? CrawlerUtils.getDoubleValueFromJSON(jsonSku, "basePrice") : null;
      JSONObject images = getJsonImages(jsonSku);
      String primaryImage = images.has("primary") ? images.get("primary").toString() : null;
      String secondaryImages = images.has("secondary") ? images.get("secondary").toString() : null;
      String name = crawlName(jsonSku);
      Prices prices = crawlPrices(price, priceFrom);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(JSONObject jsonSku) {
    return jsonSku.has("_id");
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("_id")) {
      internalId = json.get("_id").toString();
    }

    return internalId;
  }


  private String crawlName(JSONObject json) {
    String name = null;

    if (json.has("name")) {
      name = json.getString("name");
    }

    return name;
  }

  private boolean crawlAvailability(JSONObject store) {
    return store.has("available") && store.getBoolean("available");
  }

  private JSONObject getJsonImages(JSONObject json) {
    JSONObject images = new JSONObject();

    if (json.has("mediaList")) {
      JSONArray mediaList = json.getJSONArray("mediaList");
      JSONArray secondaryImages = new JSONArray();

      for (int i = 0; i < mediaList.length(); i++) {
        JSONObject imageJson = mediaList.getJSONObject(0);
        String image = null;

        if (imageJson.has("source") && !imageJson.isNull("source")) {
          image = CrawlerUtils.completeUrl(imageJson.getString("source"), "https", "sage.blob.core.windows.net");
        } else if (imageJson.has("thumbnail") && !imageJson.isNull("thumbnail")) {
          image = CrawlerUtils.completeUrl(imageJson.getString("thumbnail"), "https", "sage.blob.core.windows.net");
        }

        if (image != null) {
          if (i == 0) {
            images.put("primary", image);
          } else {
            secondaryImages.put(image);
          }
        }
      }

      if (secondaryImages.length() > 0) {
        images.put("secondary", secondaryImages);
      }
    }

    return images;
  }

  private CategoryCollection crawlCategories(JSONObject json) {
    CategoryCollection categories = new CategoryCollection();

    if (json.has("categories")) {
      JSONArray shelfList = json.getJSONArray("categories");

      for (Object o : shelfList) {
        JSONObject cat = (JSONObject) o;

        if (cat.has("name")) {
          categories.add(cat.getString("name"));
        }
      }
    }

    return categories;
  }


  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    description.append(getIngredients(json));
    description.append(getDietsWords(json));

    return description.toString();
  }

  private String getDietsWords(JSONObject json) {
    StringBuilder desc = new StringBuilder();

    if (json.has("diets")) {
      for (Object o : json.getJSONArray("diets")) {
        JSONObject diet = (JSONObject) o;

        if (diet.has("name")) {
          desc.append("<br> <div> " + diet.get("name") + " </div> </br>");
        }
      }
    }

    return desc.toString();
  }

  private String getIngredients(JSONObject json) {
    StringBuilder desc = new StringBuilder();

    if (json.has("ingredientList")) {
      JSONArray ingredientList = json.getJSONArray("ingredientList");

      if (ingredientList.length() > 0) {
        desc.append("<div id=\"ingredients\">").append("<h4> Ingredients </h4>");

        for (Object o : ingredientList) {
          desc.append("<span> " + o + " </span>");
        }

        desc.append("</div>");
      }
    }

    return desc.toString();
  }

  /**
   * In this site has no information of installments
   * 
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Double priceFrom) {
    Prices p = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      if (price > priceFrom) {
        p.setPriceFrom(priceFrom);
      }

      p.setBankTicketPrice(price);

      p.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      p.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);

    }

    return p;
  }


  private JSONObject crawlProductInformatioFromApi(String productUrl) {
    String slug = CommonMethods.getLast(productUrl.split("\\?")[0].split("/"));
    String apiUrl = "https://products.wholefoodsmarket.com/api/Product/slug/" + slug + "?store=" + STORE;

    return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, apiUrl, null, cookies);
  }
}
