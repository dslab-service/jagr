/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.ejb;

/**
 * EntityCaches can work from several keys.
 *
 * <p>A cache can use the natural primaryKey from the EJBObject, or DB
 *    dependent keys or a proprietary key
 *
 * @see EntityInstanceCache
 * 
 * @author <a href="mailto:marc.fleury@telkel.com">Marc Fleury</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface EntityCache
   extends InstanceCache
{
   /**
    * Returns the key used to cache the context
    *
    * @param id    Object id / primary key
    * @return      Cache key
    */
   Object createCacheKey(Object id);

   /**
    * Return number of cached objects;
    *
    */
   int getCacheSize();

   /**
    * Flush the cache.
    *
    */
   void flush();
}
