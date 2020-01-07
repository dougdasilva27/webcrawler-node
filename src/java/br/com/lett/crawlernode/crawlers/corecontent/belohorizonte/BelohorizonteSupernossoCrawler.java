package br.com.lett.crawlernode.crawlers.corecontent.belohorizonte;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.VtexRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 01/09/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BelohorizonteSupernossoCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.supernossoemcasa.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "super nosso em casa";

  public BelohorizonteSupernossoCrawler(Session session) {
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

    if (isProductPage(doc, session.getOriginalURL())) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, MAIN_SELLER_NAME_LOWER, HOME_PAGE, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > ul li a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".productDescription", ".productSpecification"));

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      // ean data in json
      JSONArray arrayEans = CrawlerUtils.scrapEanFromVTEX(doc);

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        JSONObject apiJSON = vtexUtil.crawlApi(internalId);
        String name = vtexUtil.crawlName(jsonSku, skuJson, " ");
        Map<String, Prices> marketplaceMap = vtexUtil.crawlMarketplace(apiJSON, internalId, true);
        List<String> sellersPacheco = CrawlerUtils.getMainSellers(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER));
        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, sellersPacheco, Arrays.asList(Card.VISA), session);
        boolean available = CrawlerUtils.getAvailabilityFromMarketplaceMap(marketplaceMap, sellersPacheco);
        String primaryImage = vtexUtil.crawlPrimaryImage(apiJSON);
        String secondaryImages = vtexUtil.crawlSecondaryImages(apiJSON);
        Prices prices = CrawlerUtils.getPrices(marketplaceMap, sellersPacheco);
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);
        Integer stock = vtexUtil.crawlStock(apiJSON);
        String ean = i < arrayEans.length() ? arrayEans.getString(i) : null;
        RatingsReviews ratingReviews = crawlRating(doc, internalId);

        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(price)
            .setPrices(prices)
            .setRatingReviews(ratingReviews)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setMarketplace(marketplace)
            .setEans(eans)
            .build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private RatingsReviews crawlRating(Document document, String id) {
    return new VtexRatingCrawler(session, HOME_PAGE, logger, cookies)
        .extractRatingAndReviews(id, document, dataFetcher);
  }


  private boolean isProductPage(Document document, String url) {
    return document.selectFirst(".productName") != null && url.startsWith(HOME_PAGE);
  }
}
