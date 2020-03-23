package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * Date: 07/12/2016
 * 
 * 1) Only one sku per page. 2) Is required put parameter sc with value 15 to access product url 3)
 * There is no informations of installments in this market 4) In time this crawler was made, it was
 * not foundo unnavailable products
 * 
 * @author Gabriel Dornelas
 *
 */
public class ArgentinaWalmartCrawler extends Crawler {

   private final String HOME_PAGE = "http://www.walmart.com.ar/";

   private static final String SELLER_FULL_NAME = "Walmart";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ArgentinaWalmartCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   /**
    * To acess product page is required put ?sc=15 in url
    */
   @Override
   public String handleURLBeforeFetch(String curURL) {

      if (curURL.endsWith("/p")) {

         try {
            String url = curURL;
            List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
            List<NameValuePair> paramsNew = new ArrayList<>();

            for (NameValuePair param : paramsOriginal) {
               if (!"sc".equals(param.getName())) {
                  paramsNew.add(param);
               }
            }

            paramsNew.add(new BasicNameValuePair("sc", "15"));
            URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

            builder.clearParameters();
            builder.setParameters(paramsNew);

            curURL = builder.build().toString();

            return curURL;

         } catch (URISyntaxException e) {
            return curURL;
         }
      }

      return curURL;

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(doc);
         CategoryCollection categories = crawlCategories(doc);
         String description = crawlDescription(doc);
         Integer stock = null;
         RatingsReviews ratingReviews = scrapRatingReviews(doc);
         JSONArray arraySkus = crawlSkuJsonArray(doc);
         JSONArray eanArray = CrawlerUtils.scrapEanFromVTEX(doc);

         for (int i = 0; i < arraySkus.length(); i++) {
            JSONObject jsonSku = arraySkus.getJSONObject(i);

            String internalId = crawlInternalId(jsonSku);
            String primaryImage = crawlPrimaryImage(doc);
            String name = crawlName(doc, jsonSku);
            String secondaryImages = crawlSecondaryImages(doc);
            String ean = i < eanArray.length() ? eanArray.getString(i) : null;
            List<String> eans = new ArrayList<>();
            eans.add(ean);

            boolean availableToBuy = jsonSku.optBoolean("available", false);
            Offers offers = availableToBuy ? scrapOffer(doc, internalId) : new Offers();

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
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document document) {
      return document.select(".productName").first() != null;
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = scrapSales(internalId, doc);
      Pricing pricing = scrapPricing(internalId, doc);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private List<String> scrapSales(String internalId, Document doc) {
      List<String> sales = new ArrayList<>();

      String salesUrl = scrapSalesApiUrl(doc);
      Request req = RequestBuilder.create()
            .setUrl(CommonMethods.sanitizeUrl(salesUrl))
            .setCookies(cookies)
            .build();

      String response = this.dataFetcher.get(session, req).getBody();
      JSONArray bdwJsonResult = JSONUtils.stringToJsonArray(CrawlerUtils.extractSpecificStringFromScript(response, "var bdwJsonResult=", false, ";", false));

      for (Object o : bdwJsonResult) {
         JSONObject saleJson = o instanceof JSONObject ? (JSONObject) o : new JSONObject();

         if (saleJson.has("Sku") && !saleJson.isNull("Sku")) {
            String skuId = saleJson.get("Sku").toString();

            if (skuId.equalsIgnoreCase(internalId) && saleJson.has("CucardaOferta") && !saleJson.isNull("CucardaOferta")) {
               sales.add(saleJson.get("CucardaOferta").toString());
            }
         }
      }

      return sales;
   }

   private String scrapSalesApiUrl(Document doc) {

      String catalogoNumber = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-user-review-category-id", "value");
      String url = "https://scustom.walmart.com.ar/tracking/track?HASH=walmartar_produccion_scxel&wordsfound=,&buyer={CLIENT.BUYER}&name=&lastname=&gender=&branchOffice=15&country=&state=&city=&email=&u=productoswalmart.braindw.com/catalogo/"
            + catalogoNumber + "/15";

      return url;
   }


   private Pricing scrapPricing(String internalId, Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".plugin-preco .skuListPrice", null, false, ',', session);
      Double priceFromcheck = priceFrom > 0.0 ? priceFrom : null; // this was necessary becouse the website have some products who doesn't have priceFrom and field
                                                                  // price_from cannot have this value -> 0.0
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".plugin-preco .skuBestPrice", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(internalId, spotlightPrice);

      return PricingBuilder.create()
            .setPriceFrom(priceFromcheck)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }

   private CreditCards scrapCreditCards(String internalId, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      String pricesApi = "https://www.walmart.com.ar/productotherpaymentsystems/" + internalId;
      Request request = RequestBuilder.create().setUrl(pricesApi).setCookies(cookies).build();
      Document doc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

      Elements cardsElements = doc.select("#ddlCartao option");

      if (!cardsElements.isEmpty()) {
         for (Element e : cardsElements) {
            String text = e.text().toLowerCase();
            String idCard = e.val();
            String card = null;
            Installments installments = scrapInstallments(doc, idCard, spotlightPrice);

            if (text.contains("visa")) {
               card = Card.VISA.toString();
            } else if (text.contains("mastercard")) {
               card = Card.MASTERCARD.toString();
            } else if (text.contains("cabal")) {
               card = Card.CABAL.toString();
            } else if (text.contains("nativa")) {
               card = Card.NATIVA.toString();
            } else if (text.contains("naranja")) {
               card = Card.NARANJA.toString();
            } else if (text.contains("american express")) {
               card = Card.AMEX.toString();
            }

            if (card != null) {
               creditCards.add(CreditCardBuilder.create()
                     .setBrand(card)
                     .setInstallments(installments)
                     .setIsShopCard(false)
                     .build());
            }
         }
      } else {
         Installments installments = new Installments();
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());

         for (String card : cards) {
            creditCards.add(CreditCardBuilder.create()
                  .setBrand(card)
                  .setInstallments(installments)
                  .setIsShopCard(false)
                  .build());
         }
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc, String idCard, Double spotlightPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsCard = doc.select(".tbl-payment-system#tbl" + idCard + " tr");
      for (Element i : installmentsCard) {
         Element installmentElement = i.select("td.parcelas").first();

         if (installmentElement != null) {
            String textInstallment = installmentElement.text().toLowerCase();
            Integer installment = null;

            if (textInstallment.contains("vista")) {
               installment = 1;
            } else {
               String text = textInstallment.replaceAll("[^0-9]", "").trim();

               if (!text.isEmpty()) {
                  installment = Integer.parseInt(text);
               }
            }

            Element valueElement = i.select("td:not(.parcelas)").first();

            if (valueElement != null && installment != null) {
               Double value = MathUtils.parseDoubleWithComma(valueElement.text());

               installments.add(InstallmentBuilder.create()
                     .setInstallmentNumber(installment)
                     .setInstallmentPrice(value)
                     .build());
            }
         }
      }

      if (installments.getInstallment(1) == null) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      return installments;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("sku")) {
         internalId = Integer.toString(json.getInt("sku")).trim();
      }

      return internalId;
   }

