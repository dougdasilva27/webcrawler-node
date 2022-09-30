package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrazilAtalaiaRacoesCrawler extends Crawler {
   public BrazilAtalaiaRacoesCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String urlCode = this.session.getOriginalURL().replaceAll("https://loja.atalaiaracoes.com.br/store/produto/", "");
      String url = "https://superon.lifeapps.com.br/api/v2/app/777d0060-40c1-11ed-a1e1-750c7c041041cc64548c0cbad6cf58d4d3bbd433142b/fornecedor/c9ecd9f3-b54f-4ab9-b856-62df3df6960c/produto/" + urlCode + "?disableSimilares=false&canalVenda=WEB&tipoEntrega=DELIVERY";
      Map<String, String> headers = new HashMap<>();
      headers.put("authority", "superon.lifeapps.com.br");
      headers.put("x-idfornecedor", "[\"c9ecd9f3-b54f-4ab9-b856-62df3df6960c\"]");
      headers.put("origin", "https://loja.atalaiaracoes.com.br");
      headers.put("referer", "https://loja.atalaiaracoes.com.br/");
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .build();
      return this.dataFetcher.get(session, request);
   }

   public List<Product> extractInformation(JSONObject jsonObject) throws Exception {

      List<Product> products = new ArrayList<>();
      if (jsonObject != null) {
         String internalId = jsonObject.optString("idproduto");
         String name = jsonObject.optString("nome");
         String primaryImage = getPrimaryImage(jsonObject);
         String description = jsonObject.optString("descricaolonga");
         String stock = jsonObject.optString("maximo_disponivel");
         Offers offers = checkAvailability(stock) ? scrapOffers(jsonObject) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            // o site não possui imagens secundarias no dia da criação deste crawler
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }
   private String getPrimaryImage(JSONObject product) {
      String id = product.optString("id");
      return id != null && !id.isEmpty() ? "https://content.lifeapps.com.br/superon/imagens/" + id + ".jpg" : null;
   }
   
   private boolean checkAvailability(String stock) {
      if (stock != null && !stock.isEmpty()) {
         stock = stock.replaceAll("\\.", "");
         Integer stockInt = Integer.parseInt(stock);
         if (stockInt > 0) {
            return true;
         }

      }
      return false;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(session.getOptions().optString("sellerName"))
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(data, "preco_original", false);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(data, "preco_sem_politica_varejo", false);
      if (priceFrom != null && spotlightPrice != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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
