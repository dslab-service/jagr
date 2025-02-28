/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
package org.jboss.invocation.jrmp.interfaces;

import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.rmi.ServerException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.MarshalledObject;
import java.rmi.server.RemoteObject;
import java.rmi.server.RemoteStub;

import javax.transaction.SystemException;

import org.jboss.invocation.Invocation;
import org.jboss.invocation.MarshalledInvocation;
import org.jboss.invocation.Invoker;
import org.jboss.invocation.local.LocalInvoker;
import org.jboss.security.SecurityAssociation;
import org.jboss.tm.TransactionPropagationContextFactory;

/**
* JRMPInvokerProxy, local to the proxy and is capable of delegating to
* the JRMP implementations
* 
* @author <a href="mailto:marc.fleury@jboss.org">Marc Fleury</a>
* @author <a href="mailto:scott.stark@jboss.org">Scott Stark</a>
* @version $Revision: 1.1.1.1 $
*/
public class JRMPInvokerProxy
   implements Invoker, Externalizable
{
   // Constants -----------------------------------------------------
   
   /** Serial Version Identifier. */
   //   private static final long serialVersionUID = 1870461898442160570L;
   
   // Attributes ----------------------------------------------------
   
   // Invoker to the remote JMX node
   protected Invoker remoteInvoker;
   
   /**
   *  Factory for transaction propagation contexts.
   *
   *  @todo: marcf remove all transaction spill from here
   * 
   *  When set to a non-null value, it is used to get transaction
   *  propagation contexts for remote method invocations.
   *  If <code>null</code>, transactions are not propagated on
   *  remote method invocations.
   */
   protected static TransactionPropagationContextFactory tpcFactory = null;
   
   // Get and set methods
   
   
   //  @todo: MOVE TO TRANSACTION
   // TPC factory
   public static void setTPCFactory(TransactionPropagationContextFactory tpcf)
   {
      tpcFactory = tpcf;
   }

   public static int MAX_RETRIES = 10;

   // Constructors --------------------------------------------------
   public JRMPInvokerProxy()
   {
      // For externalization to work
   }
   
   /**
   *  Create a new Proxy.
   *
   *  @param container
   *         The remote interface of the container invoker of the
   *         container we proxy for.
   */
   public JRMPInvokerProxy(Invoker remoteInvoker) 
   {
      this.remoteInvoker = remoteInvoker;
   }
   
   // Public --------------------------------------------------------
   
   // The name of of the server
   public String getServerHostName() throws Exception
   {
      return remoteInvoker.getServerHostName();
   }
   
   /**
   *  
   *  @Return the transaction propagation context of the transaction
   *  associated with the current thread.
   *  Returns <code>null</code> if the transaction manager was never
   *  set, or if no transaction is associated with the current thread.
   @todo: MOVE TO TRANSACTION
   */
   public Object getTransactionPropagationContext()
      throws SystemException
   {
      //  @todo: MOVE TO TRANSACTION
      return (tpcFactory == null) ? null : tpcFactory.getTransactionPropagationContext();
   }
   
   /**
   * The invocation on the delegate, calls the right invoker.  Remote if we are
   * remote, local if we are local. 
   */
   public Object invoke(Invocation invocation)
      throws Exception
   {
      // We are going to go through a Remote invocation, switch to a Marshalled Invocation
      MarshalledInvocation mi = new MarshalledInvocation(invocation);
         
      // Set the transaction propagation context
      //  @todo: MOVE TO TRANSACTION
      mi.setTransactionPropagationContext(getTransactionPropagationContext());
         
      
      // RMI seems to make a connection per invocation.
      // If too many clients are making an invocation
      // at same time, ConnectionExceptions happen
      for (int i = 0; i < MAX_RETRIES; i++)
      {
         try
         { 
            MarshalledObject result = (MarshalledObject) remoteInvoker.invoke(mi);
            return result.get();
         }
         catch (ConnectException ce)
         {
            if (i + 1 < MAX_RETRIES)
            {
               Thread.sleep(1);
               continue;
            }
            throw ce;
         }
         catch (ServerException ex)
         {
            // Suns RMI implementation wraps NoSuchObjectException in
            // a ServerException. We cannot have that if we want
            // to comply with the spec, so we unwrap here.
            if (ex.detail instanceof NoSuchObjectException)
               throw (NoSuchObjectException) ex.detail;
            throw ex;
         }  
      }
      throw new Exception("Unreachable statement");
   }

   /**
   *  Externalize this instance and handle obtaining the remoteInvoker stub
   *
   *  If this instance lives in a different VM than its container
   *  invoker, the remote interface of the container invoker is
   *  not externalized.
   */
   public void writeExternal(final ObjectOutput out)
      throws IOException
   {
      /** We need to handle obtaining the RemoteStub for the remoteInvoker
       * since this proxy may be serialized in contexts that are not JRMP
       * aware.
       */
      if( remoteInvoker instanceof RemoteStub )
      {
         out.writeObject(remoteInvoker);
      }
      else
      {
         Object replacement = RemoteObject.toStub(remoteInvoker);
         out.writeObject(replacement);
      }
   }

   /**
   *  Un-externalize this instance.
   *
   *  We check timestamps of the interfaces to see if the instance is in the original VM of creation
   */
   public void readExternal(final ObjectInput in)
      throws IOException, ClassNotFoundException
   {
      remoteInvoker = (Invoker) in.readObject();
   }
   
   // Private -------------------------------------------------------

   
   // Inner classes -------------------------------------------------
}
