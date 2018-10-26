package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

public class BrasilKitchenaidCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.kitchenaid.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "kitchenaid";


  public BrasilKitchenaidCrawler(Session session) {
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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, 10, null);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li" + CrawlerUtils.CSS_SELECTOR_IGNORE_FIRST_CHILD + " > a");
      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String description = crawlDescription(internalId, apiJSON, vtexUtil, doc);
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

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select("#product").first() != null;
  }

  /**
   * @param document
   * @param apiJSON
   * @return
   */
  private String crawlDescription(String internalId, JSONObject apiJSON, VTEXCrawlersUtils vtexUtil, Document doc) {
    StringBuilder description = new StringBuilder();

    JSONObject descriptionJson = vtexUtil.crawlDescriptionAPI(internalId, "skuId");

    Element desc = doc.selectFirst("td.Descricao-curta-topo");
    if (desc != null) {
      description.append(desc.html());
    }

    for (int i = 1; i < 4; i++) {
      if (descriptionJson.has("Texto modulo 0" + i)) {
        description.append("<div>");

        if (descriptionJson.has("Título modulo 0" + i)) {
          description.append("<h4>");
          description.append(descriptionJson.get("Título modulo 0" + i).toString().replace("[\"", "").replace("\"]", ""));
          description.append("</h4>");
        }

        description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionJson.get("Texto modulo 0" + i)));
        description.append("</div>");
      }
    }

    if (descriptionJson.has("Diferenciais")) {
      description.append("<div>");
      description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionJson.get("Diferenciais")));
      description.append("</div>");
    }

    if (descriptionJson.has("Características Técnicas")) {
      JSONArray descArray = descriptionJson.getJSONArray("Características Técnicas");

      if (descArray.length() > 0) {
        description.append("<div><h4>CARACTERÍSTICAS DO PRODUTO</h4>");
        description.append("<table>");

        for (Object o : descArray) {
          String key = o.toString();

          if (descriptionJson.has(key) && !key.equals("Portfólio")) {
            description.append("<tr>");
            description.append("<td> " + key + "</td>");
            description.append("<td> " + VTEXCrawlersUtils.sanitizeDescription(descriptionJson.get(key)) + "</td>");
            description.append("<\tr>");
          }
        }

        description.append("</table></div>");
      }
    }

    Element manual = doc.selectFirst(".value-field.Manual-do-Produto");
    if (manual != null) {

      description
          .append("<a href=\"" + manual.ownText() + "\" title=\"Baixar manual\" class=\"details__manual\" target=\"_blank\">Baixar manual</a>");
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

    return description.toString().replace("</b>", " \n</b>");
  }
}
