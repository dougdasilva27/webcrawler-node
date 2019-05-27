package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
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


public class MexicoWalmartCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.walmart.com.mx";
  private static final String SELLER = "walmart";

  public MexicoWalmartCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document document) throws Exception {
    super.extractInformation(document);
    List<Product> products = new ArrayList<>();
    String internalId = crawlInternalId(session.getOriginalURL());

    String url = "https://www.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getProduct?id=" + internalId;
    Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
    JSONObject apiJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

    if (apiJson.has("product")) {

      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      JSONObject productJson = apiJson.getJSONObject("product");

      CategoryCollection categories = crawlCategories(productJson);

      JSONArray childSkus = productJson.getJSONArray("childSKUs");

      for (Object object : childSkus) {
        JSONObject sku = (JSONObject) object;

        String name = crawlName(sku);
        String primaryImage = crawlPrimaryImage(internalId);
        String secondaryImages = crawlSecondaryImages(internalId);
        String description = crawlDescription(sku);
        Integer stock = null;
        String ean = scrapEan(sku);
        List<String> eans = new ArrayList<>();
        eans.add(ean);

        Map<String, Prices> marketplaceMap = crawlMarketplace(sku);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER), Arrays.asList(Card.AMEX), session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, Arrays.asList(SELLER));
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(SELLER));
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.AMEX);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private Map<String, Prices> crawlMarketplace(JSONObject sku) {

    Map<String, Prices> map = new HashMap<>();
    String name = null;
    Float price = null;

    if (sku.has("offerList")) {
      JSONArray offerList = sku.getJSONArray("offerList");

      for (Object object2 : offerList) {
        Prices prices = new Prices();
        JSONObject list = (JSONObject) object2;

        if (list.has("sellerName")) {
          name = list.getString("sellerName").toLowerCase().trim();

          if (list.has("priceInfo")) {
            JSONObject priceInfo = list.getJSONObject("priceInfo");
            price = priceInfo.getFloat("specialPrice");

            if (price != null) {
              Map<Integer, Float> installmentPriceMap = new TreeMap<>();
              installmentPriceMap.put(1, price);
              prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

            }

            if (priceInfo.has("originalPrice")) {
              prices.setPriceFrom(priceInfo.getDouble("originalPrice"));
            }

          }
        }
        map.put(name, prices);
      }
    }
    return map;
  }

  private String scrapEan(JSONObject sku) {
    String ean = null;

    if (sku.has("upc")) {
      ean = sku.getString("upc");
    }

    return ean;
  }

  private String crawlInternalId(String url) {
    String internalId = null;

    if (url.contains("_")) {
      if (url.contains("?")) {
        url = url.split("\\?")[0];
      }

      String[] tokens = url.split("_");
      internalId = tokens[tokens.length - 1].replaceAll("[^0-9]", "").trim();
    }

    return internalId;
  }

  private String crawlName(JSONObject sku) {
    String name = null;

    if (sku.has("displayName")) {
      name = sku.getString("displayName");
    }

    return name;
  }

  private String crawlPrimaryImage(String id) {

    return "https://www.walmart.com.mx/images/product-images/img_large/" + id + "L.jpg";
  }

  private String crawlSecondaryImages(String id) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (int i = 1; i < 4; i++) {
      String img = "https://super.walmart.com.mx/images/product-images/img_large/" + id + "L" + i + ".jpg";
      Request request = RequestBuilder.create().setUrl(img).setCookies(cookies).build();
      RequestsStatistics resp = CommonMethods.getLast(this.dataFetcher.get(session, request).getRequests());

      if (resp != null && resp.getStatusCode() > 0 && resp.getStatusCode() < 400) {
        secondaryImagesArray.put(img);
      }
    }


    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }


  private CategoryCollection crawlCategories(JSONObject sku) {
    CategoryCollection categories = new CategoryCollection();

    if (sku.has("breadcrumb")) {
      JSONObject breadcrumb = sku.getJSONObject("breadcrumb");

      if (breadcrumb.has("departmentName")) {
        categories.add(breadcrumb.get("departmentName").toString());
      }

      if (breadcrumb.has("familyName")) {
        categories.add(breadcrumb.get("familyName").toString());
      }

      if (breadcrumb.has("fineLineName")) {
        categories.add(breadcrumb.get("fineLineName").toString());
      }
    }

    return categories;
  }

  private String crawlDescription(JSONObject sku) {
    StringBuilder description = new StringBuilder();

    if (sku.has("longDescription")) {
      description.append("<div id=\"desc\"> <h3> Descripci�n </h3>");
      description.append(sku.get("longDescription") + "</div>");
    }

    return description.toString();
  }
}
