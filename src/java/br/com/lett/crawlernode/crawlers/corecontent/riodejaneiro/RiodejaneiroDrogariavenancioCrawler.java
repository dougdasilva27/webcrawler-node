package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

public class RiodejaneiroDrogariavenancioCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.drogariavenancio.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "venancio produtos farmaceuticos ltda";

  public RiodejaneiroDrogariavenancioCrawler(Session session) {
    super(session);
  }


  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);
      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".product__breadcrumb .bread-crumb ul li[typeof=\"v:Breadcrumb\"]", true);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);
        String name = crawlName(jsonSku, skuJson); // because this site always show the principal name
        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        String description = scrapDescription(vtexUtil, internalId);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
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
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String crawlName(JSONObject jsonSku, JSONObject skuJson) {
    String name = null;

    if (skuJson.has("name") && !skuJson.isNull("name")) {
      name = skuJson.getString("name");
    }

    if (name != null && jsonSku.has("dimensions")) {
      JSONObject dimensions = jsonSku.getJSONObject("dimensions");

      if (dimensions.has("TAMANHO") && !dimensions.isNull("TAMANHO") && !dimensions.get("TAMANHO").toString().equalsIgnoreCase("NA")) {
        name = name.concat(" ").concat(dimensions.get("TAMANHO").toString());
      }

      if (dimensions.has("COR") && !dimensions.isNull("COR") && !dimensions.get("COR").toString().equalsIgnoreCase("NA")) {
        name = name.concat(" ").concat(dimensions.get("COR").toString());
      }

      if (dimensions.has("QUANTIDADE") && !dimensions.isNull("QUANTIDADE") && !dimensions.get("QUANTIDADE").toString().equalsIgnoreCase("NA")) {
        name = name.concat(" ").concat(dimensions.get("QUANTIDADE").toString());
      }

      if (dimensions.has("VOLUME") && !dimensions.isNull("VOLUME") && !dimensions.get("VOLUME").toString().equalsIgnoreCase("NA")) {
        name = name.concat(" ").concat(dimensions.get("VOLUME").toString());
      }

    }

    return name;
  }


  private boolean isProductPage(Document document) {
    return document.select(".productName").first() != null;
  }

  private String scrapDescription(VTEXCrawlersUtils vtexUtil, String internalId) {
    StringBuilder sb = new StringBuilder();
    JSONObject obj = vtexUtil.crawlDescriptionAPI(internalId, "skuId");

    if (obj.has("Descrição")) {
      JSONArray arr = obj.getJSONArray("Descrição");

      if (arr.length() > 0) {
        sb.append("<div id=\"Descricao\">").append("<h4> Descrição </h4>");
        for (Object o : arr) {
          sb.append(o.toString());
        }
        sb.append("</div\">");
      }
    }

    /**
     * Because of this link: https://www.drogariavenancio.com.br/fibermais10sachesx5g-8198/p
     */

    if (obj.has("Indicações")) {
      JSONArray arr = obj.getJSONArray("Indicações");

      if (arr.length() > 0) {
        sb.append("<div id=\"Indicacoes\">").append("<h4> Indicações </h4>");
        for (Object o : arr) {
          sb.append(o.toString());
        }
        sb.append("</div\">");
      }
    }


    if (obj.has("Contra indicações")) {
      JSONArray arr = obj.getJSONArray("Contra indicações");

      if (arr.length() > 0) {
        sb.append("<div id=\"contra indicacoes\">").append("<h4> Contra indicações </h4>");
        for (Object o : arr) {
          sb.append(o.toString());
        }
        sb.append("</div\">");
      }
    }

    return sb.toString();
  }
}
