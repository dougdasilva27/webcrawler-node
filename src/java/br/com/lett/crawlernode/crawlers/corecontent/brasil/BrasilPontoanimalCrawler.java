package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.prices.Prices;

public class BrasilPontoanimalCrawler extends Crawler {
  
  private static final String HOME_PAGE = "www.pontoanimalpetshop.com.br";
  
  public BrasilPontoanimalCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
   
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
      
      String productText = CrawlerUtils.scrapStringSimpleInfo(doc, "#produto", false);
      
      String internalId = scrapInternalId();
      String internalPid = null;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1#h1", true);
      Float price = scrapPrice(productText);
      Prices prices = scrapPrices(productText, price);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb > li", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "[id*=gallery_] > a", Arrays.asList("data-zoom-image", "data-image"), "https", HOME_PAGE);
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "[id*=gallery_] > a", Arrays.asList("data-zoom-image", "data-image"), "https", HOME_PAGE, primaryImage);
      String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".descricao_div_conteudo .descricao_texto"));
      boolean available = doc.selectFirst(".bt_carrinho_falta") == null;
      
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
          .build();
      
      products.add(product);
      
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("#produto") != null;
  }
  
  private String scrapInternalId() {
    String internalId = null;  
    String[] urlSplit = session.getOriginalURL().split("/");
    
    for(int i = 0; i < urlSplit.length; i++) {
      if(urlSplit[i].equals("produto") && i+1 < urlSplit.length) {
        internalId = urlSplit[i+1];
        break;
      }
    }
    
    return internalId;
  }
  
  private Float scrapPrice(String pricesString) {
    if(pricesString.contains("em até")) {
      return CrawlerUtils.scrapFloatPriceFromString(pricesString, ',', "por apenas", "em até", session);
    } 
    
    if(pricesString.contains("ou R$")) {
      return CrawlerUtils.scrapFloatPriceFromString(pricesString, ',', "Por apenas", "ou R$", session);
    }
    
    return null;
  }
  
  private Prices scrapPrices(String pricesString, Float price) {
    Prices prices = new Prices();
    
    if(pricesString != null && price != null) {
      
      if(pricesString.contains("OFF de")) {
        prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(
            CrawlerUtils.scrapFloatPriceFromString(pricesString, ',', "OFF de", "por apenas", session).doubleValue()));
      }
      
      prices.setBankTicketPrice(MathUtils.normalizeTwoDecimalPlaces(
          CrawlerUtils.scrapFloatPriceFromString(pricesString, ',', "ou R$", "no boleto", session).doubleValue()));
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      String installmentString = pricesString.indexOf("em até") != -1 ? pricesString.substring(pricesString.indexOf("em até")) : "";
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallmentFromString(installmentString, "de", "juros", false);
      
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }
      
      List<Card> marketCards = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.DINERS, Card.ELO);
      for(Card c : marketCards) {
        prices.insertCardInstallment(c.toString(), installmentPriceMap);
      }
    }
    
    return prices;
  }
}
