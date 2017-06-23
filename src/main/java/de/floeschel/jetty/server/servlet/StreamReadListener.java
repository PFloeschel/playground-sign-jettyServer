package de.floeschel.jetty.server.servlet;

import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.CrlClientOnline;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.ICrlClient;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.IOcspClient;
import com.itextpdf.signatures.ITSAClient;
import com.itextpdf.signatures.OCSPVerifier;
import com.itextpdf.signatures.OcspClientBouncyCastle;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.TSAClientBouncyCastle;
import de.floeschel.sign.SignRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamReadListener implements ReadListener {

    private static final Logger LOG = LoggerFactory.getLogger(StreamReadListener.class);

    private final AsyncContext async;
    private final ServletRequest request;
    private final ServletResponse response;

    private final ServletInputStream in;
    private final File tmpFile;
    private final OutputStream tmpOs;
    private final byte[] buffer = new byte[64 * 1024];

    StreamReadListener(AsyncContext async, ServletRequest request, ServletResponse response) throws IOException {
        this.async = async;
        this.request = request;
        this.response = response;

        in = request.getInputStream();
        tmpFile = File.createTempFile(UUID.randomUUID().toString(), null);
        tmpOs = new FileOutputStream(tmpFile);
    }

    @Override
    public void onDataAvailable() throws IOException {
        // while we are able to read without blocking
        while (in.isReady()) {
            // read some content into the copy buffer
            int len = in.read(buffer);

            // If we are at EOF then complete
            if (len < 0) {
                in.close();
                return;
            }

            // write out the copy buffer. 
            tmpOs.write(buffer, 0, len);
        }
    }

    @Override
    public void onAllDataRead() throws IOException {
        tmpOs.close();

        //TODO
        //process the data and generate output
        RandomAccessFile raf = new RandomAccessFile(tmpFile, "r");

        int protoMessageLength = raf.readInt();
        byte[] protoData = new byte[protoMessageLength];
        raf.read(protoData);
        SignRequest signRequest = SignRequest.parseFrom(protoData);

        File sigFile = File.createTempFile(UUID.randomUUID().toString(), null);
        FileOutputStream sigOut = new FileOutputStream(sigFile);

        PdfReader pdfReader = new PdfReader(new RandomAccessSourceFactory().createSource(raf), null);
        PdfSigner pdfSigner = new PdfSigner(pdfReader, sigOut, UUID.randomUUID().toString(), false);

//        pdfSigner.getSignatureAppearance()
//                .setReason("Reason")
//                .setLocation("Location");
//        pdfSigner.setCertificationLevel(PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED);
        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream("../PF_123456.p12"), signRequest.getPin().toCharArray());
            String alias = ks.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, signRequest.getPin().toCharArray());
            Certificate[] chain = ks.getCertificateChain(alias);

            IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, BouncyCastleProvider.PROVIDER_NAME);
            IExternalDigest digest = new BouncyCastleDigest();

            ICrlClient crlClientOnline = new CrlClientOnline(chain);
            Collection<ICrlClient> crlClients = new HashSet<>();
            crlClients.add(crlClientOnline);

            IOcspClient ocspClient = new OcspClientBouncyCastle(new OCSPVerifier(null, null));
            ITSAClient tsaClient = new TSAClientBouncyCastle("https://freetsa.org/tsr");

            pdfSigner.signDetached(digest, pks, chain, crlClients, ocspClient, tsaClient, 0, PdfSigner.CryptoStandard.CADES);

            //generate response 
            ServletOutputStream out = response.getOutputStream();
            out.setWriteListener(new StreamWriteListener(async, request, response, sigFile));
        } catch (IOException | GeneralSecurityException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        } finally {
            raf.close();
            sigOut.close();
            tmpFile.delete();
            sigFile.deleteOnExit();
        }
    }

    @Override
    public void onError(Throwable t) {
        try {
            tmpOs.close();
            if (!tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }
//            response.getWriter().write(t.getLocalizedMessage());
            async.complete();
            LOG.error(t.getLocalizedMessage(), t);
        } catch (IOException ex) {
            LOG.error(ex.getLocalizedMessage(), ex);
        }
    }

}
