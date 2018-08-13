
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

public class BrasilConsulCrawler extends Crawler {

  private static final String HOME_PAGE = "https://loja.consul.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "whirlpool";

  public BrasilConsulCrawler(Session session) {
    super(session);
  }

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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = crawlCategories(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String description = crawlDescription(doc, apiJSON);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document document) {
    return document.select(".productName").first() != null;
  }

  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();

    Element category = doc.selectFirst(".bread-crumb .last a");
    if (category != null) {
      categories.add(category.ownText().trim());
    }

    return categories;
  }

  /**
   * @param document
   * @param apiJSON
   * @return
   */
  private String crawlDescription(Document document, JSONObject apiJSON) {
    StringBuilder description = new StringBuilder();
    Element specElement = document.select("#caracteristicas").first();

    if (specElement != null) {
      specElement.select(".group.Prateleira").remove();

      Elements nameFields = specElement.select(".name-field, h4");
      for (Element e : nameFields) {
        String classString = e.attr("class");

        if (classString.toLowerCase().contains("modulo") || classString.toLowerCase().contains("foto")) {
          specElement.select("." + classString.trim().replace(" ", ".")).remove();
        }
      }

      specElement.select(".Galeria, .Video").remove();
      description.append(specElement.html().replace("Arquivos", "Downloads"));
    }

    if (apiJSON.has("RealHeight")) {
      description.append("<table cellspacing=\"0\" class=\"descricao\">\n").append("<tbody>").append("<tr>").append("<th>Largura").append("</th>")
          .append("<td>").append("\n" + apiJSON.getFloat("RealHeight")).append("</td>").append("</tbody>").append("</table>");
    }

    if (apiJSON.has("RealWidth")) {
      description.append("<table cellspacing=\"0\" class=\"descricao\">\n").append("<tbody>").append("<tr>").append("<th>Altura").append("</th>")
          .append("<td>").append("\n" + apiJSON.getFloat("RealWidth")).append("</td>").append("</tbody>").append("</table>");
    }

    if (apiJSON.has("RealLength")) {
      description.append("<table cellspacing=\"0\" class=\"descricao\">\n").append("<tbody>").append("<tr>").append("<th>Profundidade")
          .append("</th>").append("<td>").append("\n" + apiJSON.getFloat("RealLength")).append("</td>").append("</tbody>").append("</table>");
    }

    if (apiJSON.has("RealWeightKg")) {
      description.append("<table cellspacing=\"0\" class=\"descricao\">\n").append("<tbody>").append("<tr>").append("<th>Peso").append("</th>")
          .append("<td>").append("\n" + apiJSON.getFloat("RealWeightKg")).append("</td>").append("</tbody>").append("</table>");
    }

    return description.toString();
  }
}
