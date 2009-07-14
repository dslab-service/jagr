/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package javax.management;

/**
 * A String that is an arguement to a query.<p>
 * 
 * <p><b>Revisions:</b>
 * <p><b>20020317 Adrian Brock:</b>
 * <ul>
 * <li>Make queries thread safe
 * </ul>
 *
 * @author  <a href="mailto:Adrian.Brock@HappeningTimes.com">Adrian Brock</a>.
 * @version $Revision: 1.1.1.1 $
 */
public class AttributeValueExp
   extends ValueExpSupport
{
   // Constants ---------------------------------------------------

   // Attributes --------------------------------------------------

   /**
    * The attribute name
    */
   private String value;

   // Static  -----------------------------------------------------

   // Constructors ------------------------------------------------

   /**
    * Construct an attribute value expression for the null attribute name
    */
   public AttributeValueExp()
   {
   }

   /**
    * Construct an attribute value expression for the passed attribute name
    *
    * @param value the attribute name
    */
   public AttributeValueExp(String value)
   {
      this.value = value;
   }

   // Public ------------------------------------------------------

   /**
    * Get the attribute name.
    *
    * @return the attribute name
    */
   public String getAttributeName()
   {
      return value;
   }

   // ValueExp Implementation -------------------------------------

   public ValueExp apply(ObjectName name)
      throws BadStringOperationException,
             BadBinaryOpValueExpException,
             BadAttributeValueExpException,
             InvalidApplicationException
   {
      Object object = getAttribute(name);
      if (object instanceof String)
         return new StringValueExp((String) object);
      if (object instanceof Boolean)
         return new BooleanValueExp((Boolean) object);
      if (object instanceof Number)
         return new NumberValueExp((Number) object);
//REVIEW: boolean, int, long, float, double how?
      throw new BadAttributeValueExpException(object);
   }

   // Object overrides --------------------------------------------

   public String toString()
   {
      return value;
   }

   // Protected ---------------------------------------------------

   /**
    * Get the value of the attribute for a given object name
    *
    * @param the object name
    * @return the value of the attribute
    */
   protected Object getAttribute(ObjectName name)
   {
      try
      {
         return getMBeanServer().getAttribute(name, value);
      }
      catch (Exception e)
      {
// REVIEW: What happens now?
         return null;
      }
   }

   // Package Private ---------------------------------------------

   // Private -----------------------------------------------------

   // Inner Classes -----------------------------------------------
}
