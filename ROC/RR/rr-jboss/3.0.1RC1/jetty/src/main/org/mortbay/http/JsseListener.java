// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: JsseListener.java,v 1.1.1.1 2002/10/03 21:07:02 candea Exp $
// ========================================================================

package org.mortbay.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.cert.Certificate;
import org.mortbay.jetty.servlet.ServletSSL;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.ThreadPool;
import org.mortbay.util.ThreadedServer;

/* ------------------------------------------------------------ */
/** JSSE Socket Listener.
 *
 * This specialization of HttpListener is an abstract listener
 * that can be used as the basis for a specific JSSE listener.
 *
 * This is heavily based on the work from Court Demas, which in
 * turn is based on the work from Forge Research.
 *
 * @version $Id: JsseListener.java,v 1.1.1.1 2002/10/03 21:07:02 candea Exp $
 * @author Greg Wilkins (gregw@mortbay.com)
 * @author Court Demas (court@kiwiconsulting.com)
 * @author Forge Research Pty Ltd  ACN 003 491 576
 * @author Jan Hlavat�
 **/
public abstract class JsseListener extends SocketListener
{
    /** String name of keystore location path property. */
    public static final String KEYSTORE_PROPERTY = "jetty.ssl.keystore";
    
    /** String name of keystore type property */
    public static final String KEYSTORE_TYPE_PROPERTY = "jetty.ssl.keystore.type";
    
    /** Default keystore type */
    public static final String DEFAULT_KEYSTORE_TYPE = System.getProperty(KEYSTORE_TYPE_PROPERTY, KeyStore.getDefaultType());

    /** String name of keystore provider name property */
    public static final String KEYSTORE_PROVIDER_NAME_PROPERTY = "jetty.ssl.keystore.provider.name";

    /** String name of keystore provider class property */
    public static final String KEYSTORE_PROVIDER_CLASS_PROPERTY = "jetty.ssl.keystore.provider.class";

    /** Default value for keystore provider class. null = use default */
    public static final String DEFAULT_KEYSTORE_PROVIDER_CLASS = System.getProperty(KEYSTORE_PROVIDER_CLASS_PROPERTY);

    /** Default value for the keystore location path. */
    public static final String DEFAULT_KEYSTORE  =
        System.getProperty("user.home" ) + File.separator + ".keystore";

    /** Default value for keystore provider name. null = use default */
    public static final String DEFAULT_KEYSTORE_PROVIDER_NAME = System.getProperty(KEYSTORE_PROVIDER_NAME_PROPERTY);

    /** String name of keystore password property. */
    public static final String PASSWORD_PROPERTY = "jetty.ssl.password";
    
    /** String name of key password property. */
    public static final String KEYPASSWORD_PROPERTY = "jetty.ssl.keypassword";
    
    /** The name of the SSLSession attribute that will contain any
     * cached information.
     */
    static final String CACHED_INFO_ATTR
	= CachedInfo.class.getName();
	
