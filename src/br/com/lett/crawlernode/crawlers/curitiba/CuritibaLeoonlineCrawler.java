package br.com.lett.crawlernode.crawlers.curitiba;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.Crawler;
import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.util.Logging;


public class CuritibaLeoonlineCrawler extends Crawler {

	private final String HOME_PAGE = "http://www.leoonline.com.br/";

	public CuritibaLeoonlineCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, "Product page identified: " + this.session.getUrl());

			// Id interno
			String id = (this.session.getUrl().split("-")[1]);
			String internalID = Integer.toString(Integer.parseInt(id));

			// Nome
			Elements elementName = doc.select("#lbDescricaoCurta");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select("#lbPreco");
			Float price = (float) 0;

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1 = ""; 
			String category2 = ""; 
			String category3 = "";
			Elements element_categories = doc.select("#lbCategorias"); 
			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			int j=0;
			for(int i = 0; i < element_categories.size(); i++) {

				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;

			}
			if(element_categories.size() == 2){
				category1 = cat[1];
				category2 = null;
				category3 = null;
			}
			else if(element_categories.size() == 2){
				category1 = cat[1];
				category2 = cat[2];
				category3 = null;
			}

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.select("meta[property=og:image]");
			primaryImage = element_foto.first().attr("content").trim();
			if(primaryImage.contains("sem_imagem")) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			String description = "";
			Element elementDescription = doc.select("div#description-tab").first();
			if (elementDescription != null) {
				description += elementDescription.html();
			}
			
			// Maketplace
			JSONArray marketplace = null;

			// Estoque
			Integer stock = null;

			Product product = new Product();
			product.setSeedId(session.getSeedId());
			product.setUrl(session.getUrl());
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
			Logging.printLogTrace(logger, "Not a product page" + session.getSeedId());
		}
		
		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.leoonline.com.br/P-");
	}

}