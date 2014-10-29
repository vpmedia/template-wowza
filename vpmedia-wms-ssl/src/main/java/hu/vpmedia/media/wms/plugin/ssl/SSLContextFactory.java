package hu.vpmedia.media.wms.plugin.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Factory to create a bougus SSLContext.
 *
 * @author The Apache Directory Project (mina-dev@directory.apache.org)
 * @version $Rev: 451854 $, $Date: 2006-10-02 11:30:11 +0900 (월, 02 10월 2006) $
 */
public class SSLContextFactory
{
    /**
     * Protocol to use.
     */
    private static final String PROTOCOL = "TLS";

    private static final String KEY_MANAGER_FACTORY_ALGORITHM;

    static {
        String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );
        if( algorithm == null )
        {
            algorithm = "SunX509";
        }

        KEY_MANAGER_FACTORY_ALGORITHM = algorithm;
    }

    /**
     * Bougus Server certificate keystore file name.
     */
    private static final String SSL_KEYSTORE = "vpmedia.jks";

    /**
     * SSL keystore password.
     */
    private static final char[] SSL_PW = { 'v', 'p', 'm', 'e', 'd', 'i', 'a' };

    private static SSLContext serverInstance = null;
    private static SSLContext clientInstance = null;

    /**
     * Get SSLContext singleton.
     *
     * @return SSLContext
     * @throws java.security.GeneralSecurityException
     *
     */
    public static SSLContext getInstance( boolean server )
            throws GeneralSecurityException
    {
        SSLContext retInstance = null;

        if( server )
        {
            if( serverInstance == null )
            {
                synchronized( SSLContextFactory.class )
                {
                    if( serverInstance == null )
                    {
                        try
                        {
                            serverInstance = createBougusServerSSLContext();
                        }
                        catch( Exception ioe )
                        {
                            throw new GeneralSecurityException(
                                    "Can't create Server SSLContext:" + ioe );
                        }
                    }
                }
            }
            retInstance = serverInstance;
        }
        else
        {
            if( clientInstance == null )
            {
                synchronized( SSLContextFactory.class )
                {
                    if( clientInstance == null )
                    {
                        clientInstance = createBougusClientSSLContext();
                    }
                }
            }
            retInstance = clientInstance;
        }
        return retInstance;
    }

    private static SSLContext createBougusServerSSLContext()
            throws GeneralSecurityException, IOException
    {
        // Create keystore
        KeyStore ks = KeyStore.getInstance( "JKS" );
        InputStream in = null;
        try
        {
            in = SSLContextFactory.class
                    .getResourceAsStream( SSL_KEYSTORE );
            ks.load( in, SSL_PW );
        }
        finally
        {
            if( in != null )
            {
                try
                {
                    in.close();
                }
                catch( IOException ignored )
                {
                }
            }
        }

        // Set up key manager factory to use our key store
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                KEY_MANAGER_FACTORY_ALGORITHM );
        kmf.init( ks, SSL_PW );

        // Initialize the SSLContext to work with our key managers.
        SSLContext sslContext = SSLContext.getInstance( PROTOCOL );
        sslContext.init( kmf.getKeyManagers(),
        		TrustManagerFactory.X509_MANAGERS, null );

        return sslContext;
    }

    private static SSLContext createBougusClientSSLContext()
            throws GeneralSecurityException
    {
        SSLContext context = SSLContext.getInstance( PROTOCOL );
        context.init( null, TrustManagerFactory.X509_MANAGERS, null );
        return context;
    }

}
