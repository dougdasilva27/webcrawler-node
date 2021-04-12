package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;


import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Collections;
import java.util.List;

public class SaopauloChoppbrahmaexpressCrawler extends VTEXNewScraper {

    public SaopauloChoppbrahmaexpressCrawler(Session session) {
        super(session);
    }

    @Override
    protected String getHomePage() {
        return "https://choppbrahmaexpress.com.br/";
    }

    @Override
    protected List<String> getMainSellersNames() {
        return Collections.singletonList("Chopp Brahma");
    }

    @Override
    protected RatingsReviews scrapRating(String internalId, String internalPid,
                                         Document doc, JSONObject jsonSku) {
        return null;
    }
}
