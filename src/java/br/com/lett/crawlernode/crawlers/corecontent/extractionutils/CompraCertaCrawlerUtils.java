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
  private static final String HOME_PAGE = "https://loja.compracerta.com.br/";
  private Session session;

  public CompraCertaCrawlerUtils(Session session, Logger logger2) {
    this.session = session;
  }

  public List<Product> extractProducts(Document doc) {
    List<Product> products = new ArrayList<>();

    VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, null);
    vtexUtil.setDiscountWithDocument(doc, ".prod-selos p[class^=flag cc-bf--desconto-a-vista-cartao-]", true, false);

    JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

    String internalPid = vtexUtil.crawlInternalPid(skuJson);
    CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");

    // sku data in json
    JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

    // ean data in json
    JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);
    
    for (int i = 0; i < arraySkus.length(); i++) {
      JSONObject jsonSku = arraySkus.getJSONObject(i);

      String internalId = vtexUtil.crawlInternalId(jsonSku);
      JSONObject apiJSON = vtexUtil.crawlApi(internalId);
      String description = crawlDescription(doc, apiJSON);
      String name = vtexUtil.crawlName(jsonSku, skuJson);
      Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
      Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
      boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
      String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
      String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
      Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
      Float price = vtexUtil.crawlMainPagePrice(prices);
      Integer stock = vtexUtil.crawlStock(apiJSON);
      String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).setEan(ean).build();

      products.add(product);
    }

    return products;
  }

  private String crawlDescription(Document doc, JSONObject apiJSON) {
    StringBuilder description = new StringBuilder();

    Element especificDescriptionTitle = doc.selectFirst("#especificacoes > h2");
    if (especificDescriptionTitle != null) {
      description.append(especificDescriptionTitle.html());
    }

    if (apiJSON.has("RealHeight")) {
      description.append("<table cellspacing=\"0\" class=\"Height\">\n").append("<tbody>").append("<tr>").append("<th>Largura").append("</th>")
          .append("<td>").append("\n" + apiJSON.get("RealHeight").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
          .append("</table>");
    }

    if (apiJSON.has("RealWidth")) {
      description.append("<table cellspacing=\"0\" class=\"Width\">\n").append("<tbody>").append("<tr>").append("<th>Altura").append("</th>")
          .append("<td>").append("\n" + apiJSON.get("RealWidth").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
          .append("</table>");
    }

    if (apiJSON.has("RealLength")) {
      description.append("<table cellspacing=\"0\" class=\"Length\">\n").append("<tbody>").append("<tr>").append("<th>Profundidade").append("</th>")
          .append("<td>").append("\n" + apiJSON.get("RealLength").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
          .append("</table>");
    }

    if (apiJSON.has("RealWeightKg")) {
      description.append("<table cellspacing=\"0\" class=\"WeightKg\">\n").append("<tbody>").append("<tr>").append("<th>Peso").append("</th>")
          .append("<td>").append("\n" + apiJSON.get("RealWeightKg").toString().replace(".0", "") + " kg").append("</td>").append("</tbody>")
          .append("</table>");
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
