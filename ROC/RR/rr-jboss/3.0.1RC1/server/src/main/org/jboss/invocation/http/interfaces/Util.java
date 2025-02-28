/*
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.invocation.http.interfaces;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.jboss.invocation.Invocation;
import org.jboss.invocation.MarshalledValue;
import org.jboss.logging.Logger;
import org.jboss.security.SecurityAssociationAuthenticator;

/** Common client utility methods
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1.1.1 $
*/
public class Util
{
   /** A serialized MarshalledInvocation */
   private static String REQUEST_CONTENT_TYPE =
      "application/x-java-serialized-object; class=org.jboss.invocation.MarshalledInvocation";
   private static Logger log = Logger.getLogger(Util.class);

   static
   {
      // Install the java.net.Authenticator to use
      try
      {
         Authenticator.setDefault(new SecurityAssociationAuthenticator());
      }
      catch(Exception e)
      {
         log.warn("Failed to install SecurityAssociationAuthenticator", e);
      }
   }

   /** Post the Invocation as a serialized MarshalledInvocation object. This is
    using the URL class for now but this should be improved to a cluster aware
    layer with full usage of HTTP 1.1 features, pooling, etc.
   */
   public static Object invoke(URL externalURL, Invocation mi)
      throws Exception
   {
      /* Post the MarshalledInvocation data. This is using the URL class
       for now but this should be improved to a cluster aware layer with
       full usage of HTTP 1.1 features, pooling, etc.
       */
      HttpURLConnection conn = (HttpURLConnection) externalURL.openConnection();
      if( conn instanceof com.sun.net.ssl.HttpsURLConnection )
      {
         // See if the org.jboss.security.ignoreHttpsHost property is set
         if( Boolean.getBoolean("org.jboss.security.ignoreHttpsHost") == true )
         {
            com.sun.net.ssl.HttpsURLConnection sconn = (com.sun.net.ssl.HttpsURLConnection) conn;
            AnyhostVerifier verifier = new AnyhostVerifier();
            sconn.setHostnameVerifier(verifier);
         }
      }
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestProperty("ContentType", REQUEST_CONTENT_TYPE);
      conn.setRequestMethod("POST");
      OutputStream os = conn.getOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);
      oos.writeObject(mi);
      oos.flush();

      // Get the response MarshalledValue object
      InputStream is = conn.getInputStream();
      ObjectInputStream ois = new ObjectInputStream(is);
      MarshalledValue mv = (MarshalledValue) ois.readObject();
      ois.close();
      oos.close();

      // If the encoded value is an exception throw it
      Object value = mv.get();
      if( value instanceof Exception )
         throw (Exception) value;

      return value;
   }

   /** First try to use the externalURLValue as a URL string and if this
       fails to produce a valid URL treat the externalURLValue as a system
       property name from which to obtain the URL string. This allows the
       proxy url to not be set until the proxy is unmarshalled in the client
       vm, and is necessary when the server is sitting behind a firewall or
       proxy and does not know what its public http interface is named.
   */
   public static URL resolveURL(String urlValue) throws MalformedURLException
   {
      if( urlValue == null )
         return null;

      URL externalURL = null;
      try
      {
         externalURL = new URL(urlValue);
      }
      catch(MalformedURLException e)
      {
         // See if externalURL refers to a property
         String urlProperty = System.getProperty(urlValue);
         if( urlProperty == null )
            throw e;
         externalURL = new URL(urlProperty);
      }
      return externalURL;
   }
}
