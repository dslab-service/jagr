/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.mx.loading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.mx.logging.Logger;

/** Utility methods for class loader to package names, etc.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1.1.1 $
 */
public class ClassLoaderUtils
{
   private static Logger log = Logger.getLogger(ClassLoaderUtils.class);


   /** Format a string buffer containing the Class, Interfaces, CodeSource,
    and ClassLoader information for the given object clazz.

    @param clazz the Class
    @param results, the buffer to write the info to
    */
   public static void displayClassInfo(Class clazz, StringBuffer results)
   {
      // Print out some codebase info for the ProbeHome
      ClassLoader cl = clazz.getClassLoader();
      results.append("\n"+clazz.getName()+"("+Integer.toHexString(clazz.hashCode())+").ClassLoader="+cl);
      ClassLoader parent = cl;
      while( parent != null )
      {
         results.append("\n.."+parent);
         URL[] urls = getClassLoaderURLs(parent);
         int length = urls != null ? urls.length : 0;
         for(int u = 0; u < length; u ++)
         {
            results.append("\n...."+urls[u]);
         }
         if( parent != null )
            parent = parent.getParent();
      }
      CodeSource clazzCS = clazz.getProtectionDomain().getCodeSource();
      if( clazzCS != null )
         results.append("\n++++CodeSource: "+clazzCS);
      else
         results.append("\n++++Null CodeSource");

      results.append("\nImplemented Interfaces:");
      Class[] ifaces = clazz.getInterfaces();
      for(int i = 0; i < ifaces.length; i ++)
      {
         Class iface = ifaces[i];
         results.append("\n++"+iface+"("+Integer.toHexString(iface.hashCode())+")");
         ClassLoader loader = ifaces[i].getClassLoader();
         results.append("\n++++ClassLoader: "+loader);
         ProtectionDomain pd = ifaces[i].getProtectionDomain();
         CodeSource cs = pd.getCodeSource();
         if( cs != null )
            results.append("\n++++CodeSource: "+cs);
         else
            results.append("\n++++Null CodeSource");
      }
   }

   /** Use reflection to access a URL[] getURLs or ULR[] getAllURLs method so
    that non-URLClassLoader class loaders, or class loaders that override
    getURLs to return null or empty, can provide the true classpath info.
    */
   public static URL[] getClassLoaderURLs(ClassLoader cl)
   {
      URL[] urls = {};
      try
      {
         Class returnType = urls.getClass();
         Class[] parameterTypes = {};
         Method getURLs = cl.getClass().getMethod("getURLs", parameterTypes);
         if( returnType.isAssignableFrom(getURLs.getReturnType()) )
         {
            Object[] args = {};
            urls = (URL[]) getURLs.invoke(cl, args);
         }
      }
      catch(Exception ignore)
      {
      }
      return urls;
   }

   /** Get all of the URLClassLoaders from cl on up the hierarchy
    *
    * @param cl the class loader to start from
    * @return The possibly empty array of URLClassLoaders from cl through
    *    its parent class loaders
    */
   public static URLClassLoader[] getClassLoaderStack(ClassLoader cl)
   {
      ArrayList stack = new ArrayList();
      while( cl != null )
      {
         if( cl instanceof URLClassLoader )
         {
            stack.add(cl);
         }
         cl = cl.getParent();
      }
      URLClassLoader[] ucls = new URLClassLoader[stack.size()];
      stack.toArray(ucls);
      return ucls;
   }

   /** Parse a class name into its package prefix. This has to handle
      array classes whose name is prefixed with [L.
    */
   public static String getPackageName(String className)
   {
      int startIndex = 0;
      // Strip any leading "[+L" found in array class names
      if( className.charAt(0) == '[' )
      {
         // Move beyond the [...[L prefix
         startIndex = className.indexOf('L') + 1;
      }
	   // Now extract the package name
      String pkgName = "";
      int endIndex = className.lastIndexOf('.');
      if( endIndex > 0 )
         pkgName = className.substring(startIndex, endIndex);
      return pkgName;
   }

