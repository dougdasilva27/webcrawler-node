package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 25/11/2016 1) Only one sku per page.
 * 
 * Price crawling notes: 1) For this market was not found product unnavailable 2) Page of product
 * disappear when javascripit is off, so is accessed this api:
 * "https://www.walmart.com.mx/WebControls/hlGetProductDetail.ashx?upc="+id 3) InternalId of product
 * is in url and a json, but to fetch api is required internalId, so it is crawl in url 4) Has no
 * bank ticket in this market 5) Has no internalPid in this market 6) IN api when have a json,
 * sometimes has duplicates keys, so is used GSON from google.
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloOnofreCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.onofre.com.br";
  private static final String HOME_PAGE_HTTP = "http://www.onofre.com.br/";

  public SaopauloOnofreCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE) || href.startsWith(HOME_PAGE_HTTP));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);
      
      JSONArray skus = JSONUtils.getJSONArrayValue(jsonInfo, "sku");

      
      String name = JSONUtils.getStringValue(jsonInfo, "name");
      name = crawlName(doc, name);
      Float price = crawlPrice(jsonInfo);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(jsonInfo);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home):not(.product) a");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-short-description", "#details"));
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image-gallery img:not(:first-child)",
          Arrays.asList("data-zoom-image", "src"), "https", "img.onofre.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-image-gallery img:not(:first-child)",
          Arrays.asList("data-zoom-image", "src"), "https", "img.onofre.com.br", primaryImage);
      Marketplace marketplace = new Marketplace();
      String ean = JSONUtils.getStringValue(jsonInfo, "gtin13");
      List<String> eans = ean != null ? Arrays.asList(ean) : null;
      
      for(Object obj : skus) {
        if(obj instanceof String) {
          String internalId = (String) obj;
          String internalPid = internalId;
          RatingsReviews ratingReviews = crawlRating(doc, internalId, primaryImage);
  
          // Creating the product
          Product product = ProductBuilder.create()
              .setUrl(session.getOriginalURL())
              .setInternalId(internalId)
              .setInternalPid(internalPid)
              .setName(name)
              .setPrice(price)
              .setPrices(prices)
              .setAvailable(available)
              .setCategory1(categories.getCategory(0))
              .setCategory2(categories.getCategory(1))
              .setCategory3(categories.getCategory(2))
              .setPrimaryImage(primaryImage)
              .setSecondaryImages(secondaryImages)
              .setDescription(description)
              .setMarketplace(marketplace)
              .setEans(eans)
              .setRatingReviews(ratingReviews)
              .build();
    
          products.add(product);
        }
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }
  
  private String crawlName(Document doc, String name) {
	  	Element el = doc.selectFirst(".product-view .product-info .marca.hide-hover");
	  	if(el != null) {
	  		String aux = doc.select(".product-view .product-info .marca.hide-hover").text();
			name  = name +" "+ el.text();
	  	}
	  	Element ele = doc.selectFirst(".product-view .product-info .quantidade.hide-hover");
	  	if(ele != null) {
			name  = name +" "+ ele.text();
		}
		return name;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-view") != null;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("offers") && !json.isNull("offers")) {
      JSONObject value = json.getJSONObject("offers");
      if (value.has("@type") && value.get("@type").toString().equalsIgnoreCase("offer")) {
        price = JSONUtils.getFloatValueFromJSON(value, "price", true);
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    boolean availability = false;

    if (json.has("offers") && !json.isNull("offers")) {
      JSONObject value = json.getJSONObject("offers");
      if (value.has("@type") && value.get("@type").toString().equalsIgnoreCase("offer") && value.has("availability") &&
          !value.isNull("availability")) {

        availability = value.get("availability").toString().toLowerCase().endsWith("instock");
      }
    }

    return availability;
  }

  /**
   * In product page has only one price, but in footer has informations of payment methods
   *
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-price .price", null, true, ',', session));

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
    }

    return prices;
  }

  private RatingsReviews crawlRating(Document doc, String internalId, String primaryImage) {
    TrustvoxRatingCrawler rating = new TrustvoxRatingCrawler(session, "109192", logger);
    rating.setPrimaryImage(primaryImage);

    return rating.extractRatingAndReviews(internalId, doc, dataFetcher);
  }
}
