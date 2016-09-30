package br.com.lett.crawlernode.database;

import org.json.JSONObject;

public class PersistenceResult {
	
	protected JSONObject information;
	
	public PersistenceResult() {
		this.setInformation(new JSONObject());
	}

	public JSONObject getInformation() {
		return information;
	}

	public void setInformation(JSONObject information) {
		this.information = information;
	}
	
}
