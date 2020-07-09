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
        headers.put("x-visitorid", VISITOR_ID);
        JSONObject payload = new JSONObject();
        payload.put("operationName", "loadProduct");
        payload.put("variables", getVariables());
        payload.put("query", "query loadProduct($id: ID, $isVisitor: Boolean!){loadProduct(id: $id, isVisitor: $isVisitor) {id displayName description isRgb price {min max} images category {id displayName} brand {id displayName} applicableDiscount {discountType finalValue presentedDiscountValue}}}");

        Request request = Request.RequestBuilder.create().setUrl(API_URL)
                .setPayload(payload.toString())
                .setCookies(cookies)
                .setHeaders(headers)
                .mustSendContentEncoding(true)
                .build();
        Response response = this.dataFetcher.post(session, request);
        return CrawlerUtils.stringToJson(response.getBody());
    }

    @Override
    public List<Product> extractInformation(JSONObject json) throws Exception {
        String teste = "0";
        return super.extractInformation(json);
    }
}