    /** Set to true if we require client certificate authentication. */
    private boolean _needClientAuth = false;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param p_address 
     * @param p_server 
     * @exception IOException 
     */
    public JsseListener()
        throws IOException
    {
        super();
        setDefaultScheme(HttpMessage.__SSL_SCHEME);
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param p_address 
     * @param p_server 
     * @param p_minThreads 
     * @param p_maxThreads 
     * @param p_maxIdleTimeMs 
     * @exception IOException 
     */
    public JsseListener( InetAddrPort p_address)
        throws IOException
    {
        super(p_address);
        if( p_address.getPort() == 0 )
        {
            p_address.setPort( 443 );
            setPort(443);
        }
        setDefaultScheme(HttpMessage.__SSL_SCHEME);
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the value of the needClientAuth property
     * @param needClientAuth true iff we require client
     * certificate authentication.
     */
    public void setNeedClientAuth(boolean needClientAuth)
    {
        _needClientAuth = needClientAuth;
    }
    
    /* ------------------------------------------------------------ */
    public boolean getNeedClientAuth()
    {
        return _needClientAuth;
    }

    /* ------------------------------------------------------------ */
    public boolean isIntegral(HttpConnection connection)
    {
        return true;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isConfidential(HttpConnection connection)
    {
        return true;
    }
    
    /* ------------------------------------------------------------ */
    protected abstract SSLServerSocketFactory createFactory()
        throws Exception;
        
    /* ------------------------------------------------------------ */
    /** 
     * @param p_address 
     * @param p_acceptQueueSize 
     * @return 
     * @exception IOException 
     */
    protected ServerSocket newServerSocket( InetAddrPort p_address,
                                            int p_acceptQueueSize )
        throws IOException
    {
        SSLServerSocketFactory factory = null;
        SSLServerSocket socket = null;

        try
        {
            factory = createFactory();
            
            if( p_address == null )
            {
                socket = (SSLServerSocket)
                    factory.createServerSocket(0, p_acceptQueueSize );
            }
            else
            {
                socket = (SSLServerSocket)
                    factory.createServerSocket(p_address.getPort(),
                                               p_acceptQueueSize,
                                               p_address.getInetAddress() );
            }

	    socket.setNeedClientAuth(_needClientAuth);
	    Log.event("JsseListener.needClientAuth=" + _needClientAuth);
	}
        catch (IOException e)
        {
            throw e;
        }
        catch( Exception e )
        {
            Code.warning(e);
            throw new IOException("Could not create JsseListener: "
				  +e.toString());
        }
        return socket;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param p_serverSocket 
     * @return 
     * @exception IOException 
     */
    protected Socket accept( ServerSocket p_serverSocket )
        throws IOException
    {
        try
        {
            SSLSocket s = (SSLSocket) p_serverSocket.accept();
            if (getMaxIdleTimeMs()>0)
                s.setSoTimeout(getMaxIdleTimeMs());
            s.startHandshake();  // block until SSL handshaking is done
            return (Socket) s;
        }
        catch( SSLException e )
        {
            Code.warning(e);
            throw new IOException( e.getMessage() );
        }
    }

    /* ------------------------------------------------------------ */
    /** Allow the Listener a chance to customise the request.
     * before the server does its stuff.
     * <br> This allows the required  attributes to be set for SSL
     * requests.
     * <br> The requirements of the Servlet specs are:
     * <ul>
     * <li> an attribute named "javax.servlet.request.cipher_suite" of
     * type String.</li>
     * <li> an attribute named "javax.servlet.request.key_size" of
     * type Integer.</li>
     * <li> an attribute named "javax.servlet.request.X509Certificate" of
     * type java.security.cert.X509Certificate[].
     * This is an array of objects of type X509Certificate, the order
     * of this array is defined as being in ascending order of
     * trust. The first certificate in the chain is the one set by the
     * client, the next is the one used to authenticate the first, and
     * so on.
     * </li>
     * </ul>
     *
     * @param socket The Socket the request arrived on. This should be
     * a javax.net.ssl.SSLSocket.
     * @param request HttpRequest to be customised.
     */
    protected void customizeRequest(Socket socket,
                                    HttpRequest request)
    {
        super.customizeRequest(socket,request);
        
        if (!(socket instanceof javax.net.ssl.SSLSocket))
            return; // I'm tempted to let it throw an exception...

        try
        {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLSession sslSession = sslSocket.getSession();
	    String cipherSuite = sslSession.getCipherSuite();
	    Integer keySize;
	    X509Certificate[] certs;

	    CachedInfo cachedInfo
		= (CachedInfo) sslSession.getValue(CACHED_INFO_ATTR);
	    if (cachedInfo != null)
	    {
		keySize = cachedInfo.getKeySize();
		certs = cachedInfo.getCerts();
	    }
	    else
	    {
		keySize = new Integer(ServletSSL.deduceKeyLength(cipherSuite));
		certs = getCertChain(sslSession);
		cachedInfo = new CachedInfo(keySize, certs);
		sslSession.putValue(CACHED_INFO_ATTR, cachedInfo);
	    }

	    if (certs != null)
		request.setAttribute("javax.servlet.request.X509Certificate",
				     certs);
	    else if (_needClientAuth) // Sanity check
		throw new HttpException(HttpResponse.__403_Forbidden);
		
            request.setAttribute("javax.servlet.request.cipher_suite",
				 cipherSuite);
	    request.setAttribute("javax.servlet.request.key_size",
				 keySize);
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }

    /**
     * Return the chain of X509 certificates used to negotiate the SSL
     * Session.
     *<p> Note: in order to do this we must 
     * convert a javax.security.cert.X509Certificate[], as used
     * by JSSE to a java.security.cert.X509Certificate[],as required
     * by the Servlet specs.
     * @param sslSession the javax.net.ssl.SSLSession to use as
     * the source of the cert chain.
     * @return the chain of java.security.cert.X509Certificates
     *  used to negotiate the SSL connection.
     *  <br>Will be null if the chain is missing or empty.
     */
    private static X509Certificate[]
	getCertChain(SSLSession sslSession)
    {
	try {
	    javax.security.cert.X509Certificate javaxCerts[]
		= sslSession.getPeerCertificateChain();
	    if (javaxCerts == null || javaxCerts.length == 0)
		return null;

	    int length = javaxCerts.length;
	    X509Certificate[] javaCerts
		= new X509Certificate[length];
	    
	    java.security.cert.CertificateFactory cf =
		java.security.cert.CertificateFactory.getInstance("X.509");
	    for (int i = 0; i < length; i++)
	    {
		byte bytes[] = javaxCerts[i].getEncoded();
		ByteArrayInputStream stream =
		    new ByteArrayInputStream(bytes);
		javaCerts[i] = (X509Certificate)
		    cf.generateCertificate(stream);
	    }

	    return javaCerts;
	} catch (SSLPeerUnverifiedException pue) {
	    return null;
        } catch (Exception e) {
	    Code.warning(e);
            return null;
        }
    }

    /**
     * Simple bundle of information that is cached in the SSLSession.
     * Stores the effective keySize and the client certificate chain.
     */
    private class CachedInfo
    {
	private Integer _keySize;
	private X509Certificate[] _certs;

	CachedInfo(Integer keySize, X509Certificate[] certs)
	{
	    this._keySize = keySize;
	    this._certs = certs;
	}

	Integer getKeySize()
	{
	    return _keySize;
	}

	X509Certificate[] getCerts()
	{
	    return _certs;
	}
    }
}
