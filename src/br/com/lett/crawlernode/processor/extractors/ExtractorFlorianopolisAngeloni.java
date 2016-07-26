package br.com.lett.crawlernode.processor.extractors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.models.ProcessedModel;
import br.com.lett.crawlernode.processor.base.Extractor;
import br.com.lett.crawlernode.util.Logging;

public class ExtractorFlorianopolisAngeloni extends Extractor {
	
	private static final Logger logger = LoggerFactory.getLogger(ExtractorFlorianopolisAngeloni.class);

	public ExtractorFlorianopolisAngeloni() {
		
	}
	
	/**
	 * Chama os métodos da classe Extractor.
	 * @author alinetorres
	 * @category Manipulação
	 */
	@Override
	public ProcessedModel extract(ProcessedModel pm) {
		super.extract(pm);
		
		// Higienizar
		this.preSanitize(pm);
		
		// Extrair marca
		this.extractBrand(pm);
		
		// Extrair classe e/ou colocar o resto no extra
		this.extractClass(pm);
				
		// Extrair recipiente e ajustar extra
		this.extractRecipient(pm);
		
		// Extrair multiplicador e ajustar extra
		this.extractMultiplier(pm);
		
		// Extrair unidade e quantidade e ajustar extra
		this.extractUnitAndQuantity(pm);
		
		return pm;
		
	}
	
	/**
	 * Envia um processedModel para função de padronização e retira, caso tenha, a string "ref.:" .
	 * @param pm objeto da classe ProcessedModel que contém as informações ja processadas.
	 * @author julinha
	 */
	public void preSanitize(ProcessedModel pm) {
		
		//Chamando o método original
		super.preSanitize(pm);
		

		if (pm.getSanitizedName().contains("ref.:")) {
			pm.setSanitizedName(pm.getSanitizedName().split("ref.:")[0]);
		} else if (pm.getSanitizedName().contains("ref .:")) {
			pm.setSanitizedName(pm.getSanitizedName().split("ref .:")[0]);
		}
				
	}
	
	/**
	 * Chama o método da classe Extractor para extrair a classe
	 * @author alinetorres
	 * @category Manipulação
	 * @param pm - objeto da classe ProcessedModel
	 */
	@Override
	public void extractClass(ProcessedModel pm) {
		
		//Chamando o método original
		super.extractClass(pm);
		
		//Coisas específicas do Angeloni...
				
	}
	
	/**
	 * Chama o método da classe Extractor para extrair quantidade e unidade. <br>Filtra o extra se for preciso para encontrar a quantidade.
	 * @author alinetorres
	 * @category Manipulação
	 * @param pm - objeto da classe ProcessedModel 
	 */
	@Override
	public void extractUnitAndQuantity(ProcessedModel pm) {
		
		//Chamando o método original
		super.extractUnitAndQuantity(pm);
		
		
		//Se não achou quantidade, olhar se está no final
		// Exemplos: vinho ale BACKER gelado 600     ->   quantity: 600
		//           vinho ale BACKER gelado 2,5     ->   quantity: 2.5 
		//           vinho ale BACKER gelado 2.0     ->   quantity: 2

		if(pm.getQuantity() == null) {

			int countOccurences = 0;
			Pattern pattern = Pattern.compile("(\\s|^)([0-9]+)([,.][0-9]{1,2})?(\\s|$)");
		    Matcher matcher = pattern.matcher(pm.getExtra());
		    
		    // Contando quantos números sozinhos existem no extra
		    while (matcher.find()) countOccurences++;
		    
	    	if (logActivated) Logging.printLogInfo(logger, "-- QUANTITY ainda é null, achei " + countOccurences + " números avulsos");

		    matcher.reset();
		    
		    // Se for só um número vou usá-lo, se não, deixo sem
		    if(countOccurences == 1) {
		    	matcher.find();
		    	
		    	String quantityString;
		        Double quantity;
		        
		        quantityString = matcher.group(1) + (matcher.group(2)!=null ? matcher.group(2) : "");
		        quantityString = quantityString.replace(",", ".").trim();
		        
		        quantity = Double.parseDouble(quantityString);
		        
	        	pm.setQuantity(quantity);
			        
		        // Atualizando extra...
		        String newExtra = pm.getExtra().substring(0, matcher.start()) + " " + pm.getExtra().substring(matcher.end(), pm.getExtra().length());
		        pm.setExtra(newExtra);
		        
		    }
			
		}
		
	}

}
