package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import static br.com.lett.crawlernode.core.models.Card.ELO;
import static br.com.lett.crawlernode.core.models.Card.HIPERCARD;
import static br.com.lett.crawlernode.core.models.Card.MASTERCARD;
import static br.com.lett.crawlernode.core.models.Card.VISA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.ExtensionsKt;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCards;
import models.pricing.Pricing;

/**
 * Date: 04/09/17
 *
 * @author gabriel
 */
public class SaopauloTendadriveCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.tendaatacado.com.br/";

   public SaopauloTendadriveCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".box-product") != null) {

         JSONObject jsonObject = JSONUtils.stringToJson(doc.selectFirst("#__NEXT_DATA__").data());
         JSONObject skuJson = (JSONObject) jsonObject.optQuery("/props/pageProps/product");

         String internalPid = skuJson.optString("name");
         List<String> categories = doc.select(".breadcrumbs a").eachText();
         if (!categories.isEmpty()) {
            categories.remove(0);
         }
         String description =
               CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".more-info", " product-table"));

         String internalId = scrapInternal(skuJson);
         String name = skuJson.optString("name");
         List<String> images = scrapImages(skuJson);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String secondaryImages = images != null && !images.isEmpty() ? new JSONArray(images).toString() : null;
         Offers offers = scrapOffers(doc, skuJson);
         Integer stock = skuJson.optInt("totalStock");
         RatingsReviews ratingsReviews = scrapRating(internalId, doc);

         Product product =
               ProductBuilder.create()
                     .setUrl(session.getOriginalURL())
                     .setOffers(offers)
                     .setInternalId(internalId)
                     .setInternalPid(internalPid)
                     .setName(name)
                     .setCategories(categories)
                     .setPrimaryImage(primaryImage)
                     .setSecondaryImages(secondaryImages)
                     .setDescription(description)
                     .setStock(stock)
                     .setRatingReviews(ratingsReviews)
                     .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Element elem, JSONObject skuJson)
         throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = null;
      Element saleElem = elem.selectFirst(".PriceQtdComponent");
      if (saleElem != null) {
         String sale = saleElem.text();
         if (sale != null) {
            if (sale.contains("un.R")) {
               sales = Collections.singletonList(sale.replace(".", ". "));
            }
         }
      }

      Double price = skuJson.optDouble("price");
      CreditCards creditCards =
            ExtensionsKt.toCreditCards(Arrays.asList(MASTERCARD, VISA, HIPERCARD, ELO), price, 1);

      offers.add(
            OfferBuilder.create()
                  .setSellerFullName("Tenda Drive")
                  .setIsBuybox(false)
                  .setPricing(
                        Pricing.PricingBuilder.create()
                              .setSpotlightPrice(price)
                              .setBankSlip(BankSlipBuilder.create().setFinalPrice(price).build())
                              .setCreditCards(creditCards)
                              .build())
                  .setIsMainRetailer(true)
                  .setUseSlugNameAsInternalSellerId(true)
                  .setSales(sales)
                  .build());

      return offers;
   }

   private List<String> scrapImages(JSONObject skuJson) {
      JSONArray photos = skuJson.optJSONArray("photos");
      List<String> images = new ArrayList<>();
      for (Object obj : photos) {
         if (obj instanceof JSONObject) {
            JSONObject json = (JSONObject) obj;
            images.add(json.optString("url", null));
         }
      }
      return images;
   }

   private String scrapInternal(JSONObject skuJson) {
      String[] tokens = skuJson.optString("token").split("-");
      return tokens[tokens.length - 1];
   }

   private RatingsReviews scrapRating(String internalId, Document doc) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "80984", logger);
      return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
   }
}
