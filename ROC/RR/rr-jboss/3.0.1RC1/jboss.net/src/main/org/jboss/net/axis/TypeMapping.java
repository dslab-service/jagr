/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// $Id: TypeMapping.java,v 1.1.1.1 2002/10/03 21:07:02 candea Exp $

package org.jboss.net.axis;

import org.apache.axis.deployment.wsdd.WSDDException;
import org.apache.axis.deployment.wsdd.WSDDTypeMapping;
import org.apache.axis.utils.LockableHashtable;

import org.w3c.dom.Element;

import java.util.Map;

/**
 * A parameterizable typemapping.
 * <br>
 * <h3>Change History</h3>
 * <ul>
 * </ul>
 * @author <a href="mailto:Christoph.Jung@infor.de">Christoph G. Jung</a>
 * @created 06.04.2002
 * @version $Revision: 1.1.1.1 $
 */


public class TypeMapping extends WSDDTypeMapping {

	//
	// Attributes
	//
	
	/** carries the options */
   protected LockableHashtable parameters = new LockableHashtable();

	//
	// Constructors 
	//
	
   /**
    * Constructor for TypeMapping.
    */
   public TypeMapping() {
      super();
   }

   /**
    * Constructor for TypeMapping.
    * @param e
    * @throws WSDDException
    */
   public TypeMapping(Element e) throws WSDDException {
      super(e);
      readParams(e);
   }

   /**
    * Constructor for TypeMapping.
    * @param e
    * @param b
    * @throws WSDDException
    */
   protected TypeMapping(Element e, boolean b) throws WSDDException {
      super(e, b);
      readParams(e);
   }

	//
	// protected helpers
	//
	
	/** reads out the additional parameter elements */
   protected void readParams(Element e) {
      Element[] paramElements = getChildElements(e, "parameter");

      for (int i = 0; i < paramElements.length; i++) {
         Element param = paramElements[i];
         String pname = param.getAttribute("name");
         String value = param.getAttribute("value");
         String locked = param.getAttribute("locked");
         parameters.put(
            pname,
            value,
            (locked != null && locked.equalsIgnoreCase("true")));
      }

   }

	//
	// public API
	//
	
	/** returns the set of parameters */   
   public Map getParametersTable() {
      return parameters;
   }
   
} // TypeMapping