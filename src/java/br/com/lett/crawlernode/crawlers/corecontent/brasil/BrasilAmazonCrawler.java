package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.AmazonScraperUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import org.json.JSONArray;
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
      super.config.setFetcher(FetchMode.MIRANHA);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private final AmazonScraperUtils amazonScraperUtils = new AmazonScraperUtils(logger, session);

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (amazonScraperUtils.isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = amazonScraperUtils.crawlInternalId(doc);
         String name = amazonScraperUtils.crawlName(doc);
         CategoryCollection categories = amazonScraperUtils.crawlCategories(doc);

         JSONArray images = this.amazonScraperUtils.scrapImagesJSONArray(doc);
         String primaryImage = this.amazonScraperUtils.scrapPrimaryImage(images, doc, IMAGES_PROTOCOL, IMAGES_HOST);
         List<String> secondaryImages = this.amazonScraperUtils.scrapSecondaryImages(images, IMAGES_PROTOCOL, IMAGES_HOST);

         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("#productDetails_feature_div", "#productDescription_feature_div.celwidget", "#featurebullets_feature_div.celwidget", ".a-normal.a-spacing-micro tbody", ".aplus-v2.desktop.celwidget"));
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
            .setInternalPid(internalId)
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
      String urlMarketPlace = "https://www.amazon.com.br/gp/product/ajax/ref=aod_page_" + page + "?asin=" + internalId + "&pc=dp&isonlyrenderofferlist=true&pageno=" + page + "&experienceId=aodAjaxMain";
      if (page > 1) {
         urlMarketPlace += "&isonlyrenderofferlist=true";
      }

      Map<String, String> headers = new HashMap<>();
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("authority", "www.amazon.com.br");
      headers.put("cache-control", "no-cache");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36");
      Request request = Request.RequestBuilder.create()
         .setUrl(urlMarketPlace)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.LUMINATI_SERVER_BR_HAPROXY,
            ProxyCollection.LUMINATI_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.LUMINATI_RESIDENTIAL_BR
         ))
         .build();
      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");
      return Jsoup.parse(response.getBody());
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
         for (Element offer : buyBox) {
            amazonScraperUtils.getOffersFromBuyBox(offer, pos, offers);
            pos++;
         }
      }

      if (!offersPages.isEmpty()) {
         for (Document offerPage : offersPages) {
            Elements buy_offers = offerPage.select("#aod-offer");
            for (Element buy_offer : buy_offers) {
               amazonScraperUtils.getOffersFromOfferPage(buy_offer, pos, offers);
               pos++;
            }
         }

      }
      return offers;
   }
}
