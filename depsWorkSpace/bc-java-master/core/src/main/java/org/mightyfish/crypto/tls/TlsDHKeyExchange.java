package org.mightyfish.crypto.tls;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import org.mightyfish.asn1.x509.KeyUsage;
import org.mightyfish.asn1.x509.SubjectPublicKeyInfo;
import org.mightyfish.crypto.params.AsymmetricKeyParameter;
import org.mightyfish.crypto.params.DHParameters;
import org.mightyfish.crypto.params.DHPrivateKeyParameters;
import org.mightyfish.crypto.params.DHPublicKeyParameters;
import org.mightyfish.crypto.util.PublicKeyFactory;

/**
 * (D)TLS DH key exchange.
 */
public class TlsDHKeyExchange
    extends AbstractTlsKeyExchange
{
    protected TlsSigner tlsSigner;
    protected DHParameters dhParameters;

    protected AsymmetricKeyParameter serverPublicKey;
    protected DHPublicKeyParameters dhAgreeServerPublicKey;
    protected TlsAgreementCredentials agreementCredentials;
    protected DHPrivateKeyParameters dhAgreeClientPrivateKey;

    protected DHPrivateKeyParameters dhAgreeServerPrivateKey;
    protected DHPublicKeyParameters dhAgreeClientPublicKey;

    public TlsDHKeyExchange(int keyExchange, Vector supportedSignatureAlgorithms, DHParameters dhParameters)
    {
        super(keyExchange, supportedSignatureAlgorithms);

        switch (keyExchange)
        {
        case KeyExchangeAlgorithm.DH_RSA:
        case KeyExchangeAlgorithm.DH_DSS:
            this.tlsSigner = null;
            break;
        case KeyExchangeAlgorithm.DHE_RSA:
            this.tlsSigner = new TlsRSASigner();
            break;
        case KeyExchangeAlgorithm.DHE_DSS:
            this.tlsSigner = new TlsDSSSigner();
            break;
        default:
            throw new IllegalArgumentException("unsupported key exchange algorithm");
        }

        this.dhParameters = dhParameters;
    }

    public void init(TlsContext context)
    {
        super.init(context);

        if (this.tlsSigner != null)
        {
            this.tlsSigner.init(context);
        }
    }

    public void skipServerCredentials()
        throws IOException
    {
        throw new TlsFatalAlert(AlertDescription.unexpected_message);
    }

    public void processServerCertificate(Certificate serverCertificate)
        throws IOException
    {
        if (serverCertificate.isEmpty())
        {
            throw new TlsFatalAlert(AlertDescription.bad_certificate);
        }

        org.mightyfish.asn1.x509.Certificate x509Cert = serverCertificate.getCertificateAt(0);

        SubjectPublicKeyInfo keyInfo = x509Cert.getSubjectPublicKeyInfo();
        try
        {
            this.serverPublicKey = PublicKeyFactory.createKey(keyInfo);
        }
        catch (RuntimeException e)
        {
            throw new TlsFatalAlert(AlertDescription.unsupported_certificate, e);
        }

        if (tlsSigner == null)
        {
            try
            {
                this.dhAgreeServerPublicKey = TlsDHUtils.validateDHPublicKey((DHPublicKeyParameters)this.serverPublicKey);
            }
            catch (ClassCastException e)
            {
                throw new TlsFatalAlert(AlertDescription.certificate_unknown, e);
            }

            TlsUtils.validateKeyUsage(x509Cert, KeyUsage.keyAgreement);
        }
        else
        {
            if (!tlsSigner.isValidPublicKey(this.serverPublicKey))
            {
                throw new TlsFatalAlert(AlertDescription.certificate_unknown);
            }

            TlsUtils.validateKeyUsage(x509Cert, KeyUsage.digitalSignature);
        }

        super.processServerCertificate(serverCertificate);
    }

    public boolean requiresServerKeyExchange()
    {
        switch (keyExchange)
        {
        case KeyExchangeAlgorithm.DHE_DSS:
        case KeyExchangeAlgorithm.DHE_RSA:
        case KeyExchangeAlgorithm.DH_anon:
            return true;
        default:
            return false;
        }
    }

    public void validateCertificateRequest(CertificateRequest certificateRequest)
        throws IOException
    {
        short[] types = certificateRequest.getCertificateTypes();
        for (int i = 0; i < types.length; ++i)
        {
            switch (types[i])
            {
            case ClientCertificateType.rsa_sign:
            case ClientCertificateType.dss_sign:
            case ClientCertificateType.rsa_fixed_dh:
            case ClientCertificateType.dss_fixed_dh:
            case ClientCertificateType.ecdsa_sign:
                break;
            default:
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }
    }

    public void processClientCredentials(TlsCredentials clientCredentials)
        throws IOException
    {
        if (clientCredentials instanceof TlsAgreementCredentials)
        {
            // TODO Validate client cert has matching parameters (see 'areCompatibleParameters')?

            this.agreementCredentials = (TlsAgreementCredentials)clientCredentials;
        }
        else if (clientCredentials instanceof TlsSignerCredentials)
        {
            // OK
        }
        else
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public void generateClientKeyExchange(OutputStream output)
        throws IOException
    {
        /*
         * RFC 2246 7.4.7.2 If the client certificate already contains a suitable Diffie-Hellman
         * key, then Yc is implicit and does not need to be sent again. In this case, the Client Key
         * Exchange message will be sent, but will be empty.
         */
        if (agreementCredentials == null)
        {
            this.dhAgreeClientPrivateKey = TlsDHUtils.generateEphemeralClientKeyExchange(context.getSecureRandom(),
                dhAgreeServerPublicKey.getParameters(), output);
        }
    }

    public byte[] generatePremasterSecret()
        throws IOException
    {
        if (agreementCredentials != null)
        {
            return agreementCredentials.generateAgreement(dhAgreeServerPublicKey);
        }

        if (dhAgreeServerPrivateKey != null)
        {
            return TlsDHUtils.calculateDHBasicAgreement(dhAgreeClientPublicKey, dhAgreeServerPrivateKey);
        }

        if (dhAgreeClientPrivateKey != null)
        {
            return TlsDHUtils.calculateDHBasicAgreement(dhAgreeServerPublicKey, dhAgreeClientPrivateKey);
        }

        throw new TlsFatalAlert(AlertDescription.internal_error);
    }
}
