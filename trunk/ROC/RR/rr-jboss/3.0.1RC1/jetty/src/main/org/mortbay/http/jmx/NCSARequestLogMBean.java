// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: NCSARequestLogMBean.java,v 1.1.1.1 2002/10/03 21:07:02 candea Exp $
// ========================================================================

package org.mortbay.http.jmx;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.jmx.LifeCycleMBean;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision: 1.1.1.1 $
 * @author Greg Wilkins (gregw)
 */
public class NCSARequestLogMBean extends LifeCycleMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param sink 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public NCSARequestLogMBean()
        throws MBeanException
    {}    

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("filename");
        defineAttribute("datedFilename");
        defineAttribute("logDateFormat");
        defineAttribute("logTimeZone");
        defineAttribute("retainDays");
        defineAttribute("extended");
        defineAttribute("append");
    }
}
