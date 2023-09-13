package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.HttpClientFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.LocaleException;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public abstract class LifeappsCrawler extends Crawler {

   private static final String API_URL = "https://superon.lifeapps.com.br/api/v2/app/";
   private static final String HOST_API_IMAGES_URL = "https://content.lifeapps.com.br/superon/imagens";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   protected LifeappsCrawler(Session session) {
      super(session);
      this.config.setParser(Parser.JSON);
   }

   protected abstract String getHomePage();

   protected abstract String getCompanyId();

   protected abstract String getApiHash();

   protected abstract String getFormaDePagamento();

   protected abstract String getSellerName();

   @Override
   protected Response fetchResponse() {
      // url must be in this format:
      // https://jcdistribuicao.superon.app/commerce/6f0ae38d-50cd-4873-89a5-6861467b5f52/produto/AGUA-MIN-SAO-LOURENCO-300ML-PET-S-GAS-3xlPvs5V/
      // api exemple:
      // https://superon.lifeapps.com.br/api/v2/app/dccca000-b2ea-11e9-a27c-b76c91df9dd6cc64548c0cbad6cf58d4d3bbd433142b/fornecedor/6f0ae38d-50cd-4873-89a5-6861467b5f52/produto/DROPS-HALLS-21X1-28GR-MENTA-PRATA-jxmXNL6F?idformapagamento=2001546a-9851-4393-bb68-7c04e932fa4c&disableSimilares=false&canalVenda=WEB

      String app_id = UUID.randomUUID().toString();

      String originalUrl = session.getOriginalURL();
      String apiUrl = null;

      if (originalUrl.contains("produto/")) {
         String[] partUrl = originalUrl.split("produto/");
         String slugUrl = partUrl[1].replace("/", "");

         apiUrl = API_URL
            .concat(app_id)
            .concat(getApiHash())
            .concat("/fornecedor/")
            .concat(getCompanyId())
            .concat("/produto/")
            .concat(slugUrl)
            .concat("?idformapagamento=")
            .concat(getFormaDePagamento())
            .concat("&disableSimilares=false&canalVenda=WEB");
      }

      Request request = Request.RequestBuilder.create()
         .setUrl(apiUrl)
         .setCookies(cookies)
         .setProxyservice(List.of(ProxyCollection.BUY_HAPROXY, ProxyCollection.LUMINATI_RESIDENTIAL_BR_HAPROXY))
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new HttpClientFetcher(), new JsoupDataFetcher()), session);
   }

   @Override
   public List<Product> extractInformation(JSONObject jsonSku) throws Exception {
      if (!session.getOriginalURL().contains(getHomePage())) {
         throw new LocaleException();
      }

      super.extractInformation(jsonSku);
      List<Product> products = new ArrayList<>();
      String productUrl = session.getOriginalURL();

      if (isProductPage(jsonSku)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(jsonSku);
         String internalPid = crawlInternalPid(jsonSku);
         String description = crawlDescription(jsonSku);
         String primaryImage = crawlPrimaryImage(jsonSku);
         String name = crawlName(jsonSku);
         List<String> eans = scrapEan(internalId);
         boolean isAvailable = JSONUtils.getValueRecursive(jsonSku, "meta_info.sem_estoque", Boolean.class);
         Offers offers = !isAvailable ? scrapOffers(jsonSku) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setEans(eans)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(JSONObject jsonSku) {
      return jsonSku.length() > 0;
   }

   private List<String> scrapEan(String internalId) {
      List<String> eans = new ArrayList<>();

      if (internalId != null && internalId.contains("-")) {
         String[] eanArray = internalId.split("-");

         if (eanArray.length > 1) {
            eans = Collections.singletonList(eanArray[1]);
         }
      }

      return eans;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("id_produto_erp") && !json.isNull("id_produto_erp")) {
         String idProdutoErp = json.optString("id_produto_erp");

         if (idProdutoErp.contains("|")) {
            String[] idProdutoErpArray = idProdutoErp.split("\\|");
            internalId = idProdutoErpArray[1].concat("-").concat(idProdutoErpArray[0]);
         }
      }

      return internalId;
   }


   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("nome") && json.get("nome") instanceof String) {
         name = json.optString("nome");
      }

      return name;
   }

   private JSONObject fetchJsonImages(String hostApiImagesUrl, String internalPid) {
      String url = hostApiImagesUrl.concat("/").concat(getCompanyId()).concat("/").concat(internalPid).concat("/").concat("all-images");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("idcadastroextraproduto") && !json.isNull("idcadastroextraproduto")) {
         internalPid = json.optString("idcadastroextraproduto");
      }

      return internalPid;
   }

   private String crawlPrimaryImage(JSONObject product) {
      String imageId = JSONUtils.getValueRecursive(product, "idcadastroextraproduto", String.class);
      if (imageId != null) {
         return "https://s3-sa-east-1.amazonaws.com/prod-superon-public-media/shared/product-image/" + imageId + ".jpg";
      }
      return null;
   }

   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();

      if (json.has("descricaolonga") && !json.isNull("descricaolonga")) {
         description.append(json.optString("descricaolonga"));
      }

      return description.toString().trim();
   }

   private Offers scrapOffers(JSONObject product) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerName())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   protected Pricing scrapPricing(JSONObject product) throws MalformedPricingException {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(product, "preco_sem_politica_varejo", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(product, "preco", false);

      BigDecimal bdSpotlightPrice = BigDecimal.valueOf(spotlightPrice);
      spotlightPrice = bdSpotlightPrice.setScale(2, RoundingMode.DOWN).doubleValue();

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditcards(spotlightPrice);

      if (priceFrom != null) {
         BigDecimal bdPriceFrom = BigDecimal.valueOf(priceFrom);
         priceFrom = bdPriceFrom.setScale(2, RoundingMode.DOWN).doubleValue();
      }

      if (Objects.equals(priceFrom, spotlightPrice)) {
         priceFrom = null;
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();

   }

   protected CreditCards scrapCreditcards(Double installmentPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(installmentPrice);

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Double installmentPrice) throws MalformedPricingException {
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(installmentPrice)
         .build());

      return installments;
   }

}
