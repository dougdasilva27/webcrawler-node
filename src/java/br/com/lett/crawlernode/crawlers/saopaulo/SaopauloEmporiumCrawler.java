package br.com.lett.crawlernode.crawlers.saopaulo;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloEmporiumCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.emporiumsaopaulo.com.br/";

	public SaopauloEmporiumCrawler(Session session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			String params = this.session.getOriginalURL().split("\\?")[1];
			Map<String, String> paramsMap = new HashMap<String, String>();
			for(String s: params.split("&")) {
				try {
					paramsMap.put(s.split("=")[0], s.split("=")[1]);
				} catch (Exception e) { }
			}

			// Id interno
			String internalID = Integer.toString(Integer.parseInt(paramsMap.get("pid")));

			// Nome
			Elements elementName = doc.select("td.titproduto");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select("td.precoGR").last();
			if (elementPrice != null) {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_cat1 = doc.select("div table tr td table tr td table tr td div span"); //TODO: Este selector está muito frágil
			String category1 = element_cat1.text().trim();

			String category2 = "";
			if(paramsMap.containsKey("gondola")) {
				category2 = paramsMap.get("gondola");
				category2 = URLDecoder.decode(category2).replace("+", " ");
			}

			String category3 = "";

			// Imagens
			String primaryImage = null;
			Elements element_foto = doc.select("td.tdtfoto img");
			if (element_foto != null && element_foto.size() > 0) {
				primaryImage = element_foto.get(0).attr("src").trim();
			}
			if(primaryImage != null && !primaryImage.equals("")) primaryImage = "http://www.emporiumsaopaulo.com.br/" + primaryImage;
			if(primaryImage != null && primaryImage.contains("produto_sem_foto")) primaryImage = "";
			String secondaryImages = null;

			// Descrição
			String description = "";

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalID);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.contains("/produto.asp") && url.contains("pid="));
	}
}