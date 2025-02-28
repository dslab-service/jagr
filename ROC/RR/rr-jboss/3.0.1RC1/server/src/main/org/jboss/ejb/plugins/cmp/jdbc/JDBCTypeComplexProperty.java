/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.ejb.plugins.cmp.jdbc;

import java.lang.reflect.Method;

/**
 * Immutable class which contins the mapping between a single Java Bean
 * (not an EJB) property and a column. This class has a flattened view of
 * the Java Bean property, which may be several properties deep in the 
 * base Java Bean. The details of how a property is mapped to a column 
 * can be found in JDBCTypeFactory.
 *
 * This class holds a description of the column and, knows how to extract
 * the column value from the Java Bean and how to set a column value info
 * the Java Bean.
 * 
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 1.1.1.1 $
 */
public class JDBCTypeComplexProperty {
   private final String propertyName;
   private final String columnName;   
   private final Class javaType;   
   private final int jdbcType;   
   private final String sqlType;
   private final boolean notNull;
   
   private final Method[] getters;
   private final Method[] setters;

   public JDBCTypeComplexProperty(
         String propertyName,
         String columnName,
         Class javaType,
         int jdbcType,
         String sqlType,
         boolean notNull,
         Method[] getters,
         Method[] setters) {

      this.propertyName = propertyName;
      this.columnName = columnName;
      this.javaType = javaType;
      this.jdbcType = jdbcType;
      this.sqlType = sqlType;
      this.notNull = notNull;
      this.getters = getters;
      this.setters = setters;
   }

   public JDBCTypeComplexProperty(
         JDBCTypeComplexProperty defaultProperty,
         String columnName,
         int jdbcType,
         String sqlType,
         boolean notNull) {

      this.propertyName = defaultProperty.propertyName;
      this.columnName = columnName;
      this.javaType = defaultProperty.javaType;
      this.jdbcType = jdbcType;
      this.sqlType = sqlType;
      this.notNull = notNull;
      this.getters = defaultProperty.getters;
      this.setters = defaultProperty.setters;
   }

   public String getPropertyName() {
      return propertyName;
   }
   
   public String getColumnName() {
      return columnName;
   }
   
   public Class getJavaType() {
      return javaType;
   }
   
   public int getJDBCType() {
      return jdbcType;
   }
   
   public String getSQLType() {
      return sqlType;
   }

   public boolean isNotNull() {
      return notNull;
   }
   
   public Object getColumnValue(Object value) throws Exception {
      Object[] noArgs = new Object[0];
      
      for(int i=0; i<getters.length; i++) {
         if(value == null) {
            return null;
         }
         value = getters[i].invoke(value, noArgs);
      }
      return value;
   }

   public Object setColumnValue(
         Object value,
         Object columnValue) throws Exception {

      // Used for invocation of get and set
      Object[] noArgs = new Object[0];
      Object[] singleArg = new Object[1];
      
      // save the first value to return
      Object returnValue = value;

      // get the second to last object in the chain
      for(int i=0; i<getters.length-1; i++) {
         // get the next object in chain
         Object next = getters[i].invoke(value, noArgs);
         
         // the next object is null creat it
         if(next == null) {
            // new type based on getter
            next = getters[i].getReturnType().newInstance();
            
            // and set it into the current value
            singleArg[0] = next;
            
            setters[i].invoke(value, singleArg);
         }
         
         // update value to the next in chain
         value = next;
      }
      
      // value is now the object on which we need to set the column value
      singleArg[0] = columnValue;
      setters[setters.length-1].invoke(value, singleArg);
      
      // return the first object in call chain
      return returnValue;
   }
}
