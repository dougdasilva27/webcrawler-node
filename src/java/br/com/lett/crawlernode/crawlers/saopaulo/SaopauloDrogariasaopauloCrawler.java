package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloDrogariasaopauloCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.drogariasaopaulo.com.br/";

	public SaopauloDrogariasaopauloCrawler(CrawlerSession session) {
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

		if ( isProductPage(doc) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			Element element_id = doc.select("div.productReference").first();
			String internalID = Integer.toString(Integer.parseInt(element_id.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));

			// Nome
			Elements elementName = doc.select(".buy_box .productName");
			String name = elementName.text().replace("'","").replace("’","").trim();

			// Categorias
			Elements elementCategories = doc.select(".bread-crumb li"); 

			String category1 = null;
			String category2 = null;
			String category3 = null;
			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			int j=0;

			for(int i=0; i < elementCategories.size(); i++) {

				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "").trim();
				j++;

			}
			category1 = cat[1];
			category2 = cat[2];
			category3 = cat[3];

			// Imagem primária
			String primaryImage = "";
			Elements elementPrimaryImage = doc.select("img#image-main");
			primaryImage = elementPrimaryImage.first().attr("src").trim();
			if(primaryImage.contains("indisponivel.gif")) {
				primaryImage = "";
			}

			// Imagens secundárias
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();
			Elements element_fotosecundaria = doc.select("ul.thumbs li a");

			if(element_fotosecundaria.size()>1){
				for(int i=1; i<element_fotosecundaria.size();i++){
					Element e = element_fotosecundaria.get(i);
					secondaryImagesArray.put(e.attr("rel"));
				}
			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			Elements elementDescription = doc.select("div.productDescription");
			Element elementInformation = doc.select(".flt_left.productSpecification #caracteristicas").first();
			String description = elementDescription.html();
			if(elementInformation != null) {
				description = description + elementInformation.html();
			}

			// Preço e disponibilidade
			Float price = null;
			boolean available = true;
			Element buyButton = doc.select("a.buy-button").first();

			if(buyButton == null || buyButton.attr("style").replace(" ", "").contains("display:none")) {
				available = false;
			} else {
				Element element_preco = doc.select("strong.skuPrice").first();
				price = Float.parseFloat(element_preco.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

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

	private boolean isProductPage(Document document) {
		Element body = document.select("body").first();
		Element element_id = document.select("div.productReference").first();
		return (body.hasClass("produto") && element_id != null);
	}

}