package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.prices.Prices;

/**
 * Date: 13/08/2018
 * @author victor
 *
 */
public class BrasilDellCrawler extends Crawler {


  private static final String HOME_PAGE = "https://www.dell.com/pt-br";

  public BrasilDellCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception{
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if(isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = crawlName(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(doc, price);
      boolean available = price != null;

      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
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
      Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
    }
    return products;
  } 

  /**
   * Checks if the page acessed is product page or not.
   * @param doc - contais html from the page to be scrapped
   * @return true if its a skupage.
   */
  private boolean isProductPage(Document doc) {
    Element metaTags = doc.selectFirst("meta[content=productdetails]");

    if(metaTags != null) {
      return true;      
    }

    return false;
  }

  /**
   * Gets the sku pid.
   * @param doc - contais html from the page to be scrapped
   * @return the extracted pid from the URL.
   */
  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element metaProducts = doc.selectFirst("meta[name=products]");
    if(metaProducts != null) {
      internalId = metaProducts.attr("content").replace(";", ""); 
    } else {
      Element metaSnpsku = doc.selectFirst("meta[name=snpsku]");

      if(metaSnpsku != null) {
        internalId = metaSnpsku.attr("content").replace(";", ""); 
      } 
    }

    return internalId;
  }

  /**
   * Gets the sku Name
   * @param doc - contais html from the page to be scrapped
   * @return the sku Name
   */
  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.selectFirst("title");

