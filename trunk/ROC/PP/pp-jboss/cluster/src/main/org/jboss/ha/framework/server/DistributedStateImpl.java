/*
 * JBoss, the OpenSource J2EE WebOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.ha.framework.server;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.rmi.server.UnicastRemoteObject;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.ha.framework.interfaces.DistributedMap;
import org.jboss.ha.framework.interfaces.DistributedState.DSListener;
import org.jboss.ha.framework.interfaces.HAPartition.HAPartitionStateTransfer;
import org.jboss.ha.framework.interfaces.HAPartition.HAMembershipListener;
import org.jboss.ha.framework.interfaces.DistributedState;
import org.jboss.ha.framework.interfaces.HAPartition;

/**
 *   This class manages distributed state across the cluster.
 *
 *   @author  <a href="mailto:sacha.labourey@cogito-info.ch">Sacha Labourey</a>.
 *   @author  <a href="mailto:bill@burkecentral.com">Bill Burke</a>.
 *   @version $Revision: 1.1.1.1 $
 *
 * <p><b>Revisions:</b><br>
 * <p><b>2001/11/26: Sacha Labourey</b>
 * <ol>
 *   <li>No more a remote object</li>
 *   <li>No more listening for Membership changes (was doing nothing with the callback)</li>
 * </ol>
 */
