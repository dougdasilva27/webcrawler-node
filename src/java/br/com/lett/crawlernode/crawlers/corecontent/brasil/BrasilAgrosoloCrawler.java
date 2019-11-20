package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.AdvancedRatingReview;
import models.RatingsReviews;
import models.prices.Prices;

public class BrasilAgrosoloCrawler extends Crawler {
	
  // De:  https://www.agrosolo.com.br/dog-chow-adultos-racas-medias-grandes-frango-arroz-15kg
  // Var: https://www.agrosolo.com.br/racao-premier-ambientes-internos-gatos-adultos-castrados-ate-7-anos-sabor-salmao
  // Ind: https://www.agrosolo.com.br/combo-royal-canin-mini-adulto-25kg-gratis-2-latas-de-alimento-umido
	  
  private static final String HOME_PAGE = "www.agrosolo.com.br";
  
  public BrasilAgrosoloCrawler(Session session) {
    super(session);
    super.config.setMustSendRatingToKinesis(true);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    Elements elements = doc.select("a[href][idgrade]");
    
    if(elements.size() > 1) {
	    for(Element e : elements) {
	    	String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "idgrade");
	    	String variationName = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, null, "title");;
	    	
	    	if(internalId != null) {
			  Request request = RequestBuilder.create()
					  .setUrl(session.getOriginalURL() + "?idgrade=" + internalId)
					  .setCookies(cookies)
					  .build();
			
			  Document productDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
		
			  Product product = extractProduct(productDoc, internalId, variationName);
			
		      if(product != null) {
			    products.add(product);
		      }
	    	}
	    }
    } else {
    	Product product = extractProduct(doc, CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ctrIdGrade", "value"), null);
		
        if(product != null) {
	      products.add(product);
        }
    }

    return products;
  }
  
  private Product extractProduct(Document doc, String internalId, String variationName) {
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ctrIdProduto", "value");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info > h1", true);
      Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, ".new-value .ctrValorMoeda", null, true, ',', session);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > ul > li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image .ctrFotoPrincipalZoomNew", Arrays.asList("href"), "https", HOME_PAGE);
      String secondaryImages = null;
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("[tab=caracteristicas]", "#caracteristicas", "[tab=especificacoes]", "#especificacoes"));
      Integer stock = null;
      String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[ean]", "ean");
      List<String> eans = ean != null && !ean.isEmpty() ? Arrays.asList(ean) : null;
      RatingsReviews ratingsReviews = null;
      
      Element availabilityElement = doc.selectFirst(".product-buy-area");
      boolean available = availabilityElement != null && !availabilityElement.hasAttr("hidden");
      
      if(variationName != null && !variationName.isEmpty()) {
    	  name += " - " + variationName;
      }
      
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
          .setStock(stock)
          .setEans(eans)
          .setRatingReviews(ratingsReviews)
          .build();
      
      return product;
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return null;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".content-product") != null;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      prices.setPriceFrom(CrawlerUtils.scrapDoublePriceFromHtml(doc, ".old-value .ctrValorDeMoeda", null, true, ',', session));
      prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, ".ctrValorVistaArea .ctrValorVistaMoeda", null, true, ',', session));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".parcel", doc, false, "x", "juros", false);
      
      if (!installment.isAnyValueNull()) {
        installmentPriceMap.put(installment.getFirst(), installment.getSecond());
      }
      

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }
    
    return prices;
  }
  
  private RatingsReviews scrapRatingReviews(Document doc) {
    RatingsReviews ratingReviews = new RatingsReviews();
    ratingReviews.setDate(session.getDate());
    
    Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"ratingCount\"]", "content", 0);
    Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".comments-wrapper [itemprop=\"ratingValue\"]", "", false, '.', session);
    Integer totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"reviewCount\"]", "content", 0);
    AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
    
    ratingReviews.setTotalRating(totalNumOfEvaluations);
    ratingReviews.setAverageOverallRating(avgRating);
    ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
    ratingReviews.setAdvancedRatingReview(advancedRatingReview);
    
    return ratingReviews;
  }
  
  private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
    Integer star1 = 0;
    Integer star2 = 0;
    Integer star3 = 0;
    Integer star4 = 0;
    Integer star5 = 0;
    
    Elements reviews = doc.select(".product-comment-list > .customer-comments .stars > .rating-stars");
    
    for(Element review : reviews) {
      if(review.hasAttr("data-score")) {
        Integer val = Integer.parseInt(review.attr("data-score").replaceAll("[^0-9]+", ""));     
        
        switch(val) {
          case 1: star1 += 1; 
          break;
          case 2: star2 += 1; 
          break;
          case 3: star3 += 1; 
          break;
          case 4: star4 += 1; 
          break;
          case 5: star5 += 1; 
          break;
        }
      }
    }
    
    return new AdvancedRatingReview.Builder()
        .totalStar1(star1)
        .totalStar2(star2)
        .totalStar3(star3)
        .totalStar4(star4)
        .totalStar5(star5)
        .build();
  }
}
