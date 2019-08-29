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
import models.Offers;
import models.prices.Prices;

public class BrasilBrastempCrawler extends Crawler {

  private static final String HOME_PAGE = "http://loja.brastemp.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "brastemp";
  private static final String MAIN_SELLER_NAME_LOWER_2 = "consul";
  private static final String MAIN_SELLER_NAME_LOWER_3 = "whirlpool";
  private static final List<String> SELLERS = Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2, MAIN_SELLER_NAME_LOWER_3);

  public BrasilBrastempCrawler(Session session) {
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
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);
      vtexUtil.setCardDiscount(getCardDiscount(doc));

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:last-child > a", false);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String description = crawlDescription(doc, apiJSON, vtexUtil, internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, apiJSON);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, SELLERS, Arrays.asList(Card.VISA), session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, SELLERS);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, SELLERS);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        Offers offers = vtexUtil.scrapBuyBox(apiJSON);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setOffers(offers).build();

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

  private Integer getCardDiscount(Document doc) {
    Integer discount = 0;

    Element d = doc.selectFirst(".prod-image p[class^=\"flag btp--desconto-cartao---percentual-\"]");
    if (d != null) {
      String text = CommonMethods.getLast(d.attr("class").split("-")).replaceAll("[^0-9]", "");

      if (!text.isEmpty()) {
        discount = Integer.parseInt(text);
      }
    }

    return discount;
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

    List<Integer> modules = Arrays.asList(1, 2, 3, 4);


    for (int i : modules) {
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
