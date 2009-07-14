/***************************************
*                                     *
*  JBoss: The OpenSource J2EE WebOS   *
*                                     *
*  Distributable under LGPL license.  *
*  See terms of license at gnu.org.   *
*                                     *
***************************************/

package org.jboss.mx.loading;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Map;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.mx.logging.Logger;

/**
* A ClassLoader which loads classes from a single URL in conjunction with
* the {@link LoaderRepository}. Notice that this classloader does
* not work independently without the repository. A repository reference
* must be provided via the constructor or the classloader must be explicitly
* registered to the repository before any attempt to load a class.
*
* @author <a href="marc.fleury@jboss.org">Marc Fleury</a>
* @author <a href="christoph.jung@jboss.org">Christoph G. Jung</a>
* @author <a href="scott.stark@jboss.org">Scott Stark</a>
* @author <a href="juha@jboss.org">Juha Lindfors</a>
* @version <tt>$Revision: 1.1.1.1 $</tt>
*
* <p><b>20011009 cgj:</b>
* <ul>
*   <li>fixed default resolution behaviour
* </ul>
*
* <p><b>20020315 Juha Lindfors:</b>
* <ul>
*  <li> Support for adding your own class definitions instead of just an URL.</li>
*
*  <li> Removed static reference to repository. This is now set as the classloader
*       is registered to the repository via setRepository() call.
*  </li>
*/
public class UnifiedClassLoader
   extends URLClassLoader
   implements UnifiedClassLoaderMBean
{
   // Static --------------------------------------------------------

   private static final Logger log = Logger.getLogger(UnifiedClassLoader.class);

   // Attributes ----------------------------------------------------

   /** Reference to the unified repository. */
   protected LoaderRepository repository = null;

   /** One URL per ClassLoader in our case */
   protected URL url = null;
   /** An optional URL from which url may have been copied. It is used to
    allow the security permissions to be based on a static url namespace. */
   protected URL origURL = null;

   /** Class definitions */
   private Map classes = new ConcurrentReaderHashMap();

   // Constructors --------------------------------------------------
   /**
    * Construct a <tt>UnifiedClassLoader</tt> without registering it to the
    * classloader repository.
    *
    * @param url   the single URL to load classes from.
    */
   public UnifiedClassLoader(URL url)
   {
      this(url, (URL) null);
   }
   /**
    * Construct a <tt>UnifiedClassLoader</tt> without registering it to the
    * classloader repository.
    *
    * @param url   the single URL to load classes from.
    * @param origURL the possibly null original URL from which url may
    * be a local copy or nested jar.
    */
   public UnifiedClassLoader(URL url, URL origURL)
   {
      super(new URL[] {url}, UnifiedClassLoader.class.getClassLoader());
      
      log.debug("New jmx UCL with url " + url);
      this.url = url;
      this.origURL = origURL;
   }

   /**
    * Construct a <tt>UnifiedClassLoader</tt> and registers it to the given
    * repository.
    *
    * @param   url   The single URL to load classes from.
    * @param   repository the repository this classloader delegates to
    */
   public UnifiedClassLoader(URL url, LoaderRepository repository)
   {
      this(url, null, repository);
   }
   /**
    * Construct a <tt>UnifiedClassLoader</tt> and registers it to the given
    * repository.
    * @param url The single URL to load classes from.
    * @param origURL the possibly null original URL from which url may
    * be a local copy or nested jar.
    * @param repository the repository this classloader delegates to
    * be a local copy or nested jar.
    */
   public UnifiedClassLoader(URL url, URL origURL, LoaderRepository repository)
   {
      this(url, origURL);
      
      // set the repository reference
      this.repository = repository;

      // register this loader to the given repository
      repository.addClassLoader(this);
   }

   /**
    * UnifiedClassLoader constructor that can be used to 
    * register with a particular Loader Repository identified by ObjectName.
    *
    * @param url an <code>URL</code> value
    * @param server a <code>MBeanServer</code> value
    * @param repositoryName an <code>ObjectName</code> value
    * @exception Exception if an error occurs
    */
   public UnifiedClassLoader(final URL url, final MBeanServer server,
      final ObjectName repositoryName) throws Exception
   {
      this(url, null, server, repositoryName);
   }
   /**
    * UnifiedClassLoader constructor that can be used to 
    * register with a particular Loader Repository identified by ObjectName.
    *
    * @param url an <code>URL</code> value
    * @param origURL the possibly null original URL from which url may
    * be a local copy or nested jar.
    * @param server a <code>MBeanServer</code> value
    * @param repositoryName an <code>ObjectName</code> value
    * @exception Exception if an error occurs
    */
   public UnifiedClassLoader(final URL url, final URL origURL,
      final MBeanServer server, final ObjectName repositoryName) throws Exception
   {
      this(url, origURL);
      this.repository = (LoaderRepository)server.invoke(repositoryName,
                    "registerClassLoader", 
                    new Object[] {this}, 
                    new String[] {getClass().getName()});
   }


   /**
    * Constructs a <tt>UnifiedClassLoader</tt> with given class definition.
    *
    * @param   names class name
    * @param   codes class definition
    */
   public UnifiedClassLoader(String name, byte[] code)
   {
      // FIXME: UCL cloader or ctx cl as parent??
      super(new URL[] {}, UnifiedClassLoader.class.getClassLoader());
      addClass(name, code); 
   }


   // Public --------------------------------------------------------

   /** Obtain the ObjectName under which the UCL can be registered with the
    JMX server. This creates a name of the form "jmx.loading:UCL=hashCode"
    since we don't currently care that UCL be easily queriable.
    */
   public ObjectName getObjectName() throws MalformedObjectNameException
   {
      return new ObjectName("jmx.loading:UCL="+super.hashCode());
   }

   public void unregister()
   {
      if (repository != null)
      {
         repository.removeClassLoader(this);
      }
   }


   public void addClass(String name, byte[] code)
   {
      classes.put(name, code);
   }

   public void setRepository(LoaderRepository repository)
   {
      this.repository = repository;
   }

   /**
   *
   */
   protected Class findClass(String name) throws ClassNotFoundException
   {
      Object o = classes.get(name);

      if (o != null)
      {
         byte[] code = (byte[])o;
         classes.remove(name);
         return defineClass(name, code, 0, code.length);
      }

      else return super.findClass(name);
   }

   /**
   */
   public Class loadClassLocally(String name, boolean resolve)
   throws ClassNotFoundException
   {
      return super.loadClass(name, resolve);
   }

   /**
   * Provides the same functionality as {@link java.net.URLClassLoader#getResource}.
   */
   public URL getResourceLocally(String name)
   {
      return super.getResource(name);
   }

   /** Get the URL associated with the UCL.
    */
   public URL getURL()
   {
      return url;
   }
   /** Get the original URL associated with the UCL. This may be null.
    */
   public URL getOrigURL()
   {
      return origURL;
   }

   /**
   * This method simply invokes the super.getURLs() method to access the
   * list of URLs that make up the UnifiedClassLoader classpath.
   */
   public URL[] getClasspath()
   {
      return super.getURLs();
   }

   // URLClassLoader overrides --------------------------------------

   /**
   * We intercept the load class to know exactly the dependencies
   * of the underlying jar.
   *
   * <p>Forwards request to {@link LoaderRepository}.
   */
   public Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException
   {
      // We delegate the repository.
      // This call is not wrapped into a synchronized (this) block;
      // however, this method is called often as a result of a class resolution
      // during defineClass0() (native) or loadClassInternal() (called by the JVM)
      // that may already have synchronized on 'this'
      return repository.loadClass(name, resolve, this);
   }

   /**
   * Attempts to load the resource from its URL and if not found
   * forwards to the request to {@link LoaderRepository}.
   */
   public URL getResource(String name)
   {
      // We delegate the repository.
      // The repository will decide the strategy to apply
      // when looking for a resource

      return repository.getResource(name, this);
   }

   /** This is here to document that this must delegate to the
   super implementation to perform identity based hashing. Using
   URL based hashing caused conflicts with the Class.forName(String,
   boolean, ClassLoader).
   */
   public final int hashCode()
   {
      return super.hashCode();
   }

   /** This is here to document that this must delegate to the
   super implementation to perform identity based equality. Using
   URL based equality caused conflicts with the Class.forName(String,
   boolean, ClassLoader).
   */
   public final boolean equals(Object other)
   {
      return super.equals(other);
   }
   /**
   * Return all library URLs associated with this UnifiedClassLoader
   *
   * <p>Do not remove this method without running the WebIntegrationTestSuite
   */
   public URL[] getAllURLs()
   {
      return repository.getURLs();
   }

   /**
   * Return an empty URL array to force the RMI marshalling subsystem to
   * use the <tt>java.server.codebase</tt> property as the annotated codebase.
   *
   * <p>Do not remove this method without discussing it on the dev list.
   *
   * @return Empty URL[]
   */
   public URL[] getURLs()
   {
      return EMPTY_URL_ARRAY;
   }

   public Package getPackage(String name)
   {
      return super.getPackage(name);
   }
   public Package[] getPackages()
   {
      return super.getPackages();
   }

   /** The value returned by {@link getURLs}. */
   private static final URL[] EMPTY_URL_ARRAY = {};


   /**
   * Retruns a string representaion of this UCL.
   */
   public String toString()
   {
      return super.toString() + "{ url=" + getURL() + " }";
   }

   /** Override the permissions accessor to build a code source
    based on the original URL if one exists. This allows the
    security policy to be defined in terms of the static URL
    namespace rather than the local copy or nested URL.
    @param cs the location and signatures of the codebase.
    */
   protected PermissionCollection getPermissions(CodeSource cs)
   {
      CodeSource permCS = cs;
      if( origURL != null )
      {
         permCS = new CodeSource(origURL, cs.getCertificates());
      }
      PermissionCollection perms = super.getPermissions(permCS);
      if( log.isTraceEnabled() )
         log.trace("getPermissions, url="+url+", origURL="+origURL+" -> "+perms);
      return perms;
   }

}
