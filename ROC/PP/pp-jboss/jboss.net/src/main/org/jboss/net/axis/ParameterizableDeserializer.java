/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

// $Id: ParameterizableDeserializer.java,v 1.1.1.1 2003/03/07 08:26:04 emrek Exp $

package org.jboss.net.axis;

import javax.xml.rpc.encoding.Deserializer;
import java.util.Map;

/**
 * Extended deserializer that may be equipped with additional
 * options.
 * @author jung
 * @created 06.04.2002
 * @version $Revision: 1.1.1.1 $
 */

public interface ParameterizableDeserializer extends Deserializer {

	public Map getOptions();
	public void setOptions(Map options);
		
}