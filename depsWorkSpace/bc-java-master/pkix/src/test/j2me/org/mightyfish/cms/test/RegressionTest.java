package org.mightyfish.cms.test;

import org.mightyfish.util.test.Test;
import org.mightyfish.util.test.TestResult;

public class RegressionTest
{
    public static Test[]    tests = {
        new BcEnvelopedDataTest(),
        new BcSignedDataTest()
    };

    public static void main(
        String[]    args)
    {
        for (int i = 0; i != tests.length; i++)
        {
            TestResult  result = tests[i].perform();
            
            if (result.getException() != null)
            {
                result.getException().printStackTrace();
            }
            
            System.out.println(result);
        }
    }
}

