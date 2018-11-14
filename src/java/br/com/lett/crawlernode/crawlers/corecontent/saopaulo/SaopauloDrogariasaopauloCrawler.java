package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
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

public class SaopauloDrogariasaopauloCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.drogariasaopaulo.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "drogaria são paulo";


  public SaopauloDrogariasaopauloCrawler(Session session) {
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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, logger, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li:not(:first-child) > a");
      String description = crawlDescription(doc, internalPid);
      String primaryImage = null;
      String secondaryImages = null;

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId);
        Marketplace marketplace = vtexUtil.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);
        primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
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
    return document.select("#___rc-p-sku-ids").first() != null;
  }

  /*******************
   * General methods *
   *******************/


  private String crawlDescription(Document doc, String internalPid) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.select(".productDescription").first();
    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element elementInformation = doc.select(".productSpecification").first();
    if (elementInformation != null) {

      Element iframe = elementInformation.select("iframe[src]").first();
      if (iframe != null) {
        description.append(GETFetcher.fetchPageGET(session, iframe.attr("src"), cookies, 1));
      }

      description.append(elementInformation.html());
    }

    Element advert = doc.select(".advertencia").first();
    if (advert != null && !advert.select("#___rc-p-id").isEmpty()) {
      description.append(advert.html());
    }

    String url = "https://www.drogariasaopaulo.com.br/api/catalog_system/pub/products/search?fq=productId:" + internalPid;
    JSONArray skuInfo = DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, url, null, cookies);

    if (skuInfo.length() > 0) {
      JSONObject product = skuInfo.getJSONObject(0);

      if (product.has("allSpecifications")) {
        JSONArray infos = product.getJSONArray("allSpecifications");

        for (Object o : infos) {
          if (!Arrays.asList("Garantia", "Parte do Corpo", "Gênero").contains(o.toString().trim())) {
            description.append("<div> <strong>" + o.toString() + ":</strong>");
            JSONArray spec = product.getJSONArray(o.toString());

            for (Object obj : spec) {
              description.append(obj.toString() + "&nbsp");
            }

            description.append("</div>");
          }
        }
      }

      if (product.has("Página Especial")) {
        JSONArray specialPage = product.getJSONArray("Página Especial");

        if (specialPage.length() > 0) {
          Element iframe = Jsoup.parse(specialPage.get(0).toString()).select("iframe").first();

          if (iframe != null && iframe.hasAttr("src") && !iframe.attr("src").contains("youtube")) {
            description.append(DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, iframe.attr("src"), null, cookies));
          }
        }
      }
    }

    return description.toString();
  }


}