    if(nameElement != null) {
      name = nameElement.text();
    }
    return name;
  }

  /**
   * Scrapps the category info from the sku page.
   * @param doc - HTML to be scrapped
   * @return category infos.
   */
  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = doc.select(".breadcrumbs > ol > li:not(:first-child) > a");

    for(Element e : elementCategories) {
      categories.add(e.text().trim());
    }
    return categories;
  }

  /**
   * Get the primary image from the sku.
   * @param doc - the html data to be scrapped
   * @return - the primary sku image
   */
  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.selectFirst(".slides > li > img");

    //In case the sku page its the type that loads data without javascript i search for this element
    Element imageElement = doc.selectFirst("#features-container .text-centered > img");

    if(primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-blzsrc");

      if(!primaryImage.startsWith("https:")) {
        primaryImage = "https:"+ primaryImageElement.attr("data-blzsrc");     
      }else if(!primaryImage.startsWith("http:")) {
        primaryImage = "http:"+ primaryImageElement.attr("data-blzsrc");
      }
    }else if(imageElement != null) {
      primaryImage = imageElement.attr("data-original");

      if(!imageElement.attr("data-original").startsWith("https:")) {
        primaryImage = "https:" + imageElement.attr("data-original");     
      }else if(!primaryImage.startsWith("http:")) {
        primaryImage = "http:" + imageElement.attr("data-original");
      }

    }

    return primaryImage;
  }

  /**
   * Get the seconday images from the sku
   * @param doc - html to be scrapped
   * @return array of images
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();
    Elements imagesElement = doc.select(".slides > li > img");

    //In case the sku page its the type that loads data without javascript i search for this element
    Elements secondaryImagesElements = doc.select("#features-container .col-xs-12 > img");

    if(imagesElement != null) {
      for(int i = 1; i < imagesElement.size(); i++) {
        Element e = imagesElement.get(i);

        if(e.attr("data-blzsrc")!= null) {
          String url = "https:" + e.attr("data-blzsrc");
          secondaryImagesArray.put(url);
        }
      }
      if(secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }
    }

    if(secondaryImagesElements != null) {
      for(int i = 1; i < secondaryImagesElements.size();i++) {
        Element item = secondaryImagesElements.get(i);

        if(item.attr("data-original") != null) {
          String url = "https:" + item.attr("data-original");
          secondaryImagesArray.put(url);
        }
        if(secondaryImagesArray.length() > 0) {
          secondaryImages = secondaryImagesArray.toString();
        } 
      }
    }

    return secondaryImages;
  }

  /**
   * Gets the description of the SKU
   * @param doc - html to be scrapped
   * @return a html formated into a string
   */
  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    
    Element descHead = doc.selectFirst("div.xs-text-centered > p");

    if(descHead != null) {
      description.append(descHead.text());

      Elements descType1 = doc.select("#product-detail-feature-container > div.md-pad-right-30 > div.row.xs-pad-offset-15");
      
      if(!descType1.isEmpty()) {
        description.append(descType1.outerHtml());
      }
       
      Elements descTecnicKey = doc.select(".configModule > modulecontroller .specs > div > h4 > span[data-testid=config_module_title]");
      Elements descTecnicValue = doc.select(".configModule > modulecontroller .specs > div > optiongroup > ul > optionlist > li > div > div > input");
                                            // .configModule .specs > div ul > li > div  p
      System.out.println(descTecnicKey);
      System.out.println(descTecnicValue); // nao tem no HTML nem na API bolar um plano
      if(!descTecnicKey.isEmpty() && descTecnicValue.isEmpty()) {
        for(int i = 0; i < descTecnicKey.size(); i++) {
          System.out.println(descTecnicKey.eq(i).outerHtml());
          System.out.println(descTecnicValue.eq(i).attr("value"));
          description.append(descTecnicKey.eq(i).outerHtml());
          description.append(descTecnicValue.eq(i).attr("value"));
        }
      }

    }else {
      //In case the sku page its the type that loads data without javascript i search for this element
      Element descType2Head = doc.selectFirst("span.marketing-blurb");

      if(descType2Head != null) {
        description.append(descType2Head.outerHtml());
      }

      Elements descType2 = doc.select("#overview> div.xs-top-offset-medium > div.row.xs-pad-offset-15");

      if(!descType2.isEmpty()) {
        description.append(descType2.outerHtml());
      }

    }

    return description.toString(); 
  }

  /**
   * Gets the SKU prices
   * @param doc - html to be scrapped
   * @return the price scrapped from the sku page
   */
  private Float crawlPrice(Document doc) {
    Float price = null;
    // Div que contém todos os dados de preço
    Element priceDiv = doc.selectFirst("div.uDetailedPrice > div > div > div > div.vertical-overflow > h5 > strong > span");
    Element priceType2 = doc.selectFirst("div.dellPricing > h5 > strong > span.pull-right > span"); // caso a pagina do SKU seja diferente

    if(priceDiv != null) {
      price = MathUtils.parseFloat(priceDiv.text());
    } else if(priceType2 != null) {
      price = MathUtils.parseFloat(priceType2.text());
    }

    return price; 
  }

  /**
   * Get the SKU Old price if it exists
   * @param doc - html to be scrapped
   * @return
   */
  private Double crawlOldPrice(Document doc) {
    Double price = null;

    Element priceDiv = doc.selectFirst("div.dell-pricing-total-savings-section > p > span > small");

    if(priceDiv != null) {
      price = MathUtils.parseDouble(priceDiv.text());
    }
    return price;
  }

  /**
   * Create a map of Prices and payment way
   * @param doc - html to be scrapped
   * @param price - price of the SKU
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if(price != null) {
      Map<Integer,Float> paymentPriceMap = new TreeMap<>();

      prices.setBankTicketPrice(price);
      prices.setPriceFrom(crawlOldPrice(doc));

      Element sightPriceElement = doc.selectFirst("div.lease-rate-adjusted-amount-per-pay-period > div > p > span");

      if(sightPriceElement != null) {
        Float sightPrice = MathUtils.parseFloat(sightPriceElement.ownText());
        paymentPriceMap.put(Math.round(sightPrice), MathUtils.normalizeTwoDecimalPlaces(price/sightPrice));
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), paymentPriceMap);

    }
    return prices;
  }

}
