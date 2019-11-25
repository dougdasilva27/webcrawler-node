package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilTudodebichoCrawler extends Crawler {
  
  private static final String HOME_PAGE = "tudodebicho.com.br";
  
  public BrasilTudodebichoCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    
    List<Product> products = extractPageProduct(doc);
    
    Elements colorVariations = doc.select(".atributos-container a[href]");
    if(colorVariations != null & !colorVariations.isEmpty()) {
      for(Element e : colorVariations) {
        String pageUrl = CrawlerUtils.scrapUrl(e, null, Arrays.asList("href"), "https", HOME_PAGE);
        
        Request variationRequest =  RequestBuilder.create().setUrl(pageUrl).setCookies(cookies).build();
        Document variationDoc = Jsoup.parse(this.dataFetcher.get(session, variationRequest).getBody());
        
        List<Product> variationProducts = extractPageProduct(variationDoc);
        products.addAll(variationProducts);
      }
    }
    
    return products;
  }
  
  private List<Product> extractPageProduct(Document doc) {
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".item-name h1", true);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".fbits-breadcrumb li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fbits-produto-imagens ul > li > a", 
          Arrays.asList("data-zoom-image", "data-image"), "http", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fbits-produto-imagens ul > li > a", 
          Arrays.asList("data-zoom-image", "data-image"), "http", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".informacao-abas"));
      Integer stock = null;
      Marketplace marketplace = null;
      RatingsReviews ratingReviews = scrapRatingsReviews(doc);
      
      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setName(name)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setStock(stock)
          .setMarketplace(marketplace)
          .setRatingReviews(ratingReviews)
          .build();

      JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".content.produto script[type=\"application/ld+json\"]", "", "", false, true);
      JSONArray skus = json.has("offers") && json.get("offers") instanceof JSONArray ? json.getJSONArray("offers") : new JSONArray() ;
      
      for(Object obj : skus) {
        if(obj instanceof JSONObject) {
          json = (JSONObject) obj;
          
          Product clone = product.clone();
          
          String internalId = JSONUtils.getStringValue(json, "sku");
          String internalPid = JSONUtils.getStringValue(json, "mpn");
          boolean available = JSONUtils.getStringValue(json, "availability") != null && JSONUtils.getStringValue(json, "availability").toLowerCase().contains("instock");
          Float price = JSONUtils.getFloatValueFromJSON(json, "price", true);
          Prices prices = scrapPrices(doc, price);
          String ean = JSONUtils.getStringValue(json, "gtin14");
          List<String> eans = ean != null ? Arrays.asList(ean) : null;
          
          clone.setInternalId(internalId);
          clone.setInternalPid(internalPid);
          clone.setAvailable(available);
          clone.setPrice(price);
          clone.setPrices(prices);
          clone.setEans(eans);
          
          products.add(clone);
        }
      }        
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#bodyProduto") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoDe", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private RatingsReviews scrapRatingsReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Integer totalNumOfEvaluations = 0;
    Integer totalWrittenReviews = 0;
    Double avgRating = 0.0d;
    
    Element ratingElement = doc.selectFirst("[itemprop=\"aggregateRating\"]");
    if(ratingElement != null) { 
      
      totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(ratingElement, "[itemprop=\"ratingCount\"]", "content", 0);
      totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(ratingElement, "[itemprop=\"reviewCount\"]", "content", 0);
      avgRating = CrawlerUtils.scrapDoublePriceFromHtml(ratingElement, "[itemprop=\"ratingValue\"]", "content", false, '.', session);
      
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating != null ? avgRating : 0.0d);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
    }
    
    return ratingReviews;
  }
}
