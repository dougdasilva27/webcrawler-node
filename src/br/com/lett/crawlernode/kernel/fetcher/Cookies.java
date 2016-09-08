package br.com.lett.crawlernode.kernel.fetcher;

import java.util.List;

import org.apache.http.cookie.Cookie;
import org.joda.time.DateTime;

public class Cookies {
	
	/** List of cookies used to perform requests */
	private List<Cookie> cookieList;
	
	/** Instant of the last cookies refresh */
	private DateTime lastRefresh;
	
	/** Mutex used to control access to the cookie list and the last refresh instant */
	private Object mutex;
	
	public Cookies() {
		this.lastRefresh = null;
	}
	
	public void refresh(List<Cookie> cookieList) {
		synchronized (mutex) {
			this.cookieList = cookieList;
			this.lastRefresh = DateTime.now();
		}
	}
	
	public boolean isEmpty() {
		synchronized (mutex) {
			if (this.cookieList == null) return true;
			if (this.cookieList.size() == 0) return true;
			return false;
		}
	}
	
	public DateTime getLastRefreshInstant() {
		synchronized (mutex) {
			return this.lastRefresh;
		}
	}
	
	public List<Cookie> getCookieList() {
		synchronized (mutex) {
			return this.cookieList;
		}
	}

}
