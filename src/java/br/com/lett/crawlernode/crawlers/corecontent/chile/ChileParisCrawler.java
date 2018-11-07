package br.com.lett.crawlernode.crawlers.corecontent.chile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 19/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ChileParisCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.paris.cl/";
  private static final String IMAGE_URL_FIRST_PART = "https://imagenes.paris.cl/is/image/";

  public ChileParisCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    JSONObject productJson = new JSONObject();

    try {
      URL url = new URL(session.getOriginalURL());
      String path = CommonMethods.getLast(url.getPath().split("/")).split("\\?")[0];
      String payload = "{\"query\":{\"bool\":{\"minimum_should_match\":1,\"should\":[{\"term\":" + "{\"url.keyword\":\"" + path + "\"}},"
          + "{\"term\":{\"children.url.keyword\":\"" + path + "\"" + "}}]}}}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");
      headers.put("referer", session.getOriginalURL());
      headers.put("authority", "www.paris.cl");

      String response = POSTFetcher.fetchPagePOSTWithHeaders("https://www.paris.cl/store-api/pyload/_search", session, payload, cookies, 1, headers);
      JSONObject json = CrawlerUtils.stringToJson(response);

      if (json.has("hits")) {
        JSONObject hits = json.getJSONObject("hits");

        if (hits.has("hits")) {
          JSONArray productArray = hits.getJSONArray("hits");

          if (productArray.length() > 0) {
            JSONObject product = productArray.getJSONObject(0);

            if (product.has("_source")) {
              productJson = product.getJSONObject("_source");
            }
          }
        }
      }

    } catch (MalformedURLException e) {
      Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
    }

    return productJson;
  }

  @Override
  public List<Product> extractInformation(JSONObject productJson) throws Exception {
    super.extractInformation(productJson);
    List<Product> products = new ArrayList<>();

    if (isProductPage(productJson)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(productJson);
      CategoryCollection categories = new CategoryCollection();
      String description = crawlDescription(productJson);
      boolean isPackage = "packagebean".equalsIgnoreCase(crawlType(productJson));

      Map<String, List<String>> colorsMap = new HashMap<>();

      // This specific case is for package products
      if (isPackage) {
        String internalId = crawlInternalIdPackage(productJson, internalPid);
        String name = crawlNamePackage(productJson);
        boolean available = crawlPackageAvailability(productJson);

        JSONArray arrayPrices = fetchPrices(productJson, available);
        Prices prices = crawlPrices(productJson, arrayPrices);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

        List<String> images = crawlImages(productJson, colorsMap, internalPid);
        String primaryImage = images.isEmpty() ? null : images.get(0);
        String secondaryImages = crawlSecondaryImages(images, primaryImage);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setSecondaryImages(null)
            .setDescription(description).setStock(null).setMarketplace(new Marketplace()).build();

        products.add(product);
      } else {
        JSONArray arraySkus = productJson.has("children") ? productJson.getJSONArray("children") : new JSONArray();

        for (int i = 0; i < arraySkus.length(); i++) {
          JSONObject skuJson = arraySkus.getJSONObject(i);

          String internalId = crawlInternalId(skuJson);
          String name = crawlName(skuJson);
          Integer stock = crawlStock(skuJson);
          boolean available = stock != null && stock > 0;

          List<String> images = crawlImages(skuJson, colorsMap, internalPid);
          String primaryImage = images.isEmpty() ? null : images.get(0);
          String secondaryImages = crawlSecondaryImages(images, primaryImage);

          JSONArray arrayPrices = fetchPrices(skuJson, available);
          Prices prices = crawlPrices(skuJson, arrayPrices);
          Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid)
              .setName(name).setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(new Marketplace()).build();

          products.add(product);
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  /**
   * @param productJson
   * @return
   */
  private boolean isProductPage(JSONObject productJson) {
    return productJson.has("partNumber");
  }

  private String crawlType(JSONObject skuJson) {
    String type = null;

    if (skuJson.has("type")) {
      type = skuJson.getString("type");
    }

    return type;
  }

  /**
   * @param skuJson
   * @return
   */
  private Integer crawlStock(JSONObject skuJson) {
    Integer stock = null;

    if (skuJson.has("stocktienda")) {
      stock = skuJson.getInt("stocktienda");
    }

    return stock;
  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has("SKU")) {
      internalId = skuJson.getString("SKU");
    }

    return internalId;
  }

  private String crawlInternalIdPackage(JSONObject skuJson, String internalPid) {
    String internalId = null;

    if (skuJson.has("id_prod")) {
      internalId = skuJson.getString("id_prod") + "-" + internalPid;
    }

    return internalId;
  }


  private String crawlInternalPid(JSONObject productJson) {
    String internalPid = null;

    if (productJson.has("partNumber")) {
      internalPid = productJson.get("partNumber").toString().replace("-PPP-", "");
    }

    return internalPid;
  }

  private boolean crawlPackageAvailability(JSONObject productJson) {
    boolean availability = false;

    JSONArray arraySkus = productJson.has("children") ? productJson.getJSONArray("children") : new JSONArray();

    for (int i = 0; i < arraySkus.length(); i++) {
      JSONObject skuJson = arraySkus.getJSONObject(i);

      Integer stock = crawlStock(skuJson);
      if (stock != null && stock > 0) {
        availability = true;
      } else {
        availability = false;
        break;
      }
    }

    return availability;
  }

  private String crawlNamePackage(JSONObject productJson) {
    String name = null;

    if (productJson.has("name")) {
      name = productJson.get("name").toString();
    }

    return name;
  }

  private String crawlName(JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (skuJson.has("name")) {
      name.append(skuJson.get("name"));
      if (skuJson.has("specs")) {
        JSONArray specs = skuJson.getJSONArray("specs");

        for (Object o : specs) {
          JSONObject spec = (JSONObject) o;

          if (spec.has("VALUE")) {
            name.append(" ").append(spec.get("VALUE"));
          }
        }
      }
    }

    return name.toString();
  }

  private String crawlSecondaryImages(List<String> images, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (String image : images) {
      if (!image.equalsIgnoreCase(primaryImage)) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private List<String> crawlImages(JSONObject skuJson, Map<String, List<String>> colorsMap, String pid) {
    List<String> images = new ArrayList<>();

    String colorId = pid;

    if (skuJson.has("ESTILOCOLOR")) {
      colorId = skuJson.get("ESTILOCOLOR").toString();
    }

    if (colorId != null) {

      if (colorsMap.containsKey(colorId)) {
        images = colorsMap.get(colorId);
      } else {
        String url = IMAGE_URL_FIRST_PART + "Cencosud/" + colorId + "?req=set,json";

        String response = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, cookies);

        JSONObject json = CrawlerUtils.stringToJson(CrawlerUtils.extractSpecificStringFromScript(response, "esponse(", ",", true));

        if (json.has("set")) {
          JSONObject set = json.getJSONObject("set");

          if (set.has("item")) {
            JSONArray items = new JSONArray();

            if (set.get("item") instanceof JSONArray) {
              items = set.getJSONArray("item");
            } else if (set.get("item") instanceof JSONObject) {
              items.put(set.get("item"));
            }

            for (Object o : items) {
              JSONObject item = (JSONObject) o;

              JSONObject imageJson = new JSONObject();

              if (item.has("s")) {
                imageJson = item.getJSONObject("s");
              } else if (item.has("i")) {
                imageJson = item.getJSONObject("i");
              }

              if (imageJson.has("n")) {
                images.add(IMAGE_URL_FIRST_PART + imageJson.get("n") + "?wid=1080&fmt=jpg");
              }
            }

            colorsMap.put(colorId, images);
          }
        }
      }
    }

    return images;
  }

  private String crawlDescription(JSONObject productJson) {
    StringBuilder description = new StringBuilder();

    if (productJson.has("shortDescription")) {
      description.append(productJson.get("shortDescription").toString());
    }

    if (productJson.has("longDescription")) {
      description.append(Jsoup.parse(productJson.get("longDescription").toString()));
    }

    return description.toString();
  }

  private Prices crawlPrices(JSONObject skuJson, JSONArray arrayPrices) {
    Prices prices = new Prices();

    if (skuJson.has("price")) {
      prices.setPriceFrom(MathUtils.parseDoubleWithComma(skuJson.get("price").toString()));
    }

    for (Object o : arrayPrices) {
      JSONObject cardJson = (JSONObject) o;

      if (cardJson.has("paymentMethodName")) {
        Map<Integer, Float> mapInstallments = new HashMap<>();
        String paymentMethodName = cardJson.getString("paymentMethodName");
        Card card = null;

        if (paymentMethodName.equalsIgnoreCase("amex")) {
          card = Card.AMEX;
        } else if (paymentMethodName.equalsIgnoreCase("visa")) {
          card = Card.VISA;
        } else if (paymentMethodName.equalsIgnoreCase("master card")) {
          card = Card.MASTERCARD;
        }

        if (cardJson.has("installmentOptions")) {
          for (Object obj : cardJson.getJSONArray("installmentOptions")) {
            JSONObject installmentJson = (JSONObject) obj;

            if (installmentJson.has("option") && installmentJson.has("amount")) {
              String installment = installmentJson.get("option").toString().replaceAll("[^0-9]", "");
              Float value = MathUtils.parseFloatWithComma(installmentJson.get("amount").toString());

              if (!installment.isEmpty() && value != null) {
                mapInstallments.put(Integer.parseInt(installment), value);
              }
            }
          }
        }

        if (card != null) {
          prices.insertCardInstallment(card.toString(), mapInstallments);
        }
      }
    }

    return prices;

  }

  public JSONArray fetchPrices(JSONObject skuJson, boolean available) {
    JSONArray jsonPrice = new JSONArray();

    if (available && skuJson.has("price_internet")) {
      String url = "https://www.paris.cl/webapp/wcs/stores/servlet/GetCatalogEntryInstallmentPrice?storeId=10801"
          + "&langId=-5&catalogId=40000000629&catalogEntryId=152117851&nonInstallmentPrice=" + skuJson.get("price_internet");

      String json = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, url, null, null);
      jsonPrice = CrawlerUtils.stringToJsonArray(json.replace("*/", "").replace("/*", "").trim());
    }

    return jsonPrice;
  }
}
