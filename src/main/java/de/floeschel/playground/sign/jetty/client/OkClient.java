package de.floeschel.playground.sign.jetty.client;

import com.google.common.io.ByteStreams;
import de.floeschel.playground.sign.pdf.PAdES;
import de.floeschel.playground.sign.util.Configuration;
import de.floeschel.sign.SignRequest;
import de.floeschel.playground.sign.util.StreamUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 *
 * @author Pascal
 */
public class OkClient {

    private static final Class CLAZZ = JettyClient.class;

    private static final Logger LOG = LoggerFactory.getLogger(CLAZZ);

    public static void main(String[] args) throws Exception {
        Properties clientProperties = Configuration.load("clientOk");
        Properties signProperties = Configuration.load("sign");

//        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//        root.setLevel(Level.DEBUG);
        if (!SLF4JBridgeHandler.isInstalled()) {
            //Remove existing handlers attached to j.u.l root logger
            SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
            // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
            // the initialization phase of your application
            SLF4JBridgeHandler.install();
        }

//        SSLSocketFactory sslSocketFactory = SSLContexts
//                .custom()
//                .loadTrustMaterial(new File(clientProperties.getProperty("truststoreFile")),
//                        clientProperties.getProperty("truststorePassword").toCharArray())
//                .build()
//                .getSocketFactory();
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (final FileInputStream inStream = new FileInputStream(new File(clientProperties.getProperty("truststoreFile")))) {
            trustStore.load(inStream, clientProperties.getProperty("truststorePassword").toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        var sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                .addInterceptor(logging)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        SignRequest sr = SignRequest.newBuilder()
                .setCertificate(signProperties.getProperty("certificate"))
                .setPin(signProperties.getProperty("pin"))
                .setType(PAdES.convert(signProperties.getProperty("type")))
                .build();

        byte[] protoHeader = StreamUtil.buildProtobufStream(sr);
        File file = new File(signProperties.getProperty("file"));

        Request request = new Request.Builder()
                .url(clientProperties.getProperty("uri"))
                .post(OkRequestBody.create(null, protoHeader, file))
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
                    try (OutputStream os = new FileOutputStream(signProperties.getProperty("signed"));
                            InputStream stream = responseBody.byteStream();) {
                        de.floeschel.sign.Response signResponse = StreamUtil.parseStream(stream, de.floeschel.sign.Response.class);
                        LOG.info("Result: (" + signResponse.getResult() + ") " + signResponse.getMsg());
                        ByteStreams.copy(stream, os);
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
