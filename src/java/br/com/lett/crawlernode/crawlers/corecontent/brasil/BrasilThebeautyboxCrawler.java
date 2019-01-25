package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
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

public class BrasilThebeautyboxCrawler extends Crawler {

  public BrasilThebeautyboxCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.thebeautybox.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "the beauty box";

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
      VTEXCrawlersUtils vtexUtil =
          new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a");
      String description = null;
      // sku data in json
      JSONArray arraySkus =
          skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();
      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        description =
            description == null ? crawlDescription(internalId, doc, vtexUtil) : description;
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, false);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER)
            ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER)
            : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
            .setDescription(description).setStock(stock).setMarketplace(marketplace).setEan(ean)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private String crawlDescription(String internalId, Document doc, VTEXCrawlersUtils vtexUtil) {
    StringBuilder description = new StringBuilder();

    JSONObject descriptionJson = vtexUtil.crawlDescriptionAPI(internalId, "skuId");

    if (descriptionJson.has("A gente ama porque")) {
      description.append("<div><h4>A gente ama porque...</h4>");
      description.append(sanitizeDescription(descriptionJson.get("A gente ama porque")));
      description.append("</div>");
    }

    if (descriptionJson.has("description")) {
      description.append("<div><h4>Descrição</h4>");

      if (descriptionJson.has("Título da Descrição")) {
        description.append(descriptionJson.get("Título da Descrição").toString().replace("[\"", "")
            .replace("\"]", ""));
      }

      description.append(sanitizeDescription(descriptionJson.get("description")));

      if (descriptionJson.has("Descricao extra")) {
        description.append("<span>");
        description.append(sanitizeDescription(descriptionJson.get("Descricao extra")));
        description.append("</span>");
      }

      description.append("</div>");
    }

    if (descriptionJson.has("Modo de Uso")) {
      description.append("<div><h4>Modo de Usar</h4>");
      description.append(sanitizeDescription(descriptionJson.get("Modo de Uso")));
      description.append("</div>");
    }

    Element specs = doc.selectFirst(".Caracterisca td.Ficha-Tecnica");
    if (specs != null) {

      description.append("<div><h4>Especificações Técnicas</h4>");
      description.append(specs.html());
      description.append("</div>");

      // this description appears 2 times
      description.append("<div><h4>Ingredientes</h4>");
      description.append(specs.html());
      description.append("</div>");
    }

    return description.toString();
  }

  private Document sanitizeDescription(Object obj) {
    return Jsoup.parse(obj.toString().replace("[\"", "").replace("\"]", "").replace("\\", ""));
  }

}
