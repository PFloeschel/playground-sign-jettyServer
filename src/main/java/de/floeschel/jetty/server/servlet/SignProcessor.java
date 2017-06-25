package de.floeschel.jetty.server.servlet;

import com.google.protobuf.GeneratedMessageV3;
import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.CrlClientOnline;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.ICrlClient;
import com.itextpdf.signatures.IExternalDigest;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.IOcspClient;
import com.itextpdf.signatures.ITSAClient;
import com.itextpdf.signatures.LtvVerification;
import com.itextpdf.signatures.OCSPVerifier;
import com.itextpdf.signatures.OcspClientBouncyCastle;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.TSAClientBouncyCastle;
import de.floeschel.sign.SignRequest;
import de.floeschel.sign.SignResponse;
import de.floeschel.sign.pdf.PAdES;
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
import java.util.Map;
import java.util.UUID;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.LoggerFactory;

public class SignProcessor implements Processor {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SignProcessor.class);

    @Override
    public ProcessResult process(GeneratedMessageV3 msg, RandomAccessFile raf) {

        try {
            File resultFile = processSign((SignRequest) msg, raf);
            return new ProcessResult(resultFile, SignResponse.newBuilder().build());
        } catch (IOException ex) {
            LOG.warn(ex.getLocalizedMessage(), ex);
            String errorMsg = ex.getLocalizedMessage();
            if (errorMsg != null) {
                return new ProcessResult(null, SignResponse.newBuilder().setResult(1).setMsg(errorMsg).build());
            } else {
                return new ProcessResult(null, SignResponse.newBuilder().setResult(1).build());
            }
        }

    }

    private File processSign(SignRequest signRequest, RandomAccessFile raf) throws IOException {
        SignRequest.Type signType = signRequest.getType();
        File signedFile = null;
        switch (signType) {
            case PAdES:
                signedFile = signPdf(raf, PAdES.B, signRequest.getCertificate(), signRequest.getPin().toCharArray(), signRequest.getSettingsMap());
                break;
            case PAdES_B_T:
                signedFile = signPdf(raf, PAdES.B_T, signRequest.getCertificate(), signRequest.getPin().toCharArray(), signRequest.getSettingsMap());
                break;
            case PAdES_B_LT:
                signedFile = signPdf(raf, PAdES.B_LT, signRequest.getCertificate(), signRequest.getPin().toCharArray(), signRequest.getSettingsMap());
                break;
            case PAdES_B_LTA:
                signedFile = signPdf(raf, PAdES.B_LTA, signRequest.getCertificate(), signRequest.getPin().toCharArray(), signRequest.getSettingsMap());
                break;
        }
        return signedFile;
    }

    private File signPdf(RandomAccessFile raf, PAdES level, String certificate, char[] pin, Map<String, String> settingsMap) throws IOException {
        File tmpSigFile = null, sigFile = null, ltvFile = null, ltvTsFile = null;
        PdfSigner pdfSigner = null;
        OutputStream sigOs = null;

        boolean isError = false;

        try {
            sigFile = File.createTempFile(UUID.randomUUID().toString(), null);
            tmpSigFile = File.createTempFile(UUID.randomUUID().toString(), null);
            LOG.debug("sigFile: " + sigFile);
            LOG.debug("tmpSigFile: " + tmpSigFile);
            switch (level) {
                case B_LT:
                    ltvFile = File.createTempFile(UUID.randomUUID().toString(), null);
                    LOG.debug("ltvFile: " + ltvFile);
                    break;
                case B_LTA:
                    ltvFile = File.createTempFile(UUID.randomUUID().toString(), null);
                    LOG.debug("ltvFile: " + ltvFile);
                    ltvTsFile = File.createTempFile(UUID.randomUUID().toString(), null);
                    LOG.debug("ltvTsFile: " + ltvTsFile);
                    break;
            }

            PdfReader pdfReader = new PdfReader(new RandomAccessSourceFactory().createSource(raf), null);
            sigOs = new FileOutputStream(sigFile);
            pdfSigner = new PdfSigner(pdfReader, sigOs, tmpSigFile.toString(), false);

//TODO: parse settings
//        pdfSigner.getSignatureAppearance()
//                .setReason("Reason")
//                .setLocation("Location");
//        pdfSigner.setCertificationLevel(PdfSigner.CERTIFIED_NO_CHANGES_ALLOWED);
//
            String sigFieldName = pdfSigner.getNewSigFieldName();
            pdfSigner.setFieldName(sigFieldName);

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(new FileInputStream("../" + certificate + ".p12"), pin);
            String alias = ks.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, pin);
            Certificate[] chain = ks.getCertificateChain(alias);

            IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, BouncyCastleProvider.PROVIDER_NAME);
            IExternalDigest digest = new BouncyCastleDigest();

            Collection<ICrlClient> crlClients = null;
            IOcspClient ocspClient = null;
            ITSAClient tsaClient = null;

            if (level == PAdES.B_T || level == PAdES.B_LT || level == PAdES.B_LTA) {
                tsaClient = new TSAClientBouncyCastle("https://freetsa.org/tsr");
            }
            pdfSigner.signDetached(digest, pks, chain, crlClients, ocspClient, tsaClient, 0, PdfSigner.CryptoStandard.CADES);

            if (level == PAdES.B_LT || level == PAdES.B_LTA) {
                crlClients = new HashSet<>();
                ICrlClient crlClientOnline = new CrlClientOnline(chain);
                crlClients.add(crlClientOnline);
                ocspClient = new OcspClientBouncyCastle(new OCSPVerifier(null, null));

                try (PdfDocument document = new PdfDocument(new PdfReader(sigFile), new PdfWriter(ltvFile), new StampingProperties().useAppendMode())) {
                    LtvVerification ltvVerification = new LtvVerification(document);
                    ltvVerification.addVerification(sigFieldName, ocspClient, crlClientOnline, LtvVerification.CertificateOption.WHOLE_CHAIN, LtvVerification.Level.OCSP_OPTIONAL_CRL, LtvVerification.CertificateInclusion.YES);
                    ltvVerification.merge();
                }

                if (level != PAdES.B_LTA) {
                    return ltvFile;
                } else {
                    sigOs = new FileOutputStream(ltvTsFile);
                    pdfSigner = new PdfSigner(new PdfReader(ltvFile), sigOs, tmpSigFile.toString(), true);
                    pdfSigner.timestamp(tsaClient, pdfSigner.getNewSigFieldName());
                    return ltvTsFile;
                }
            }

            return sigFile;
        } catch (IOException ex) {
            isError = true;
            throw ex;
        } catch (RuntimeException | GeneralSecurityException ex) {
            isError = true;
            throw new IOException(ex);
        } finally {
            if (pdfSigner != null) {
                pdfSigner.getDocument().close();
            }
            try {
                raf.close();
            } catch (IOException e) {
            }
            if (sigOs != null) {
                try {
                    sigOs.close();
                } catch (IOException e) {
                }
            }
            if (tmpSigFile != null) {
                tmpSigFile.delete();
                tmpSigFile.deleteOnExit();
            }

            switch (level) {
                case B:
                case B_T:
                    if (isError && sigFile != null) {
                        sigFile.delete();
                        sigFile.deleteOnExit();
                    }
                    break;
                case B_LT:
                    if (isError && ltvFile != null) {
                        ltvFile.delete();
                        ltvFile.deleteOnExit();
                    }
                    if (sigFile != null) {
                        sigFile.delete();
                        sigFile.deleteOnExit();
                    }
                    break;
                case B_LTA:
                    if (isError && ltvTsFile != null) {
                        ltvTsFile.delete();
                        ltvTsFile.deleteOnExit();
                    }
                    if (ltvFile != null) {
                        ltvFile.delete();
                        ltvFile.deleteOnExit();
                    }
                    if (sigFile != null) {
                        sigFile.delete();
                        sigFile.deleteOnExit();
                    }
                    break;
            }
        }
    }

}
