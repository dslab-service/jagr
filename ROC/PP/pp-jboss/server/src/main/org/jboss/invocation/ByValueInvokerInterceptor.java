/*
* JBoss, the OpenSource J2EE webOS
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/
package org.jboss.invocation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
* An InvokerInterceptor that does not optimize remote invocations.<p>
*
* This interceptor implements spec compliant behaviour.<p>
*
* @todo The performance could be improved by simulating marshalling
*       for "local" remote, rather than going straight for the invoker
* 
* @author <a href="mailto:adrian.brock@happeningtimes.com">Adrian Brock</a>
* @version $Revision: 1.1.1.1 $
*/
public class ByValueInvokerInterceptor
   extends InvokerInterceptor
   implements Externalizable
{
   // Constants -----------------------------------------------------
   
   // Attributes ----------------------------------------------------
   
   // Constructors --------------------------------------------------
   
   public ByValueInvokerInterceptor()
   {
      // For externalization to work
   }
   
   // Public --------------------------------------------------------

   /**
    * Are you local?
    */
   public boolean isLocal(Invocation invocation)
   {
      int type = invocation.getType();
      if (type == Invocation.LOCAL || type == Invocation.LOCALHOME)
         return true;
      return false;
   }
   
   /**
    * Invoke using the invoker for remote invocations
    */
   public Object invoke(Invocation invocation)
      throws Exception
   {
      // local interface
      if (isLocal(invocation))
         // The payload as is is good
         return localInvoker.invoke(invocation);
      else
         // through the invoker
         return invocation.getInvocationContext().getInvoker().invoke(invocation);
   }
   
   /**
    *  Externalize this instance.
    */
   public void writeExternal(final ObjectOutput out)
      throws IOException
   { 
      // We have no state
   }
   
   /**
    *  Un-externalize this instance.
    */
   public void readExternal(final ObjectInput in)
      throws IOException, ClassNotFoundException
   {
      // We have no state
   }
   
   // Private -------------------------------------------------------
   
   // Inner classes -------------------------------------------------
}
