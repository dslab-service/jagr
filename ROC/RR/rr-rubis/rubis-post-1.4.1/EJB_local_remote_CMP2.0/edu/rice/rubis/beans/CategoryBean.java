package edu.rice.rubis.beans;

import java.net.URLEncoder;
import java.rmi.*;
import javax.ejb.*;
import javax.rmi.PortableRemoteObject;
import javax.naming.InitialContext;
import java.util.Collection;

/**
 * CategoryBean is an entity bean with "container managed persistence". 
 * The state of an instance is stored into a relational database. 
 * The following table should exist:<p>
 * <pre>
 * CREATE TABLE categories (
 *    id   INTEGER UNSIGNED NOT NULL UNIQUE,
 *    name VARCHAR(50),
 *    PRIMARY KEY(id)
 * );
 * </pre>
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */

public abstract class CategoryBean implements EntityBean 
{
  private EntityContext entityContext;
  private transient boolean isDirty; // used for the isModified function


  /****************************/
  /* Abstract accessor methods*/
  /****************************/


  /**
   * Set category's id
   *
   * @param newId category id
   */
  public abstract void setId(Integer newId);

  /**
   * Get category's id.
   *
   * @return category id
   */
  public abstract Integer getId();


  /**
   * Get the category name.
   *
   * @return category name
   */
  public abstract String getName();


  /**
   * Set category's name
   *
   * @param newName category name
   */
  public abstract void setName(String newName);

  /*****************/
  /* relationships */
  /*****************/

  // This entity bean has a one to many relationship with the Item entity.

  public abstract Collection getItems();
  public abstract void setItems(Collection items);

  /*********************/
  /* ejbSelect methods */
  /*********************/

  /** 
   * Get all the items that match a specific category and that are still
   * to sell (auction end date is not passed). You must select the starting
   * row and number of rows to fetch from the database to get only a limited 
   *number of items.
   * For example, returns 25 Books.
   *
   * @param categoryId id of the category you are looking for
   * @param regionId id of the region you are looking for
   * @param startingRow row where result starts (0 if beginning)
   * @param nbOfRows number of rows to get
   *
   * @return Collection of items
   * @since 1.1
   */
  public abstract Collection ejbSelectCurrentItemsInCategory(Integer categoryId, int startingRow, int nbOfRows) throws FinderException;


  /** 
   * Get all the items that match a specific category and region and
   * that are still to sell (auction end date is not passed). You must
   * select the starting row and number of rows to fetch from the database
   * to get only a limited number of items.
   * For example, returns 25 Books to sell in Houston.
   *
   * @param categoryId id of the category you are looking for
   * @param regionId id of the region you are looking for
   * @param startingRow row where result starts (0 if beginning)
   * @param nbOfRows number of rows to get
   *
   * @return Collection of items
   * @since 1.1
   */
  public abstract Collection ejbSelectCurrentItemsInCategoryAndRegion(Integer categoryId, Integer regionId, int startingRow, int nbOfRows) throws FinderException;


  /*****************/
  /* other methods */
  /*****************/

 /** 
   * Call the corresponding ejbSelect method.
   */
  public Collection getCurrentItemsInCategory(Integer categoryId, int startingRow, int nbOfRows) throws FinderException
  {
      return ejbSelectCurrentItemsInCategory(categoryId, startingRow, nbOfRows);
  }

 /** 
   * Call the corresponding ejbSelect method.
   */
  public Collection getCurrentItemsInCategoryAndRegion(Integer categoryId, Integer regionId, int startingRow, int nbOfRows) throws FinderException
  {
      return ejbSelectCurrentItemsInCategoryAndRegion(categoryId, regionId, startingRow, nbOfRows);
  }

