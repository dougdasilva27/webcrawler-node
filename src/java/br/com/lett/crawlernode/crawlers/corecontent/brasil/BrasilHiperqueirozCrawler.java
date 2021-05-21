package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilHiperqueirozCrawler extends Crawler {

   private static final String CLIENT_ID = "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff";
   private static final String CLIENT_SECRET = "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c";

   protected Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString());

   public BrasilHiperqueirozCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected JSONObject fetch() {
      JSONObject json = new JSONObject();

      String[] splittedUrl = session.getOriginalURL().split("/");

      if (splittedUrl.length > 0) {
         String lastIndex = splittedUrl[splittedUrl.length - 1];
         String url = "https://www.merconnect.com.br/mapp/v2/markets/167/items/" + lastIndex.substring(0, lastIndex.indexOf("?"));

         Map<String, String> headers = new HashMap<>();
         headers.put("Accept-Encoding", "gzip, deflate, br");
         headers.put("Content-Type", "application/json;charset=UTF-8");
         headers.put("Connection", "keep-alive");
         headers.put("Authorization", "Bearer " + fetchApiToken(headers));

         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .setHeaders(headers)
            .build();

         Response response = dataFetcher.get(session, request);

         json = CrawlerUtils.stringToJson(response.getBody());
      }

      return json;
   }

   protected String fetchApiToken(Map<String, String> headers) {
      String apiToken = "";

      JSONObject payload = new JSONObject();
      payload.put("client_id", CLIENT_ID);
      payload.put("client_secret", CLIENT_SECRET);
      payload.put("grant_type", "client_credentials");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.merconnect.com.br/oauth/token")
         .setHeaders(headers)
         .setPayload(payload.toString())
         .build();

      JSONObject json = CrawlerUtils.stringToJson(dataFetcher.post(session, request).getBody());

      if (json != null && !json.isEmpty()) {
         apiToken = json.optString("access_token");
      }

      return apiToken;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (json.has("item")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

//         String internalId = crawlInternalId(doc);
//         String internalPid = internalId;
//         String name = crawlName(doc);
//         CategoryCollection categories = crawlCategories(doc);
//
//         JSONArray images = this.amazonScraperUtils.scrapImagesJSONArray(doc);
//         String primaryImage = this.amazonScraperUtils.scrapPrimaryImage(images, doc, IMAGES_PROTOCOL, IMAGES_HOST);
//         String secondaryImages = this.amazonScraperUtils.scrapSecondaryImages(images, IMAGES_PROTOCOL, IMAGES_HOST);
//
//         String description = crawlDescription(doc);
//         Integer stock = null;
//
//         Offer mainPageOffer = scrapMainPageOffer(doc);
//         List<Document> docOffers = fetchDocumentsOffers(doc, internalId);
//         Offers offers = scrapOffers(doc, docOffers, mainPageOffer);
//
//         String ean = crawlEan(doc);
//
//         RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
//         ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
//         RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);

//         List<String> eans = new ArrayList<>();
//         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
//            .setInternalId(internalId)
//            .setInternalPid(internalPid)
//            .setName(name)
//            .setCategory1(categories.getCategory(0))
//            .setCategory2(categories.getCategory(1))
//            .setCategory3(categories.getCategory(2))
//            .setPrimaryImage(primaryImage)
//            .setSecondaryImages(secondaryImages)
//            .setDescription(description)
//            .setStock(stock)
//            .setEans(eans)
//            .setRatingReviews(ratingReviews)
//            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


}
