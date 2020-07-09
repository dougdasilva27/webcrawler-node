package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.net.HttpHeaders;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilZedeliveryCrawler extends Crawler {

    private static final String HOME_PAGE = "https://www.ze.delivery";
    private static final String API_URL = "https://api.ze.delivery/public-api";
    //TODO check if visitor id changes with address
    private static final String VISITOR_ID = "2d5a638d-fc7b-4143-9379-86ddb12832b5";

    public BrasilZedeliveryCrawler(Session session) {
        super(session);
    }

    @Override
    public boolean shouldVisit() {
        //TODO check if url is correct
        String href = session.getOriginalURL().toLowerCase();
        return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
    }

    private String getIdFromUrl() {
        //https://www.ze.delivery/entrega-produto/9250/conhaque-dreher-900ml
        String[] split = this.session.getOriginalURL().split("/");
        return split[split.length - 2];
    }

    private JSONObject getVariables() {
        JSONObject variables = new JSONObject();
        variables.put("id", getIdFromUrl());
        variables.put("isVisitor", false);
        return variables;
    }

    @Override
    protected Object fetch() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.put(HttpHeaders.REFERER, "https://www.ze.delivery/produtos");
        headers.put("x-visitorid", VISITOR_ID);
        headers.put("x-request-origin", "WEB");
        headers.put("origin", "https://www.ze.delivery");
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");

        JSONObject payload = new JSONObject();
        payload.put("operationName", "loadProduct");
        payload.put("variables", getVariables());
        payload.put("query", "query loadProduct($id: ID, $isVisitor: Boolean!) {\\n  loadProduct(id: $id, isVisitor: $isVisitor) {\\n    id\\n    displayName\\n    description\\n    isRgb\\n    price {\\n      min\\n      max\\n    }\\n    images\\n    category {\\n      id\\n      displayName\\n    }\\n    brand {\\n      id\\n      displayName\\n    }\\n    applicableDiscount {\\n      discountType\\n      finalValue\\n      presentedDiscountValue\\n    }\\n  }\\n}\\n");

        Request request = Request.RequestBuilder.create().setUrl(API_URL)
                .setPayload(payload.toString())
                .setCookies(cookies)
                .setHeaders(headers)
                .build();
        Response response = this.dataFetcher.post(session, request);
        return CrawlerUtils.stringToJson(response.getBody());
    }

    @Override
    public List<Product> extractInformation(JSONObject json) throws Exception {
        String teste = "0";
        return super.extractInformation(json);
    }

/*    @Override
    public List<Product> extractInformation(Document doc) throws Exception {
        super.extractInformation(doc);
        List<Product> products = new ArrayList<>();

        if (doc.selectFirst(".css-11ecsv0-productContainer") != null) {

            JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, "}", false, false);
            JSONObject skuJson = (JSONObject) jsonObject.optQuery("/props/pageProps/product");

            List<String> categories = doc.select(".breadcrumbs a").eachText();
            if (!categories.isEmpty()) {
                categories.remove(0);
            }
            String description =
                    CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".more-info", " product-table"));

            String internalId = skuJson.optString("productId");
            String name = skuJson.optString("productName");
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
        CreditCards creditCards = new CreditCards();

        Installments installments = new Installments();
        installments.add(Installment.InstallmentBuilder.create()
                .setInstallmentNumber(1)
                .setInstallmentPrice(price)
                .build());

        creditCards.add(CreditCard.CreditCardBuilder.create()
                .setIsShopCard(false)
                .setBrand(Card.VISA.toString())
                .setInstallments(installments)
                .build());


        offers.add(
                Offer.OfferBuilder.create()
                        .setSellerFullName("Tenda Drive")
                        .setIsBuybox(false)
                        .setPricing(
                                Pricing.PricingBuilder.create()
                                        .setSpotlightPrice(price)
                                        .setBankSlip(BankSlip.BankSlipBuilder.create().setFinalPrice(price).build())
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

    private RatingsReviews scrapRating(String internalId, Document doc) {
        TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "80984", logger);
        return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
    }*/
}
