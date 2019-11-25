package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

public class BrasilFarvetCrawler extends Crawler {
  
  public BrasilFarvetCrawler(Session session) {
    super(session);
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".informacoes #IdProduto", "value");
      String internalPid = scrapInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".informacoes > p", true);
      Float price = scrapPrice(doc);
      Prices prices = scrapPrices(doc, price);
      CategoryCollection categories = scrapCategories(doc);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imagens #zoomProduto", Arrays.asList("href"), "https:", "static3.minhalojanouol.com.br");
      String secondaryImages = null;
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".detalhes .detalheresumo", ".descricao"));
      Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "#Estoque", "value", 0);
      boolean available = doc.selectFirst(".semEstoque[style=\"display:none;\"]") != null;
      Marketplace marketplace = null;
          
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
          .setMarketplace(marketplace)
          .build();
      
        products.add(product);
        
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }
    
    return products;
  }
  
  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".conteudo .detalhes") != null;
  }
  
  private String scrapInternalPid(Document doc) {
    String internalPid = null;
    Element e = doc.selectFirst(".informacoes > p:not(:first-child)");
    
    if(e != null) {
      String text = e.text();
      int index = text.indexOf(':');
      
      if(index > -1) {
        internalPid = text.substring(index + 1).trim();
      }
    }
    
    return internalPid;
  }
  
  private CategoryCollection scrapCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Element categoriesElement = doc.selectFirst(".breadCrumbsdetails span");
    
    if(categoriesElement != null) {
      Element firstCatElement = categoriesElement.selectFirst("a");
      
      if(firstCatElement != null) {
        categories.add(firstCatElement.text().trim());
      }
      
      categories.add(categoriesElement.ownText().replace("/", "").trim());
    }
    
    return categories;
  }
  
  private Float scrapPrice(Document doc) {
    Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#PrecoPromocaoProduto", null, true, ',', session);
    
    if(price == null) {
      price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#PrecoProduto", null, true, ',', session);
    }
    
    return price;
  }
  
  private Prices scrapPrices(Document doc, Float price) {
    Prices prices = new Prices();
    
    if(price != null) {
      
      Float priceFrom = CrawlerUtils.scrapFloatPriceFromHtml(doc, "#PrecoProduto", null, true, ',', session);
      
      if(priceFrom > price) {
        prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(priceFrom.doubleValue()));
      }
      
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
    }
    
    return prices;
  }
}
