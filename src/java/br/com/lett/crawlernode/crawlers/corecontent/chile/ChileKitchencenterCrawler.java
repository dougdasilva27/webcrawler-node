package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 10/12/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileKitchencenterCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.kitchencenter.cl/";

  public ChileKitchencenterCrawler(Session session) {
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

      JSONObject skuJson = crawlSkuJson(doc);

      String internalId = crawlInternalId(skuJson);
      String internalPid = crawlInternalPid(skuJson);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-title", false);
      Float price = crawlPrice(skuJson);
      Prices prices = crawlPrices(price, skuJson);
      Integer stock = crawlStock(skuJson);
      boolean available = stock != null && stock > 0;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) a span", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#thumbnails span[data-src]", Arrays.asList("data-src"), "https:",
          "kitchencenter-production.s3.amazonaws.com");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#thumbnails span[data-src]", Arrays.asList("data-src"), "https:",
          "kitchencenter-production.s3.amazonaws.com", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".menu-tabs"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setStock(stock).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("#variant_id").isEmpty();
  }

  private JSONObject crawlSkuJson(Document doc) {
    JSONObject jsonSku = new JSONObject();

    JSONObject productJson = CrawlerUtils.selectJsonFromHtml(doc, "#color-form script", "newVariantOptions(", ");", true, true);
    if (productJson.has("options")) {
      JSONObject options = productJson.getJSONObject("options");

      if (options.has("1")) {
        JSONObject option1 = options.getJSONObject("1");

        Set<String> set = option1.keySet();
        for (String key : set) {
          JSONObject json = option1.getJSONObject(key);

          if (json.has("sku")) {
            jsonSku = json;
          }
        }
      }
    }

    return jsonSku;
  }

  private String crawlInternalId(JSONObject jsonSku) {
    String internalId = null;

    if (jsonSku.has("sku")) {
      internalId = jsonSku.get("sku").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject jsonSku) {
    String internalPid = null;

    if (jsonSku.has("id")) {
      internalPid = jsonSku.get("id").toString();
    }

    return internalPid;
  }

  private Integer crawlStock(JSONObject jsonSku) {
    Integer stock = null;

    if (jsonSku.has("stock")) {
      String text = jsonSku.get("stock").toString().replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        stock = Integer.parseInt(text);
      }
    }

    return stock;
  }

  private Float crawlPrice(JSONObject jsonSku) {
    Float price = null;

    if (jsonSku.has("price")) {
      price = MathUtils.parseFloatWithComma(jsonSku.get("price").toString());
    }

    return price;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, JSONObject skuJson) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);

      if (skuJson.has("promotion")) {
        JSONObject promotion = skuJson.getJSONObject("promotion");

        if (promotion.has("original_price")) {
          prices.setPriceFrom(MathUtils.parseDoubleWithComma(promotion.get("original_price").toString()));
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

}
