package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.CrawlerSession;
import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.util.Logging;


public class SaopauloMamboCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.mambo.com.br/";

	public SaopauloMamboCrawler(CrawlerSession session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Id interno
			String internalID = null;
			Element element_id = doc.select(".hidden-sku-default").first(); 
			if (element_id != null) {
				internalID = Integer.toString(Integer.parseInt(element_id.text().trim().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));
			}

			// Nome
			Elements element_nome = doc.select(".productName");
			String name = element_nome.text().replace("'","").replace("’","").trim();

			// Preço
			Element element_preco = doc.select(".skuBestPrice").last();
			if (element_preco==null) return products;
			Float price = Float.parseFloat(element_preco.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			Elements element_categories = doc.select(".bread-crumb ul li"); 
			String category1; 
			String category2; 
			String category3;

			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";

			int j=0;
			for(int i=0; i < element_categories.size(); i++) {

				Element e = element_categories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;

			}
			category1 = cat[1];
			category2 = cat[2];

			if(element_categories.size()>3){
				category3 = cat[3];
			} 
			else {
				category3 = null;
			}

			// Imagens
			Elements elementPrimaryImage = doc.select("#image-main");
			String primaryImage = elementPrimaryImage.attr("src");
			String secondaryImages = null;

			// Descrição
			String description = "";
			Elements element_descricao = doc.select(".productDescription");
			description = element_descricao.first().html().replace("'","").replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

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

	private boolean isProductPage(Document document) {
		Element element_id = document.select(".hidden-sku-default").first();
		return element_id != null;
	}
}