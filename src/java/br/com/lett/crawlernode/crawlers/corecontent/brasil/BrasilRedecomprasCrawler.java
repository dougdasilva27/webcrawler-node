package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class BrasilRedecomprasCrawler extends Crawler {

   private static final String HOME_PAGE = "https://delivery.redecompras.com/";
   private static final String SELLER_FULL_NAME = "Rede Compras";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilRedecomprasCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   private String crawIdProduto(Document doc) {
      String idProduto = null;

      Elements scripts = doc.select("body script[type=\"text/javascript\"]");

      for (Element e : scripts) {
         String script = e.html();

         if (script.contains("var idProduto")) {
            Integer indexOf = script.indexOf("=");
            idProduto = indexOf != null ? script.substring(indexOf).split(";")[0].replace("= ", "") : null;
         }

      }

      return idProduto;
   }

   private JSONObject crawJsonFromApi(String idProduto) {
      JSONObject json = new JSONObject();

      String url = "https://delivery.redecompras.com/api/produto?id=" + idProduto;

      Request request = RequestBuilder.create().setUrl(url).build();
      json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());


      return json;

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawIdProduto(doc);
         String internalPid = internalId;

         JSONObject jsonInfo = crawJsonFromApi(internalId);
         JSONArray productsArray = JSONUtils.getJSONArrayValue(jsonInfo, "Produtos");

         for (Object json : productsArray) {

            JSONObject produto = (JSONObject) json;


            String name = produto.optString("str_nom_produto");
            boolean available = produto.optString("str_disponibilidade").equals("Em Estoque");

            String primaryImage = produto.getString("str_img_path").replace("str.blob.core.windows.net//", "-img.azureedge.net/") + "-g.jpg";
            /*
             * When this crawler was made the only way to extract the main image was to remove the url from a
             * Json and change the url.
             * 
             * Example:
             * 
             * jsonUrl -
             * "https://redecomprasstr.blob.core.windows.net//product/6707-leite-condensado-piracanjuba-270g-tp
             * 
             * modified url -
             * https://redecompras-img.azureedge.net/product/6707-leite-condensado-piracanjuba-270g-tp-g.jpg
             */

            Offers offers = available ? scrapOffer(produto) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(produto.optString("str_categoria"))
                  .setCategory2(produto.optString("str_subcategoria"))
                  .setCategory3(produto.optString("str_tricategoria"))
                  .setPrimaryImage(primaryImage)
                  .setOffers(offers)
                  .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".pageProdutoDetalhe") != null;
   }

   private Offers scrapOffer(JSONObject jsonInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonInfo);
      List<String> sales = scrapSales(jsonInfo);

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

   private List<String> scrapSales(JSONObject jsonInfo) {
      List<String> sales = new ArrayList<>();

      String firstSales = jsonInfo.optString("mny_perc_desconto", null);

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add("-" + firstSales + "%");
      }

      return sales;
   }

   private Pricing scrapPricing(JSONObject jsonInfo) throws MalformedPricingException {
      Double priceFrom = jsonInfo.optDouble("mny_vlr_produto_tabela_preco");
      Double spotlightPrice = jsonInfo.optDouble("mny_vlr_parcela");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();


   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }


}
