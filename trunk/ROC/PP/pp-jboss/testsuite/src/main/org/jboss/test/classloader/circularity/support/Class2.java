/*
 * Class0.java
 *
 * Created on October 19, 2002, 3:34 PM
 */

package org.jboss.test.classloader.circularity.support;

/**
 *
 * @author  starksm
 */
public class Class2
{
   
   /** Creates a new instance of Class0 */
   public Class2()
   {
   }

   public Class0 get0()
   {
      return new Class0();
   }
}
