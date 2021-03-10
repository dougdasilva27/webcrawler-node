package br.com.lett.crawlernode.crawlers.ranking.keywords.ribeiraopreto;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.crawlers.extractionutils.ranking.SavegnagoRanking;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class RibeiraopretoSavegnagoCrawler extends SavegnagoRanking {

    public RibeiraopretoSavegnagoCrawler(Session session) {
        super(session);
    }

}
