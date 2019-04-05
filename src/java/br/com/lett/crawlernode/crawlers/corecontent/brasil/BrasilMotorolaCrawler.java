package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;


public class BrasilMotorolaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.motorola.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "morotola";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "motorola";

  public BrasilMotorolaCrawler(Session session) {
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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      Integer discount = scrapDiscount(doc);
      vtexUtil.setBankTicketDiscount(discount);
      vtexUtil.setCardDiscount(discount);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li:not(:first-child) a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#caracteristicas"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap,
            Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2), Card.VISA, session);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) || marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER_2);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2));
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

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
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private Integer scrapDiscount(Document doc) {
    Integer discount = 0;

    Element d = doc.selectFirst(".flags-hightlight p[class^=\"flag -c--desconto-a-vista\"]");
    if (d != null) {
      String classText = d.attr("class");

      if (classText.contains("--cartao")) {
        String text = CommonMethods.getLast(classText.split("--cartao")[0].split("-")).replaceAll("[^0-9]", "");

        if (!text.isEmpty()) {
          discount = Integer.parseInt(text);
        }
      }
    }

    return discount;
  }

  public String crawlName(JSONObject jsonSku, JSONObject skuJson) {
    String name = null;

    String nameVariation = jsonSku.has("skuname") ? jsonSku.getString("skuname") : null;

    if (skuJson.has("name")) {
      name = skuJson.getString("name");

      if (nameVariation != null) {
        name += " " + nameVariation;
      }
    }

    return name;
  }
}
