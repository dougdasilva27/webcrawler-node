package br.com.lett.crawlernode.crawlers.corecontent.unitedstates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.UnitedstatesWalmartCrawlerUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 15/11/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class UnitedstatesWalmartCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.walmart.com";
  private static final String SELLER_NAME_LOWER = "walmart.com";

  public UnitedstatesWalmartCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public void handleCookiesBeforeFetch() {
    BasicClientCookie cookie = new BasicClientCookie("akgeo", "US");
    cookie.setDomain("www.walmart.com");
    cookie.setPath("/");
    this.cookies.add(cookie);

    BasicClientCookie cookie2 = new BasicClientCookie("usgmtbgeo", "US");
    cookie2.setDomain(".www.walmart.com");
    cookie2.setPath("/");
    this.cookies.add(cookie2);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    JSONArray skus = UnitedstatesWalmartCrawlerUtils.sanitizeINITIALSTATEJson(doc);

    if (skus.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String description = crawlDescription(doc);
      CategoryCollection categories = crawlCategories(doc);

      for (Object sku : skus) {
        JSONObject skuJson = (JSONObject) sku;

        String internalId = crawlInternalId(skuJson);
        String internalPid = crawlInternalPid(skuJson);
        String name = crawlName(skuJson, doc);
        String primaryImage = crawlPrimaryImage(skuJson);
        String secondaryImages = crawlSecondaryImages(skuJson);
        Map<String, Prices> marketplaceMap = crawlMarketplaces(skuJson);
        Marketplace marketplace =
            CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(UnitedstatesWalmartCrawlerUtils.SELLER_NAME_LOWER), session);
        Float price = crawlPrice(marketplaceMap);
        Prices prices = crawlPrices(marketplaceMap);
        boolean available = crawlAvailability(marketplaceMap);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private String crawlInternalId(JSONObject skuJson) {
    String internalId = null;

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.INTERNAL_ID)) {
      internalId = skuJson.getString(UnitedstatesWalmartCrawlerUtils.INTERNAL_ID);
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    String internalPid = null;

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.INTERNAL_PID)) {
      internalPid = skuJson.getString(UnitedstatesWalmartCrawlerUtils.INTERNAL_PID);
    }

    return internalPid;
  }


  private String crawlName(JSONObject skuJson, Document doc) {
    StringBuilder name = new StringBuilder();

    Element nameElement = doc.select(".prod-ProductTitle").first();
    if (nameElement != null) {
      name.append(nameElement.text().trim());
    }

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.NAME)) {
      name.append(" " + skuJson.getString(UnitedstatesWalmartCrawlerUtils.NAME));
    }

    return name.toString();
  }

  private Float crawlPrice(Map<String, Prices> marketplaces) {
    Float price = null;
    Prices prices = new Prices();

    if (marketplaces.containsKey(SELLER_NAME_LOWER)) {
      prices = marketplaces.get(SELLER_NAME_LOWER);
    }

    if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.SHOP_CARD.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.SHOP_CARD.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaces) {
    boolean available = false;

    for (String seller : marketplaces.keySet()) {
      if (seller.equalsIgnoreCase(SELLER_NAME_LOWER)) {
        available = true;
        break;
      }
    }

    return available;
  }

  private Map<String, Prices> crawlMarketplaces(JSONObject skuJson) {
    Map<String, Prices> marketplace = new HashMap<>();

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.OFFERS)) {
      JSONArray offers = skuJson.getJSONArray(UnitedstatesWalmartCrawlerUtils.OFFERS);

      for (Object offerObj : offers) {
        JSONObject offerJson = (JSONObject) offerObj;

        if (offerJson.has(UnitedstatesWalmartCrawlerUtils.OFFERS_AVAILABLE) && offerJson.getBoolean(UnitedstatesWalmartCrawlerUtils.OFFERS_AVAILABLE)
            && offerJson.has(UnitedstatesWalmartCrawlerUtils.OFFERS_PRICE) && offerJson.has(UnitedstatesWalmartCrawlerUtils.OFFERS_SELLER_NAME)) {

          Float price = CrawlerUtils.getFloatPriceFromJSON(offerJson, UnitedstatesWalmartCrawlerUtils.OFFERS_PRICE);
          Double oldPrice = null;
          if (offerJson.has(UnitedstatesWalmartCrawlerUtils.OFFERS_OLD_PRICE)) {
            oldPrice = CrawlerUtils.getDoublePriceFromJSON(offerJson, UnitedstatesWalmartCrawlerUtils.OFFERS_OLD_PRICE);
          }

          marketplace.put(offerJson.get(UnitedstatesWalmartCrawlerUtils.OFFERS_SELLER_NAME).toString(), crawlPrices(price, oldPrice));
        }
      }
    }

    return marketplace;
  }

  private String crawlPrimaryImage(JSONObject skuJson) {
    String primaryImage = null;

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.IMAGES)) {
      JSONObject images = skuJson.getJSONObject(UnitedstatesWalmartCrawlerUtils.IMAGES);

      if (images.has(UnitedstatesWalmartCrawlerUtils.IMAGES_PRIMARY)) {
        primaryImage = images.getString(UnitedstatesWalmartCrawlerUtils.IMAGES_PRIMARY);
      }
    }

    return primaryImage;
  }

  /**
   * Quando este crawler foi feito, nao tinha imagens secundarias
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONObject skuJson) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (skuJson.has(UnitedstatesWalmartCrawlerUtils.IMAGES)) {
      JSONObject images = skuJson.getJSONObject(UnitedstatesWalmartCrawlerUtils.IMAGES);

      if (images.has(UnitedstatesWalmartCrawlerUtils.IMAGES_SECONDARY)) {
        secondaryImagesArray = images.getJSONArray(UnitedstatesWalmartCrawlerUtils.IMAGES_SECONDARY);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb-slash a");

    for (int i = 1; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".prod-IDML-container").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }


  /**
   * 
   * @param marketplaceMap
   * @return
   */
  private Prices crawlPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    if (marketplaceMap.containsKey(SELLER_NAME_LOWER)) {
      prices = marketplaceMap.get(SELLER_NAME_LOWER);
    }

    return prices;
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Double oldPrice) {
    Prices prices = new Prices();
    Map<Integer, Float> installments = new HashMap<>();

    if (price != null) {
      installments.put(1, price);
      prices.setPriceFrom(oldPrice);

      if (!installments.isEmpty()) {
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), installments);
      }
    }

    return prices;
  }
}
