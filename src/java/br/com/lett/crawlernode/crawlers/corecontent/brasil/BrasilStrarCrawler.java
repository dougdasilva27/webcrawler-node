package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
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

public class BrasilStrarCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.strar.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "str ar condicionado";

  public BrasilStrarCrawler(Session session) {
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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = crawlCategories(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = scrapPrices(marketplaceMap, internalId);
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String finalUrl =
            internalId != null && !internalId.isEmpty() ? CrawlerUtils.crawlFinalUrl(session.getOriginalURL(), session) : session.getOriginalURL();

        String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        String description = crawlDescription(doc, ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(finalUrl).setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
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


  private Prices scrapPrices(Map<String, Prices> marketplaceMap, String internalId) {
    Prices prices = new Prices();
    String url =
        "https://service.smarthint.co/box/GetInitialData?callback=jQuery183017903389537648629_1554390035009&key=SH-940097&pageType=product&_="
            + internalId;
    Float spotPriceValue = null;
    prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
    JSONObject spotPriceJson = DataFetcher.fetchJSONObject(DataFetcher.GET_REQUEST, session, url, null, cookies);
    System.err.println(spotPriceJson);
    if (spotPriceJson.has("Templates")) {
      System.err.println("1");
      JSONArray templates = spotPriceJson.getJSONArray("Templates");
      JSONObject template = templates.getJSONObject(0);
      if (template.has("Html")) {
        System.err.println("2");
        Document spotPriceDocument = Jsoup.parse(template.getString("Html"));
        spotPriceValue = CrawlerUtils.scrapFloatPriceFromHtml(spotPriceDocument, ".in-cash", null, false, ',');
      }
    }

    prices.setBankTicketPrice(spotPriceValue);
    return prices;
  }

  private boolean isProductPage(Document document) {
    return document.select(".productName").first() != null;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".bread-crumb li > a");

    for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }

  private String crawlDescription(Document document, String ean) {
    StringBuilder description = new StringBuilder();

    Element shortDesc = document.select(".productSpecification").first();

    if (shortDesc != null) {
      description.append(shortDesc.html().replace("style=\"display: none;\"", ""));
    }

    description.append(CrawlerUtils.crawlDescriptionFromFlixMedia("7034", ean, session));

    return description.toString();
  }
}
