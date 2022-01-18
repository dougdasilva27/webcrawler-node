package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.aws.sqs.QueueService;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.TestCrawlerSession;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import enums.QueueName;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 15/11/2017
 *
 * @author Gabriel Dornelas
 */
public class BrasilAmazonCrawler extends Crawler {

   private static final String HOME_PAGE = "https://" + AmazonScraperUtils.HOST;

   private static final String IMAGES_HOST = "images-na.ssl-images-amazon.com";
   private static final String IMAGES_PROTOCOL = "https";


   protected Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.ELO.toString());

   public BrasilAmazonCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      this.cookies = amazonScraperUtils.handleCookiesBeforeFetch(HOME_PAGE, cookies, new JsoupDataFetcher());
   }

   private String requestMethod(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("authority", "www.amazon.com.br");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.104 Safari/537.36");

      Request requestApache = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create().setForbiddenCssSelector("#captchacharacters").build())
         .build();


      Request requestJSOUP = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.INFATICA_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .setFetcheroptions(FetcherOptions.FetcherOptionsBuilder.create()
            .mustRetrieveStatistics(true)
            .mustUseMovingAverage(false)
            .setForbiddenCssSelector("#captchacharacters").build())
         .build();

      Request request = dataFetcher instanceof JsoupDataFetcher ? requestJSOUP : requestApache;

      Response response = dataFetcher.get(session, request);

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {

         if (dataFetcher instanceof ApacheDataFetcher) {
            response = new ApacheDataFetcher().get(session, requestApache);
         } else {
            response = new JsoupDataFetcher().get(session, requestJSOUP);
         }
      }

      return response.getBody();
   }

   @Override
   protected Document fetch() {
      String url = session.getOriginalURL();
      String content = requestMethod(url);
      return Jsoup.parse(content);

   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private final AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   protected Response fetchResponse() {
      return amazonScraperUtils.fetchProductPageResponse(cookies, dataFetcher);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (amazonScraperUtils.isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = amazonScraperUtils.crawlInternalId(doc);
         String internalPid = internalId;
         String name = amazonScraperUtils.crawlName(doc);
         CategoryCollection categories = amazonScraperUtils.crawlCategories(doc);

         JSONArray images = this.amazonScraperUtils.scrapImagesJSONArray(doc);
         String primaryImage = this.amazonScraperUtils.scrapPrimaryImage(images, doc, IMAGES_PROTOCOL, IMAGES_HOST);
         List<String> secondaryImages = this.amazonScraperUtils.scrapSecondaryImages(images, IMAGES_PROTOCOL, IMAGES_HOST);

         String description = this.amazonScraperUtils.crawlDescription(doc);
         Integer stock = null;
         List<String> eans = amazonScraperUtils.crawlEan(doc);
         Offer mainPageOffer = amazonScraperUtils.scrapMainPageOffer(doc);
         List<Document> docOffers = fetchDocumentsOffers(doc, internalId);
         Offers offers = scrapOffers(doc, docOffers, mainPageOffer);

         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
         ratingReviewsCollection.addRatingReviews(amazonScraperUtils.crawlRating(doc, internalId));
         RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setEans(eans)
            .setRatingReviews(ratingReviews)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   public Document fetchDocumentsOffersRequest(String internalId, int page) {
      Document doc;
      String urlMarketPlace = "https://www.amazon.com.br/gp/product/ajax/ref=aod_page_" + page + "?asin=" + internalId + "&pc=dp&isonlyrenderofferlist=true&pageno=" + page + "&experienceId=aodAjaxMain";
      int maxAttempt = 3;
      int attempt = 1;

      do {
         String content = requestMethod(urlMarketPlace);
         doc = Jsoup.parse(content);
         attempt++;
      } while (doc.selectFirst("#aod-offer") == null && attempt <= maxAttempt);

      return doc;
   }


   /**
    * Fetch pages when have marketplace info
    *
    * @return documents
    */
   public List<Document> fetchDocumentsOffers(Document doc, String internalId) {
      List<Document> docs = new ArrayList<>();

      Element marketplaceUrl = doc.selectFirst(".a-section.olp-link-widget a[href]");

      if (marketplaceUrl == null) {
         Elements buyBox = doc.select("#moreBuyingChoices_feature_div .a-box.a-text-center h5 span");
         if (buyBox != null && !buyBox.isEmpty()) {
            return docs;
         } else {
            marketplaceUrl = doc.selectFirst(".a-section.a-spacing-base span .a-declarative a");
         }
      }

      if (marketplaceUrl == null) {
         marketplaceUrl = doc.selectFirst(".a-box-inner .olp-text-box");
      }

      if (marketplaceUrl == null) {
         marketplaceUrl = doc.selectFirst("#buybox-see-all-buying-choices a");
      }

      if (marketplaceUrl != null) {

         int totalOffers = CrawlerUtils.scrapIntegerFromHtml(doc, ".a-box-inner .olp-text-box span", true, 0);

         if (totalOffers == 0) {
            totalOffers = CrawlerUtils.scrapIntegerFromHtml(doc, "#olp_feature_div span.a-declarative .a-link-normal span", true, 0);
         }

         int paginationOffers = totalOffers / 10 + 1;

         for (int i = 1; i <= paginationOffers; i++) {
            Document docMarketplace = fetchDocumentsOffersRequest(internalId, i);
            docs.add(docMarketplace);

         }
      }
      return docs;
   }

   public Offers scrapOffers(Document doc, List<Document> offersPages, Offer mainPageOffer) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int pos = 1;

      if (mainPageOffer != null) {
         mainPageOffer.setSellersPagePosition(pos);
         offers.add(mainPageOffer);
         pos = 2;
      }

      Elements buyBox = doc.select(".a-box.mbc-offer-row.pa_mbc_on_amazon_offer");

      if (buyBox != null && !buyBox.isEmpty()) {
         for (Element oferta : buyBox) {
            amazonScraperUtils.getOffersFromBuyBox(oferta, pos, offers);
            pos++;
         }
      }

      //send to webdriver only whirpool and gopro
      if (!offersPages.isEmpty()) {
         for (Document offerPage : offersPages) {
            if (checkIfSendToQueue(offerPage, doc)) {
               sendMessage();
               Logging.printLogDebug(logger, session, "Block in offers page - sending to try webdriver");
            } else {
               Elements ofertas = offerPage.select("#aod-offer");
               for (Element oferta : ofertas) {
                  amazonScraperUtils.getOffersFromOfferPage(oferta, pos, offers);
                  pos++;
               }
            }
         }

      }
      return offers;
   }

   private boolean pageOfferIsBlocked(Document offerPage) {
      Element block = offerPage.selectFirst("#aod-offer");
      return block == null;
   }

   private boolean checkIfSendToQueue(Document offerPage, Document doc) {
      List<Long> specificSuppliers = Arrays.asList(174l, 1470l);
      boolean hasPageOffers = !doc.select(AmazonScraperUtils.listSelectors.get("iconArrowOffer")).isEmpty() || !doc.select(AmazonScraperUtils.listSelectors.get("linkOffer")).isEmpty();

      return pageOfferIsBlocked(offerPage) && session.getSupplierId() != null && specificSuppliers.contains(session.getSupplierId()) && hasPageOffers;

   }


   private void sendMessage() {
      if (session instanceof TestCrawlerSession) {
         Logging.printLogWarn(logger, session, "Block in offers page - don't send to queue because is a test");
      } else {
         JSONObject jsonToSentToQueue = mountMessageToSendToQueue(session.getMarket());
         QueueService.SendMessageResult(Main.queueHandler.getSqs(), QueueName.WEB_SCRAPER_PRODUCT_AMAZON_WD.toString(), jsonToSentToQueue.toString());
      }
   }

   public JSONObject mountMessageToSendToQueue(Market market) {
      String sessionId = UUID.randomUUID().toString();

      JSONObject jsonToSendToCrawler = new JSONObject();
      JSONObject marketInfo = new JSONObject();
      marketInfo.put("code", market.getCode());
      marketInfo.put("regex", market.getFirstPartyRegex());
      marketInfo.put("fullName", market.getFullName());
      marketInfo.put("marketId", market.getId());
      marketInfo.put("use_browser", true);
      marketInfo.put("name", market.getName());
      jsonToSendToCrawler.put("type", session.getScraperType());
      jsonToSendToCrawler.put("sessionId", sessionId);
      jsonToSendToCrawler.put("options", new JSONObject());
      jsonToSendToCrawler.put("market", marketInfo);
      jsonToSendToCrawler.put("className", "br.com.lett.crawlernode.crawlers.corecontent.brasil.BrasilAmazonWDCrawler");
      jsonToSendToCrawler.put("parameters", session.getOriginalURL());
      if (session.getProcessedId() != null) {
         jsonToSendToCrawler.put("processedId", session.getProcessedId());
      }
      if (session.getProcessedId() != null) {
         jsonToSendToCrawler.put("internalId", session.getInternalId());
      }

      return jsonToSendToCrawler;
   }

}
