package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 21/02/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class UnitedstatesWalgreensCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.walgreens.com/";
  private static final String HOST_IMAGES = "pics.drugstore.com";

  public UnitedstatesWalgreensCrawler(Session session) {
    super(session);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE, null, ".walgreens.com", "/", cookies, session, new HashMap<>());
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

    JSONObject results = scrapProductInfo(doc);

    if (results.has("productInfo")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject productInfo = results.getJSONObject("productInfo");

      String internalId = productInfo.has("skuId") ? productInfo.get("skuId").toString() : null;
      String internalPid = productInfo.has("productId") ? productInfo.get("productId").toString() : null;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "#productTitle", true);
      Float price = crawlPrice(results);
      Prices prices = crawlPrices(doc, price);
      boolean available = !doc.select("#receiveing-addToCartbtn").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".nav__bread-crumbs .breadcrumbspdp a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#productImg", Arrays.asList("src", "content"), "https:", HOST_IMAGES);
      String secondaryImages = crawlSecondaryImages(productInfo, primaryImage);
      String description = crawlDescription(doc, internalPid);

      String ean = crawlEan(results);
      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }

    return products;

  }

  private JSONObject scrapProductInfo(Document doc) {
    JSONObject productInfo = new JSONObject();

    JSONObject initialState = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__APP_INITIAL_STATE__=", "};", true, false);
    if (initialState.has("product")) {
      JSONObject product = initialState.getJSONObject("product");

      if (product.has("results")) {
        productInfo = product.getJSONObject("results");
      }
    }

    return productInfo;
  }

  private String crawlSecondaryImages(JSONObject prodInfo, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (prodInfo.has("filmStripUrl")) {
      JSONArray filmStripUrl = prodInfo.getJSONArray("filmStripUrl");

      for (int i = 1; i <= filmStripUrl.length(); i++) {
        JSONObject imageJson = filmStripUrl.getJSONObject(i - 1);

        String image = null;

        if (imageJson.has("zoomImageUrl" + i) && !imageJson.isNull("zoomImageUrl" + i)) {
          image = CrawlerUtils.completeUrl(imageJson.getString("zoomImageUrl" + i), "https", HOST_IMAGES);
        } else if (imageJson.has("largeImageUrl" + i) && !imageJson.isNull("largeImageUrl" + i)) {
          image = CrawlerUtils.completeUrl(imageJson.getString("largeImageUrl" + i), "https", HOST_IMAGES);
        } else if (imageJson.has("stripUrl" + i) && !imageJson.isNull("stripUrl" + i)) {
          image = CrawlerUtils.completeUrl(imageJson.getString("stripUrl" + i), "https", HOST_IMAGES);
        }

        if (image != null && !image.equalsIgnoreCase(primaryImage)) {
          secondaryImagesArray.put(image);
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Float crawlPrice(JSONObject results) {
    Float price = null;

    if (results.has("priceInfo")) {
      JSONObject priceInfo = results.getJSONObject("priceInfo");

      if (priceInfo.has("salePrice") && !priceInfo.isNull("salePrice")) {
        price = CrawlerUtils.getFloatValueFromJSON(priceInfo, "salePrice");
      } else if (priceInfo.has("regularPrice") && !priceInfo.isNull("regularPrice")) {
        price = CrawlerUtils.getFloatValueFromJSON(priceInfo, "regularPrice");
      }
    }

    return price;
  }

  private String crawlDescription(Document doc, String internalPid) {
    StringBuilder str = new StringBuilder();

    str.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#prod-DescriptionShopMore")));

    String url = "https://scontent.webcollage.net/walgreens/power-page?ird=true&channel-product-id=" + internalPid;
    String script = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (script.contains("_wccontent =")) {
      JSONObject content = CrawlerUtils.stringToJson(CrawlerUtils.extractSpecificStringFromScript(script, "_wccontent = ", "};", false));

      if (content.has("aplus")) {
        JSONObject aplus = content.getJSONObject("aplus");

        if (aplus.has("html")) {
          str.append(aplus.get("html"));
        }
      }
    }

    return str.toString();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, "#regular-price-wag-hn-lt-bold", true));
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }
    return prices;
  }

  private String crawlEan(JSONObject results) {
    String ean = null;

    if (results.has("prodDetails")) {
      JSONObject prodDetails = results.getJSONObject("prodDetails");

      if (prodDetails.has("gtin")) {
        ean = prodDetails.get("gtin").toString();
      }
    }

    return ean;
  }
}
