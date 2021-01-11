package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

/**
 * Date: 08/06/2018
 *
 * @author Gabriel Dornelas
 */
public class BrasilDrogarianisseiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.farmaciasnissei.com.br/";

   public BrasilDrogarianisseiCrawler(Session session) {

      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".row .mt-3 .small", false).split("produto: ")[1];
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".mt-3 h4", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".small a", true);
         String primaryImage = fixUrlImage(doc, internalId);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(" .d-flex.mt-4 .text-border-bottom-amarelo", "div .row div .mt-1"));

         JSONObject json = accesAPIOffers(internalId);
         Offers offers = scrapOffers(json);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".row[data-target=\"produto_view\"]").isEmpty();
   }

   private String fixUrlImage(Document doc, String internalId) {
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".swiper-slide img", Arrays.asList("src"), "https:", "www.farmaciasnissei.com.br");

      if (primaryImage.contains("caixa-nissei")) {
         return primaryImage.replace("caixa-nissei", internalId);

      }
      return primaryImage;
   }


   private Offers scrapOffers(JSONObject jsonInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonInfo);
      List<String> sales = scrapSales(jsonInfo);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Drogaria Nissei")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(JSONObject jsonInfo) {
      List<String> sales = new ArrayList<>();

      String firstSales = jsonInfo.optString("per_desc");

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject jsonInfo) throws MalformedPricingException {
      Double priceFrom = !scrapSales(jsonInfo).isEmpty() ? jsonInfo.optDouble("valor_ini") : null;
      Double spotlightPrice = jsonInfo.optDouble("valor_fim");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


   private JSONObject accesAPIOffers(String internalId) {

      String url = "https://farmaciasnissei.com.br/pegar/preco";

      Map<String, String> headers = new HashMap<>();
      headers.put("cookie", "csrftoken=4ZfApcfcNgqlhsExAB2h5xkCfe4nsQH0Q0DHja4oHF9iuZxeeDPpf5yb72PMhYkh; _fbp=fb.2.1610386362281.901362755; _ga=GA1.3.1690412403.1610386362; _gid=GA1.3.145967739.1610386362; _gat_gtag_UA_81392705_6=1");
      headers.put("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
      headers.put("referer", session.getOriginalURL());


      String payload = "csrfmiddlewaretoken=SOat3XiN6pCXPjFVDa9efuZv6CaahHsbEPyAXV7Z0OlU2QyChcWmp2d4YqVz6P5s&produtos_ids%5B%5D=" + internalId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setCookies(cookies)
         .build();

      String content = this.dataFetcher
         .post(session, request)
         .getBody();


      JSONObject response = CrawlerUtils.stringToJson(content);

      JSONArray precos = response != null ? response.optJSONArray("precos") : null;

      if (precos != null && !precos.isEmpty()) {
         for (Object objectJson : precos) {

            JSONObject productInfo = (JSONObject) objectJson;

            if (productInfo.optString("produto_id").equals(internalId)) {
               return productInfo;
            }
         }
      }
      return new JSONObject();
   }


}
