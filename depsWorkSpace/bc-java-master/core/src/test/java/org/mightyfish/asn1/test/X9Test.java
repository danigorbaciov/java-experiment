package org.mightyfish.asn1.test;

import java.math.BigInteger;

import org.mightyfish.asn1.ASN1OctetString;
import org.mightyfish.asn1.ASN1Primitive;
import org.mightyfish.asn1.DEROctetString;
import org.mightyfish.asn1.pkcs.PrivateKeyInfo;
import org.mightyfish.asn1.sec.ECPrivateKey;
import org.mightyfish.asn1.x509.AlgorithmIdentifier;
import org.mightyfish.asn1.x509.SubjectPublicKeyInfo;
import org.mightyfish.asn1.x9.X962NamedCurves;
import org.mightyfish.asn1.x9.X962Parameters;
import org.mightyfish.asn1.x9.X9ECParameters;
import org.mightyfish.asn1.x9.X9ECPoint;
import org.mightyfish.asn1.x9.X9IntegerConverter;
import org.mightyfish.asn1.x9.X9ObjectIdentifiers;
import org.mightyfish.math.ec.ECPoint;
import org.mightyfish.util.Arrays;
import org.mightyfish.util.encoders.Base64;
import org.mightyfish.util.test.SimpleTest;

public class X9Test
    extends SimpleTest
{
    private byte[] namedPub = Base64.decode("MDcwEwYHKoZIzj0CAQYIKoZIzj0DAQEDIAADG5xRI+Iki/JrvL20hoDUa7Cggzorv5B9yyqSMjYu");
    private byte[] expPub = Base64.decode(
                "MIH8MIHXBgcqhkjOPQIBMIHLAgEBMCkGByqGSM49AQECHn///////////////3///////4AAAA" +
                "AAAH///////zBXBB5///////////////9///////+AAAAAAAB///////wEHiVXBfoqMGZUsfTL" +
                "A9anUKMMJQEC1JiHF9m6FattPgMVAH1zdBaP/jRxtgqFdoahlHXTv6L/BB8DZ2iujhi7ks/PAF" +
                "yUmqLG2UhT0OZgu/hUsclQX+laAh5///////////////9///+XXetBs6YFfDxDIUZSZVECAQED" +
                "IAADG5xRI+Iki/JrvL20hoDUa7Cggzorv5B9yyqSMjYu");

    private byte[] namedPriv = Base64.decode("MCICAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQEECDAGAgEBBAEK");
    private byte[] expPriv = Base64.decode(
                "MIHnAgEAMIHXBgcqhkjOPQIBMIHLAgEBMCkGByqGSM49AQECHn///////////////3///////4"
              + "AAAAAAAH///////zBXBB5///////////////9///////+AAAAAAAB///////wEHiVXBfoqMGZU"
              + "sfTLA9anUKMMJQEC1JiHF9m6FattPgMVAH1zdBaP/jRxtgqFdoahlHXTv6L/BB8DZ2iujhi7ks"
              + "/PAFyUmqLG2UhT0OZgu/hUsclQX+laAh5///////////////9///+XXetBs6YFfDxDIUZSZVEC"
              + "AQEECDAGAgEBBAEU");
 
    private void encodePublicKey()
        throws Exception
    {
        X9ECParameters          ecP = X962NamedCurves.getByOID(X9ObjectIdentifiers.prime239v3);

        X9IntegerConverter conv = new X9IntegerConverter();

        if (conv.getByteLength(ecP.getCurve()) != 30)
        {
            fail("wrong byte length reported for curve");
        }

        if (ecP.getCurve().getFieldSize() != 239)
        {
            fail("wrong field size reported for curve");
        }

        //
        // named curve
        //
        X962Parameters          params = new X962Parameters(X9ObjectIdentifiers.prime192v1);
        ECPoint                 point = ecP.getG().multiply(BigInteger.valueOf(100));

        ASN1OctetString         p = new DEROctetString(point.getEncoded(true));

        SubjectPublicKeyInfo    info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), p.getOctets());
        if (!areEqual(info.getEncoded(), namedPub))
        {
            fail("failed public named generation");
        }

        X9ECPoint               x9P = new X9ECPoint(ecP.getCurve(), p);

        if (!Arrays.areEqual(p.getOctets(), x9P.getPoint().getEncoded()))
        {
            fail("point encoding not preserved");
        }

        ASN1Primitive           o = ASN1Primitive.fromByteArray(namedPub);
        
        if (!info.equals(o))
        {
            fail("failed public named equality");
        }
        
        //
        // explicit curve parameters
        //
        params = new X962Parameters(ecP);
        
        info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), p.getOctets());

        if (!areEqual(info.getEncoded(), expPub))
        {
            fail("failed public explicit generation");
        }

        o = ASN1Primitive.fromByteArray(expPub);
        
        if (!info.equals(o))
        {
            fail("failed public explicit equality");
        }
    }
    
    private void encodePrivateKey()
        throws Exception
    {
        X9ECParameters          ecP = X962NamedCurves.getByOID(X9ObjectIdentifiers.prime239v3);

        //
        // named curve
        //
        X962Parameters          params = new X962Parameters(X9ObjectIdentifiers.prime192v1);

        PrivateKeyInfo          info = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), new ECPrivateKey(BigInteger.valueOf(10)));

        if (!areEqual(info.getEncoded(), namedPriv))
        {
            fail("failed private named generation");
        }

        ASN1Primitive           o = ASN1Primitive.fromByteArray(namedPriv);

        if (!info.equals(o))
        {
            fail("failed private named equality");
        }
        
        //
        // explicit curve parameters
        //
        params = new X962Parameters(ecP);
        
        info = new PrivateKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, params), new ECPrivateKey(BigInteger.valueOf(20)));

        if (!areEqual(info.getEncoded(), expPriv))
        {
            fail("failed private explicit generation");
        }

        o = ASN1Primitive.fromByteArray(expPriv);
        
        if (!info.equals(o))
        {
            fail("failed private explicit equality");
        }
    }
    
    public void performTest()
        throws Exception
    {
        encodePublicKey();
        encodePrivateKey();
    }

    public String getName()
    {
        return "X9";
    }

    public static void main(
        String[]    args)
    {
        runTest(new X9Test());
    }
}