  /**
   * This method is used to create a new Category Bean. Note that the category
   * id is automatically generated by the database (AUTO_INCREMENT) on the
   * primary key.
   *
   * @param categoryName Category name
   *
   * @return pk primary key set to null
   * @exception CreateException if an error occurs
   */
  public CategoryPK ejbCreate(String categoryName) throws CreateException
  {
      // Connecting to IDManager Home interface thru JNDI
      IDManagerLocalHome home = null;
      IDManagerLocal idManager = null;
      
      try 
      {
        InitialContext initialContext = new InitialContext();
        home = (IDManagerLocalHome)initialContext.lookup(
               "java:comp/env/ejb/IDManager");
      } 
      catch (Exception e)
      {
        throw new EJBException("Cannot lookup IDManager: " +e);
      }
     try 
      {
        IDManagerPK idPK = new IDManagerPK(IDManagerPK.HOSTS_KEY);
        try
        {
            idManager = home.findByPrimaryKey(idPK);
        }
        catch (FinderException e)
        {
            idManager = home.create(idPK);
        }

        setId(idManager.getNextCategoryID());
        setName(categoryName);
      } 
      catch (Exception e)
      {
        throw new EJBException("Cannot create category: " +e);
      }
      return null; 
  }


  /** This method just set an internal flag to 
      reload the id generated by the DB */
  public void ejbPostCreate(String categoryName)
  {
    isDirty = true; // the id has to be reloaded from the DB
  }

  /** Persistence is managed by the container and the bean
      becomes up to date */
  public void ejbLoad()
  {
    isDirty = false;
  }

  /** Persistence is managed by the container and the bean
      becomes up to date */
  public void ejbStore()
  {
    isDirty = false;
  }

  /** This method is empty because persistence is managed by the container */
  public void ejbActivate(){}
  /** This method is empty because persistence is managed by the container */
  public void ejbPassivate(){}
  /** This method is empty because persistence is managed by the container */
  public void ejbRemove(){}

  /**
   * Sets the associated entity context. The container invokes this method 
   *  on an instance after the instance has been created. 
   * 
   * This method is called in an unspecified transaction context. 
   * 
   * @param context - An EntityContext interface for the instance. The instance should 
   *              store the reference to the context in an instance variable. 
   * @exception EJBException  Thrown by the method to indicate a failure 
   *                          caused by a system-level error.
   */
  public void setEntityContext(EntityContext context)
  {
    entityContext = context;
  }

  /**
   * Unsets the associated entity context. The container calls this method 
   *  before removing the instance. This is the last method that the container 
   *  invokes on the instance. The Java garbage collector will eventually invoke 
   *  the finalize() method on the instance. 
   *
   * This method is called in an unspecified transaction context. 
   * 
   * @exception EJBException  Thrown by the method to indicate a failure 
   *                          caused by a system-level error.
   */
  public void unsetEntityContext() 
  {
    entityContext = null;
  }

  /**
   * Display category information for the BrowseCategories servlet
   *
   * @return a <code>String</code> containing HTML code
   * @exception RemoteException if an error occurs
   * @since 1.0
   */
  public String printCategory()
  {
    return "<a href=\""+BeanConfig.context+"/servlet/edu.rice.rubis.beans.servlets.SearchItemsByCategory?category="+getId()+
                  "&categoryName="+URLEncoder.encode(getName())+"\">"+getName()+"</a><br>\n";
  }

  /**
   * Display category information for the BrowseCategories servlet
   *
   * @return a <code>String</code> containing HTML code
   * @exception RemoteException if an error occurs
   * @since 1.0
   */
  public String printCategoryByRegion(int regionId)
  {
    return "<a href=\""+BeanConfig.context+"/servlet/edu.rice.rubis.beans.servlets.SearchItemsByRegion?category="+getId()+
      "&categoryName="+URLEncoder.encode(getName())+"&region="+regionId+"\">"+getName()+"</a><br>\n";
  }


  /**
   * Returns true if the beans has been modified.
   * It prevents the EJB server from reloading a bean
   * that has not been modified.
   *
   * @return a <code>boolean</code> value
   */
  public boolean isModified() 
  {
    return isDirty;
  }


  /**
   * Display category information for the BrowseCategories servlet
   *
   * @return a <code>String</code> containing HTML code
   * @exception RemoteException if an error occurs
   * @since 1.0
   */
  public String printCategoryToSellItem(int userId)
  {
      //
      //   eliminate userId from URL arguments
      //
      //    return "<a href=\""+BeanConfig.context+"/servlet/edu.rice.rubis.beans.servlets.SellItemForm?category="+getId()+"&user="+userId+"\">"+getName()+"</a><br>\n";

      return "<a href=\""+BeanConfig.context+"/servlet/edu.rice.rubis.beans.servlets.SellItemForm?category="+getId()+"\">"+getName()+"</a><br>\n";
  }
}
