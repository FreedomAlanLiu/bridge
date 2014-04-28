/**
 * ©2013-2015 Alan L. Rights Reserved.
 */
package org.daybreak.openfire.plugin.bridge.utils;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Alan
 */
public class HttpConnectionManager {

    // 连接池里的最大连接数 
    public static final int MAX_TOTAL_CONNECTIONS = 100;

    // 每个路由的默认最大连接数  
    public static final int MAX_ROUTE_CONNECTIONS = 50;

    // 连接超时时间 
    public static final int CONNECT_TIMEOUT = 30000; // 1min

    // 套接字超时时间
    public static final int SOCKET_TIMEOUT = 30000;// 1min

    // 连接池中 连接请求执行被阻塞的超时时间
    public static final int CONN_MANAGER_TIMEOUT = 30000;// 1min

    // http线程池管理器
    private static final PoolingHttpClientConnectionManager connection_manager;

    // http客户端
    private static CloseableHttpClient httpClient;

    // HTTP头
    public static String HEAD_X_REQUESTED_WITH = "X-Requested-With";

    private static final Logger Log = LoggerFactory.getLogger(HttpConnectionManager.class);

    private static final X509TrustManager tm = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] xcs, String string)
                throws java.security.cert.CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] xcs, String string)
                throws java.security.cert.CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

    /**
     * 初始化
     */
    static {
        ConnectionSocketFactory plainsf = new PlainConnectionSocketFactory();
        SSLContext sslContext = SSLContexts.createSystemDefault();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER);
        Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        connection_manager = new PoolingHttpClientConnectionManager(r);

        connection_manager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        connection_manager.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);

        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(connection_manager);
        httpClientBuilder.setUserAgent("Mozilla/5.0 (Windows NT 5.1; rv:26.0) Gecko/20100101 Firefox/26.0");
        httpClientBuilder.addInterceptorFirst(new HttpRequestInterceptor() {
            @Override
            public void process(
                    final HttpRequest request,
                    final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip, deflate");
                }
            }
        });
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setTcpNoDelay(true).build();
        httpClientBuilder.setDefaultSocketConfig(socketConfig);
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(5, true));
        httpClient = httpClientBuilder.build();
    }

    /**
     * GET请求
     *
     * @param url
     * @param parameters
     * @return
     */
    public static HttpResponse getHttpRequest(String url, List<NameValuePair> parameters) throws IOException {
        Log.debug("------------------------------------------------------------------------");
        if (parameters != null && parameters.size() > 0) {
            String paramURL = URLEncodedUtils.format(parameters, HTTP.UTF_8);
            if (url.indexOf("?") > -1) {
                url = url + "&" + paramURL;
            } else {
                url = url + "?" + paramURL;
            }
        }
        Log.debug("GET URL: " + url);

        HttpGet httpGet = new HttpGet(url);

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(CONN_MANAGER_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT);
        httpGet.setConfig(requestConfigBuilder.build());

        if (parameters != null) {
            Log.debug(" + Request parameters: ");
            for (NameValuePair param : parameters) {
                Log.debug("   - " + param.getName() + " : " + param.getValue());
            }
        }
        Log.debug(" + Request headers: ");
        for (Header header : httpGet.getAllHeaders()) {
            Log.debug("   - " + header.getName() + " : " + header.getValue());
        }

        HttpResponse response = httpClient.execute(httpGet);
        Log.debug(" + Response headers: ");
        for (Header header : response.getAllHeaders()) {
            Log.debug("   - " + header.getName() + " : " + header.getValue());
        }
        Log.debug("***********************************************************************");
        return response;
    }

    /**
     * 返回GET请求响应字符串
     *
     * @param url
     * @param parameters
     * @return
     */
    public static String getHttpRequestAsString(String url, List<NameValuePair> parameters) throws IOException {
        HttpResponse response = getHttpRequest(url, parameters);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Get url " + url + ": " + response.getStatusLine().getStatusCode());
        }
        HttpEntity entity = response.getEntity();
        String responseContent = readContentFromEntity(entity);
        Log.debug("GET: " + url);
        if (responseContent.length() > 300) {
            Log.debug(" + Response content(0-300):\n" + responseContent.substring(0, 100));
        } else {
            Log.debug(" + Response content:\n" + responseContent);
        }
        return responseContent;
    }

    /**
     * 新建一个HttpPost对象
     *
     * @param url
     * @param parameters
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    public static HttpPost createHttpPost(String url, List<NameValuePair> parameters) throws IOException {

        HttpPost httpPost = new HttpPost(url);

        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(CONN_MANAGER_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT);
        httpPost.setConfig(requestConfigBuilder.build());

        if (parameters != null) {
            UrlEncodedFormEntity uef = new UrlEncodedFormEntity(parameters, "UTF-8");
            httpPost.setEntity(uef);
        }

        return httpPost;
    }

    /**
     * 从流中将字符串读出
     *
     * @param is
     * @return
     * @throws java.io.IOException
     */
    public static String readStringFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();
    }

    /**
     * POST请求
     *
     * @param url
     * @param parameters
     * @return
     */
    public static HttpResponse postHttpRequest(String url, List<NameValuePair> parameters) throws IOException {
        Log.debug("------------------------------------------------------------------------");
        Log.debug("POST URL: " + url);

        HttpPost httpPost = createHttpPost(url, parameters);

        if (parameters != null) {
            Log.debug(" + Request parameters: ");

            for (NameValuePair param : parameters) {
                Log.debug("   - " + param.getName() + " : " + param.getValue());
            }
        }
        Log.debug(" + Request headers: ");
        for (Header header : httpPost.getAllHeaders()) {
            Log.debug("   - " + header.getName() + " : " + header.getValue());
        }

        HttpResponse response = httpClient.execute(httpPost);
        Log.debug(" + Response headers: ");
        for (Header header : response.getAllHeaders()) {
            Log.debug("   - " + header.getName() + " : " + header.getValue());
        }
        Log.debug("***********************************************************************");
        return response;
    }

    /**
     * 返回POST请求响应字符串
     *
     * @param url
     * @param parameters
     * @return
     */
    public static String postHttpRequestAsString(String url, List<NameValuePair> parameters) throws IOException {
        HttpResponse response = postHttpRequest(url, parameters);
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Post url " + url + ": " + response.getStatusLine().getStatusCode());
        }
        HttpEntity entity = response.getEntity();
        String responseContent = readContentFromEntity(entity);
        Log.debug("POST: " + url);
        if (responseContent.length() > 300) {
            Log.debug(" + Response content(0-300):\n" + responseContent.substring(0, 100));
        } else {
            Log.debug(" + Response content:\n" + responseContent);
        }
        return responseContent;
    }

    /**
     * 从response返回的实体中读取页面代码
     *
     * @param httpEntity Http实体
     * @return 页面代码
     * @throws org.apache.http.ParseException
     * @throws java.io.IOException
     */
    public static String readContentFromEntity(HttpEntity httpEntity) throws ParseException, IOException {
        String html;
        Header header = httpEntity.getContentEncoding();
        if (header != null && "gzip".equals(header.getValue())) {
            html = EntityUtils.toString(new GzipDecompressingEntity(httpEntity));
        } else {
            html = EntityUtils.toString(httpEntity);
        }
        return html;
    }
}