   /** Given the URL UCL this method determine what packages
    it contains and create a mapping from the package names to the cl.
    * @param cl the UCL that loads from url
    * @param packagesMap the Map<cl, String[]> to update
    * @return the updated unique set of package names
    * @throws Exception
    */
   public static String[] updatePackageMap(UnifiedClassLoader cl, HashMap packagesMap)
      throws Exception
   {
      URL url = cl.getURL();
      ClassPathIterator cpi = new ClassPathIterator(url);
      HashSet pkgNameSet = new HashSet();
      return updatePackageMap(cl, packagesMap, cpi, pkgNameSet);
   }
   /** Augment the package name associated with a UCL.
    * @param cl the UCL that loads from url
    * @param packagesMap the Map<cl, String[]> to update
    * @param url the URL to parse for package names
    * @param prevPkgNames the set of pckage names already associated with cl
    * @return the updated unique set of package names
    * @throws Exception
    */
   public static String[] updatePackageMap(UnifiedClassLoader cl, HashMap packagesMap,
      URL url, String[] prevPkgNames)
      throws Exception
   {
      ClassPathIterator cpi = new ClassPathIterator(url);
      HashSet pkgNameSet = new HashSet(Arrays.asList(prevPkgNames));
      return updatePackageMap(cl, packagesMap, cpi, pkgNameSet);
   }

   static String[] updatePackageMap(UnifiedClassLoader cl, HashMap packagesMap,
      ClassPathIterator cpi, HashSet pkgNameSet)
      throws Exception
   {
      boolean trace = log.isTraceEnabled();
      ClassPathEntry entry;
      while( (entry = cpi.getNextEntry()) != null )
      {
         String name = entry.getName();
         // First look for a META-INF/INDEX.LIST entry
         if( name.equals("META-INF/INDEX.LIST") )
         {
            readJarIndex(cl, cpi, packagesMap, pkgNameSet);
            // We are done
            break;
         }

         // Skip empty directory entries
         if( entry.isDirectory() == true )
            continue;

         String pkgName = entry.toPackageName();
         addPackage(pkgName, packagesMap, pkgNameSet, cl, trace);
      }
      cpi.close();

      // Return an array of the package names
      String[] pkgNames = new String[pkgNameSet.size()];
      pkgNameSet.toArray(pkgNames);
      return pkgNames;
   }

   /** Read the JDK 1.3+ META-INF/INDEX.LIST entry to obtain the package
    names without having to iterate through all entries in the jar.
    */
   private static void readJarIndex(UnifiedClassLoader cl, ClassPathIterator cpi,
      HashMap packagesMap, HashSet pkgNameSet)
      throws Exception
   {
      boolean trace = log.isTraceEnabled();
      InputStream zis = cpi.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(zis));
      String line;
      // Skip the jar index header
      while( (line = br.readLine()) != null )
      {
         if( line.length() == 0 )
            break;
      }

