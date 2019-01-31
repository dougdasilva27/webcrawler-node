package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
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
 * Date: 30/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class MexicoHebCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.heb.com.mx/";
  private static final String SELLER_NAME_LOWER = "heb";

  public MexicoHebCrawler(Session session) {
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
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(doc);
      CategoryCollection categories =
          CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home):not(.product)");
      boolean available = !doc.select(".availability.in-stock").isEmpty();
      String primaryImage =
          CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img:not(#image-main)",
              Arrays.asList("data-zoom-image", "src"), "https:", "www.heb.com.mx/");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
          ".product-image-gallery img:not(#image-main)", Arrays.asList("data-zoom-image", "src"),
          "https:", "www.heb.com.mx/", primaryImage);
      String description = crawlDescription(doc);
      String ean = scrapEan(doc, ".extra-info span span[data-upc]");

      Map<String, JSONObject> productsMap = extractProductsJson(doc);

      if (productsMap.isEmpty()) {
        String internalId = internalPid;
        String name = crawlName(doc, new JSONObject());
        Map<String, Prices> marketplaceMap =
            available ? crawlMarketplace(doc, new JSONObject(), internalPid) : new HashMap<>();
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap,
            Arrays.asList(SELLER_NAME_LOWER), Card.AMEX, session);
        Prices prices =
            marketplaceMap.containsKey(SELLER_NAME_LOWER) ? marketplaceMap.get(SELLER_NAME_LOWER)
                : new Prices();
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Arrays.asList(Card.AMEX));

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
            .setDescription(description).setStock(null).setMarketplace(marketplace).build();

        products.add(product);
      } else {

        for (Entry<String, JSONObject> entry : productsMap.entrySet()) {
          JSONObject skuJson = entry.getValue();
          String internalId = entry.getKey();
          String name = crawlName(doc, skuJson);
          Map<String, Prices> marketplaceMap =
              available ? crawlMarketplace(doc, skuJson, internalPid) : new HashMap<>();
          Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap,
              Arrays.asList(SELLER_NAME_LOWER), Card.AMEX, session);
          Prices prices =
              marketplaceMap.containsKey(SELLER_NAME_LOWER) ? marketplaceMap.get(SELLER_NAME_LOWER)
                  : new Prices();
          Float price = CrawlerUtils.extractPriceFromPrices(prices, Arrays.asList(Card.AMEX));

          // Creating the product
          Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
              .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
              .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
              .setDescription(description).setStock(null).setMarketplace(marketplace).build();

          products.add(product);
        }
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=product]").isEmpty();
  }

  private Map<String, JSONObject> extractProductsJson(Document doc) {
    Map<String, JSONObject> productsMap = new HashMap<>();

    JSONObject json =
        CrawlerUtils.selectJsonFromHtml(doc, "script", "Product.Config(", ");", false, true);
    if (json.has("attributes")) {
      JSONObject attributes = json.getJSONObject("attributes");

      for (String key : attributes.keySet()) {
        JSONObject attribute = attributes.getJSONObject(key);

        if (attribute.has("options")) {
          JSONArray options = attribute.getJSONArray("options");

          for (Object o : options) {
            JSONObject product = (JSONObject) o;

            if (product.has("label") && product.has("products")) {

              JSONArray products = product.getJSONArray("products");

              for (Object obj : products) {
                String id = obj.toString();

                if (productsMap.containsKey(id)) {
                  JSONObject productInfo = productsMap.get(id);

                  String name = "";
                  if (productInfo.has("name")) {
                    name = productInfo.get("name").toString();
                  }

                  if (product.has("label")) {
                    productInfo.put("name", name + " " + product.get("label"));
                  }

                  if (!productInfo.has("price") && product.has("price")
                      && !product.get("price").toString().equals("0")) {
                    productInfo.put("price", product.get("price"));
                  }

                  if (!productInfo.has("oldPrice") && product.has("oldPrice")
                      && !product.get("oldPrice").toString().equals("0")) {
                    productInfo.put("oldPrice", product.get("oldPrice"));
                  }

                  productsMap.put(id, productInfo);
                } else {
                  JSONObject productInfo = new JSONObject();

                  if (product.has("label")) {
                    productInfo.put("name", product.get("label"));
                  }

                  if (product.has("price") && !product.get("price").toString().equals("0")) {
                    productInfo.put("price", product.get("price"));
                  }

                  if (product.has("oldPrice") && !product.get("oldPrice").toString().equals("0")) {
                    productInfo.put("oldPrice", product.get("oldPrice"));
                  }

                  productsMap.put(id, productInfo);
                }
              }
            }
          }
        }
      }
    }

    return productsMap;
  }

  private Map<String, Prices> crawlMarketplace(Document doc, JSONObject jsonSku,
      String internalPid) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName;

    Element seller = doc.selectFirst(".infoProduct strong a.btn");
    if (seller != null) {
      sellerName = seller.ownText().toLowerCase().trim();
    } else {
      sellerName = SELLER_NAME_LOWER;
    }

    marketplace.put(sellerName, crawlPrices(doc, jsonSku, internalPid));

    return marketplace;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element pid = doc.selectFirst("input[name=product]");
    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private String crawlName(Document doc, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    Element productName = doc.selectFirst(".product-name .h1");
    if (productName != null) {
      name.append(productName.text());

      Element marca = doc.selectFirst(".product-marca");
      if (marca != null) {
        name.append(" ");
        name.append(marca.text());
      }

      if (skuJson.has("name")) {
        name.append(" ").append(skuJson.get("name"));
      }
    }

    return name.toString().trim();
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements descriptions = doc.select(".product-collateral dd.tab-container");
    for (Element e : descriptions) {
      if (e.select("#customer-reviews").isEmpty()) {
        description.append(e.html());
      }
    }

    return description.toString();
  }

  private Prices crawlPrices(Document doc, JSONObject jsonSku, String internalPid) {
    Prices prices = new Prices();

    Float price =
        CrawlerUtils.scrapSimplePriceFloatWithDots(doc, "#product-price-" + internalPid, false);
    if (price == null) {
      price = CrawlerUtils.scrapSimplePriceFloatWithDots(doc, ".price-info .special-price .price",
          false);
    }

    if (jsonSku.has("price")) {
      price = MathUtils.parseFloatWithDots(jsonSku.get("price").toString());
    }

    prices.setPriceFrom(
        CrawlerUtils.scrapSimplePriceDoubleWithDots(doc, "#old-price-" + internalPid, false));
    if (jsonSku.has("oldPrice")) {
      prices.setPriceFrom(MathUtils.parseDoubleWithDot(jsonSku.get("oldPrice").toString()));
    }

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

  private String scrapEan(Document doc, String selector) {
    String ean = null;
    Element e = doc.selectFirst(selector);

    if (e != null) {
      String aux = e.attr("data-upc");
      ean = aux.length() == 12 ? "0" + aux : aux;
    }

    return ean;
  }
}
