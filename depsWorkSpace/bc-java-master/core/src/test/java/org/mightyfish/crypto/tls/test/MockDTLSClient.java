package org.mightyfish.crypto.tls.test;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Vector;

import org.mightyfish.asn1.x509.Certificate;
import org.mightyfish.crypto.tls.AlertDescription;
import org.mightyfish.crypto.tls.AlertLevel;
import org.mightyfish.crypto.tls.CertificateRequest;
import org.mightyfish.crypto.tls.ClientCertificateType;
import org.mightyfish.crypto.tls.DefaultTlsClient;
import org.mightyfish.crypto.tls.MaxFragmentLength;
import org.mightyfish.crypto.tls.ProtocolVersion;
import org.mightyfish.crypto.tls.SignatureAlgorithm;
import org.mightyfish.crypto.tls.SignatureAndHashAlgorithm;
import org.mightyfish.crypto.tls.TlsAuthentication;
import org.mightyfish.crypto.tls.TlsCredentials;
import org.mightyfish.crypto.tls.TlsExtensionsUtils;
import org.mightyfish.crypto.tls.TlsSession;
import org.mightyfish.util.Arrays;
import org.mightyfish.util.encoders.Hex;

public class MockDTLSClient
    extends DefaultTlsClient
{
    protected TlsSession session;

    public MockDTLSClient(TlsSession session)
    {
        this.session = session;
    }

    public TlsSession getSessionToResume()
    {
        return this.session;
    }

    public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Throwable cause)
    {
        PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
        out.println("DTLS client raised alert: " + AlertLevel.getText(alertLevel)
            + ", " + AlertDescription.getText(alertDescription));
        if (message != null)
        {
            out.println(message);
        }
        if (cause != null)
        {
            cause.printStackTrace(out);
        }
    }

    public void notifyAlertReceived(short alertLevel, short alertDescription)
    {
        PrintStream out = (alertLevel == AlertLevel.fatal) ? System.err : System.out;
        out.println("DTLS client received alert: " + AlertLevel.getText(alertLevel)
            + ", " + AlertDescription.getText(alertDescription));
    }

    public ProtocolVersion getClientVersion()
    {
        return ProtocolVersion.DTLSv12;
    }

    public ProtocolVersion getMinimumVersion()
    {
        return ProtocolVersion.DTLSv10;
    }

//    public int[] getCipherSuites()
//    {
//        return Arrays.concatenate(super.getCipherSuites(),
//            new int[]
//            {
//                CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,
//                CipherSuite.TLS_ECDHE_RSA_WITH_ESTREAM_SALSA20_SHA1,
//                CipherSuite.TLS_ECDHE_RSA_WITH_SALSA20_SHA1,
//                CipherSuite.TLS_RSA_WITH_ESTREAM_SALSA20_SHA1,
//                CipherSuite.TLS_RSA_WITH_SALSA20_SHA1,
//            });
//    }

    public Hashtable getClientExtensions() throws IOException
    {
        Hashtable clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(super.getClientExtensions());
        TlsExtensionsUtils.addEncryptThenMACExtension(clientExtensions);
        // TODO[draft-ietf-tls-session-hash-01] Enable once code-point assigned (only for compatible server though)
//        TlsExtensionsUtils.addExtendedMasterSecretExtension(clientExtensions);
        TlsExtensionsUtils.addMaxFragmentLengthExtension(clientExtensions, MaxFragmentLength.pow2_9);
        TlsExtensionsUtils.addTruncatedHMacExtension(clientExtensions);
        return clientExtensions;
    }

    public void notifyServerVersion(ProtocolVersion serverVersion) throws IOException
    {
        super.notifyServerVersion(serverVersion);

        System.out.println("Negotiated " + serverVersion);
    }

    public TlsAuthentication getAuthentication()
        throws IOException
    {
        return new TlsAuthentication()
        {
            public void notifyServerCertificate(org.mightyfish.crypto.tls.Certificate serverCertificate)
                throws IOException
            {
                Certificate[] chain = serverCertificate.getCertificateList();
                System.out.println("Received server certificate chain of length " + chain.length);
                for (int i = 0; i != chain.length; i++)
                {
                    Certificate entry = chain[i];
                    // TODO Create fingerprint based on certificate signature algorithm digest
                    System.out.println("    fingerprint:SHA-256 " + TlsTestUtils.fingerprint(entry) + " ("
                        + entry.getSubject() + ")");
                }
            }

            public TlsCredentials getClientCredentials(CertificateRequest certificateRequest)
                throws IOException
            {
                short[] certificateTypes = certificateRequest.getCertificateTypes();
                if (certificateTypes == null || !Arrays.contains(certificateTypes, ClientCertificateType.rsa_sign))
                {
                    return null;
                }

                SignatureAndHashAlgorithm signatureAndHashAlgorithm = null;
                Vector sigAlgs = certificateRequest.getSupportedSignatureAlgorithms();
                if (sigAlgs != null)
                {
                    for (int i = 0; i < sigAlgs.size(); ++i)
                    {
                        SignatureAndHashAlgorithm sigAlg = (SignatureAndHashAlgorithm)
                            sigAlgs.elementAt(i);
                        if (sigAlg.getSignature() == SignatureAlgorithm.rsa)
                        {
                            signatureAndHashAlgorithm = sigAlg;
                            break;
                        }
                    }

                    if (signatureAndHashAlgorithm == null)
                    {
                        return null;
                    }
                }

                return TlsTestUtils.loadSignerCredentials(context, new String[] { "x509-client.pem", "x509-ca.pem" },
                    "x509-client-key.pem", signatureAndHashAlgorithm);
            }
        };
    }

    public void notifyHandshakeComplete() throws IOException
    {
        super.notifyHandshakeComplete();

        TlsSession newSession = context.getResumableSession();
        if (newSession != null)
        {
            byte[] newSessionID = newSession.getSessionID();
            String hex = Hex.toHexString(newSessionID);

            if (this.session != null && Arrays.areEqual(this.session.getSessionID(), newSessionID))
            {
                System.out.println("Resumed session: " + hex);
            }
            else
            {
                System.out.println("Established session: " + hex);
            }

            this.session = newSession;
        }
    }
}
