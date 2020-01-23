package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import models.AdvancedRatingReview;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;

/**
 * Date: 18/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilCissamagazineCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.cissamagazine.com.br/";
   private static final String SELLER_NAME_LOWER = "cissa magazine";

   public BrasilCissamagazineCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
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
         String name = crawlName(doc);
         Map<String, Prices> marketplaceMap = crawlMarketplaceMap(doc);
         Prices prices = CrawlerUtils.getPrices(marketplaceMap, Arrays.asList(SELLER_NAME_LOWER));
         Float price = CrawlerUtils.extractPriceFromPrices(prices, Card.MASTERCARD);
         boolean available = marketplaceMap.containsKey(SELLER_NAME_LOWER);
         Marketplace marketplace = CrawlerUtils.assembleMarketplaceFromMap(marketplaceMap, Arrays.asList(SELLER_NAME_LOWER), Card.MASTERCARD, session);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb div:not(:first-child) a span");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".img-thumb a", Arrays.asList("data-img-full", "data-img-max", "href"),
               "https:", "29028l.ha.azioncdn.net");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".img-thumb a", Arrays.asList("data-img-full", "data-img-max", "href"),
               "https:", "29028l.ha.azioncdn.net", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc,
               Arrays.asList(".garantia-produto", ".caracteristicas-do-produto", ".band-descricao", ".produto-acompanha-caixa"));
         RatingsReviews ratingReviews = crawRating(doc);

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setName(name)
               .setPrice(price)
               .setPrices(prices)
               .setAvailable(available)
               .setCategory1(categories
                     .getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setMarketplace(marketplace)
               .setRatingReviews(ratingReviews)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select("#prod-codigo").isEmpty();
   }


   private Map<String, Prices> crawlMarketplaceMap(Document doc) {
      Map<String, Prices> marketplaceMap = new HashMap<>();

      Element principalSeller = doc.selectFirst(".vendido-por-loja strong");
      Element principalSellerPrices = doc.selectFirst(".type-payment-condiction .form-pag-link.prevent-default");

      if (principalSeller != null && principalSellerPrices != null) {
         String sellerName = principalSeller.ownText().toLowerCase().trim();

         Float bankPrice = MathUtils.parseFloatWithDots(principalSellerPrices.attr("data-valor-boleto"));
         Float price = MathUtils.parseFloatWithDots(principalSellerPrices.attr("data-valor"));
         String dataPgto = principalSellerPrices.attr("data-conf-pagto");

         marketplaceMap.put(sellerName, crawlPrices(price, bankPrice, dataPgto));
      }

      Elements sellers = doc.select(".lista-lojas-outras-ofertas .informacoes-loja-vendendo");
      for (Element e : sellers) {
         Element nameSeller = e.selectFirst(".vendido-entregue-por .loja-vendendo strong");
         Element priceSeller = e.selectFirst(".form-pag-link.prevent-default");

         if (nameSeller != null && priceSeller != null) {
            String sellerName = nameSeller.ownText().toLowerCase().trim();

            Float bankPrice = MathUtils.parseFloatWithDots(priceSeller.attr("data-valor-boleto"));
            Float price = MathUtils.parseFloatWithDots(priceSeller.attr("data-valor"));
            String dataPgto = priceSeller.attr("data-conf-pagto");

            marketplaceMap.put(sellerName, crawlPrices(price, bankPrice, dataPgto));
         }
      }

      return marketplaceMap;
   }

   private String crawlName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1[itemprop=name]", true);
      String complement = CrawlerUtils.scrapStringSimpleInfo(doc, ".complemento-titulo", true);

      if (complement != null) {
         name += " " + complement;
      }

      return name;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element infoElement = doc.selectFirst("#prod-codigo");
      if (infoElement != null) {
         internalId = infoElement.val();
      }

      return internalId;
   }

   /**
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, Float bankPrice, String dataPgto) {
      Prices prices = new Prices();

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(bankPrice);

      String url = session.getOriginalURL() + "?operation=calculaFormasPagamento";
      String payload = new StringBuilder().append("valor=").append(price).append("&valorBoleto=").append(bankPrice).append("&confPagto=")
            .append(dataPgto).toString();
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).setPayload(payload).build();
      JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      if (json.has("formasPagamento")) {
         Document doc = Jsoup.parse(json.get("formasPagamento").toString());

         Elements parcels = doc.select(".price-formas-list tr td:first-child");
         for (Element e : parcels) {
            Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(null, e, true, "x");

            if (!installment.isAnyValueNull()) {
               installmentPriceMap.put(installment.getFirst(), installment.getSecond());
            }
         }
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);

      return prices;
   }

   RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      String internalId = crawlInternalId(doc);

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);
      AdvancedRatingReview advancedRating = scrapAdvancedRatingReview(doc);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRating);


      return ratingReviews;

   }

   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;

      Element avg = doc.selectFirst(".avaliacao-geral-topo span [itemprop=ratingValue]");
      if (avg != null) {
         avgRating = MathUtils.parseDoubleWithDot(avg.attr("content"));

         if (avgRating == null) {
            avgRating = 0d;
         }
      }

      return avgRating;
   }



   private Integer getTotalNumOfRatings(Document doc) {
      Integer rating = 0;

      Element reviewCount = doc.selectFirst(".avaliacao-geral-topo [itemprop=reviewCount]");
      if (reviewCount != null) {
         String text = reviewCount.attr("content").replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            rating = Integer.parseInt(text);
         }
      }

      return rating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".avaliacao-geral .content-nota-recomendacoes");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst("div .star-back"); // <span class="star-back big" title="5 de 5">
         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("title"); // 5 de 5
            String sN = stringStarNumber.replaceAll("[^0-9]", "").trim(); // String 55
            Integer numberOfStars = !sN.isEmpty() ? Integer.parseInt(sN) : 0; // Integer 55

            Element elementVoteNumber = review.selectFirst(".count"); // <span class="count">142</span>
            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim(); // 142 our ""
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0; // 142 our 0

               switch (numberOfStars) {
                  case 55:
                     star5 = numberOfVotes;
                     break;
                  case 45:
                     star4 = numberOfVotes;
                     break;
                  case 35:
                     star3 = numberOfVotes;
                     break;
                  case 25:
                     star2 = numberOfVotes;
                     break;
                  case 15:
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

}
