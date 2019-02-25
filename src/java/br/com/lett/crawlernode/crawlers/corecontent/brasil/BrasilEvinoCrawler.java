package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.prices.Prices;

public class BrasilEvinoCrawler extends Crawler {

  public BrasilEvinoCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject skuJson = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"text/javascript\"]", "var TC = ", null, false, false);
      JSONObject productBiggyJson = skuJson.has("productBiggyJson") ? new JSONObject(skuJson.get("productBiggyJson").toString()) : new JSONObject();
      JSONArray biggyJson = productBiggyJson.has("skus") ? productBiggyJson.getJSONArray("skus") : new JSONArray();

      String name = crawlName(productBiggyJson);
      String primaryImage = crawlPrimaryImage(productBiggyJson);
      Float price = crawlPrice(productBiggyJson);
      CategoryCollection categories = crawlCategories(productBiggyJson);
      Prices prices = crawlPrices(productBiggyJson);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".Product__details"));
      String internalPid = crawlInternalPid(productBiggyJson);

      for (Object object : biggyJson) {
        JSONObject variation = (JSONObject) object;

        String internalId = crawlInternalId(variation);
        Boolean available = crawlAvailability(variation);
        Integer stock = crawlStock(variation);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(null).setDescription(description)
            .setStock(stock).setMarketplace(null).setEans(null).build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private Integer crawlStock(JSONObject variation) {
    return variation.has("stockQuantity") ? variation.getInt("stockQuantity") : null;
  }

  private Prices crawlPrices(JSONObject productBiggyJson) {

    Prices prices = new Prices();
    Double priceFrom = productBiggyJson.has("oldPrice") ? MathUtils.parseDoubleWithDot(productBiggyJson.get("oldPrice").toString()) : null;
    Float price = productBiggyJson.has("price") ? MathUtils.parseFloatWithDots(productBiggyJson.get("price").toString()) : null;
    Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    installmentPriceMap.put(1, price);

    prices.setBankTicketPrice(price);
    prices.setPriceFrom(priceFrom);

    prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

    return prices;
  }

  private CategoryCollection crawlCategories(JSONObject productBiggyJson) {
    CategoryCollection categories = new CategoryCollection();
    JSONArray categoriesArray = productBiggyJson.has("categories") ? productBiggyJson.getJSONArray("categories") : new JSONArray();

    for (Object object : categoriesArray) {
      JSONObject categoriesObject = (JSONObject) object;
      String name = categoriesObject.has("name") ? categoriesObject.get("name").toString() : null;
      categories.add(name);
    }

    return categories;
  }

  private Float crawlPrice(JSONObject productBiggyJson) {
    return productBiggyJson.has("price") ? MathUtils.parseFloatWithDots(productBiggyJson.get("price").toString()) : null;
  }

  private String crawlPrimaryImage(JSONObject productBiggyJson) {
    String primaryImage = null;
    JSONObject images = productBiggyJson.has("images") ? productBiggyJson.getJSONObject("images") : new JSONObject();

    if (images.has("extralarge")) {
      primaryImage = images.getString("extralarge");
    } else if (images.has("large")) {
      primaryImage = images.getString("large");
    } else if (images.has("medium")) {
      primaryImage = images.getString("medium");
    } else if (images.has("small")) {
      primaryImage = images.getString("small");
    }
    return primaryImage;
  }

  private Boolean crawlAvailability(JSONObject variation) {
    return variation.has("status") && variation.get("status").toString().equalsIgnoreCase("available");
  }

  private String crawlInternalId(JSONObject variation) {
    return variation.has("sku") ? variation.get("sku").toString() : null;
  }

  private String crawlName(JSONObject skuJson) {
    return skuJson.has("name") ? skuJson.get("name").toString() : null;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    return skuJson.has("id") ? skuJson.get("id").toString() : null;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".Product") != null;
  }

}
