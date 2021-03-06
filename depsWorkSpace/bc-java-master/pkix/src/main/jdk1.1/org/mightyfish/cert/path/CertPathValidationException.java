package org.mightyfish.cert.path;

public class CertPathValidationException
    extends Exception
{
    private Exception cause;

    public CertPathValidationException(String msg)
    {
        this(msg, null);
    }

    public CertPathValidationException(String msg, Exception cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