   private String crawlInternalPid(Document document) {
      String internalPid = null;
      Element internalPidElement = document.select("#___rc-p-id").first();

      if (internalPidElement != null) {
         internalPid = internalPidElement.attr("value").trim();
      }

      return internalPid;
   }

   private String crawlName(Document document, JSONObject jsonSku) {
      String name = null;
      Element nameElement = document.select(".productName").first();

      String nameVariation = jsonSku.getString("skuname");

      if (nameElement != null) {
         name = nameElement.text().trim();

         if (name.length() > nameVariation.length()) {
            name += " " + nameVariation;
         } else {
            name = nameVariation;
         }
      }

      return name;
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;

      Element image = doc.select("#botaoZoom").first();

      if (image != null) {
         primaryImage = image.attr("zoom").trim();

         if (primaryImage == null || primaryImage.isEmpty()) {
            primaryImage = image.attr("rel").trim();
         }
      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document doc) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imageThumbs = doc.select("#botaoZoom");

      for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image is the primary image
         String url = imageThumbs.get(i).attr("zoom");

         if (url == null || url.isEmpty()) {
            url = imageThumbs.get(i).attr("rel");
         }

         if (url != null && !url.isEmpty()) {
            secondaryImagesArray.put(url);
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".bread-crumb > ul li a");

      for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first is the market name
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      String description = "";

      Element descElement = document.select(".prod-desc .productDescription").first();
      if (descElement != null) {
         description = description + descElement.html();
      }

      return description;
   }

