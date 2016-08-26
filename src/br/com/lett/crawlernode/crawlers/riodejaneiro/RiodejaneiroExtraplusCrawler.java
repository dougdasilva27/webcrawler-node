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

public class RiodejaneiroExtraplusCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.extraplus.com.br/";

	public RiodejaneiroExtraplusCrawler(CrawlerSession session) {
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
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// Id interno
			String id = this.session.getUrl().split("/")[4];
			String internalID = Integer.toString(Integer.parseInt(id.split("-")[0]));

			// Nome
			Elements elementName = doc.select("p.prodNome");
			String name = elementName.text().replace("'", "").trim();

			// Preço
			Elements elementPrice = doc.select("div.preco p");
			Float price = Float.parseFloat(elementPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

			// Disponibilidade
			boolean available = true;

			// Categorias
			String category1; 
			String category2; 
			String category3;
			Elements element_categories = doc.select("div.migalha ul li"); 
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
			category1 = cat[2];
			category2 = cat[3];
			category3 = null;

			// Imagens
			String primaryImage = "";
			Elements element_foto = doc.head().getElementsByAttributeValue("property", "og:image");
			if(element_foto.size()>1){
				primaryImage = element_foto.get(1).attr("content").trim();
			}
			else{
				primaryImage = element_foto.first().attr("src").trim();	
			}
			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";

			String secondaryImages = null;

			// Descrição
			String description = "";

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

	private boolean isProductPage(String url) {
		return url.contains("produto");
	}
}