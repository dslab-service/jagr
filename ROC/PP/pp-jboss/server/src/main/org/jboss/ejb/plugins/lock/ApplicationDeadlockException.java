/*
* JBoss, the OpenSource EJB server
*
* Distributable under LGPL license.
* See terms of license at gnu.org.
*/

package org.jboss.ejb.plugins.lock;

import java.rmi.RemoteException;

import javax.ejb.EJBException;

/**
 * This exception class is thrown when application deadlock is detected when trying to lock an entity bean
 * This is probably NOT a result of a jboss bug, but rather that the application is access the same entity
 * beans within 2 different transaction in a different order.  Remember, with a PessimisticEJBLock, 
 * Entity beans are locked until the transaction commits or is rolled back.
 *
 * @author <a href="bill@burkecentral.com">Bill Burke</a>
 *
 * @version $Revision: 1.1.1.1 $
 *
 * <p><b>Revisions:</b><br>
 * <p><b>2002/02/13: billb</b>
 *  <ol>
 *  <li>Initial revision
 *  </ol>
 */
public class ApplicationDeadlockException extends RemoteException
{
   private static final long serialVersionUID = -3805931241669789217L;

   protected boolean retry = false;

   /**
    * Detects exception contains is or a ApplicationDeadlockException.
    */
   public static ApplicationDeadlockException isADE(Throwable t)
   {
      while (t!=null)
      {
         if (t instanceof ApplicationDeadlockException)
         {
            return (ApplicationDeadlockException)t;
         }
         else if (t instanceof RemoteException)
         {
            t = ((RemoteException)t).detail;
         }
         else if (t instanceof EJBException)
         {
            t = ((EJBException)t).getCausedByException();
         }
         else
         {
            return null;
         }
      }
      return null;
   } 

   public ApplicationDeadlockException()
   {
      super();
   }

   public ApplicationDeadlockException(String msg, boolean retry)
   {
      super(msg);
      this.retry = retry;
   }

   public boolean retryable()
   {
      return retry;
   }
}

