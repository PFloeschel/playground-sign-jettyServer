package de.floeschel.jetty;

import ch.qos.logback.classic.Level;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 *
 * @author HTPC
 */
public class OkClient {

    private static final Class CLAZZ = JettyClient.class;

    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);

    public static void main(String[] args) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);

        if (!SLF4JBridgeHandler.isInstalled()) {
            //Remove existing handlers attached to j.u.l root logger
            SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
            // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
            // the initialization phase of your application
            SLF4JBridgeHandler.install();
        }

//        SSLSocketFactory sslSocketFactory = SSLContexts.createDefault().getSocketFactory();
//        SSLSocketFactory sslSocketFactory = SSLContexts
//                .custom()
//                .loadTrustMaterial(new File("../localhost_123456.p12"), "123456".toCharArray())
//                .build()
//                .getSocketFactory();
        SSLSocketFactory sslSocketFactory = SSLContexts
                .custom()
                .loadTrustMaterial((X509Certificate[] chain, String authType) -> true)
                .build()
                .getSocketFactory();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory)
                .addInterceptor(logging)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url("http://localhost:8080/")
                .post(RequestBody.create(null, new File("C:\\Users\\HTPC\\Downloads\\Windows.iso")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                try (ResponseBody responseBody = response.body()) {
                    try (OutputStream os = new FileOutputStream("content")) {
                        ByteStreams.copy(responseBody.byteStream(), os);
                    }
                }
            }
        }
        );
//        try (Response response = client.newCall(request).execute()) {
//            if (!response.isSuccessful()) {
//                throw new IOException("Unexpected code " + response);
//            }
//            try (ResponseBody responseBody = response.body()) {
//                try (OutputStream os = new FileOutputStream("content")) {
//                    ByteStreams.copy(responseBody.byteStream(), os);
//                }
//            }
//        }
    }
}
