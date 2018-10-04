package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.Marketplace;
import models.prices.Prices;

public class CompraCertaCrawlerUtils {

  private static final String MAIN_SELLER_NAME_LOWER = "compra certa";
  private static final String HOME_PAGE = "http://loja.compracerta.com.br/";
  private Logger logger;
  private Session session;

  public CompraCertaCrawlerUtils(Session session, Logger logger2) {
    this.session = session;
    this.logger = logger2;
  }

  public List<Product> extractProducts(Document doc) {
    List<Product> products = new ArrayList<>();

    VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, null);

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

    String internalPid = vtexUtil.crawlInternalPid(skuJson);
    CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:last-child > a", false);

    // sku data in json
    JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

    for (int i = 0; i < arraySkus.length(); i++) {
      JSONObject jsonSku = arraySkus.getJSONObject(i);

      String internalId = vtexUtil.crawlInternalId(jsonSku);
      JSONObject apiJSON = vtexUtil.crawlApi(internalId);
      String description = crawlDescription(doc, apiJSON);
      String name = vtexUtil.crawlName(jsonSku, skuJson, apiJSON);
      Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
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

    return products;
  }

  private String crawlDescription(Document doc, JSONObject apiJson) {
    StringBuilder description = new StringBuilder();

    Element especificDescriptionTitle = doc.selectFirst("#especificacoes > h2");
    if (especificDescriptionTitle != null) {
      description.append(especificDescriptionTitle.html());
    }

    if (apiJson.has("RealHeight")) {
      description.append("<table cellspacing=\"0\" class=\"Height\">\n").append("<tbody>").append("<tr>").append("<th>Largura").append("</th>")
          .append("<td>").append("\n" + apiJson.getFloat("RealHeight")).append("</td>").append("</tbody>").append("</table>");
    }

    if (apiJson.has("RealWidth")) {
      description.append("<table cellspacing=\"0\" class=\"Width\">\n").append("<tbody>").append("<tr>").append("<th>Altura").append("</th>")
          .append("<td>").append("\n" + apiJson.getFloat("RealWidth")).append("</td>").append("</tbody>").append("</table>");
    }

    if (apiJson.has("RealLength")) {
      description.append("<table cellspacing=\"0\" class=\"Length\">\n").append("<tbody>").append("<tr>").append("<th>Profundidade").append("</th>")
          .append("<td>").append("\n" + apiJson.getFloat("RealLength")).append("</td>").append("</tbody>").append("</table>");
    }

    if (apiJson.has("RealWeightKg")) {
      description.append("<table cellspacing=\"0\" class=\"WeightKg\">\n").append("<tbody>").append("<tr>").append("<th>Peso").append("</th>")
          .append("<td>").append("\n" + apiJson.getFloat("RealWeightKg")).append("</td>").append("</tbody>").append("</table>");
    }


    Element caracteristicas = doc.select("#caracteristicas").first();

    if (caracteristicas != null) {
      Element caracTemp = caracteristicas.clone();
      caracTemp.select(".group.Prateleira").remove();

      Elements nameFields = caracteristicas.select(".name-field, h4");
      for (Element e : nameFields) {
        String classString = e.attr("class");

        if (classString.toLowerCase().contains("modulo") || classString.toLowerCase().contains("foto")) {
          caracTemp.select("th." + classString.trim().replace(" ", ".")).remove();
        }
      }

      caracTemp.select("h4.group, .Galeria, .Video, .Manual-do-Produto, h4.Arquivos").remove();
      description.append(caracTemp.html());

    }

    // Element shortDescription = doc.select(".productDescription").first();
    // if (shortDescription != null) {
    // description.append(shortDescription.html());
    // }

    return description.toString();
  }
}
