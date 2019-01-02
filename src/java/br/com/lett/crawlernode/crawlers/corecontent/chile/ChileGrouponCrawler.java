package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 02/01/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileGrouponCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.groupon.cl/";

  public ChileGrouponCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("division", "santiago-centro");
    cookie.setDomain("www.groupon.cl");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    JSONObject productJson = extractProductJson(doc);

    if (productJson.has("templateId")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(productJson);
      CategoryCollection categories = new CategoryCollection();
      String description = carwlDescription(productJson);

      JSONArray arraySkus = productJson.has("options") ? productJson.getJSONArray("options") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject skuJson = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(skuJson);
        String name = crawlName(skuJson);
        boolean available = skuJson.has("isSoldOut") && !skuJson.getBoolean("isSoldOut");
        Prices prices = crawlPrices(skuJson);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Arrays.asList(Card.VISA, Card.SHOP_CARD));
        String primaryImage =
            productJson.has("bigImageUrl") ? CrawlerUtils.completeUrl(productJson.getString("bigImageUrl"), "https:", "cdn.needish.com") : null;
        String secondaryImages = crawlSecondaryImages(productJson.has("images") ? productJson.getJSONArray("images") : new JSONArray(), primaryImage);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available && price != null).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private JSONObject extractProductJson(Document doc) {
    JSONObject productJson = new JSONObject();

    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APP_INITIAL_STATE__ = ", null, false, true);
    if (json.has("deal")) {
      productJson = json.getJSONObject("deal");
    }

    return productJson;
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("id")) {
      internalId = skuJson.get("id").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("templateId")) {
      internalPid = productJson.get("templateId").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (skuJson.has("title")) {
      name.append(skuJson.get("title"));
    }

    return name.toString();
  }

  private String crawlSecondaryImages(JSONArray images, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (Object o : images) {
      JSONObject imageJson = (JSONObject) o;

      if (imageJson.has("big")) {
        String image = CrawlerUtils.completeUrl(imageJson.getString("big"), "https:", "cdn.needish.com");

        if (!image.equalsIgnoreCase(primaryImage)) {
          secondaryImagesArray.put(image);
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String carwlDescription(JSONObject productApi) {
    StringBuilder description = new StringBuilder();

    if (productApi.has("descriptor")) {
      description.append(productApi.get("descriptor").toString());
    }

    if (productApi.has("finePrint")) {
      description.append(productApi.get("finePrint").toString());
    }

    return description.toString();
  }

  private Prices crawlPrices(JSONObject productApi) {
    Prices prices = new Prices();

    if (productApi.has("price")) {
      JSONObject pricesJson = productApi.getJSONObject("price");

      if (pricesJson.has("formattedAmount")) {
        Map<Integer, Float> mapInstallments = new HashMap<>();
        mapInstallments.put(1, MathUtils.parseFloatWithComma(pricesJson.get("formattedAmount").toString()));

        if (productApi.has("value")) {
          JSONObject valueJson = productApi.getJSONObject("value");

          if (valueJson.has("formattedAmount")) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(valueJson.get("formattedAmount").toString()));
          }
        }

        prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      }
    }

    return prices;
  }
}
