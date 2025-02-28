package edu.rice.rubis.beans;

/**
 * IDManager Primary Key class
 * We should have only one instance of the IDManager bean so the value 
 * of the primary key is 0.
 * @author <a href="mailto:cecchet@rice.edu">Emmanuel Cecchet</a> and <a href="mailto:julie.marguerite@inrialpes.fr">Julie Marguerite</a>
 * @version 1.0
 */
public class IDManagerPK implements java.io.Serializable {
  
  public Integer id; 

  //machine dependent number. like rr4--> new Integer(0)
  //rr5 --> new Integer(1). But note that one instantce
  //should be always kept Integer(0).
  //public static Integer HOSTS_KEY = <Machine dependent number>;
  public static Integer HOSTS_KEY = new Integer(0);
  //public static int QUANTITY_PER_HOST = <maximum quantity of ids per hosts>;
  public static int QUANTITY_PER_HOST = 1000000;

  /**
   * Creates a new <code>IDManagerPK</code> instance.
   *
   * @param uniqueId an <code>Integer</code> value
   */
  public IDManagerPK()
  {
    id = new Integer(0);
  }

  /**
   * Creates a new <code>IDManagerPK</code> instance.
   *
   * @param uniqueId an <code>Integer</code> value
   */
  public IDManagerPK(Integer uniqueId)
  {
      //id = uniqueId;
      id = PKFaultInjector.getId("IDManagerPK", uniqueId);
  }

  /**
   * Specific <code>hashCode</code> just returning the id.
   *
   * @return the hash code
   */
  public int hashCode() 
  {
    if (id == null)
      return 0;
    else
      return id.intValue();
  }
  
  /**
   * Specific <code>equals</code> method.
   *
   * @param other the <code>Object</code> to compare with
   * @return true if both objects have the same primary key
   */
  public boolean equals(Object other) 
  {
    boolean isEqual = false;
    if (other instanceof IDManagerPK)
    {
      if (id == null)
        isEqual = (id == ((IDManagerPK)other).id);
      else
        isEqual = (id.intValue() == ((IDManagerPK)other).id.intValue());
    }
    return isEqual;
  }

}
