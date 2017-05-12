package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Prices;

public class BrasilEletrozemaCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.zema.com";

	public BrasilEletrozemaCrawler(Session session) {
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

		if( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// internalId
			String internalId = null;
			Element elementInternalId = doc.select("input#IdProduto").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.attr("value").trim();
			}

			// internalPid
			String internalPid = null;
			Element elementPid = doc.select("p.codigo").first();
			if (elementPid != null) {
				String tmp = elementPid.text();
				internalPid = tmp.substring(tmp.indexOf(":")+1, tmp.length()).trim();
			}

			// name
			String name = null;
			Element elementName = doc.select(".produtoPrincipal .nomeMarca h1.nome").first();
			if(elementName != null){
				name = elementName.text().replace("'","").replace("’","").trim();
			}

			// availability
			Element elementNotifyButton = doc.select(".flagEsgotado").first();
			boolean available = true;
			if(elementNotifyButton != null) {
				if( elementNotifyButton.attr("style").equals("display:block;") ) {
					available = false;
				}
			}
			
			// price
			Float price = null;
			
			if(available){
				Element elementPrice = doc.select(".valores .preco #PrecoProduto").first();
				if(elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
			}

			// categories
			String category1 = "";
			String category2 = "";
			String category3 = "";
			String categories[] = doc.select(".breadCrumbs").text().split("/");

			for(int i=1; i<categories.length; i++) {
				if(category1.isEmpty()) {
					category1 = categories[i].trim();
				} 
				else if(category2.isEmpty()) {
					category2 = categories[i].trim();
				} 
				else if(category3.isEmpty()) {
					category3 = categories[i].trim();
				}
			}

			// images
			Elements elementSecondaryImages = doc.select("#ListarMultiFotos li a img.foto");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e: elementSecondaryImages) {

				// Tirando o 87x87 para pegar imagem original
				if(primaryImage == null) {
					primaryImage = e.attr("src");
				} else {
					secondaryImagesArray.put(e.attr("src"));
				}

			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription1 = doc.select(".aba1.descResumida").first();
			Element elementDescription2 = doc.select(".aba2.descricao").first();
			if(elementDescription1 != null) description = elementDescription1.text().replace(".", ".\n").replace("'","").replace("’","").trim();
			if(elementDescription2 != null) description = description + "\n\n" + elementDescription2.text().replace(".", ".\n").replace("'","").replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			// Prices
			Prices prices = crawlPrices(price);
			
			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setPrices(prices);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}

		return products;
	}


	private Prices crawlPrices(Float price){
		Prices prices = new Prices();
		
		if(price != null){
			String url = "https://www.zema.com/simulador_parcelas.asp?ValorParcelar=" + price.toString().replace(".", ",");
			
			Document doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);		
			Elements formasElements = doc.select(".forma");
			
			for(Element e : formasElements){
				Element nomeFormaElement = e.select("> p").first();
				
				if(nomeFormaElement != null){
					String nomeForma = nomeFormaElement.ownText().toLowerCase();
					
					if(nomeForma.contains("boleto")){
						Element priceElement = e.select(".parcelamento li").first();
						
						if(priceElement != null){
							Float priceBank = Float.parseFloat(priceElement.ownText().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
							prices.setBankTicketPrice(priceBank);
						}
						
					} else if(nomeForma.contains("american")){
						Map<Integer,Float> installmentPriceMap = crawlInstallments(e);
						prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
						
					} else if(nomeForma.contains("mastercard")){
						Map<Integer,Float> installmentPriceMap = crawlInstallments(e);
						prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
						
					} else if(nomeForma.contains("visa")){
						Map<Integer,Float> installmentPriceMap = crawlInstallments(e);
						prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
						
					}
				}
			}
		}
		
		return prices;
	}
	
	private Map<Integer,Float> crawlInstallments(Element e){
		Map<Integer,Float> installmentPriceMap = new HashMap<>();
		Elements priceElements = e.select(".parcelamento li span");
		
		for(Element l : priceElements){
			String text = l.text().toLowerCase();
			int x = text.indexOf("x");
			
			Integer installment = Integer.parseInt(text.substring(0, x).trim());
			Float value = Float.parseFloat(text.substring(x).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
			
			installmentPriceMap.put(installment, value);
		}
		
		return installmentPriceMap;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("https://www.zema.com/produto/") || url.startsWith("http://www.zema.com/produto/"));
	}

}
