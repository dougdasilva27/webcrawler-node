package br.com.lett.crawlernode.core.server;

public class BeanstalkRequest {

	private String environment;
	private String mode;
	private Integer page;
	private String toDate;
	private String fromDate;
	private Boolean debug;
	
	@Override
	public String toString() {
		return "BeanstalkRequest [environment=" + environment + ", mode="
				+ mode + ", page=" + page + ", toDate=" + toDate
				+ ", fromDate=" + fromDate + ", debug=" + debug + "]";
	}
	
	public String getEnvironment() {
		return environment;
	}
	
	public void setEnvironment(String environment) {
		this.environment = environment;
	}
	
	public Integer getPage() {
		return page;
	}
	
	public void setPage(Integer page) {
		this.page = page;
	}
	
	public String getMode() {
		return mode;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	
	public String getToDate() {
		return toDate;
	}
	
	public void setToDate(String toDate) {
		this.toDate = toDate;
	}
	
	public String getFromDate() {
		return fromDate;
	}
	
	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}
	
	public Boolean getDebug() {
		return debug;
	}
	
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	
}
