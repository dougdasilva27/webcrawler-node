package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
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
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilPetzCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.petz.com.br/";

   public BrasilPetzCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.WEBDRIVER);
   }

   @Override
   protected Object fetch() {
      Document doc = new Document("");
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

      if (this.webdriver != null) {
         doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

         Element script = doc.select("head script").last();
         Element robots = doc.select("meta[name=robots]").first();

         if (script != null && robots != null) {
            String eval = script.html().trim();

            if (!eval.isEmpty()) {
               Logging.printLogDebug(logger, session, "Execution of incapsula js script...");
               this.webdriver.executeJavascript(eval);
            }
         }

         String requestHash = FetchUtilities.generateRequestHash(session);
         this.webdriver.waitLoad(12000);

         doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

         // saving request content result on Amazon
         S3Service.saveResponseContent(session, requestHash, doc.toString());
      }

      return doc;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = crawlInternalPid(doc);
         CategoryCollection categories = crawlCategories(doc);
         Elements variations = doc.select(".opt_radio_variacao[data-urlvariacao]");

         if (variations.size() > 1) {
            Logging.printLogInfo(logger, session, "Page with more than one product.");
            for (Element e : variations) {
               String nameVariation = crawlNameVariation(e);

               if (e.hasClass("active")) {
                  Product p = crawlProduct(doc, nameVariation);
                  p.setInternalPid(internalPid);
                  p.setCategory1(categories.getCategory(0));
                  p.setCategory2(categories.getCategory(1));
                  p.setCategory3(categories.getCategory(2));

                  products.add(p);
               } else {
                  String url = (HOME_PAGE + e.attr("data-urlvariacao")).replace("br//", "br/");
                  Document docVariation = DynamicDataFetcher.fetchPage(this.webdriver, url, session);

                  Product p = crawlProduct(docVariation, nameVariation);
                  p.setInternalPid(internalPid);
                  p.setCategory1(categories.getCategory(0));
                  p.setCategory2(categories.getCategory(1));
                  p.setCategory3(categories.getCategory(2));

                  products.add(p);
               }
            }
         } else {
            Logging.printLogInfo(logger, session, "Page with only on product.");
            Product p = crawlProduct(doc, null);
            p.setInternalPid(internalPid);
            p.setCategory1(categories.getCategory(0));
            p.setCategory2(categories.getCategory(1));
            p.setCategory3(categories.getCategory(2));

            products.add(p);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page: " + this.session.getOriginalURL());
      }

      return products;
   }

   private Product crawlProduct(Document doc, String nameVariation) {
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = crawlName(doc, nameVariation);
         String internalId = crawlInternalId(doc);
         boolean available = doc.select(".is_available").first() != null;
         String description = crawlDescription(doc);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(price, doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc, primaryImage);
         List<String> eans = crawlEans(doc);

         return ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price).setPrices(prices)
               .setAvailable(available).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description).setEans(eans)
               .setMarketplace(new Marketplace()).build();
      }

      return new Product();
   }

   private List<String> crawlEans(Document doc) {
      List<String> eans = new ArrayList<>();
      Element meta = doc.selectFirst("meta[itemprop=\"gtin13\"]");

      if (meta != null) {
         eans.add(meta.attr("content"));
      }

      return eans;
   }

   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document document) {
      return document.select(".prod-info").first() != null;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlNameVariation(Element e) {
      String nameVariation = null;

      Element name = e.select("label > div").first();
      if (name != null) {
         nameVariation = name.ownText().replace("\"", "");
      }

      return nameVariation;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element sku = doc.select(".prod-info .reset-padding").first();
      if (sku != null) {
         internalId = sku.ownText().replace("\"", "").trim();
      }

      return internalId;
   }

   private String crawlInternalPid(Document doc) {
      String internalPid = null;

      Element pid = doc.select("#prodNotificacao").first();
      if (pid != null) {
         internalPid = pid.val();
      }

      return internalPid;
   }

   private String crawlName(Document doc, String nameVariation) {
      StringBuilder name = new StringBuilder();

      Element nameElement = doc.select("h1[itemprop=name]").first();

      if (nameElement != null) {
         name.append(nameElement.ownText());

         if (nameVariation != null) {
            name.append(" " + nameVariation);
         }
      }

      return name.toString();
   }

   private Float crawlPrice(Document doc) {
      Float price = null;
      Element priceElement = doc.select(".price-current").first();

      if (priceElement != null) {
         price = MathUtils.parseFloatWithComma(priceElement.ownText());
      }

      return price;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select(".sp-wrap a").first();

      if (image != null) {
         primaryImage = CrawlerUtils.sanitizeUrl(image, "href", "https:", "www.petz.com.br");
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc, String primaryImage) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".sp-wrap .slick-track a:not(:first-child)");

      for (Element e : images) {
         String image = CrawlerUtils.sanitizeUrl(e, "href", "https:", "www.petz.com.br");

         if (!image.equals(primaryImage)) {
            secondaryImagesArray.put(image);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select("#breadcrumbList li[itemprop] a span");

      for (int i = 1; i < elementCategories.size(); i++) { // first item is the home page
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element prodInfo = doc.selectFirst(".col-md-7.prod-info > div:not([class])");
      if (prodInfo != null) {
         description.append(prodInfo.html());
      }

      // Element shortDescription = doc.select("p[dir=ltr]").first();
      // if (shortDescription != null) {
      // description.append(shortDescription.html());
      // }

      Elements elementsInformation = doc.select(".infos, #especificacoes");
      for (Element e : elementsInformation) {
         if (e.select(".depoimento, #depoimentos, .depoimentoTexto").isEmpty()) {
            description.append(e.html());
         }
      }

      return description.toString();
   }

   /**
    * To crawl this prices is accessed a api Is removed all accents for crawl price 1x like this: Visa
    * à vista R$ 1.790,00
    * 
    * @param internalId
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         prices.setBankTicketPrice(price);

         Element priceFrom = doc.select(".de-riscado").first();

         if (priceFrom != null) {
            prices.setPriceFrom(MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseFloatWithComma(priceFrom.ownText()).doubleValue()));
         }

         Map<Integer, Float> mapInstallments = new HashMap<>();
         mapInstallments.put(1, price);

         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".de-apagado", doc, true, "x", "", true);
         if (!pair.isAnyValueNull()) {
            mapInstallments.put(pair.getFirst(), pair.getSecond());
         }

         prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
         prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
         prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
         prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
         prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
         prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
      }

      return prices;
   }
}
