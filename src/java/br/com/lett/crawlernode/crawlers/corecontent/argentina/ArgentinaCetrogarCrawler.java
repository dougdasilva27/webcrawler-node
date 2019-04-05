package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaCetrogarCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.cetrogar.com.ar/";

  public ArgentinaCetrogarCrawler(Session session) {
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
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name .h1", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "#product-price-" + internalId, true);
      Prices prices = crawlPrices(doc, price);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = new CategoryCollection(); // has no categories in this market
      String primaryImage =
          CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery #image-main", Arrays.asList("src"), "https:", "cdn.cetrogar.com.ar");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-gallery img:not(.visible):not(:first-child)",
          Arrays.asList("src"), "https:", "cdn.cetrogar.com.ar", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-collateral"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-name").isEmpty();
  }

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input[name=product]");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  private boolean crawlAvailability(Document doc) {
    boolean available = false;

    Element stock = doc.selectFirst(".product-shop [itemprop=availability]");
    if (stock != null) {
      available = stock.attr("href").toLowerCase().contains("/instock");
    }

    return available;
  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if(price != null) {
    	Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    	installmentPriceMap.put(1, price);

    	Element priceFrom = doc.selectFirst(".product-shop .old-price .price");
    	if (priceFrom != null) {
    		prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.ownText()));
    	}

    	JSONObject installmentsJson = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.dataBankingJson=", ";", true, false);

    	if (installmentsJson.has("15")) {
    		JSONObject credcard = installmentsJson.getJSONObject("15");

    		JSONObject installments = new JSONObject();
    		if (credcard.has("998")) {
    			installments = credcard.getJSONObject("998");
    		} else if (credcard.has("-1")) {
    			installments = credcard.getJSONObject("-1");
    		}

    		for (String installmentNumber : installments.keySet()) {
    			JSONObject parcel = installments.getJSONObject(installmentNumber);

    			Float value = CrawlerUtils.getFloatValueFromJSON(parcel, "monto_cuota");
    			String text = installmentNumber.replaceAll("[^0-9]", "");
    			if (value != null && !text.isEmpty()) {
    				installmentPriceMap.put(Integer.parseInt(text), MathUtils.normalizeTwoDecimalPlaces(value));
    			}
    		}
    	}

    	prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }
    
    return prices;
  }

}
