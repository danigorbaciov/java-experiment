package org.mightyfish.jce.exception;

import org.mightyfish.jce.cert.CertPath;
import org.mightyfish.jce.cert.CertPathBuilderException;

public class ExtCertPathBuilderException
    extends CertPathBuilderException
    implements ExtException
{
    private Throwable cause;

    public ExtCertPathBuilderException(String message, Throwable cause)
    {
        super(message);
        this.cause = cause;
    }

    public ExtCertPathBuilderException(String msg, Throwable cause, 
        CertPath certPath, int index)
    {
        super(msg, cause);
        this.cause = cause;
    }
    
    public Throwable getCause()
    {
        return cause;
    }
}
