package br.com.lett.crawlernode.crawlers.riodejaneiro;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroSuperprixCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.superprix.com.br/";

	public RiodejaneiroSuperprixCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();

		// Não pegaremos as páginas que contém "?ftr=" pois elas são categorias filtradas, que não nos interessam.
		return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Id interno
			Element span = doc.select("div.short-description").first();
			if(span==null){
				span = doc.select("div.short-description span").first();
			}
			String id = span.text().replaceAll("[^0-9,]+", "").trim();
			String internalID = Integer.toString(Integer.parseInt(id));

			// Nome
			Elements elementName = doc.select("h1[itemprop=name]");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select("span[itemprop=price]");
			Float price = Float.parseFloat(elementPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			Boolean available = true;

			// Categorias
			String category1; 
			String category2; 
			String category3;
			Elements element_categories = doc.select("span[itemprop=title]"); 

			String[] cat = new String[3];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			int j=0;

			for(int i=0; i < element_categories.size(); i++) {
				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace("/", "");
				j++;
			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = "";

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.select("img[itemprop=image]");
			primaryImage = element_foto.attr("src").trim();
			primaryImage = "http://www.superprix.com.br" + primaryImage;
			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";

			String secondaryImages = null;


			// Descrição
			String description = "";
			Elements element_descricao = doc.select("div#description-tab");
			if(element_descricao==null){
				element_descricao = doc.select("div#description-tab span");
			}

			if(element_descricao.text().equals("Descrição não disponível")) {
				description = null;
			} 
			else { 
				description = element_descricao.html().replace("'", "\"").trim(); 
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getUrl());
			product.setSeedId(this.session.getSeedId());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(Document document) {
		Element span = document.select("div.short-description").first();
		if (span == null){
			span = document.select("div.short-description span").first();
		}
		return span != null;
	}
}