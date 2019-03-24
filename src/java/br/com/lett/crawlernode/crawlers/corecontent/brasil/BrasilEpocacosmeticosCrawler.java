package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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

public class BrasilEpocacosmeticosCrawler extends Crawler {
  public BrasilEpocacosmeticosCrawler(Session session) {
    super(session);
  }


  private static final String HOME_PAGE = "https://www.epocacosmeticos.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "época cosméticos";


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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();
      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);


      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);

        JSONObject descriptionArray = vtexUtil.crawlDescriptionAPI(internalId, "skuId");
        String description = crawlDescription(descriptionArray);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = vtexUtil.crawlStock(apiJSON);

        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

        products.add(product);
      }

    } else

    {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document document) {
    return document.selectFirst(".productName") != null;
  }

  private String crawlDescription(JSONObject json) {
    StringBuilder description = new StringBuilder();

    if (json.has("description")) {
      description.append("<div><h3>Descrição</h3></div>");
      description.append(json.get("description"));
    }

    if (json.has("Gênero:")) {
      description.append("<div>Gênero:");
      description.append(json.getJSONArray("Gênero:").get(0).toString());
      description.append("</div>");
    }


    if (json.has("Concentração:")) {
      description.append("<div>Concentração:");
      description.append(json.getJSONArray("Concentração:").get(0).toString());
      description.append("</div>");
    }


    if (json.has("Familia Olfativa:")) {
      description.append("<div>Familia Olfativa:");
      description.append(VTEXCrawlersUtils.sanitizeDescription(json.getJSONArray("Familia Olfativa:").toString()));
      description.append("</div>");
    }


    if (json.has("Conteúdo Especial")) {
      description.append("<div>Conteúdo Especial:");
      description.append(VTEXCrawlersUtils.sanitizeDescription(json.getJSONArray("Conteúdo Especial")));
      description.append("</div>");
    }


    if (json.has("Notas de Topo:")) {
      description.append("<div>Notas de Topo:");
      description.append(VTEXCrawlersUtils.sanitizeDescription(json.getJSONArray("Notas de Topo:").toString()));
      description.append("</div>");
    }

    if (json.has("Notas de Coração:")) {
      description.append("<div>Notas de Coração:");
      description.append(VTEXCrawlersUtils.sanitizeDescription(json.getJSONArray("Notas de Coração:").toString()));
      description.append("</div>");
    }

    if (json.has("Notas de Fundo:")) {
      description.append("<div>Notas de Fundo:");
      description.append(VTEXCrawlersUtils.sanitizeDescription(json.getJSONArray("Notas de Fundo:").toString()));
      description.append("</div>");
    }

    if (json.has("Tempo de Fixação:")) {
      description.append("<div>Tempo de Fixação:");
      description.append(json.getJSONArray("Tempo de Fixação:").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Estilo:")) {
      description.append("<div>Estilo:");
      description.append(json.getJSONArray("Estilo:").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Ocasião")) {
      description.append("<div>Ocasião:");
      description.append(json.getJSONArray("Ocasião").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Sazonalidade:")) {
      description.append("<div>Sazonalidade:");
      description.append(VTEXCrawlersUtils.sanitizeDescription(json.getJSONArray("Sazonalidade:").toString()));
      description.append("</div>");
    }

    if (json.has("Ano de Lançamento:")) {
      description.append("<div>Ano de Lançamento:");
      description.append(json.getJSONArray("Ano de Lançamento:").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Perfumista")) {
      description.append("<div>Perfumista:");
      description.append(json.getJSONArray("Perfumista").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Sobre a Marca:")) {
      description.append("<div>Sobre a Marca:");
      description.append(json.getJSONArray("Sobre a Marca:").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Tipo")) {
      description.append("<div>Tipo:");
      description.append(json.getJSONArray("Tipo").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Proposta")) {
      description.append("<div>Proposta:");
      description.append(json.getJSONArray("Proposta").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Público")) {
      description.append("<div>Público:");
      description.append(json.getJSONArray("Público").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Proteção Solar")) {
      description.append("<div>Proteção Solar:");
      description.append(json.getJSONArray("Proteção Solar").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Apresentação")) {
      description.append("<div>Apresentação:");
      description.append(json.getJSONArray("Apresentação").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Resistência")) {
      description.append("<div>Resistência:");
      description.append(json.getJSONArray("Resistência").get(0).toString());
      description.append("</div>");
    }

    if (json.has("Fórmula")) {
      description.append("<div>Fórmula:");
      description.append(json.getJSONArray("Fórmula").get(0).toString());
      description.append("</div>");
    }

    return description.toString();
  }

}
