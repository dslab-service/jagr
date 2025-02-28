/*
 * Copyright 1999 by dreamBean Software,
 * All rights reserved.
 */
package org.jboss.test.bankiiop.interfaces;

import java.rmi.RemoteException;
import javax.ejb.CreateException;

/**
 *      
 *   @see <related>
 *   @author $Author: mikechen $
 *   @version $Revision: 1.1.1.1 $
 */
public class AccountData
   implements java.io.Serializable
{
   // Constants -----------------------------------------------------
    
   // Attributes ----------------------------------------------------
   public String id;
   public float balance;
   public Customer owner;
   
   // Static --------------------------------------------------------

   // Constructors --------------------------------------------------
   
   // Public --------------------------------------------------------
   public String getId()
   {
      return id;
   }
   
   public void setId(String id)
   {
      this.id = id;
   }
   
   public float getBalance()
   {
      return balance;
   }
   
   public void setBalance(float balance)
   {
      this.balance = balance;
   }
   
   public Customer getOwner()
   {
      return owner;
   }
   
   public void setOwner(Customer owner)
   {
      this.owner = owner;
   }
}

/*
 *   $Id: AccountData.java,v 1.1.1.1 2002/11/16 03:16:42 mikechen Exp $
 *   Currently locked by:$Locker:  $
 *   Revision:
 *   $Log: AccountData.java,v $
 *   Revision 1.1.1.1  2002/11/16 03:16:42  mikechen
 *   Import of JBoss v3.0.3
 *
 *   Revision 1.1  2002/03/15 22:36:28  reverbel
 *   Initial version of the bank test for JBoss/IIOP.
 *
 *   Revision 1.2  2001/01/07 23:14:35  peter
 *   Trying to get JAAS to work within test suite.
 *
 *   Revision 1.1.1.1  2000/06/21 15:52:38  oberg
 *   Initial import of jBoss test. This module contains CTS tests, some simple examples, and small bean suites.
 *
 *
 *  
 */
