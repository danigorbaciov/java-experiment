package org.mightyfish.cert.selector.jcajce;

import java.io.IOException;
import java.math.BigInteger;
import org.mightyfish.jce.cert.X509CertSelector;

import org.mightyfish.asn1.DEROctetString;
import org.mightyfish.asn1.x500.X500Name;
import org.mightyfish.cert.selector.X509CertificateHolderSelector;

public class JcaX509CertSelectorConverter
{
    public JcaX509CertSelectorConverter()
    {
    }

    protected X509CertSelector doConversion(X500Name issuer, BigInteger serialNumber, byte[] subjectKeyIdentifier)
    {
        X509CertSelector selector = new X509CertSelector();

        if (issuer != null)
        {
            try
            {
                selector.setIssuer(issuer.getEncoded());
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("unable to convert issuer: " + e.getMessage());
            }
        }

        if (serialNumber != null)
        {
            selector.setSerialNumber(serialNumber);
        }

        if (subjectKeyIdentifier != null)
        {
            try
            {
                selector.setSubjectKeyIdentifier(new DEROctetString(subjectKeyIdentifier).getEncoded());
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("unable to convert issuer: " + e.getMessage());
            }
        }

        return selector;
    }

    public X509CertSelector getCertSelector(X509CertificateHolderSelector holderSelector)
    {
        return doConversion(holderSelector.getIssuer(), holderSelector.getSerialNumber(), holderSelector.getSubjectKeyIdentifier());
    }
}