   private JSONArray crawlSkuJsonArray(Document document) {
      Elements scriptTags = document.getElementsByTag("script");
      JSONObject skuJson = null;
      JSONArray skuJsonArray = null;

      for (Element tag : scriptTags) {
         for (DataNode node : tag.dataNodes()) {
            if (tag.html().trim().startsWith("var skuJson_0 = ")) {
               skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
                     + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1].split(Pattern.quote("}]};"))[0]);
            }
         }
      }

      if (skuJson != null && skuJson.has("skus")) {
         skuJsonArray = skuJson.getJSONArray("skus");
      }

      if (skuJsonArray == null) {
         skuJsonArray = new JSONArray();
      }

      return skuJsonArray;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      if (isProductPage(doc)) {
         ratingReviews.setDate(session.getDate());

         JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

         if (skuJson.has("productId")) {
            String internalPid = Integer.toString(skuJson.getInt("productId"));
            Document docRating = crawlApiRatings(session.getOriginalURL(), internalPid);
            Integer totalNumOfEvaluations = getTotalNumOfRatings(docRating);
            Double avgRating = getTotalAvgRating(docRating, totalNumOfEvaluations);
            AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(docRating, doc);

            ratingReviews.setTotalRating(totalNumOfEvaluations);
            ratingReviews.setAverageOverallRating(avgRating);
            ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
            ratingReviews.setAdvancedRatingReview(advancedRatingReview);
         }
      }
      return ratingReviews;
   }

   private Document crawlApiRatings(String url, String internalPid) {

      String[] tokens = url.split("/");
      String productLinkId = tokens[tokens.length - 2];
      String payload = "productId=" + internalPid + "&productLinkId=" + productLinkId;
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");
      headers.put("Accept-Language", "pt-BR,pt;q=0.8,en-US;q=0.6,en;q=0.4");
      Request request =
            RequestBuilder.create().setUrl("https://www.walmart.com.ar/userreview").setCookies(cookies).setHeaders(headers).setPayload(payload).build();
      return Jsoup.parse(this.dataFetcher.post(session, request).getBody());
   }

   private Double getTotalAvgRating(Document docRating, Integer totalRating) {

      Double avgRating = 0.0;
      Elements rating = docRating.select("ul.rating");

      if (totalRating != null) {
         Double total = 0.0;

         for (Element e : rating) {
            Element star = e.select("strong").first();
            Element totalStar = e.select("li >  span:last-child").first();
            if (totalStar != null) {
               String votes = totalStar.text().replaceAll("[^0-9]", "").trim();
               if (!votes.isEmpty()) {
                  Integer totalVotes = Integer.parseInt(votes);
                  if (star != null) {
                     if (star.hasClass("avaliacao50")) {
                        total += totalVotes * 5;
                     } else if (star.hasClass("avaliacao40")) {
                        total += totalVotes * 4;
                     } else if (star.hasClass("avaliacao30")) {
                        total += totalVotes * 3;
                     } else if (star.hasClass("avaliacao20")) {
                        total += totalVotes * 2;
                     } else if (star.hasClass("avaliacao10")) {
                        total += totalVotes * 1;
                     }
                  }
               }
            }
         }

         avgRating = MathUtils.normalizeTwoDecimalPlaces(total / totalRating);
      }

      return avgRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document docRating, Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = docRating.select(".rating li");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst("Strong"); // <strong class="rating-demonstrativo avaliacao50"></strong>

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("class"); // rating-demonstrativo avaliacao50
            String sN = stringStarNumber.replaceAll("[^0-9]", "").trim(); // String 50
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0; // Integer 50

            Element elementVoteNumber = review.selectFirst("li > span:last-child"); // <span> 1 Voto</span>

            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim(); // 1 our ""
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0; // 1 our 0

               switch (numberOfStars) {
                  case 50:
                     star5 = numberOfVotes;
                     break;
                  case 44:
                     star4 = numberOfVotes;
                     break;
                  case 30:
                     star3 = numberOfVotes;
                     break;
                  case 20:
                     star2 = numberOfVotes;
                     break;
                  case 10:
                     star1 = numberOfVotes;
                     break;
                  default:
                     break;
               }
            }
         }
      }

      return new AdvancedRatingReview.Builder()
            .totalStar1(star1)
            .totalStar2(star2)
            .totalStar3(star3)
            .totalStar4(star4)
            .totalStar5(star5)
            .build();
   }

   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = null;
      Element totalRatingElement = docRating.selectFirst(".media em > span");

      if (totalRatingElement != null) {
         String totalText = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!totalText.isEmpty()) {
            totalRating = Integer.parseInt(totalText);
         }
      }

      return totalRating;
   }



}
