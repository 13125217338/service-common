package org.city.common.core.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * @作者 ChengShi
 * @日期 2022-10-25 16:52:15
 * @版本 1.0
 * @描述 忽略SSL证书配置
 */
public class SSLConfig extends SimpleClientHttpRequestFactory {
	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (connection instanceof HttpsURLConnection) {init((HttpsURLConnection) connection);}
		super.prepareConnection(connection, httpMethod);
	}
	
	/* 初始化安全链接 */
	private void init(HttpsURLConnection connection) {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			/* 初始化证书 */
			context.init(null, new TrustManager[] {
				new X509TrustManager() {
					@Override
					public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}
				}
			}, new SecureRandom());
			/* 域名默认成功 */
			connection.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {return true;}
			});
			/* 设置安全链接工厂 */
			connection.setSSLSocketFactory(context.getSocketFactory());
		} catch (Exception e) {throw new RuntimeException(e);}
	}
}
