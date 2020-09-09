package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * 1) Only one sku per page.
 * <p>
 * Price crawling notes: 1) This market needs some cookies for request product pages. 2) There is no
 * bank slip (boleto bancario) payment option. 3) There is no installments for card payment. So we
 * only have 1x payment, and to this value we use the cash price crawled from the sku page. (nao
 * existe divisao no cartao de credito).
 *
 * @author Gabriel Dornelas
 */
public class MexicoSorianasuperCrawler extends Crawler {

   private static final String DOMAIN = "superentucasa.soriana.com";
   private static final String HOME_PAGE = "https://superentucasa.soriana.com";

   public MexicoSorianasuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override protected Object fetch() {

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie",
         "ASP.NET_SessionId=0wp2ktrgbecpwtdsjlbglupi;" +
            " NumTipServProduccion=NumTipServ=1;" +
            " NumTdaProduccion=NumTda=51;" +
            " NombreTiendaProduccion=NombreTienda=Las Palmas;" +
            " _ga=GA1.3.1791614647.1599660498;" +
            " _gid=GA1.3.1529849727.1599660498;" +
            " _fbp=fb.1.1599660500363.329928573;" +
            " _hjTLDTest=1;" +
            " _hjid=6e343037-4ee8-48c7-9e49-a5abf1093868;" +
            " _hjAbsoluteSessionInProgress=0;" +
            " isPushNotificationClient=false;" +
            " ak_bmsc=FAA16663A66E0F5F168113993D59768DC99443049A7200009203595FB4A02F38~plGlmlEPwo2uZmAnc8Kp1S6r7J4PHxeo8fZdsQyCxivkFnMy79uYhX4zzeFQV048EejNofGCvAxkXTf8Z/8d+wAoaMTIKGbfONX4oDIb3l8TF6ru4GRIHlSBX10QbXry+VoT0ohgwdcN43IW8H6fsFrRvs8iHglOLvpPPGLQW36txg6tWUEmvlRHUGwT08cA7E+09H/bjRmYu5yMNt9C/d4XnKybwohteUITk0tF7VYbE=;" +
            " BIGipServerPh2uQ04SKDHu6Ndp9q1iLA=704843948.47873.0000;" +
            " TS01f618f3=015c364ca02d8099276b3f5afc64ddce99835feebe09798e997d7e5f2f3e064bc458956f7a8024c5c7bef5aef3c8c5e1825a688f596271691c4ea69ba5e55f1e03869e2788e5b2a0c3acaa36568db10fa6806b23d7e1d2ce06ccd55f493807f5709d58f7d1c7510e701f2e14311745805e6a7cd229;" +
            " _dc_gtm_UA-4339337-8=1; RT=\"z=1&dm=superentucasa.soriana.com&si=a71g0751lcn&ss=kevmm7fo&sl=b&tt=vbv&obo=5&rl=1&ld=yq4h&r=503a27d1844e119d78b20a710d16ee86&ul=yq4j&hd=yqjy\";" +
            " bm_sv=83F1A0443FAF4034DEBFCBA5C91D4923~4vx6pm1K1rTO/t3qGX1g+0erqSZu9BKrnZLLoggVwXIzN4SlVhWbvQlybvwQ6MdAg9B9Rv+H39SBkkg064JEEUH2WbJ84dRP1X4bJK+mhjIQBkbK3vN1e5J3xNFNzcf6fDEgiNXeEGrzi9/NWD73AB9HRgNUOJYdlTB/HZtHFyY=");

      Request request = RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_MX
            )
         )
         .setCookies(cookies)
         .build();

      Document response = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      return response;
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = crawlName(doc);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(price);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = crawlDescription(doc);
         Integer stock = null;
         Marketplace marketplace = crawlMarketplace(doc);

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(marketplace).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select("#DivDesc").first() != null;
   }

   private String crawlInternalId(Document document) {
      String internalId = null;

      Element internalIdElement = document.select("form[target=EditarCarrito] input[name=s]").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   /**
    * There is no internalPid.
    *
    * @param document
    * @return
    */
   private String crawlInternalPid(Document document) {
      String internalPid = null;

      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("#DivDesc").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private Float crawlPrice(Document document) {
      Float price = null;

      String priceText = null;
      Element salePriceElement = document.select("#DivPrecio .precioarticulo").first();

      if (salePriceElement != null) {
         priceText = salePriceElement.ownText();
      }

      if (priceText != null && !priceText.isEmpty()) {
         price = Float.parseFloat(priceText.replaceAll(MathUtils.PRICE_REGEX, ""));
      }

      return price;
   }

   private boolean crawlAvailability(Document document) {
      boolean available = false;

      Element outOfStockElement = document.select("#DivObsart textarea").first();
      if (outOfStockElement != null) {
         available = true;
      }

      return available;
   }

   private Marketplace crawlMarketplace(Document document) {
      return new Marketplace();
   }



   private String crawlPrimaryImage(Document document) {
      String primaryImage = null;
      Element primaryImageElement = document.select("#ImgArt img").first();

      if (primaryImageElement != null) {
         primaryImage = primaryImageElement.attr("src").trim();
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document document) {
      return null;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      Elements activeSubstancesElements = document.select("div.DivSustanciasActivas");
      if (!activeSubstancesElements.isEmpty())
         description.append(activeSubstancesElements.html());

      return description.toString();
   }

   /**
    * There is no bankSlip price.
    * <p>
    * There is no card payment options, other than cash price. So for installments, we will have only
    * one installment for each card brand, and it will be equals to the price crawled on the sku main
    * page.
    *
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price) {
      Prices prices = new Prices();

      if (price != null) {
         Map<Integer, Float> installmentPriceMap = new TreeMap<>();

         installmentPriceMap.put(1, price);

         prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
         prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      }

      return prices;
   }

}
