package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * 
 * 1) Only one sku per page.
 * 
 * Price crawling notes: 1) In time crawler was made, there no product unnavailable. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito).
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoWalmartsuperCrawler extends Crawler {

  private static final String HOME_PAGE = "https://super.walmart.com.mx";

  public MexicoWalmartsuperCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    String url = session.getOriginalURL();

    if (url.contains("?")) {
      url = url.split("\\?")[0];
    }

    String finalParameter = CommonMethods.getLast(url.split("/"));

    if (finalParameter.contains("_")) {
      finalParameter = CommonMethods.getLast(finalParameter.split("_")).trim();
    }

    String apiUrl =
        "https://super.walmart.com.mx/api/rest/model/atg/commerce/catalog/ProductCatalogActor/getSkuSummaryDetails?storeId=0000009999&upc="
            + finalParameter + "&skuId=" + finalParameter;

    return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, apiUrl, null, cookies);
  }

  @Override
  public List<Product> extractInformation(JSONObject apiJson) throws Exception {
    super.extractInformation(apiJson);
    List<Product> products = new ArrayList<>();

    if (apiJson.has("skuId")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(apiJson);
      String name = crawlName(apiJson);
      Float price = crawlPrice(apiJson);
      Prices prices = crawlPrices(price);
      boolean available = crawlAvailability(apiJson);
      CategoryCollection categories = crawlCategories(apiJson);
      String primaryImage = crawlPrimaryImage(internalId);
      String secondaryImages = crawlSecondaryImages(apiJson);
      String description = crawlDescription(apiJson);
      Integer stock = null;

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlInternalId(JSONObject apiJson) {
    String internalId = null;

    if (apiJson.has("skuId")) {
      internalId = apiJson.getString("skuId");
    }

    return internalId;
  }

  private String crawlName(JSONObject apiJson) {
    String name = null;

    if (apiJson.has("skuDisplayNameText")) {
      name = apiJson.getString("skuDisplayNameText");
    }

    return name;
  }

  private Float crawlPrice(JSONObject apiJson) {
    Float price = null;

    if (apiJson.has("specialPrice")) {
      String priceText = apiJson.get("specialPrice").toString().replaceAll("[^0-9.]", "");

      if (!priceText.isEmpty()) {
        price = Float.parseFloat(priceText);
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject apiJson) {
    boolean available = false;

    if (apiJson.has("status")) {
      String status = apiJson.getString("status");

      available = status.equalsIgnoreCase("SELLABLE");
    }

    return available;
  }

  private String crawlPrimaryImage(String id) {

    return "https://super.walmart.com.mx/images/product-images/img_large/" + id + "L.jpg";
  }

  /**
   * Não achei imagens secundarias
   * 
   * @param document
   * @return
   */
  private String crawlSecondaryImages(JSONObject apiJson) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(JSONObject apiJson) {
    CategoryCollection categories = new CategoryCollection();

    if (apiJson.has("breadcrumb")) {
      JSONObject breadcrumb = apiJson.getJSONObject("breadcrumb");

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

  private String crawlDescription(JSONObject apiJson) {
    StringBuilder description = new StringBuilder();

    if (apiJson.has("longDescription")) {
      description.append("<div id=\"desc\"> <h3> Descripción </h3>");
      description.append(apiJson.get("longDescription") + "</div>");
    }

    StringBuilder nutritionalTable = new StringBuilder();
    StringBuilder caracteristicas = new StringBuilder();

    if (apiJson.has("attributesMap")) {
      JSONObject attributesMap = apiJson.getJSONObject("attributesMap");

      for (String key : attributesMap.keySet()) {
        JSONObject attribute = attributesMap.getJSONObject(key);

        if (attribute.has("attrGroupId")) {
          JSONObject attrGroupId = attribute.getJSONObject("attrGroupId");

          if (attrGroupId.has("optionValue")) {
            String optionValue = attrGroupId.getString("optionValue");

            if (optionValue.equalsIgnoreCase("Tabla nutrimental")) {
              setAPIDescription(attribute, nutritionalTable);
            } else if (optionValue.equalsIgnoreCase("Caracterisitcas")) {
              setAPIDescription(attribute, caracteristicas);
            }
          }
        }
      }
    }

    if (!nutritionalTable.toString().isEmpty()) {
      description.append("<div id=\"table\"> <h3> Nutrición </h3>");
      description.append(nutritionalTable + "</div>");
    }

    if (!caracteristicas.toString().isEmpty()) {
      description.append("<div id=\"short\"> <h3> Características </h3>");
      description.append(caracteristicas + "</div>");
    }

    return description.toString();
  }

  private void setAPIDescription(JSONObject attributesMap, StringBuilder desc) {
    if (attributesMap.has("attrDesc") && attributesMap.has("value")) {
      desc.append("<div>");
      desc.append("<span float=\"left\">" + attributesMap.get("attrDesc") + "&nbsp </span>");
      desc.append("<span float=\"right\">" + attributesMap.get("value") + " </span>");
      desc.append("</div>");
    }
  }

  /**
   * There is no bankSlip price.
   * 
   * There is no card payment options, other than cash price. So for installments, we will have only
   * one installment for each card brand, and it will be equals to the price crawled on the sku main
   * page.
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

}
