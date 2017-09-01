package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import java.util.List;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.PaguemenosCrawler;
import br.com.lett.crawlernode.util.Logging;

public class FortalezaPaguemenosCrawler extends Crawler {
	
	private static final String HOME_PAGE = "http://loja.paguemenos.com.br/";

	public FortalezaPaguemenosCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public void handleCookiesBeforeFetch() {
		Logging.printLogDebug(logger, session, "Adding cookie...");
		
		BasicClientCookie cookie = new BasicClientCookie("StoreCodePagueMenos", "52");
		cookie.setDomain("loja.paguemenos.com.br");
		cookie.setPath("/");
		this.cookies.add(cookie);
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
	
		return PaguemenosCrawler.extractInformation(doc, logger, session);
	}
}
