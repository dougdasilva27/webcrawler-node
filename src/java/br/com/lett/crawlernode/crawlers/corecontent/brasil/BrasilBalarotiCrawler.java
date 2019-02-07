package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;


public class BrasilBalarotiCrawler extends Crawler {

  public BrasilBalarotiCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.balaroti.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "balaroti";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "balaroti comércio de materiais de construção sa";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("VTEXSC", "sc=1");
    cookie.setDomain(".balaroti.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER_2, HOME_PAGE, cookies);
      vtexUtil.setBankTicketDiscount(5);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
      String description = crawlDescription(internalPid, vtexUtil);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace =
            CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2), session);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) || marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = getPrices(marketplaceMap);
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
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private Prices getPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    if (marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER)) {
      prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER);
    } else if (marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2)) {
      prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER_2);
    }

    return prices;
  }

  private String crawlDescription(String internalPid, VTEXCrawlersUtils vtexUtil) {
    StringBuilder description = new StringBuilder();

    JSONObject descriptionApi = vtexUtil.crawlDescriptionAPI(internalPid, "productId");
    if (descriptionApi.has("description")) {
      description.append(descriptionApi.get("description").toString());
    }

    List<String> specs = new ArrayList<>();

    if (descriptionApi.has("allSpecifications")) {
      JSONArray keys = descriptionApi.getJSONArray("allSpecifications");
      for (Object o : keys) {
        if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
          specs.add(o.toString());
        }
      }
    }

    for (String spec : specs) {

      description.append("<div>");
      description.append("<h4>").append(spec).append("</h4>");
      description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionApi.get(spec)));
      description.append("</div>");
    }

    return description.toString();
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }
}
