package edp.davinci.task.bean;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
public class RestClientConfig {

    /**
     * 最大连接数
     */
    private int maxTotalConnect = 20;

    /**
     * 同路由并发数
     */
    private int maxConnectPerRoute = 10;

    /**
     * 客户端和服务器建立连接超时，默认2s
     */
    private int connectTimeout = 2 * 1000;

    /**
     * 指客户端从服务器读取数据包的间隔超时时间, 不是总读取时间，默认30s
     */
    private int readTimeout = 30 * 1000;

    /**
     * 重试次数
     */
    private int retryTimes = 2;

    /**
     * 从连接池获取连接的超时时间, 单位ms
     */
    private int connectionRequestTimout = 200;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(httpComponentsClientHttpRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        return restTemplate;
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, ((x509Certificates, s) -> true)).build();

            httpClientBuilder.setSSLContext(sslContext);
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                    new PoolingHttpClientConnectionManager(
                            RegistryBuilder.<ConnectionSocketFactory>create()
                                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                                    .register("https", new SSLConnectionSocketFactory(sslContext, hostnameVerifier))
                                    .build()
                    );

            poolingHttpClientConnectionManager.setMaxTotal(maxTotalConnect);
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxConnectPerRoute);
            httpClientBuilder.setConnectionManager(poolingHttpClientConnectionManager);
            httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(retryTimes, true));
            HttpClient httpClient = httpClientBuilder.build();
            HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
                    httpClient);
            clientHttpRequestFactory.setConnectTimeout(connectTimeout);
            clientHttpRequestFactory.setReadTimeout(readTimeout);
            clientHttpRequestFactory.setConnectionRequestTimeout(connectionRequestTimout);
            return clientHttpRequestFactory;
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Initializing HTTP connection pool error", e);
        }
        return null;
    }
}
