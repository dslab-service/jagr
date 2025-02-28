package edu.rice.rubis.beans;

import java.rmi.RemoteException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.sql.DataSource;
import java.io.Serializable;

/**
 * This is a stateless session bean used to provides user authentication 
 * services to servlets.
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.1
 */

public class SB_AuthBean implements SessionBean 
{
  protected SessionContext sessionContext;
  protected Context initialContext = null;

  /**
   * Describe <code>authenticate</code> method here.
   *
   * @param name user nick name
   * @param password user password
   * @return an <code>int</code> value corresponding to the user id or -1 on error
   */
  public int authenticate (String name, String password)
  {
    int userId = -1;

    // Connecting to user Home interface thru JNDI
    UserLocalHome userHome = null;
    try 
    {
      userHome = (UserLocalHome)initialContext.lookup("java:comp/env/ejb/User");
    } 
    catch (Exception e)
    {
      throw new EJBException("Cannot lookup User: " +e);
    }
    // get the User ID
    try
    {
      UserLocal user = userHome.findByNickName(name);
      String pwd = user.getPassword();
      if (pwd.equals(password))
      {
        userId = user.getId().intValue();
      }
    }
    catch (Exception e)
    {
      throw new EJBException(" User "+name+" does not exist in the database!<br>(got exception: " +e);
    }
    return userId;
  }


  // ======================== EJB related methods ============================

  /**
   * This method is empty for a stateless session bean
   */
  public void ejbCreate() throws CreateException
  {
  }

  /** This method is empty for a stateless session bean */
  public void ejbActivate(){}
  /** This method is empty for a stateless session bean */
  public void ejbPassivate(){}
  /** This method is empty for a stateless session bean */
  public void ejbRemove(){}


  /** 
   * Sets the associated session context. The container calls this method 
   * after the instance creation. This method is called with no transaction context. 
   * We also retrieve the Home interfaces of all RUBiS's beans.
   *
   * @param sessionContext - A SessionContext interface for the instance. 
   */
    public void setSessionContext(SessionContext sessionContext) 
	throws RemoteException {
	this.sessionContext = sessionContext;
	try {
	    initialContext = new InitialContext();  
	} catch (Exception e) {
	    throw new RemoteException("Cannot get JNDI InitialContext: "+e);
	}
    }
}
