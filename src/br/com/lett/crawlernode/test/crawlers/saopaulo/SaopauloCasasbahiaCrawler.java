package br.com.lett.crawlernode.test.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.kernel.fetcher.DataFetcher;
import br.com.lett.crawlernode.test.kernel.task.Crawler;
import br.com.lett.crawlernode.test.kernel.task.CrawlerSession;

public class SaopauloCasasbahiaCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.casasbahia.com.br/";

	public SaopauloCasasbahiaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();		

		if ( isProductPage(this.session.getUrl(), doc) ) { // se não tiver o id interno na página, então não é página de produto
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			Element elementInternalID = doc.select(".fn.name span").first();
			String internalID = null;
			if (elementInternalID != null) {
				internalID = Integer.toString(Integer.parseInt(elementInternalID.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "")));
			}

			// Pid
			// o id interno mudará
			String internalPid = internalID;

			// Nome
			Elements elementName = doc.select(".fn.name b");
			String name = elementName.text().replace("'","").replace("’", "").trim();

			// Categorias
			Elements elementCategories = doc.select(".breadcrumb a"); 
			String category1 = null;
			String category2 = null;
			String category3 = null;
			String[] cat = new String[4];
			cat[0] = "";
			cat[1] = "";
			cat[2] = "";
			cat[3] = "";
			int j = 0;
			for(int i = 0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				cat[j] = e.text().toString();
				cat[j] = cat[j].replace(">", "");
				j++;
			}

			category1 = cat[1];
			category2 = cat[2];
			category3 = cat[3];

			// Imagem primária
			String primaryImage = "";
			Elements elementPrimaryImage = doc.select(".photo");
			primaryImage = elementPrimaryImage.first().attr("src").trim();
			if(primaryImage.contains("indisponivel.gif")) primaryImage = "";

			// Imagens secundárias
			String secondaryImages = null;
			Elements elementSecondaryImages = doc.select(".carouselBox .thumbsImg li a");
			JSONArray secondaryImagesArray = new JSONArray();

			if(elementSecondaryImages.size() > 1) {
				for(int i = 1; i < elementSecondaryImages.size(); i++) { // primeira imagem é a primaria
					Element e = elementSecondaryImages.get(i);
					String secundaria = e.attr("href");
					secondaryImagesArray.put(secundaria);
				}
			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			Elements elementDescription = doc.select("#detalhes");
			String description = elementDescription.html();

			boolean mustInsert = true;

			if(mustInsert) {

				// Marketplace
				JSONArray marketplace = new JSONArray();
				Integer stock = null;
				Float price = null;
				boolean available = false;

				String urlMarketplaceInfo = (this.session.getUrl().split(".html")[0] + "/lista-de-lojistas.html");
				Document docMarketplaceInfo = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlMarketplaceInfo, null, null);
				
				Elements lines = docMarketplaceInfo.select("table#sellerList tbody tr");

				for(Element linePartner: lines) {
					String partnerName = linePartner.select("a.seller").first().text().trim().toLowerCase();
					Float partnerPrice = Float.parseFloat(linePartner.select(".valor").first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));;

					if(partnerName.equals("casas bahia")) {
						available = true;
						price = partnerPrice;
					} 
					else { // só inserimos no marketplace se não for a própria loja
						JSONObject partner = new JSONObject();
						partner.put("name", partnerName);
						partner.put("price", partnerPrice);
						marketplace.put(partner);
					}
				}

				Product product = new Product();
				product.setUrl(this.session.getUrl());
				product.setSeedId(this.session.getSeedId());
				product.setInternalId(internalID);
				product.setInternalPid(internalPid);
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

			}

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url, Document doc) {
		Element elementInternalID = doc.select(".fn.name span").first();
		return (elementInternalID != null && url.startsWith("http://www.casasbahia.com.br/"));
	}

}