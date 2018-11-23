package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 27/09/2018
 * 
 * @author gabriel
 *
 */

public class BrasilZattiniCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.zattini.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "zattini";
  private static final String PROTOCOL = "https:";

  public BrasilZattiniCrawler(Session session) {
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

    JSONObject chaordicJson = crawlChaordicJson(doc);

    if (chaordicJson.length() > 0) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(chaordicJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a span", false);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#features"));

      // sku data in json
      JSONArray arraySkus = chaordicJson != null && chaordicJson.has("skus") ? chaordicJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(doc, jsonSku);
        boolean availableToBuy = jsonSku.has("status") && jsonSku.get("status").toString().equalsIgnoreCase("available");

        Map<String, Prices> marketplaceMap = availableToBuy ? crawlMarketplace(doc) : new HashMap<>();
        boolean available = availableToBuy && marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);

        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), Card.VISA, session);
        Prices prices = available ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".photo-figure > img", Arrays.asList("data-large-img-url", "src"), PROTOCOL,
            "static.zattini.com.br");
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".swiper-slide:not(.active) img",
            Arrays.asList("data-src-large", "src"), PROTOCOL, "static.zattini.com.br", primaryImage);

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

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = json.getString("sku").trim();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject skuJson) {
    String internalPid = null;

    if (skuJson.has("id")) {
      internalPid = skuJson.get("id").toString();
    }

    return internalPid;
  }

  private String crawlName(Document doc, JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    Element nameElement = doc.selectFirst("h1[itemprop=name]");
    if (nameElement != null) {
      name.append(nameElement.ownText());

      if (skuJson.has("specs")) {
        JSONObject specs = skuJson.getJSONObject("specs");

        Set<String> keys = specs.keySet();

        for (String key : keys) {
          if (!key.equalsIgnoreCase("color")) {
            name.append(" " + specs.get(key));
          }
        }
      }
    }

    return name.toString();
  }

  private Map<String, Prices> crawlMarketplace(Document doc) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName = MAIN_SELLER_NAME_LOWER;
    Element sellerNameElement = doc.selectFirst(".product-seller-name");

    if (sellerNameElement != null) {
      sellerName = sellerNameElement.ownText().toLowerCase();
    }

    marketplace.put(sellerName, crawlPrices(doc));

    return marketplace;

  }

  /**
   * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
   * à vista R$ 1.790,00
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();

    Element priceElement = doc.selectFirst(".price.normal [itemprop=price]");

    if (priceElement == null) {
      priceElement = doc.selectFirst(".price [itemprop=price]");
    }

    if (priceElement != null) {
      Float price = MathUtils.parseFloatWithComma(priceElement.ownText());

      if (price != null) {
        prices.setBankTicketPrice(price);

        Map<Integer, Float> mapInstallments = new HashMap<>();
        mapInstallments.put(1, price);

        Element priceFrom = doc.selectFirst(".buy-box .reduce");
        if (priceFrom != null) {
          prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.ownText()));
        }

        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".block.prices .installments .installments-price", doc, true);
        if (!pair.isAnyValueNull()) {
          mapInstallments.put(pair.getFirst(), pair.getSecond());
        }

        prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
        prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
        prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
        prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
        prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
        prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
      }
    }

    return prices;
  }

  private JSONObject crawlChaordicJson(Document doc) {
    JSONObject skuJson = new JSONObject();

    Elements scripts = doc.select("script");

    for (Element e : scripts) {
      String script = e.outerHtml();


      if (script.contains("freedom.metadata.chaordic(")) {
        String token = "loader.js', '";
        int x = script.indexOf(token) + token.length();
        int y = script.indexOf("');", x);

        String json = script.substring(x, y);

        if (json.startsWith("{") && json.endsWith("}")) {
          try {
            JSONObject chaordic = new JSONObject(json);

            if (chaordic.has("product")) {
              skuJson = chaordic.getJSONObject("product");
            }
          } catch (Exception e1) {
            Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
          }
        }

        break;
      }
    }

    return skuJson;
  }
}
