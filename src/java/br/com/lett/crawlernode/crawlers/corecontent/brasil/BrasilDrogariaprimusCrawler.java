package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import br.com.lett.crawlernode.util.Pair;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Date: 14/08/2017 Refeito por Samir Leão na data: 04/10/2017 (Site mudou)
 *
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogariaprimusCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.drogariaprimus.com.br/";

   public BrasilDrogariaprimusCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".item [itemprop=name]", true);
         Float price = crawlPrice(doc);
         Prices prices = crawlPrices(doc, price);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-photos-list li a[href]", Collections.singletonList("href"), "https",
                 "w1.ezcdn.com.br", primaryImage);
         String description = crawlDescription(doc);
         Integer stock = null;
         Marketplace marketplace = crawlMarketplace();
         RatingsReviews ratingsReviews = scraptRatingAndReviews(doc, internalId);

         // Creating the product
         Product product = ProductBuilder.create()
                 .setUrl(session.getOriginalURL())
                 .setInternalId(internalId)
                 .setInternalPid(internalPid)
                 .setRatingReviews(ratingsReviews)
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
      return doc.select("div.product-contents").first() != null;
   }

   private Float crawlPrice(Document doc) {
      Float price = null;

      Element priceElement = doc.selectFirst(".price .sale[itemprop=\"price\"]");
      if (priceElement != null) {
         price = MathUtils.parseFloatWithComma(priceElement.ownText());
      }

      return price;
   }

   /**
    * Crawl the internalId trying two different approaches.
    *
    * @return the internalId or null if it wasn't found
    */
   private String crawlInternalId(Document document) {
      // two possible options here
      // #produtoid -> is a simple html tag with the id as a value attribute
      // #productdata -> is a json

      // we try to get the data from the json before, because
      // it's less prone to format changes, as it is probably
      // an API response.
      // In case of any error we try to get the id from the
      // attribute 'value' in the #produtoid tag

      Element productDataElement = document.select("#productdata").first();
      if (productDataElement != null) {
         String jsonText = productDataElement.attr("value");
         if (jsonText != null && !jsonText.isEmpty()) {
            try {
               JSONObject productData = new JSONObject(jsonText);
               return productData.getString("id");
            } catch (JSONException jsonException) {
               Logging.printLogDebug(logger, session, "InternalId error [" + jsonException.getMessage() + "]");
               Logging.printLogDebug(logger, "Trying backup approach ... ");
            }
         }
      }

      Element productIdElement = document.select("#produtoid").first();
      if (productIdElement != null) {
         String attrValue = productIdElement.attr("value");
         if (attrValue == null || attrValue.isEmpty()) {
            Logging.printLogDebug(logger, session, "Backup approach also failed [attrValue = " + attrValue + "]");
         } else {
            return attrValue;
         }
      }

      return null;
   }

   /**
    * Crawl the internalPid which is the EAN on the retailer website.
    *
    * @return the internalPid or null if it wasn't found
    */
   private String crawlInternalPid(Document document) {
      Element internalPidElement = document.select(".sku span[itemprop='sku']").first();
      if (internalPidElement != null) {
         return internalPidElement.text().trim();
      }
      return null;
   }

   /**
    * Always get an empty Marketplace. No SKU was found with a marketplace on the retailer website.
    *
    * @return an empty marketplace object
    */
   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }

   /**
    * Crawl the primary image url.
    *
    * @return the primary image url or null if it wasn't found
    */
   private String crawlPrimaryImage(Document doc) {
      Element elementPrimaryImage = doc.select("div.img-wrapper #a-zoom").first();
      if (elementPrimaryImage != null) {
         String href = elementPrimaryImage.attr("href").trim();

         if (href.equalsIgnoreCase("javascript:void(0)")) {
            href = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-photos-list li[data-img]", Collections.singletonList("data-img"), "https", "w1.ezcdn.com.br");
         }

         try {
            URI rawUri = new URI(href);
            if (rawUri.getScheme() == null) {
               return new URIBuilder().setScheme("https").setHost(rawUri.getHost()).setPath(rawUri.getPath()).build().toString();
            }

            return rawUri.toString();

         } catch (URISyntaxException uriSyntaxException) {
            Logging.printLogDebug(logger, session, "Not a valid image URL");
         }
      }

      return null;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements categoryElementsCollection = document.select("div.top div.breadcrumb span[itemprop=breadcrumb] span[itemscope] span[itemprop=title]");
      for (int i = 1; i < categoryElementsCollection.size(); i++) {
         String categoryText = categoryElementsCollection.get(i).text().trim();
         if (!categoryText.isEmpty()) {
            categories.add(categoryText);
         }
      }
      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element descriptionElement = doc.select("div.informations-wrapper #informations").first();
      if (descriptionElement != null) {
         descriptionElement.select(".information-toolbar, .legend, .tab-toolbar").remove();
         description.append(descriptionElement.html().trim());
      }

      return description.toString();
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select("#btn-notify.disponibility-di.hide").first() != null;
   }

   private Prices crawlPrices(Document doc, Float price) {
      Prices prices = new Prices();

      Element priceFrom = doc.select(".list-price .list").first();
      if (priceFrom != null) {
         prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Element bankTicket = doc.selectFirst(".price .savings b");
      if (bankTicket != null) {
         prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bankTicket.ownText()));
      } else {
         prices.setBankTicketPrice(price);
      }

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);


      Document paymentPage = retrievePaymentWebpage(doc);

      if (paymentPage != null) {

         // this code block was created because in some cases the first table contains bank payments methods
         // so because of it, we need sum one index to get the others payments informations
         int index = 0;

         Element specialPage = paymentPage.selectFirst("table tr:first-child > td:first-child >  strong");
         if (specialPage != null) {
            index++;
         }

         // get the other payment options
         Elements tableElementsCollection = paymentPage.select("table"); // each table line

         if (tableElementsCollection.size() > 1) {
            Elements trElements = tableElementsCollection.get(3 + index).select("tr");
            if (trElements.size() > 2) {
               Elements cardPriceElements = trElements.last().select("div");

               for (Element e : cardPriceElements) {
                  String text = e.ownText().toLowerCase();
                  if (text.contains("vista")) {
                     installmentPriceMap.put(1, MathUtils.parseFloatWithComma(text));
                  } else if (text.contains("x") && text.contains("(")) {
                     int x = text.indexOf('x');
                     int y = text.indexOf('(');

                     String installment = text.substring(0, x).replaceAll("[^0-9]", "").trim();
                     if (!installment.isEmpty()) {
                        installmentPriceMap.put(Integer.parseInt(installment), MathUtils.parseFloatWithComma(text.substring(x, y)));
                     }
                  }
               }
            }
         }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);

      return prices;
   }

   private Document retrievePaymentWebpage(Document document) {
      Element paymenstPopup = document.select("#payments").first();
      if (paymenstPopup != null) {
         String href = paymenstPopup.attr("href");
         try {
            URL rawUrl = new URL(href);

            String[] query = rawUrl.getQuery().split(Pattern.quote("="));
            String name = query[0];
            String value = null;
            if (query.length > 1) {
               value = query[1];
            }

            URI uri = new URIBuilder().setScheme("https").setHost(rawUrl.getHost()).setPath(rawUrl.getPath()).setParameter(name, value).build();

            Request request = RequestBuilder.create().setUrl(uri.toString()).setCookies(cookies).build();
            return Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         } catch (MalformedURLException | URISyntaxException malformedUrlException) {
            Logging.printLogDebug(logger, session, "Not a valid payment popup url");
         }
      }

      return null;
   }

   private RatingsReviews scraptRatingAndReviews(Document document, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Pair<Integer, Double> rating = getRating(document);
      Integer totalNumOfEvaluations = rating.getFirst();
      Double avgRating = rating.getSecond();

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);

      return ratingReviews;
   }

   /**
    * Avg is calculated
    */
   private Pair<Integer, Double> getRating(Document doc) {
      Double avgRating = 0D;
      int ratingNumber = 0;

      Element ratingCount = doc.selectFirst(".product-rating [itemprop=\"ratingCount\"]");
      if (ratingCount != null) {
         String text = ratingCount.attr("content").replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            ratingNumber = Integer.parseInt(text);
         }
      }

      Element ratingAverage = doc.selectFirst(".product-rating .rating .average");
      if (ratingAverage != null) {
         avgRating = MathUtils.parseDoubleWithComma(ratingAverage.ownText());
      }

      return new Pair<>(ratingNumber, avgRating);
   }
}
