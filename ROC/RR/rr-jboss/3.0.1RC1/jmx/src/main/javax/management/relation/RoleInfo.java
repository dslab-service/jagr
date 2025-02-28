/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javax.management.relation;

import java.io.Serializable;
import java.lang.reflect.Modifier;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;

import org.jboss.mx.metadata.MBeanCapability;

/**
 * This class contains information about a role. For example the
 * number of mbean class name of the role, how many mbeans
 * are part of the role, whether the role is read/write, etc.<p>
 *
 * @author  <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>.
 * @version $Revision: 1.1.1.1 $
 *
 */
public class RoleInfo
  implements Serializable
{
   // Constants ---------------------------------------------------

   /**
    * A value used to specify an infinite number of mbeans are allowed
    * in the role
    */
   public static int ROLE_CARDINALITY_INFINITY = -1;

   // Attributes --------------------------------------------------

   /**
    * The name of the role.
    */
   private String name;

   /**
    * The className of the MBeans in the role.
    */
   private String className;

   /**
    * The readable attribute of the role.
    */
   boolean readable;

   /**
    * The writable attribute of the role.
    */
   boolean writable;

   /**
    * The minimum degree of the role.
    */
   int minDegree;

   /**
    * The maximum degree of the role.
    */
   int maxDegree;

   /**
    * The description of the role
    */
   String description;

   // Static ------------------------------------------------------

   // Constructors ------------------------------------------------

   /**
    * Copies the role info.
    *
    * @param other the role to copy.
    * @exception IllegalArgumentException for a null value.
    */
   public RoleInfo(RoleInfo other)
     throws IllegalArgumentException
   {
     if (other == null)
       throw new IllegalArgumentException("Null role info");
     this.name = other.name;
     this.className = other.className;
     this.readable = other.readable;
     this.writable = other.writable;
     this.minDegree = other.minDegree;
     this.maxDegree = other.maxDegree;
     this.description = other.description;
   }

   /**
    * Construct a role info with the given name and class name.
    * It is set to read/writable with a minimum and maximum degree of 1.
    * The description is null.
    *
    * @param name the name of the role.
    * @param className the name of the MBean class.
    * @exception IllegalArgumentException for a null value.
    * @exception ClassNotFoundException when the className does not exist.
    * @exception NotCompliantMBeanException when the className is not an
    *            mbean class.
    */
   public RoleInfo(String name, String className)
     throws IllegalArgumentException, ClassNotFoundException,
            NotCompliantMBeanException
   {
     this(name, className, true, true);
   }

   /**
    * Construct a role info with the given name, class name and
    * read/write attributes. It has a minimum and maximum degree of 1.
    * The description is null.
    *
    * @param name the name of the role.
    * @param className the name of the MBean class.
    * @param readable true for readable, false otherwise.
    * @param writable true for writable, false otherwise.
    * @exception IllegalArgumentException for a null value.
    * @exception ClassNotFoundException when the className does not exist.
    * @exception NotCompliantMBeanException when the className is not an
    *            mbean class.
    */
   public RoleInfo(String name, String className, boolean readable,
                   boolean writable)
     throws IllegalArgumentException, ClassNotFoundException,
            NotCompliantMBeanException
   {
     if (name == null)
       throw new IllegalArgumentException("Null name");
     if (className == null)
       throw new IllegalArgumentException("Null class name");
     this.name = name;
     this.className = className;
     this.readable = readable;
     this.writable = writable;
     minDegree = 1;
     maxDegree = 1;
     checkClass();
   }

   /**
    * Construct a role info with the given name, class name,
    * read/write attributes, minimum/maximum degree and description.
    * The description can be null.<p>
    *
    * Pass <i>ROLE_CARDINALITY_INFINITY</i> for an unlimited degree.
    * The minimum must be less than or equal to the maximum.
    *
    * @param name the name of the role.
    * @param className the name of the MBean class.
    * @param readable true for readable, false otherwise.
    * @param writable true for writable, false otherwise.
    * @param minDegree the minimum degree.
    * @param maxDegree the maximum degree.
    * @param description the description.
    * @exception IllegalArgumentException for a null value.
    * @exception ClassNotFoundException when the className does not exist.
    * @exception NotCompliantMBeanException when the className is not an
    *            mbean class.
    * @exception InvalidRoleInfoException when the minimum degree is
    *            greater than the maximum.
    */
   public RoleInfo(String name, String className, boolean readable,
                   boolean writable, int minDegree, int maxDegree,
                   String description)
     throws IllegalArgumentException, ClassNotFoundException,
            NotCompliantMBeanException, InvalidRoleInfoException
   {
     if (name == null)
       throw new IllegalArgumentException("Null name");
     if (className == null)
       throw new IllegalArgumentException("Null class name");
     if (maxDegree < minDegree && maxDegree != ROLE_CARDINALITY_INFINITY)
       throw new InvalidRoleInfoException("maximum less than minimum");
     if (minDegree == ROLE_CARDINALITY_INFINITY &&
         maxDegree != ROLE_CARDINALITY_INFINITY)
       throw new InvalidRoleInfoException("maximum less than minimum");
     this.name = name;
     this.className = className;
     this.minDegree = minDegree;
     this.maxDegree = maxDegree;
     this.readable = readable;
     this.writable = writable;
     this.description = description;
     checkClass();
   }

   // Public ------------------------------------------------------

   /**
    * Check to see whether a given value is greater than or equal to the
    * minimum degree.
    *
    * @param value the value to check.
    * @return true when it is greater than or equal to the minimum degree,
    *         false otherwise.
    */
   public boolean checkMinDegree(int value)
   {
     return (value >= minDegree || minDegree == ROLE_CARDINALITY_INFINITY);
   }

   /**
    * Check to see whether a given value is less than or equal to the
    * maximum degree.
    *
    * @param value the value to check.
    * @return true when it is less than or equal to the maximum degree,
    *         false otherwise.
    */
   public boolean checkMaxDegree(int value)
   {
     return (value <= maxDegree || maxDegree == ROLE_CARDINALITY_INFINITY);
   }

   /**
    * Retrieve the description of the role.
    *
    * @return the description
    */
   public String getDescription()
   {
     return description;
   }

   /**
    * Retrieve the minimum degree.
    *
    * @return the minimum degree
    */
   public int getMinDegree()
   {
     return minDegree;
   }

   /**
    * Retrieve the maximum degree.
    *
    * @return the maximum degree
    */
   public int getMaxDegree()
   {
     return maxDegree;
   }

   /**
    * Retrieve the name of the role.
    *
    * @return the name
    */
   public String getName()
   {
     return name;
   }

   /**
    * Retrieve the class name of MBeans in this role.
    *
    * @return the class name
    */
   public String getRefMBeanClassName()
   {
     return className;
   }

   /**
    * Retrieve the readable attribute.
    *
    * @return true for readable, false otherwise
    */
   public boolean isReadable()
   {
     return readable;
   }

   /**
    * Retrieve the writable attribute.
    *
    * @return true for writable, false otherwise
    */
   public boolean isWritable()
   {
     return writable;
   }

   // Object Overrides --------------------------------------------

   /**
    * Retrieve a string description of the role info.
    */
   public String toString()
   {
     StringBuffer buffer = new StringBuffer("RoleInfo for name: (");
     buffer.append(name);
     buffer.append(") class name: (");
     buffer.append(className);
     buffer.append(") description: (");
     buffer.append(description);
     buffer.append(") readable: (");
     buffer.append(readable);
     buffer.append(") writable: (");
     buffer.append(writable);
     buffer.append(") minimum degree: (");
     buffer.append(minDegree);
     buffer.append(") maximum degree: (");
     buffer.append(maxDegree);
     buffer.append(")");
     return buffer.toString();
   }

   // Private -----------------------------------------------------

   /**
    * Check the MBean class name.
    *
    * @exception ClassNotFoundException when the className does not exist.
    * @exception NotCompliantMBeanException when the className is not an
    *            mbean class.
    */
   private void checkClass()
     throws ClassNotFoundException, NotCompliantMBeanException
   {
     Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);

     // See if this is capable of being an MBean
     MBeanCapability.of(clazz);
   }
}
