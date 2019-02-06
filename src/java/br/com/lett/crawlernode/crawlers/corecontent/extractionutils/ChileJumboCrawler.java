package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.cookie.Cookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class ChileJumboCrawler {

  public static final String JUMBO_DEHESA_ID = "13";
  public static final String JUMBO_LAFLORIDA_ID = "18";
  public static final String JUMBO_LAREINA_ID = "11";
  public static final String JUMBO_LOSDOMINICOS_ID = "12";
  public static final String JUMBO_LASCONDES_ID = "12";
  public static final String JUMBO_VINA_ID = "16";

  public static final String RANKING_SELECTOR = "li[layout] .product-item";
  public static final String RANKING_SELECTOR_URL = ".product-item__info a";
  public static final String RANKING_SELECTOR_TOTAL = ".resultado-busca-numero";
  public static final String RANKING_ATTRIBUTE_ID = "data-id";

  private static final String MAIN_SELLER_NAME_LOWER = "jumbo";
  public static final String HOME_PAGE = "https://nuevo.jumbo.cl/";
  public static final String HOST = "nuevo.jumbo.cl";

  private JSONObject promotionsJson = new JSONObject();

  private Session session;
  private List<Cookie> cookies = new ArrayList<>();
  private Logger logger;

  public ChileJumboCrawler(Session session, List<Cookie> cookies, Logger logger) {
    this.session = session;
    this.cookies = cookies;
    this.logger = logger;
  }

  public List<Product> extractProducts(Document doc) {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, this.cookies);
      vtexUtil.setHasBankTicket(false);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a", false);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription", "#caracteristicas table.Ficha-Tecnica"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        JSONObject descriptionApi = vtexUtil.crawlDescriptionAPI(internalId, "skuId");
        Float pricePromotion = getPromotionShopCardPrice(descriptionApi, internalId);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        if (pricePromotion != null) {
          setPricePromotionInMarketplaceMap(pricePromotion, marketplaceMap);
        }
        List<String> mainSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, mainSellers, Card.AMEX, session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, mainSellers);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, mainSellers);
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);

        String ean = i < eanArray.length() ? eanArray.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
    }

    return products;
  }

  private static boolean isProductPage(Document document) {
    return document.select(".product-content").first() != null;
  }

  private void setPricePromotionInMarketplaceMap(Float price, Map<String, Prices> marketplaceMap) {
    Prices pricesToBeSet = new Prices();
    String selletToBeSet = null;

    for (Entry<String, Prices> entry : marketplaceMap.entrySet()) {
      Prices prices = entry.getValue();
      Map<Integer, Float> shopCardMap = new HashMap<>();
      shopCardMap.put(1, price);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), shopCardMap);

      pricesToBeSet = prices;
      selletToBeSet = entry.getKey();
      break;
    }

    if (selletToBeSet != null) {
      marketplaceMap.put(selletToBeSet, pricesToBeSet);
    }
  }

  private JSONObject crawlPromotionsAPI() {
    return DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session,
        "https://nuevo.jumbo.cl/jumbo/dataentities/PM/documents/Promos?_fields=value%2Cid", null, cookies);
  }

  private Float getPromotionShopCardPrice(JSONObject descriptionApi, String id) {
    Float pricePromotion = null;

    if (descriptionApi.has("JumboAhora")) {
      String ahora = descriptionApi.get("JumboAhora").toString().toLowerCase();

      if (ahora.contains("si") && descriptionApi.has("SkuData")) {
        JSONObject promotionsData = new JSONObject();
        JSONArray skuData = descriptionApi.getJSONArray("SkuData");

        for (Object o : skuData) {
          JSONObject sku = CrawlerUtils.stringToJson(o.toString());

          if (sku.has(id)) {
            promotionsData = sku.getJSONObject(id);
            break;
          }
        }

        if (this.promotionsJson.length() < 1) {
          this.promotionsJson = crawlPromotionsAPI();
        }

        JSONObject promotionsList = this.promotionsJson.has("value") ? this.promotionsJson.getJSONObject("value") : new JSONObject();

        if (promotionsData.has("promotions")) {
          for (Object o : promotionsData.getJSONArray("promotions")) {
            if (promotionsList.has(o.toString())) {
              JSONObject promotionJson = promotionsList.getJSONObject(o.toString());

              if (promotionJson.has("group") && promotionJson.has("discountType") && promotionJson.has("value")) {
                String group = promotionJson.get("group").toString();
                String discountType = promotionJson.get("discountType").toString();

                if (group.equalsIgnoreCase("t-cenco") && !discountType.equalsIgnoreCase("percentual")) {
                  pricePromotion = CrawlerUtils.getFloatValueFromJSON(promotionJson, "value");
                  break;
                }
              }
            }
          }
        }
      }
    }

    return pricePromotion;
  }
}