      // Read the main jar section
      String jarName = br.readLine();
      if( trace )
         log.trace("Reading INDEX.LIST for jar: "+jarName);
      while( (line = br.readLine()) != null )
      {
         if( line.length() == 0 )
            break;
         String pkgName = line.replace('/', '.');
         addPackage(pkgName, packagesMap, pkgNameSet, cl, trace);
      }
      br.close();
   }

   private static void addPackage(String pkgName, HashMap packagesMap,
      HashSet pkgNameSet, UnifiedClassLoader cl, boolean trace)
   {
      // Skip the standard J2EE archive directories
      if( pkgName.startsWith("META-INF") || pkgName.startsWith("WEB-INF") )
         return;

      HashSet pkgSet = (HashSet) packagesMap.get(pkgName);
      if( pkgSet == null )
      {
         pkgSet = new HashSet();
         packagesMap.put(pkgName, pkgSet);
      }
      if( pkgSet.contains(cl) == false )
      {
         pkgSet.add(cl);
         pkgNameSet.add(pkgName);
         // Anytime more than one class loader exists this may indicate a problem
         if( pkgSet.size() > 1 )
         {
            log.debug("Multiple class loaders found for pkg: "+pkgName);
         }
         if( trace )
            log.trace("Indexed pkg: "+pkgName+", UCL: "+cl);
      }
   }

   /**
   */
   static class FileIterator
   {
      LinkedList subDirectories = new LinkedList();
      FileFilter filter;
      File[] currentListing;
      int index = 0;

      FileIterator(File start)
      {
         currentListing = start.listFiles();
      }
      FileIterator(File start, FileFilter filter)
      {
         currentListing = start.listFiles(filter);
         this.filter = filter;
      }

      File getNextEntry()
      {
         File next = null;
         if( index >= currentListing.length && subDirectories.size() > 0 )
         {
            do
            {
               File nextDir = (File) subDirectories.removeFirst();
               currentListing = nextDir.listFiles(filter);
            } while( currentListing.length == 0 && subDirectories.size() > 0 );
            index = 0;
         }
         if( index < currentListing.length )
         {
            next = currentListing[index ++];
            if( next.isDirectory() )
               subDirectories.addLast(next);
         }
         return next;
      }
   }

   /** A filter that allows directories and .class files
   */
   static class ClassFilter implements FileFilter
   {
      public boolean accept(File file)
      {
         boolean accept = file.isDirectory() || file.getName().endsWith(".class");
         return accept;
      }
   }

   /**
    */
   static class ClassPathEntry
   {
      String name;
      ZipEntry zipEntry;
      File fileEntry;

      ClassPathEntry(ZipEntry zipEntry)
      {
         this.zipEntry = zipEntry;
         this.name = zipEntry.getName();
      }
      ClassPathEntry(File fileEntry, int rootLength)
      {
         this.fileEntry = fileEntry;
         this.name = fileEntry.getPath().substring(rootLength);
      }

      String getName()
      {
         return name;
      }
      /** Convert the entry path to a package name
       */
      String toPackageName()
      {
         String pkgName = name;
         char separatorChar = zipEntry != null ? '/' : File.separatorChar;
         int index = name.lastIndexOf(separatorChar);
         if( index > 0 )
         {
            pkgName = name.substring(0, index);
            pkgName = pkgName.replace(separatorChar, '.');
         }
         else
         {
            // This must be an entry in the default package (e.g., X.class)
            pkgName = "";
         }
         return pkgName;
      }

      boolean isDirectory()
      {
         boolean isDirectory = false;
         if( zipEntry != null )
            isDirectory = zipEntry.isDirectory();
         else
            isDirectory = fileEntry.isDirectory();
         return isDirectory;
      }
   }

   /** An iterator for jar entries or directory structures.
   */
   static class ClassPathIterator
   {
      ZipInputStream zis;
      FileIterator fileIter;
      File file;
      int rootLength;

      ClassPathIterator(URL url) throws IOException
      {
         String protocol = url.getProtocol();
         if( protocol.equals("file") )
         {
            File tmp = new File(url.getFile());
            String name = tmp.getName();
            if( tmp.isDirectory() )
            {
               rootLength = tmp.getPath().length() + 1;
               fileIter = new FileIterator(tmp, new ClassFilter());
            }
            else
            {
               // Assume this is a jar archive
               InputStream is = new FileInputStream(tmp);
               zis = new ZipInputStream(is);
            }
         }
         else
         {
            // Assume this points to a jar
            InputStream is = url.openStream();
            zis = new ZipInputStream(is);
         }
      }

      ClassPathEntry getNextEntry() throws IOException
      {
         ClassPathEntry entry = null;
         if( zis != null )
         {
            ZipEntry zentry = zis.getNextEntry();
            if( zentry != null )
               entry = new ClassPathEntry(zentry);
         }
         else
         {
            File fentry = fileIter.getNextEntry();
            if( fentry != null )
               entry = new ClassPathEntry(fentry, rootLength);
            file = fentry;
         }

         return entry;
      }

      InputStream getInputStream() throws IOException
      {
         InputStream is = zis;
         if( zis == null )
         {
            is = new FileInputStream(file);
         }
         return is;
      }

      void close() throws IOException
      {
         if( zis != null )
            zis.close();
      }

   }
}


