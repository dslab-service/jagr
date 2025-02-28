/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.mx.server;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import javax.management.DynamicMBean;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.MBeanException;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.loading.DefaultLoaderRepository;

import org.jboss.mx.metadata.StandardMetaData;
import org.jboss.mx.interceptor.Interceptor;
import org.jboss.mx.interceptor.Invocation;
import org.jboss.mx.interceptor.StandardMBeanInterceptor;
import org.jboss.mx.interceptor.LogInterceptor;
import org.jboss.mx.interceptor.SecurityInterceptor;
import org.jboss.mx.interceptor.InvocationException;
import org.jboss.mx.logging.Logger;

/**
 *
 *
 *
 * @author  <a href="mailto:juha@jboss.org">Juha Lindfors</a>.
 * @version $Revision: 1.1.1.1 $
 *
 */
public abstract class MBeanInvoker
         implements DynamicMBean
{
   // Attributes ----------------------------------------------------
   protected Interceptor stack        = null;
   protected Object resource          = null;
   protected Descriptor[] descriptors = null;
   protected MBeanInfo info = null;

   // Static --------------------------------------------------------

   private static final Logger log = Logger.getLogger(MBeanInvoker.class);

   // Public --------------------------------------------------------
   public Object getResource()
   {
      return resource;
   }

   // DynamicMBean implementation -----------------------------------
   public Object invoke(String operationName, Object[] args, String[] signature) throws MBeanException, ReflectionException
   {
      try
      {
         Invocation invocation = new Invocation(
                                    operationName,
                                    Invocation.OPERATION,
                                    0, args, signature, descriptors
                                 );
         return stack.invoke(invocation);
      }
      catch (InvocationException e)
      {
         if (e.getTargetException() instanceof MBeanException)
            throw (MBeanException)e.getTargetException();
         else if (e.getTargetException() instanceof ReflectionException)
            throw (ReflectionException)e.getTargetException();
         else
            throw (RuntimeException)e.getTargetException();
      }
      catch (Throwable t)
      {
         // this indicates an error in the server
         log.error("SERVER ERROR: " + t.getMessage(), t);

         throw new Error(t.toString());
      }
   }

   public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
   {
      try
      {
         Invocation invocation = new Invocation(
                                    attribute,
                                    Invocation.ATTRIBUTE,
                                    Invocation.READ,
                                    null, null, descriptors
                                 );
         return stack.invoke(invocation);
      }
      catch (InvocationException e)
      {
         if (e.getTargetException() instanceof AttributeNotFoundException)
            throw (AttributeNotFoundException)e.getTargetException();
         else if (e.getTargetException() instanceof MBeanException)
            throw (MBeanException)e.getTargetException();
         else if (e.getTargetException() instanceof ReflectionException)
            throw (ReflectionException)e.getTargetException();
         else
            throw (RuntimeException)e.getTargetException();
      }
      catch (Throwable t)
      {
         // this indicates an error in the server
         log.error("SERVER ERROR: " + t.getMessage(), t);

         throw new Error(t.toString());
      }
   }

   public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
   {
      // REVIEW: This NEEDS to be optimized
      // Get the real signature of the attribute
      String attributeClassName = null;
      String attributeName = attribute.getName();
      MBeanAttributeInfo[] attInfos = info.getAttributes();
      for (int i = 0; i < attInfos.length; i++)
      {
         if (attInfos[i].getName().equals(attributeName))
         {
            attributeClassName = attInfos[i].getType();
            break;
         }
      }
      if (attributeClassName == null)
      {
         throw new AttributeNotFoundException(attributeName);
      }

      // Set the value
      try
      {
         Invocation invocation = new Invocation(
                                    attributeName,
                                    Invocation.ATTRIBUTE,
                                    Invocation.WRITE,
                                    new Object[] { attribute.getValue() },
                                    new String[] { attributeClassName },
                                    descriptors
                                 );
         stack.invoke(invocation);
      }
      catch (InvocationException e)
      {
         // FIXME: InvalidAttributeValueException!

         if (e.getTargetException() instanceof AttributeNotFoundException)
            throw (AttributeNotFoundException)e.getTargetException();
         else if (e.getTargetException() instanceof MBeanException)
            throw (MBeanException)e.getTargetException();
         else if (e.getTargetException() instanceof ReflectionException)
            throw (ReflectionException)e.getTargetException();
         else
            throw (RuntimeException)e.getTargetException();
      }
      catch (Throwable t)
      {
         // this indicates an error in the server
         log.error("SERVER ERROR: " + t.getMessage(), t);

         throw new Error(t.toString());
      }
   }

   public AttributeList getAttributes(java.lang.String[] attributes)
   {
      if (attributes == null)    // FIXME: runtimeoperationsexception?
         throw new IllegalArgumentException("null array");

      AttributeList list = new AttributeList();

      for (int i = 0; i < attributes.length; ++i)
      {
         try
         {
            list.add(new Attribute(attributes[i], getAttribute(attributes[i])));
         }
         catch (JMException ignored)
         {
            // if the attribute could not be retrieved, skip it
         }
      }

      return list;
   }

   public AttributeList setAttributes(AttributeList attributes)
   {

      if (attributes == null)     // FIXME: runtimeoperationsexception?
         throw new IllegalArgumentException("null list");

      AttributeList results = new AttributeList();
      Iterator it           = attributes.iterator();

      while (it.hasNext())
      {
         try
         {
            Attribute attr = (Attribute)it.next();
            setAttribute(attr);
            results.add(attr);
         }
         catch (JMException ignored)
         {
            // if unable to set the attribute, skip it
         }
      }

      return results;
   }
}



