package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

/**
 * Date: 15/12/20
 *
 * @author Matheus Bussolotti
 */

public abstract class Vipcommerce extends Crawler {


   private final String HOME_PAGE = getHomePage();
   private final String SELLER_FULL_NAME = getSellerFullName();
   private final String DOMAIN = getDomain();
   private final String LOCATE_CODE = getLocateCode();
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public Vipcommerce(Session session) {
      super(session);
   }

   protected abstract String getHomePage();
   protected abstract String getSellerFullName();
   protected abstract String getDomain();
   protected String getLocateCode(){
      return "1";
   }



   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   public String getToken() {
      String token = null;

      String url = "https://api."+ DOMAIN +"/v1/auth/loja/login";

      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/json");
      headers.put("origin", "https://www."+ DOMAIN +"");

      JSONObject payload = new JSONObject()
         .put("domain", DOMAIN)
         .put("username", "loja")
         .put("key", "df072f85df9bf7dd71b6811c34bdbaa4f219d98775b56cff9dfa5f8ca1bf8469");

      Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).setPayload(payload.toString()).setCookies(cookies).build();
      JSONObject tokenJson = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      token = JSONUtils.getStringValue(tokenJson, "data");

      return token;
   }

   public JSONObject crawlApi(String token) {

      // there is no info about internalId on product page HTML so capture this info from URL
      Integer x = session.getOriginalURL().indexOf("detalhe/");
      String[] vetURL = x != null ? session.getOriginalURL().substring(x).split("/") : new String[0];
      String internalIdFromURL = vetURL.length >2 ? vetURL[1] :null;
      String url = "https://api."+ DOMAIN +"/v1/loja/produtos/" + internalIdFromURL + "/filial/1/centro_distribuicao/"+LOCATE_CODE+"/detalhes";

      Map<String, String> headers = new HashMap<>();
      headers.put("authorization", token);

      Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
      JSONObject jsonObject = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      JSONObject produtoInfo = JSONUtils.getJSONValue(jsonObject, "data");

      return produtoInfo;
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

         String token = getToken();

         JSONObject jsonData = crawlApi(token);
         JSONObject productInfo = JSONUtils.getJSONValue(jsonData, "produto");
         JSONObject OffersInfo = JSONUtils.getJSONValue(productInfo, "oferta");

         String internalId = productInfo.optString("produto_id");
         String internalPid = JSONUtils.getStringValue(productInfo, "id");
         String name = JSONUtils.getStringValue(productInfo, "descricao");
         String primaryImage = CrawlerUtils.completeUrl(JSONUtils.getStringValue(productInfo, "imagem"), " https://", "s3.amazonaws.com/produtos.vipcommerce.com.br/250x250");
         boolean availeble = productInfo.optBoolean("disponivel");
         Integer stock = JSONUtils.getIntegerValueFromJSON(productInfo, "quantidade_maxima", null);
         Offers offers = availeble ? scrapOffers(OffersInfo, productInfo) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setStock(stock)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page.");
      }
      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".loader__container") != null;
   }

   private Offers scrapOffers(JSONObject OffersInfo, JSONObject productInfo) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(OffersInfo, productInfo);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
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

   private Pricing scrapPricing(JSONObject OffersInfo, JSONObject productInfo) throws MalformedPricingException {
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (!OffersInfo.isEmpty()) {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(OffersInfo, "preco_oferta", true);
         priceFrom = JSONUtils.getDoubleValueFromJSON(OffersInfo, "preco_antigo", true);
      } else {
         spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "preco", true);

      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

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

}
