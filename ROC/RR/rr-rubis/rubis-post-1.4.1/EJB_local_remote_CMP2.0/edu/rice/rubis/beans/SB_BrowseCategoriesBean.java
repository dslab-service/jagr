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
import javax.transaction.UserTransaction;
import java.util.Collection;
import java.util.Iterator;
import java.net.URLEncoder;

/**
 * This is a stateless session bean used to get the list of 
 * categories from database and return the information to the BrowseRegions servlet. 
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.1
 */

public class SB_BrowseCategoriesBean implements SessionBean 
{
  protected SessionContext sessionContext;
  protected Context initialContext = null;
  private UserTransaction utx = null;


  /**
   * Get all the categories from the database.
   *
   * @return a string that is the list of categories in html format
   * @since 1.1
   */
  /** List all the categories in the database */
  public Object[] getCategories(String regionName, String username, 
				String password) throws RemoteException
    {
	Object[] r = new Object[2];
     
	Collection list;
	CategoryLocal cat;
	CategoryLocalHome home = null;
	String html = "";
	int regionId = -1;
	int userId = -1;

	if (regionName != null && regionName !="") {
	    // Connecting to region Home interface thru JNDI
	    RegionLocalHome regionHome = null;
	    try {
		regionHome = (RegionLocalHome)initialContext.
		    lookup("java:comp/env/ejb/Region");
	    } catch (Exception e) {
		throw new RemoteException("Cannot lookup Region: " +e);
	    }

	    // get the region ID
	    try {
		RegionLocal region = regionHome.findByName(regionName);
		regionId = region.getId().intValue();
	    } catch (Exception e) {
		throw new RemoteException(" Region "+regionName
					  +" does not exist in the database!<br>(got exception: " +e+")");
	    }
	} else {
	    // Authenticate the user who wants to sell items
	    if ((username != null && !username.equals("")) || 
		(password != null && !password.equals(""))) {

		SB_AuthLocalHome authHome = null;
		SB_AuthLocal auth = null;
		try {
		    authHome = (SB_AuthLocalHome)initialContext.
			lookup("java:comp/env/ejb/SB_Auth");
		    auth = authHome.create();
		} catch (Exception e) {
		    throw new RemoteException("Cannot lookup SB_Auth: " +e);
		}
		try {
		    userId = auth.authenticate(username, password);
		} catch (Exception e) {
		    throw new RemoteException("Authentication failed: " +e);
		}
		if (userId == -1) {
		    html = (" You don't have an account on RUBiS!<br>You have to register first.<br>");
		    r[1]=html;
		    return r;
		}
		r[0] = new Integer(userId);
	    }
	}
	
	// Connecting to Category Home
    try {
	home = (CategoryLocalHome)initialContext.
	    lookup("java:comp/env/ejb/Category");
    } catch (Exception e) {
	throw new RemoteException("Cannot lookup Category: " +e);
    }

    utx = sessionContext.getUserTransaction();

    try {
	utx.begin();
	list = home.findAllCategories();
	if (list.isEmpty())
	    html = ("<h2>Sorry, but there is no category available at this time. Database table is empty</h2><br>");
	else {
	    Iterator it = list.iterator();
	    while (it.hasNext()) {
		cat = (CategoryLocal)it.next();
		if (regionId != -1) {
		    html = html + printCategoryByRegion(cat, regionId);
		} else {
		    if (userId != -1)
			html = html + printCategoryToSellItem(cat, userId);
		    else
			html = html + printCategory(cat);
		}
	    }
	}
	utx.commit();
    } 
    catch (Exception e) 
    {
      try
      {
        utx.rollback();
        throw new RemoteException("Exception getting category list: " + e);
      }
      catch (Exception se) 
      {
        throw new RemoteException("Transaction rollback failed: " + e);
      }
    }
    r[1] = html;
    return r;
  }


