package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
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


/************************************************************************************************************************************************************************************
 * Crawling notes (03/11/2016):
 * 
 * 1) For this crawler, we have one url per each sku. At the time this crawler was done, no example
 * of multiple skus in an URL was found. Although there was some indication that this case could
 * occur (eg. analysing the sku json), no concrete example was found. The crawler always tries to
 * get all the data only from the sku URL, without using any endpoint. We choose to use endpoint,
 * only if the information isn't available anywhere else.
 * 
 * 2) The images are crawled from the sku json, fetched from an endpoint of shopfato.
 * 
 * 3) There is stock information for skus in this ecommerce only in the json from the endpoint.
 * 
 * 4) There is no marketplace in this ecommerce by the time this crawler was made. There are some
 * indications that could exist some other seller than the shopfato, but no concrete example was
 * found. Still we try to get the seller name in the sku page, that is 'shopfato' on all observed
 * cases.
 * 
 * 6) The sku page identification is done simply looking the URL format.
 * 
 * 7) When a product is unavailable, its price is not shown. But the crawler doesn't consider this
 * as a global rule. It tries to crawl the price the same way in both cases.
 * 
 * 8) There is internalPid for skus in this ecommerce.
 * 
 * 9) In the json from the endpoint we have the stock from the seller. There is a field with sellers
 * informations, but we didn't saw any example with more than one seller (different from shopfato),
 * for an sku.
 * 
 * 10) We have one method for each type of information for a sku (please carry on with this
 * pattern).
 * 
 * Examples: Caso particular: possui marketplace na resposta da API, porém o mesmo não é exibido na
 * página principal do sku e este é dado como indisponível. Neste caso o crawler precisa usar o
 * atribute Availability da repsosta da API para determinar se o sku está realmente disponível ou
 * não. A url abaixo apresenta este caso:
 * http://www.shopfato.com.br/ar-condicionado-split-liva-18000-btus-sistema-ar-puro-frio-220v-midea-30807/p
 * 
 * 
 * Optimizations notes: ...
 *
 ************************************************************************************************************************************************************************************/

public class BrasilShopfatoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.shopfato.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "shopfato";

  public BrasilShopfatoCrawler(Session session) {
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
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies);
      vtexUtil.setBankTicketDiscount(5);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".prd-accordion-description-holder", "#caracteristicas"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
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
    return document.selectFirst(".productName") != null;
  }
}