public class DistributedStateImpl
   implements DistributedStateImplMBean, DistributedMap, HAPartitionStateTransfer
{
   // Constants -----------------------------------------------------
   
   protected final static String SERVICE_NAME = "DistributedState";
   
   // Attributes ----------------------------------------------------
   
   protected HashMap categories = new HashMap ();
   /** The <String, ArrayList<DSListener>> map */
   protected HashMap keyListeners = new HashMap ();
   /** The <String, ArrayList<DMListener>> map */
   protected HashMap dmListeners = new HashMap ();
   protected HAPartition partition;

   protected org.jboss.logging.Logger log = null;
   
   protected MBeanServer mbeanServer = null;
   protected String name = null;
   /** The Logger.isTraceEnabled level set during init */
   protected boolean trace;

   // Static --------------------------------------------------------c
   
   // Constructors --------------------------------------------------
   
   public DistributedStateImpl () {} // for JMX checks
   
   public DistributedStateImpl (HAPartition partition, MBeanServer server)
   {
      this.partition = partition;
      this.mbeanServer = server;
      this.log = org.jboss.logging.Logger.getLogger (this.getClass ());
   }
   
   // Public --------------------------------------------------------
   
   public void init () throws Exception
   {
      trace = log.isTraceEnabled();
      // When we subscribe to state transfer events, GetState will be called to initialize
      // this service.
      //
      partition.subscribeToStateTransferEvents (SERVICE_NAME, this);
      partition.registerRPCHandler (SERVICE_NAME, this);  
      log.debug("Registered as RPCHandler: "+SERVICE_NAME);

      // subscribed this "sub-service" of HAPartition with JMX
      // TODO: In the future (when state transfer issues will be completed), 
      // we will need to redesign the way HAPartitions and its sub-protocols are
      // registered with JMX. They will most probably be independant JMX services.
      //
      this.name = "jboss:service=" + SERVICE_NAME + 
                    ",partitionName=" + this.partition.getPartitionName();
      ObjectName jmxName = new ObjectName(this.name);
      mbeanServer.registerMBean(this, jmxName);
      org.jboss.system.Registry.bind (this.name, this);
   }
   
   public void start () throws Exception
   {
   }
   
   public void stop () throws Exception
   {
      org.jboss.system.Registry.unbind (this.name);
      ObjectName jmxName = new ObjectName(this.name);
      mbeanServer.unregisterMBean (jmxName);
   }
   
   public String listContent () throws Exception
   {
      StringBuffer result = new StringBuffer ();
      Collection cats = this.getAllCategories ();
      java.util.Iterator catsIter = cats.iterator ();
      while (catsIter.hasNext ())
      {
         String category = (String)catsIter.next ();
         java.util.Iterator keysIter = this.getAllKeys(category).iterator ();
         
         result.append ("-----------------------------------------------\n");
         result.append ("Category : ").append (category).append ("\n\n");
         result.append ("KEY\t:\tVALUE\n");

         while (keysIter.hasNext ())
         {
            String key = keysIter.next().toString();
            String value = this.get (category, key).toString ();
            result.append ("\"").append(key).append ("\"\t:\t\"").append (value).append("\"\n");
         }
         
         result.append ("\n");
         
      }
      
      return result.toString ();
   }
   
   public String listXmlContent () throws Exception
   {
      StringBuffer result = new StringBuffer ();
      Collection cats = this.getAllCategories ();
      java.util.Iterator catsIter = cats.iterator ();
      
      result.append ("<DistributedState>\n");
      
      while (catsIter.hasNext ())
      {
         String category = (String)catsIter.next ();
         java.util.Iterator keysIter = this.getAllKeys(category).iterator ();
         
         result.append ("\t<Category>\n");
         result.append ("\t\t<CategoryName>").append (category).append ("</CategoryName>\n");

         while (keysIter.hasNext ())
         {
            String key = (String)keysIter.next().toString();
            String value = this.get (category, key).toString ();
            result.append ("\t\t<Entry>\n");
            result.append ("\t\t\t<Key>").append (key).append ("</Key>\n");
            result.append ("\t\t\t<Value>").append (value).append ("</Value>\n");
            result.append ("\t\t</Entry>\n");
            result.append ("\"").append(key).append ("\"\t:\t\"").append (value).append("\"\n");
         }
         
         result.append ("\t</Category>\n");
         
      }

      result.append ("</DistributedState>\n");
      
      return result.toString ();
   }
   
   // DistributedState implementation ----------------------------------------------

   public void set (String category, String key, Serializable value) throws Exception
   {
      set (category, (Serializable) key, value, true);
   }
   public void set (String category, String key, Serializable value, boolean asynchronousCall) throws Exception
   {
      set(category, (Serializable) key, value, asynchronousCall);
   }

   public Serializable remove (String category, String key) throws Exception
   {
      return remove (category, (Serializable) key, true);
   }
   
   public Serializable remove (String category, String key, boolean asynchronousCall) throws Exception
   {
      return remove (category, (Serializable) key, asynchronousCall);
   }

   public Serializable get (String category, String key)
   {
      return get(category, (Serializable) key);
   }

   public Collection getAllCategories ()
   {
      synchronized(this.categories)
      {
         return categories.keySet ();
      }
   }
   
   public Collection getAllKeys (String category)
   {
      synchronized(this.categories)
      {
         HashMap cat = (HashMap)categories.get (category);
         if (cat == null) return null;
         
         return cat.keySet ();
      }
   }

   public Collection getAllValues (String category)
   {
      synchronized(this.categories)
      {
         HashMap cat = (HashMap)categories.get (category);
         if (cat == null) return null;
         
         return cat.values ();
      }
   }

   public void registerDSListener (String category, DSListener subscriber)
   {
      synchronized(this.keyListeners)
      {
         ArrayList listeners = (ArrayList)keyListeners.get (category);
         if (listeners == null)
         {
            listeners = new ArrayList ();
            keyListeners.put (category, listeners);
         }
         listeners.add (subscriber);
      }
   }
   
   public void unregisterDSListener (String category, DSListener subscriber)
   {
      synchronized(this.keyListeners)
      {
         ArrayList listeners = (ArrayList)keyListeners.get (category);
         if (listeners == null) return;
         
         listeners.remove (subscriber);
         if (listeners.size () == 0)
         {
            keyListeners.remove (category);
         }
      }
   }

// Begin DistributedMap interface methods
   public void registerDMListener(String category, DMListener subscriber)
   {
      synchronized(this.dmListeners)
      {
         ArrayList listeners = (ArrayList) dmListeners.get(category);
         if (listeners == null)
         {
            listeners = new ArrayList();
            dmListeners.put(category, listeners);
         }
         listeners.add(subscriber);
      }
   }
   public void unregisterDMListener(String category, DMListener subscriber)
   {
      synchronized(this.dmListeners)
      {
         ArrayList listeners = (ArrayList)dmListeners.get (category);
         if (dmListeners == null)
            return;

         listeners.remove (subscriber);
         if (listeners.size () == 0)
         {
            dmListeners.remove (category);
         }
      }
   }

   public Serializable get (String category, Serializable key)
   {
      synchronized(this.categories)
      {
         HashMap cat = (HashMap)categories.get (category);
         Serializable value = null;
         if( cat != null )
         {
            value = (Serializable) cat.get (key);
         }
         return value;
      }
   }

   public Serializable remove(String category, Serializable key)
      throws Exception
   {
      return remove (category, key, true);
   }

   public Serializable remove(String category, Serializable key,
      boolean asynchronousCall) throws Exception
   {
      if( trace )
      {
         log.trace("remove, category="+category+", key="+key
            +", asynchronousCall="+asynchronousCall);
      }

      Object[] args = {category, key};
      if (asynchronousCall)
         partition.callAsynchMethodOnCluster (SERVICE_NAME, "_remove", args, true);
      else
         partition.callMethodOnCluster (SERVICE_NAME, "_remove", args, true);

      Serializable removed = this._removeInternal (category, key);
      if( trace )
      {
         log.trace("remove, category="+category+", key="+key
            +", removed="+removed);
      }
      notifyKeyListenersOfRemove (category, key, removed , true);

      return removed ;
   }

   public void set(String category, Serializable key, Serializable value)
      throws Exception
   {
      set (category, key, value, true);
   }

   public void set(String category, Serializable key, Serializable value,
      boolean asynchronousCall) throws Exception
   {
      if( trace )
      {
         log.trace("set, category="+category+", key="+key
            +", value="+value+", asynchronousCall="+asynchronousCall);
      }

      Object[] args = {category, key, value};
      if (asynchronousCall)
         partition.callAsynchMethodOnCluster (SERVICE_NAME, "_set", args, true);
      else
         partition.callMethodOnCluster (SERVICE_NAME, "_set", args, true);
      this._setInternal (category, key, value);
      notifyKeyListeners (category, key, value, true);
   }

// End DistributedMap interface methods

   // HAPartition RPC method invocations implementation ----------------------------------------------
   
   public void _set (String category, String key, Serializable value)
      throws Exception
   {
      _set(category, (Serializable) key, value);
   }
   public void _set (String category, Serializable key, Serializable value)
      throws Exception
   {
      if( trace )
      {
         log.trace("_set, category="+category+", key="+key+", value="+value);
      }
      this._setInternal (category, key, value);
      notifyKeyListeners (category, key, value, false);
   }

   public void _setInternal (String category, String key, Serializable value)
      throws Exception
   {
      _setInternal (category, (Serializable) key, value);
   }
   public void _setInternal (String category, Serializable key, Serializable value)
      throws Exception
   {
      if( trace )
      {
         log.trace("_setInternal, category="+category+", key="+key
            +", value="+value);
      }

      synchronized(this.categories)
      {
         HashMap cat = (HashMap)categories.get (category);
         if (cat == null)
         {
            cat = new HashMap ();
            categories.put (category, cat);
         }
         cat.put (key, value);
      }
   }

   public void _remove (String category, String key) throws Exception
   {
      Serializable removed = this._removeInternal (category, key);
      notifyKeyListenersOfRemove (category, key, removed, false);
   }

   public Serializable _removeInternal (String category, String key)
      throws Exception
   {
      return _removeInternal(category, (Serializable) key);
   }
   public Serializable _removeInternal (String category, Serializable key)
      throws Exception
   {
      if( trace )
      {
         log.trace("_removeInternal, category="+category+", key="+key);
      }

      synchronized(this.categories)
      {
         HashMap cat = (HashMap)categories.get (category);
         if (cat == null) return null;
         Object removed = cat.remove (key);
         if (removed != null)
         {
            if (cat.size () == 0)
            {
               categories.remove (category);
            }
         }
         if( trace )
            log.trace("_removeInternal, removed="+removed);
         return (Serializable)removed;
      }
   }

   // HAPartitionStateTransfer implementation ----------------------------------------------
   
   public Serializable getCurrentState ()
   {
      return categories;
   }
   
   public void setCurrentState (Serializable newState)
   {
      synchronized (this.categories)
      {
         categories.clear ();
         categories.putAll ((HashMap)newState);
         if (keyListeners.size () > 0)
         {
            cleanupKeyListeners ();
         }
      }
   }
      
   // Package protected ---------------------------------------------
   
   // Protected -----------------------------------------------------
   
   protected void notifyKeyListeners (String category, String key,
      Serializable value, boolean locallyModified)
   {
      notifyKeyListeners(category, (Serializable) key, value, locallyModified);
   }
   protected void notifyKeyListeners (String category, Serializable key,
      Serializable value, boolean locallyModified)
   {
      synchronized(this.keyListeners)
      {
         ArrayList listeners = (ArrayList)keyListeners.get (category);
         if( listeners != null )
         {
            String strKey = key.toString();
            for (int i = 0; i < listeners.size (); i++)
            {
               DistributedState.DSListener listener = (DistributedState.DSListener)listeners.get (i);
               listener.valueHasChanged (category, strKey, value, locallyModified);
            }
         }

         listeners = (ArrayList) dmListeners.get (category);
         if( listeners != null )
         {
            for (int i = 0; i < listeners.size (); i++)
            {
               DMListener listener = (DMListener)listeners.get (i);
               listener.valueHasChanged (category, key, value, locallyModified);
            }
         }
      }
   }

   protected void notifyKeyListenersOfRemove (String category, Serializable key,
      Serializable oldContent, boolean locallyModified)
   {
      synchronized(this.keyListeners)
      {
         ArrayList listeners = (ArrayList)keyListeners.get (category);
         if( listeners != null )
         {
            String strKey = key.toString();
            for (int i = 0; i < listeners.size (); i++)
            {
               DistributedState.DSListener listener = (DistributedState.DSListener)listeners.get (i);
               listener.keyHasBeenRemoved (category, strKey, oldContent, locallyModified);
            }
         }

         listeners = (ArrayList) dmListeners.get (category);
         if( listeners != null )
         {
            for (int i = 0; i < listeners.size (); i++)
            {
               DMListener listener = (DMListener)listeners.get (i);
               listener.keyHasBeenRemoved (category, key, oldContent, locallyModified);
            }
         }
      }
   }

   protected void cleanupKeyListeners ()
   {
      // NOT IMPLEMENTED YET
   }
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
      
}
