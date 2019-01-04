package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilMercadolivrecotyCrawler extends Crawler {

  private static final String HOME_PAGE = "https://loja.mercadolivre.com.br/coty";
  private static final String MAIN_SELLER_NAME_LOWER = "coty";

  public BrasilMercadolivrecotyCrawler(Session session) {
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

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=itemId]", "value");

      Map<String, Document> variations = getVariationsHtmls(doc);
      for (Entry<String, Document> entry : variations.entrySet()) {
        Document docVariation = entry.getValue();

        String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(docVariation, "input[name=variation]", "value");

        if(variations.size() > 1 && (internalId == null || internalId.trim().isEmpty())) {
        	continue;
        }
        
        internalId = internalId == null && variations.size() < 2 ? internalPid : internalPid + "-" + internalId;
        
        String name = crawlName(docVariation);
        CategoryCollection categories = CrawlerUtils.crawlCategories(docVariation, "a.breadcrumb");
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docVariation, "figure.gallery-image-container a", Arrays.asList("href"), "https:",
            "http2.mlstatic.com");
        String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docVariation, "figure.gallery-image-container a", Arrays.asList("href"),
            "https:", "http2.mlstatic.com", primaryImage);
        String description =
            CrawlerUtils.scrapSimpleDescription(docVariation, Arrays.asList(".vip-section-specs", ".section-specs", ".item-description"));

        boolean availableToBuy = !docVariation.select(".item-actions [value=\"Comprar agora\"]").isEmpty();
        Map<String, Prices> marketplaceMap = availableToBuy ? crawlMarketplace(doc) : new HashMap<>();
        boolean available = availableToBuy && marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER);

        Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(MAIN_SELLER_NAME_LOWER), Card.VISA, session);
        Prices prices = available ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
        Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.VISA);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(entry.getKey()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setMarketplace(marketplace).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=itemId]").isEmpty();
  }

  private Map<String, Document> getVariationsHtmls(Document doc) {
    Map<String, Document> variations = new HashMap<>();
    
    String originalUrl = session.getOriginalURL();
    variations.putAll(getSizeVariationsHmtls(doc, originalUrl));

    Elements colors = doc.select(".variation-list--full li:not(.variations-selected)");
    for (Element e : colors) {
    	String dataValue = e.attr("data-value");
    	String url = originalUrl + (originalUrl.contains("?") ? "&" : "?") 
    			+ "attribute=COLOR_SECONDARY_COLOR%7C" + dataValue + "&quantity=1&noIndex=true";
      Document docColor = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
      variations.putAll(getSizeVariationsHmtls(docColor, url));
    }

    return variations;
  }

  private Map<String, Document> getSizeVariationsHmtls(Document doc, String urlColor) {
    Map<String, Document> variations = new HashMap<>();
    variations.put(urlColor, doc);

    Elements sizes = doc.select(".variation-list li:not(.variations-selected)");
    for (Element e : sizes) {
    	String dataValue = e.attr("data-value");
    	String url = urlColor + (urlColor.contains("?") ? "&" : "?") + "variation=" + dataValue;
      Document docSize = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);

      variations.put(url, docSize);
    }

    return variations;
  }

  private static String crawlName(Document doc) {
    StringBuilder name = new StringBuilder();
    name.append(CrawlerUtils.scrapStringSimpleInfo(doc, "h1.item-title__primary", true));

    Element sizeElement = doc.selectFirst(".variation-list li.variations-selected");
    if (sizeElement != null) {
      name.append(" ").append(sizeElement.attr("data-title"));
    }

    Element colorElement = doc.selectFirst(".variation-list--full li.variations-selected");
    if (colorElement != null) {
      name.append(" ").append(colorElement.attr("data-title"));
    }

    return name.toString();
  }

  private Map<String, Prices> crawlMarketplace(Document doc) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName = MAIN_SELLER_NAME_LOWER;
    Element sellerNameElement = doc.selectFirst(".official-store-info .title");

    if (sellerNameElement != null) {
      sellerName = sellerNameElement.ownText().toLowerCase().trim();
    } else {
      sellerNameElement = doc.selectFirst(".new-reputation > a");

      if (sellerNameElement != null) {
        try {
          sellerName = URLDecoder.decode(CommonMethods.getLast(sellerNameElement.attr("href").split("/")), "UTF-8").toLowerCase();
        } catch (UnsupportedEncodingException e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    marketplace.put(sellerName, crawlPrices(doc));

    return marketplace;

  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc) {
    Prices prices = new Prices();

    Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".item-price span.price-tag:not(.price-tag__del)", false);
    if (price != null) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);
      prices.setBankTicketPrice(price);

      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, "del .price-tag-fraction", true));

      Elements installments = doc.select(".payment-installments");
      for (Element e : installments) {
        Element eParsed = Jsoup.parse(e.toString().replace("<sup>", ",").replace("</sup>", ""));
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, eParsed, false, "x");
        if (!pair.isAnyValueNull()) {
          mapInstallments.put(pair.getFirst(), pair.getSecond());
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
    }

    return prices;
  }

}
