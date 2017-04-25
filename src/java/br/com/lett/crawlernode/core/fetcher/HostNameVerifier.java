package br.com.lett.crawlernode.core.fetcher;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class HostNameVerifier implements HostnameVerifier {

     @Override
     public boolean verify(String string, SSLSession ssls) {
         return true;
     }

}
