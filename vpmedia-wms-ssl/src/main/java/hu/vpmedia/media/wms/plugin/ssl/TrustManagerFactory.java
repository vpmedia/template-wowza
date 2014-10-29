package hu.vpmedia.media.wms.plugin.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactorySpi;
import javax.net.ssl.X509TrustManager;

/**
 * Bogus trust manager factory. Creates BogusX509TrustManager
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 451854 $, $Date: 2006-10-02 11:30:11 +0900 (월, 02 10월 2006) $
 */
class TrustManagerFactory extends TrustManagerFactorySpi
{

    static final X509TrustManager X509 = new X509TrustManager()
    {
        public void checkClientTrusted( X509Certificate[] x509Certificates,
                                       String s ) throws CertificateException
        {
        }

        public void checkServerTrusted( X509Certificate[] x509Certificates,
                                       String s ) throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return new X509Certificate[ 0 ];
        }
    };

    static final TrustManager[] X509_MANAGERS = new TrustManager[] { X509 };

    public TrustManagerFactory()
    {
    }

    protected TrustManager[] engineGetTrustManagers()
    {
        return X509_MANAGERS;
    }

    protected void engineInit( KeyStore keystore ) throws KeyStoreException
    {
        // noop
    }

    protected void engineInit(
                              ManagerFactoryParameters managerFactoryParameters )
            throws InvalidAlgorithmParameterException
    {
        // noop
    }
}
