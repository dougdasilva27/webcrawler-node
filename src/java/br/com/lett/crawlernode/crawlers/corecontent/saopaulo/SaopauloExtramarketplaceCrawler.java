package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.CNOVANewCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.*;

public class SaopauloExtramarketplaceCrawler extends CNOVANewCrawler {

   private static final String STORE = "extra";
   private static final String INITIALS = "EX";
   private static final List<String> SELLER_NAMES = Arrays.asList("Extra", "extra.com.br");

   public SaopauloExtramarketplaceCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected String getStore() {
      return STORE;
   }

   @Override
   protected List<String> getSellerName() {
      return SELLER_NAMES;
   }

   @Override
   protected String getInitials() {
      return INITIALS;
   }

   @Override
   protected Response fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("accept-encoding", "");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
      headers.put("cache-control", "no-cache");
      headers.put("pragma", "no-cache");
      headers.put("sec-fetch-dest", "document");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-user", "?1");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("authority", "www.extra.com.br");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setSendUserAgent(false)
         .setHeaders(headers)
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY

            )
         )
         .build();

      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new FetcherDataFetcher(), new ApacheDataFetcher()), session);
   }

   @Override
   protected String crawlDescription(JSONObject infoProductJson, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      description.append("<div id=\"desciption\">" + infoProductJson.optString("description") + "</div>");

      JSONArray specsGroup = infoProductJson.optJSONArray("specGroups");
      if (specsGroup != null && !specsGroup.isEmpty()) {
         description.append("<div id=\"specs\">");

         for (Object o : specsGroup) {
            JSONObject specGroup = (JSONObject) o;

            JSONArray rows = specGroup.optJSONArray("specs");
            if (rows != null && !rows.isEmpty()) {
               description.append("<table> <h4>" + specGroup.optString("name") + "</h4>");

               for (Object obj : rows) {
                  JSONObject row = (JSONObject) obj;
                  description.append("<tr>");
                  description.append("<td>" + row.optString("name") + "</td>");
                  description.append("<td>" + row.optString("value") + "</td>");
                  description.append("</tr>");
               }

               description.append("</table>");
            }
         }

         description.append("</div>");
      }


      return Normalizer.normalize(description.toString(), Normalizer.Form.NFD).replaceAll("[^\n\t\r\\p{Print}]", "");
   }

   protected Offers scrapOffers(String internalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sellersIdList = new ArrayList<>();
      String url = "https://pdp-api." + getStore() + ".com.br/api/v2/sku/" + internalId + "/price/source/" + getInitials() + "?&take=all";
      JSONObject offersJson = JSONUtils.stringToJson(fetchPage(url).getBody());
      JSONArray sellerInfo = offersJson.optJSONArray("sellers");

      if (sellerInfo != null && offersJson.optBoolean("buyButtonEnabled", false)) {
         // The Business logic is: if we have more than 1 seller is buy box
         boolean isBuyBox = sellerInfo.length() > 1;

         for (int i = 0; i < sellerInfo.length(); i++) {
            JSONObject info = (JSONObject) sellerInfo.get(i);

            if (info.has("name") && !info.isNull("name") && info.has("id") && !info.isNull("id")) {
               String name = info.optString("name");
               String internalSellerId = info.optString("id");

               if (internalSellerId != null) {
                  sellersIdList.add(internalSellerId);
               }

               Integer mainPagePosition = (i + 1) <= 3 ? i + 1 : null;
               Integer sellersPagePosition = i + 1;

               boolean isMainRetailer = false;

               for (String sellerName : getSellerName()) {
                  if (sellerName.equalsIgnoreCase(name)) {
                     isMainRetailer = true;
                  }
               }

               boolean principalSeller = info.optBoolean("elected", false);
               List<String> salesList = principalSeller ? scrapSales(offersJson) : new ArrayList<>();

               Pricing pricing = scrapPricing(offersJson, info, principalSeller);

               if (pricing != null) {
                  Offer offer = Offer.OfferBuilder.create()
                     .setInternalSellerId(internalSellerId)
                     .setSellerFullName(name)
                     .setMainPagePosition(mainPagePosition)
                     .setSellersPagePosition(sellersPagePosition)
                     .setPricing(pricing)
                     .setIsBuybox(isBuyBox)
                     .setIsMainRetailer(isMainRetailer)
                     .setSales(salesList)
                     .build();

                  offers.add(offer);
               }
            }
         }
         Map<String, Double> scrapSallersIdAndRating = scrapSallersRating(sellersIdList);

         int position = 1;
         for (Map.Entry<String, Double> entry : scrapSallersIdAndRating.entrySet()) {
            for (Offer offer : offers.getOffersList()) {
               if (offer.getInternalSellerId().equals(entry.getKey())) {
                  offer.setSellersPagePosition(position);
                  position++;
               }
            }
         }
      }

      return offers;
   }


}
