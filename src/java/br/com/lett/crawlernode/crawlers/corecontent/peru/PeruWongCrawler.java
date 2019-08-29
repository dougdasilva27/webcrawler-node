package br.com.lett.crawlernode.crawlers.corecontent.peru;

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
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class PeruWongCrawler extends Crawler {
  public PeruWongCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.wong.pe/";
  private static final String MAIN_SELLER_NAME_LOWER = "wong";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "metro";

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      String internalPid = vtexUtil.crawlInternalPid(skuJson);

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
      String description = crawlDescription(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();
      JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);


      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        List<String> wongSellers = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, wongSellers, Arrays.asList(Card.VISA), session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, wongSellers);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, wongSellers);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
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
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element descriptionElement = doc.selectFirst(".description");
    if (descriptionElement != null) {
      Element descriptionContent = descriptionElement.selectFirst(".productDescription");

      if (descriptionContent != null && !descriptionContent.html().trim().isEmpty()) {
        description.append(descriptionElement.html());
      }
    }

    Element descriptionTitle = doc.selectFirst(".title[data-destec=\"descp\"]");
    Element descriptionElement2 = doc.selectFirst(".content-description .value-field.Descripcion");

    if (descriptionElement2 != null) {
      if (descriptionTitle != null) {
        description.append(descriptionTitle.outerHtml());
      }

      description.append(descriptionElement2.html());
    }

    Element specsTitle = doc.selectFirst(".title[data-destec=\"tech\"]");
    Element specsElement = doc.selectFirst(".content-description table.group.Especificaciones-Tecnicas");

    if (specsElement != null) {
      if (specsTitle != null) {
        description.append(specsTitle.outerHtml());
      }

      description.append(specsElement.outerHtml());
    }

    Element caracElement = doc.selectFirst(".content-description table.group.Caracteristicas");

    if (caracElement != null) {
      if (specsTitle != null) {
        description.append(specsTitle.outerHtml());
      }

      description.append(caracElement.outerHtml());
    }

    return description.toString();
  }
}
