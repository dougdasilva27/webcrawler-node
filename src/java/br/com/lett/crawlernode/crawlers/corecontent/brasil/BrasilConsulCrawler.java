
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.Offers;
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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      vtexUtil.setDiscountWithDocument(doc, ".prod-selos p[class^=flag cns-e-btp--desconto-a-vista-cartao-percentual-]", true, false);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = crawlCategories(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String description = crawlDescription(doc, apiJSON, vtexUtil, internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, apiJSON);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;
        Offers offers = new Offers();
        try {
          offers = vtexUtil.scrapBuyBox(apiJSON);
        } catch (Exception e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).setOffers(offers).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
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
  private String crawlDescription(Document document, JSONObject apiJSON, VTEXCrawlersUtils vtexCrawlersUtils, String internalId) {
    StringBuilder description = new StringBuilder();

    JSONObject descriptionJson = vtexCrawlersUtils.crawlDescriptionAPI(internalId, "skuId");

    if (descriptionJson.has("description")) {
      description.append("<div>");
      description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionJson.get("description")));
      description.append("</div>");
    }

    List<String> specs = new ArrayList<>();

    if (descriptionJson.has("Caracteristícas Técnicas")) {
      JSONArray keys = descriptionJson.getJSONArray("Caracteristícas Técnicas");
      for (Object o : keys) {
        if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
          specs.add(o.toString());
        }
      }
    }

    for (String spec : specs) {
      if (descriptionJson.has(spec)) {

        String label = spec;

        if (spec.equals("Tipo do produto")) {
          label = "Tipo";
        } else if (spec.equalsIgnoreCase("Garantia do Fornecedor (mês)")) {
          label = "Garantia";
        } else if (spec.equalsIgnoreCase("Mais Informações")) {
          label = "Informações";
        }

        description.append("<div>");
        description.append("<h4>").append(label).append("</h4>");
        description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionJson.get(spec)) + (label.equals("Garantia") ? " meses" : ""));
        description.append("</div>");
      }
    }

    Element manual = document.selectFirst(".value-field.Manual-do-Produto");
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

    return description.toString();
  }
}