  /**
   * Get all the categories from the database.
   *
   * @return a string that is the list of categories in html format
   * @since 1.1
   */
  /** List all the categories in the database */
    public String getCategories(String regionName, int userId) throws RemoteException {
	Collection list;
	CategoryLocal cat;
	CategoryLocalHome home = null;
	String html = "";
	int regionId = -1;

	if (regionName != null && regionName !="") {
	    // Connecting to region Home interface thru JNDI
	    RegionLocalHome regionHome = null;
	    try {
		regionHome = (RegionLocalHome)initialContext.lookup("java:comp/env/ejb/Region");
	    } catch (Exception e) {
		throw new RemoteException("Cannot lookup Region: " +e);
	    }
	    // get the region ID
	    try {
		RegionLocal region = regionHome.findByName(regionName);
		regionId = region.getId().intValue();
	    } catch (Exception e) {
		throw new RemoteException(" Region "+regionName+" does not exist in the database!<br>(got exception: " +e+")");
	    }
	} 
	
	// Connecting to Category Home
	try {
	    home = (CategoryLocalHome)initialContext.lookup("java:comp/env/ejb/Category");
	} catch (Exception e) {
	    throw new RemoteException("Cannot lookup Category: " +e);
	}

	utx = sessionContext.getUserTransaction();

	try {
	    utx.begin();
	    list = home.findAllCategories();
	    if (list.isEmpty())
		html = ("<h2>Sorry, but there is no category available at this time. Database table is empty</h2><br>");
	    else {
		Iterator it = list.iterator();
		while (it.hasNext()) {
		    cat = (CategoryLocal)it.next();
		    if (regionId != -1) {
			html = html + printCategoryByRegion(cat, regionId);
		    } else {
			if (userId != -1)
			    html = html + printCategoryToSellItem(cat, userId);
			else
			    html = html + printCategory(cat);
		    }
		}
	    }
	    utx.commit();
	} catch (Exception e) {
	    try {
		utx.rollback();
		throw new RemoteException("Exception getting category list: " + e);
	    } catch (Exception se) {
		throw new RemoteException("Transaction rollback failed: " + e);
	    }
	}
	return html;
    }



  /** 
   * Category related printed functions
   *
   * @param category the category to display
   * @return a string in html format
   * @since 1.1
   */

  public String printCategory(CategoryLocal category) throws RemoteException
  {
    String html = "";
    try
    {
      html = (category.printCategory());
    }
    catch (EJBException re)
    {
      throw new EJBException("Unable to print Category 1 (exception: "+re+")");
    }
    return html;
  }

  /** 
   * List all the categories with links to browse items by region
   * @return a string in html format
   * @since 1.1
   */
  public String printCategoryByRegion(CategoryLocal category, int regionId) throws RemoteException
  {
    String html = "";
    try
    {
      html = (category.printCategoryByRegion(regionId));
    }
    catch (EJBException re)
    {
      throw new EJBException("Unable to print Category 2 (exception: "+re+")<br>\n");
    }
    return html;
  }

  /** 
   * Lists all the categories and links to the sell item page
   * @return a string in html format
   * @since 1.1
   */
  public String printCategoryToSellItem(CategoryLocal category, int userId) throws RemoteException
  {
    String html= "";
    try
    {
      html = (category.printCategoryToSellItem(userId));
    }
    catch (EJBException re)
    {
      throw new EJBException("Unable to print Category 3 (exception: "+re+")<br>\n");
    }
    return html;
  }



  // ======================== EJB related methods ============================

  /**
   * This method is empty for a stateless session bean
   */
  public void ejbCreate() throws CreateException, RemoteException
  {
  }

  /** This method is empty for a stateless session bean */
  public void ejbActivate() throws RemoteException {}
  /** This method is empty for a stateless session bean */
  public void ejbPassivate() throws RemoteException {}
  /** This method is empty for a stateless session bean */
  public void ejbRemove() throws RemoteException {}


  /** 
   * Sets the associated session context. The container calls this method 
   * after the instance creation. This method is called with no transaction context. 
   * We also retrieve the Home interfaces of all RUBiS's beans.
   *
   * @param sessionContext - A SessionContext interface for the instance. 
   * @exception RemoteException - Thrown if the instance could not perform the function 
   *            requested by the container because of a system-level error. 
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
