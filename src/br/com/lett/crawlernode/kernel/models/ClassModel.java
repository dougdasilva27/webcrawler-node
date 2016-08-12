package br.com.lett.crawlernode.kernel.models;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Processor model - Class
 * 
 * Classe responsável por modelar retorno do banco de dados das Lett Classes e seus erros comuns.
 * Classe possúi uma String lettName e um mapa com os erros comuns dessa classe de produto e o conteúdo a ser direcionado para o extra. 
 * 
 * @author doug
 */
public class ClassModel {
	
	private String lettName;
	private Map<String, String> mistakes;
	
	public ClassModel() {
		super();
	}
	
	public ClassModel(String lettName){
		this.lettName = lettName;
		this.mistakes = new LinkedHashMap<String, String>();
	}
	
	public ClassModel(String lettName, Map<String, String> mistake){
		this(lettName);	
		if(!mistake.isEmpty())
			putOnMap(mistake);
	}
	
	public String toString(){
		if(mistakes.isEmpty()) {
			return "Class: " + lettName;
		}
		
		return "Class: " + lettName + "\nContains: " + mistakes;
	}
	
	/**
	 * Método que insere valores no mapa com a condição desde que o conteúdo do erro comun não seja nulo
	 * 
	 * @param mistake = Erro comum a inserir na tabela
	 * @param extra = caso no erro contenha algo a ser encaminhado no extra também será colocado no mapa
	 * @return Retorna true caso algo foi inserido na tabela ou false caso contrário
	 */
	public boolean putOnMap(String mistake, String extra){
		
		if(mistake == null || mistake.isEmpty()) return false;
		if(extra == null) extra = "";
					
		if(mistakes.containsKey("mistake")) {
			return false;
		} else {
			mistakes.put(mistake, extra);
			return true;
		}
	}
	
	/**
	 * Aloca todo o mapa recebido no mapa mistake
	 * @param aux = Mapa a ser alocado
	 */
	public void putOnMap(Map<String, String> aux){
		mistakes.putAll(aux);
	}
	
	public String getLettName(){
		return lettName;
	}
	
	public void setLettName(String lettName){
		this.lettName = lettName;
	}
	
	public Map<String, String>  getMistakes(){
		return mistakes;
	}
	
	public void setMistakes(Map<String, String> mistakes){
		this.mistakes = mistakes;
	}
}
