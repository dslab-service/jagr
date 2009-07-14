// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id: ServletHolder.java,v 1.11 2003/04/07 08:55:32 emrek Exp $
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.Date;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.util.Code;
import org.mortbay.util.URI;
import org.jboss.RR.FailureReport;

       //// MIKECHEN: BEGIN ////
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;
       //// MIKECHEN: END   ////
import roc.config.ROCConfig;


/* --------------------------------------------------------------------- */
/** Servlet Instance and Context Holder.
 * Holds the name, params and some state of a javax.servlet.Servlet
 * instance. It implements the ServletConfig interface.
 * This class will organise the loading of the servlet when needed or
 * requested.
 *
 * @version $Id: ServletHolder.java,v 1.11 2003/04/07 08:55:32 emrek Exp $
 * @author Greg Wilkins
 */
public class ServletHolder extends Holder
    implements Comparable
{
    /* ---------------------------------------------------------------- */
    
    private int _initOrder;
    private boolean _initOnStartup=false;
    private Map _roleMap;
    private String _forcedPath;
    private String _run_as;
    private UserRealm _realm;

    
    private transient Stack _servlets;
    private transient Servlet _servlet;
    private transient Config _config;
    private transient long _unavailable;
    private transient UnavailableException _unavailableEx;

    
    /* ---------------------------------------------------------------- */
    /** Constructor for Serialization.
     */
    ServletHolder()
    {}
    

    /* ---------------------------------------------------------------- */
    /** Constructor.
     * @param handler The ServletHandler instance for this servlet.
     * @param name The name of the servlet.
     * @param className The class name of the servlet.
     */
    ServletHolder(ServletHandler handler,
                  String name,
                  String className)
    {
        super(handler,(name==null)?className:name,className);
    }

    /* ---------------------------------------------------------------- */
    /** Constructor. 
     * @param handler The ServletHandler instance for this servlet.
     * @param name The name of the servlet.
     * @param className The class name of the servlet.
     * @param forcedPath If non null, the request attribute
     * javax.servlet.include.servlet_path will be set to this path before
     * service is called.
     */
    ServletHolder(ServletHandler handler,
                  String name,
                  String className,
                  String forcedPath)
    {
        this(handler,(name==null)?className:name,className);
        _forcedPath=forcedPath;
    }

    
    /* ------------------------------------------------------------ */
    public int getInitOrder()
    {
        return _initOrder;
    }

    /* ------------------------------------------------------------ */
    /** Set the initialize order.
     * Holders with order<0, are initialized on use. Those with
     * order>=0 are initialized in increasing order when the handler
     * is started.
     */
    public void setInitOrder(int order)
    {
        _initOnStartup=true;
        _initOrder = order;
    }

    /* ------------------------------------------------------------ */
    /** Comparitor by init order.
     */
    public int compareTo(Object o)
    {
        if (o instanceof ServletHolder)
        {
            ServletHolder sh= (ServletHolder)o;
            if (sh==this)
                return 0;
            if (sh._initOrder<_initOrder)
                return 1;
            if (sh._initOrder>_initOrder)
                return -1;
            int c=_className.compareTo(sh._className);
            if (c==0)
                c=_name.compareTo(sh._name);
            if (c==0)
                c=this.hashCode()>o.hashCode()?1:-1;
            return c;
        }
        return 1;
    }

    /* ------------------------------------------------------------ */
    public boolean equals(Object o)
    {
        return compareTo(o)==0;
    }

    /* ---------------------------------------------------------------- */
    public ServletContext getServletContext()
    {
        return ((ServletHandler)_httpHandler).getServletContext();
    }

    /* ------------------------------------------------------------ */
    /** Link a user role.
     * Translate the role name used by a servlet, to the link name
     * used by the container.
     * @param name The role name as used by the servlet
     * @param link The role name as used by the container.
     */
    public synchronized void setUserRoleLink(String name,String link)
    {
        if (_roleMap==null)
            _roleMap=new HashMap();
        _roleMap.put(name,link);
    }
    
    /* ------------------------------------------------------------ */
    /** get a user role link.
     * @param name The name of the role
     * @return The name as translated by the link. If no link exists,
     * the name is returned.
     */
    public String getUserRoleLink(String name)
    {
        if (_roleMap==null)
            return name;
        String link=(String)_roleMap.get(name);
        return (link==null)?name:link;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param role Role name that is added to UserPrincipal when this servlet
     * is called. 
     */
    public void setRunAs(String role)
    {
        _run_as=role;
    }
    
    /* ------------------------------------------------------------ */
    public String getRunAs()
    {
        return _run_as;
    }
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        _unavailable=0;
        super.start();
        
        if (!javax.servlet.Servlet.class
            .isAssignableFrom(_class))
        {
            Exception ex = new IllegalStateException("Servlet "+_class+
                                            " is not a javax.servlet.Servlet");
            super.stop();
            throw ex;
        }        

        if (javax.servlet.SingleThreadModel.class
            .isAssignableFrom(_class))
            _servlets=new Stack();

        if (_initOnStartup)
        {
            _servlet=(Servlet)newInstance();
            _config=new Config();
            try
            {
                _servlet.init(_config);
            }
            catch(Throwable e)
            {
                _servlet=null;
                _config=null;
                if (e instanceof Exception)
                    throw (Exception) e;
                else if (e instanceof Error)
                    throw (Error)e;
                else
                    throw new ServletException(e);
            }            
        }

        if (_run_as!=null)
            _realm=((ServletHandler)_httpHandler).getHttpContext().getRealm();
        
    }

    /* ------------------------------------------------------------ */
    public void stop()
    {
        if (_servlet!=null)
            _servlet.destroy();
        _servlet=null;
        
        while (_servlets!=null && _servlets.size()>0)
        {
            Servlet s = (Servlet)_servlets.pop();
            s.destroy();
        }
        _config=null;
        super.stop();
    }
    

    /* ------------------------------------------------------------ */
    /** Get the servlet.
     * @return The servlet
     */
    public synchronized Servlet getServlet()
        throws UnavailableException
    {
        // Handle previous unavailability
        if (_unavailable!=0)
        {
            if (_unavailable<0 || _unavailable>0 && System.currentTimeMillis()<_unavailable)
                throw _unavailableEx;
            _unavailable=0;
            _unavailableEx=null;
        }
        
        try
        {
            if (_servlet==null)
                _servlet=(Servlet)newInstance();
        
            if (_config==null)
            {
                _config=new Config();
                _servlet.init(_config);
            }
            
            if (_servlets!=null)
            {
                Servlet servlet=null;
                if (_servlets.size()==0)
                {
                    servlet= (Servlet)newInstance();
                    servlet.init(_config);
                }
                else
                    servlet = (Servlet)_servlets.pop();

                return servlet;
            }

            return _servlet;
        }
        catch(UnavailableException e)
        {
            _servlet=null;
            _config=null;
            _unavailableEx=e;
            _unavailable=-1;
            if (_unavailableEx.getUnavailableSeconds()>0)
                _unavailable=System.currentTimeMillis()+
                    1000*_unavailableEx.getUnavailableSeconds();
            throw _unavailableEx;
        }
        catch(Exception e)
        {
            _servlet=null;
            _config=null;
            Code.warning(e);
            throw new UnavailableException(_servlet,e.toString());
        }    
    }
    
    /* ------------------------------------------------------------ */
    /** Service a request with this servlet.
     */
    public void handle(ServletRequest request,
                       ServletResponse response)
        throws ServletException,
               UnavailableException,
               IOException
    {
        if (_class==null)
            throw new UnavailableException("Servlet Not Initialized");
        
        Servlet servlet=_servlet;
        if (servlet==null || _servlets!=null)
            servlet=getServlet();
        
        // Check that we got one in the end
        if (servlet==null)
            throw new UnavailableException("Could not instantiate "+_class);

        // Service the request
        boolean servlet_error=true;
        UserPrincipal user=null;
        HttpRequest http_request=null;


	//// ROC PINPOINT MIKECHEN EMK BEGIN ////

	Map PP_originInfo = null;
	Map PP_attributes = null;

	if( ROCConfig.ENABLE_PINPOINT &&
	    ROCConfig.ENABLE_PINPOINT_TRACING_SERVLET ) {

	    PP_originInfo = new HashMap( roc.pinpoint.tracing.java.EnvironmentDetails.GetDetails() );
	    String name = servlet.getClass().getName();
	    int idx = 1 + name.lastIndexOf(".");
	    name = name.substring(idx).trim();
	    PP_originInfo.put( "name", name );
	    PP_originInfo.put( "type", "servlet" );

	    PP_attributes = new HashMap();
	    PP_attributes.put( "observationLocation", 
			       "org.mortbay.jetty.servlet.ServletHolder" );
	}
	//// ROC PINPOINT MIKECHEN EMK END   ////

        try
        {
            // Handle aliased path
            if (_forcedPath!=null)
                request.setAttribute("javax.servlet.include.servlet_path",_forcedPath);

            // Handle run as
            if (_run_as!=null && _realm!=null)
            {
                ServletHttpRequest servletHttpRequest=
                    ServletHttpRequest.unwrap(request);
                http_request=servletHttpRequest.getHttpRequest();

                user=_realm.pushRole(http_request.getUserPrincipal(),_run_as);
                http_request.setUserPrincipal(user);
            }
            

	    //// ROC PINPOINT EMK BEGIN
	    if( ROCConfig.ENABLE_PINPOINT &&
		ROCConfig.ENABLE_PINPOINT_TRACING_SERVLET ) {
		// report that we're about to start the servlet
		roc.pinpoint.tracing.RequestInfo PP_reqInfo = roc.pinpoint.tracing.ThreadedRequestTracer.getRequestInfo();
		PP_reqInfo.incrementSeqNum();
		PP_attributes.put( "stage", "METHODCALLBEGIN" );
		
		roc.pinpoint.tracing.Observation PP_obs = 
		    new roc.pinpoint.tracing.Observation( roc.pinpoint.tracing.Observation.EVENT_COMPONENT_USE,
							  PP_reqInfo,
							  PP_originInfo,
							  null, 
							  PP_attributes );
		
		roc.pinpoint.tracing.GlobalObservationPublisher.Send( PP_obs );
	    }
	    //// ROC PINPOINT EMK END


	    // ROC PINPOINT EMK BEGIN
	    if( ROCConfig.ENABLE_PINPOINT &&
		ROCConfig.ENABLE_PINPOINT_TRACING_SERVLET &&
		ROCConfig.ENABLE_PINPOINT_FAULT_INJECTION ) {
		// fault injection
		int fault = roc.pinpoint.injection.FaultGenerator.CheckFaultTriggers( PP_originInfo );
		if( fault == roc.pinpoint.injection.FaultTrigger.FT_NOFAULT ) {
		    // invoke normally . don't inject any fault
		    servlet.service(request,response);
		    servlet_error = false;
		} else if( roc.pinpoint.injection.FaultGenerator.isAutomatableFault( fault )) {
		    roc.pinpoint.injection.FaultGenerator.GenerateFault( fault );
		}
		else if( fault == roc.pinpoint.injection.FaultTrigger.FT_THROWEXPECTEDEXCEPTION ) {
		    // todo: what's the best thing to do? we'll just use the
		    //  ServletException now.
		    throw new ServletException( "injected fault" );
		}
		else if( fault == roc.pinpoint.injection.FaultTrigger.FT_NULLCALL ) {
		    // do nothing. it's a null call.
		}
	    }
	    // ROC PINPOINT EMK END
	    else {
		// original code
		servlet.service(request,response);
		servlet_error=false;
	    }
        }
        catch(UnavailableException e)
        {
            if (_servlets!=null && servlet!=null)
                servlet.destroy();
            servlet=null;
            throw e;
        }
	catch(IOException ioe) {
	    reportException(request, servlet, ioe);
	}
	catch(ServletException se) {
	    reportException(request, servlet, se);
	}

        finally
        {


            // pop run-as role
            if (_run_as!=null && _realm!=null && user!=null)
            {
                user=_realm.popRole(user);
                http_request.setUserPrincipal(user);
            }

            // Handle error params.
            if (servlet_error)
                request.setAttribute("javax.servlet.error.servlet_name",getName());

            // Return to singleThreaded pool
            synchronized(this)
            {
                if (_servlets!=null && servlet!=null)
                    _servlets.push(servlet);
            }

	    //// ROC PINPOINT MIKECHEN EMK BEGIN
	    if( ROCConfig.ENABLE_PINPOINT &&
		ROCConfig.ENABLE_PINPOINT_TRACING_SERVLET ) {
		// if there was an error, report it...
		if( servlet_error ) {
		    roc.pinpoint.tracing.RequestInfo PP_reqInfo = roc.pinpoint.tracing.ThreadedRequestTracer.getRequestInfo();
		    PP_reqInfo.incrementSeqNum();
		    if( PP_attributes.containsKey( "stage" ))
			PP_attributes.remove( "stage" );
		    
		    roc.pinpoint.tracing.Observation PP_obs = 
			new roc.pinpoint.tracing.Observation( roc.pinpoint.tracing.Observation.EVENT_ERROR,
							      PP_reqInfo,
							      PP_originInfo,
							      null, 
							      PP_attributes );
		    roc.pinpoint.tracing.GlobalObservationPublisher.Send( PP_obs );
		}

		// report that we're finished with servlet
		roc.pinpoint.tracing.RequestInfo PP_reqInfo = roc.pinpoint.tracing.ThreadedRequestTracer.getRequestInfo();
		PP_reqInfo.incrementSeqNum();
		PP_attributes.put( "stage", "METHODCALLEND" );
		
		roc.pinpoint.tracing.Observation PP_obs = 
		    new roc.pinpoint.tracing.Observation( roc.pinpoint.tracing.Observation.EVENT_COMPONENT_USE,
							  PP_reqInfo,
							  PP_originInfo,
							  null, 
							  PP_attributes );
		roc.pinpoint.tracing.GlobalObservationPublisher.Send( PP_obs );
		
		// save our seq information in the response
		try {
		    ((HttpServletResponse)response).setHeader("TRACER_REQUEST_ID", PP_reqInfo.getRequestId() );
		    ((HttpServletResponse)response).setHeader("TRACER_REQUEST_SEQ", "" +  PP_reqInfo.getSeqNum() );
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    //// ROC PINPOINT MIKECHEN EMK END
        }
    }


    protected void reportException(ServletRequest request, Servlet servlet, Exception e) 
    {
	if (!(e instanceof org.apache.jasper.JasperException)) 
        {

	    //// MIKECHEN: BEGIN ////
            System.err.println("=============================================================");
            System.err.println("================  BEGIN STACKTRACE ==========================");
            e.printStackTrace();
            System.err.println("================  END   STACKTRACE ==========================");
            System.err.println("=============================================================");

            try 
            {
		String caller = null;
		String callee = null;
		String callerMethod = null;
		String calleeMethod = null;
	   
		callee = servlet.getClass().getName();
		calleeMethod = "unknown";

		HttpServletRequest httpRequest = (HttpServletRequest)request;
		caller = httpRequest.getRequestURI();
		callerMethod = httpRequest.getQueryString();
		System.err.println(caller + "." + callerMethod + " --> " + callee + "." + calleeMethod);
                
		//// if we have found 2 EJB calls, then report to the FailureMonitor
		//if (remaining == 0) {
		if (caller != null && callee != null) 
                {
                    // START STEVE ZHANG

                    // Use the short name
                    int idx1 = 1 + callee.lastIndexOf(".");
                    int idx2 = 1 + caller.lastIndexOf(".");

                    // send directly to brain
                    
                    try
                    {  
                        // temperarily hardcoded
                        InetSocketAddress sockAddr = new InetSocketAddress(InetAddress.getLocalHost(), 
									   2374);
                        DatagramSocket socket = new DatagramSocket();
                        FailureReport report = new FailureReport((callee.substring(idx1)).trim(), 
                                                                 (caller.substring(idx2)).trim(), 
								 new Date()); 
                        ByteArrayOutputStream bArray_out = new ByteArrayOutputStream();
                        ObjectOutputStream obj_out = new ObjectOutputStream(bArray_out);
                        obj_out.writeObject(report);
                        DatagramPacket packet = new DatagramPacket(bArray_out.toByteArray(), 
                                                                   bArray_out.size(), 
                                                                   sockAddr);
                        socket.send(packet);
                        System.out.println("ServletHolder: FailureReport message sent to TheBrain!");
                    }
                    catch (SocketException sockExp)
                    {
                        System.err.println("ServletHolder: failed to bind to a UDP port!");
                    }
                    catch (IOException ioExp)
                    {
                        System.err.println("ServletHolder: Error sending FailureReport to TheBrain!");
                    }
                    // END STEVE ZHANG

		    // for now we do both (report to brain and to FailureMonitor)
                    
		    System.err.println("======= invoking FailureMonitor ====== ");
		    //// report to the FaultMonitor MBean
		    InitialContext ic = new InitialContext();
		    //RMIAdaptor server = (RMIAdaptor) ic.lookup("jmx:localhost:rmi");
		    String serverName = java.net.InetAddress.getLocalHost().getHostName();
		    RMIAdaptor server = (RMIAdaptor) ic.lookup("jmx:" + serverName + ":rmi");
	       
		    ObjectName name = new ObjectName("jboss:type=FailureMonitor");
		    //MBeanInfo info = server.getMBeanInfo(name);
		    //ObjectInstance instance = server.getObjectInstance(name);
		    //String[] sig = {"org.jboss.ejb.Container", "org.jboss.ejb.Container"};
		    String[] sig = {"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.Throwable"};
		    Object[] opArgs = {caller, callerMethod, callee, calleeMethod, null};
		    Object result = server.invoke(name, "reportFailure", opArgs, sig);
		    //System.out.println("JNDIView.list(true) output:\n"+result);
		    //Thread.sleep(10000);
		}
            }
                
            catch (Exception e2) {
                e2.printStackTrace();
            }
	}
    }

	    //// MIKECHEN: END   ////

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Config implements ServletConfig
    {   
        /* -------------------------------------------------------- */
        public String getServletName()
        {
            return getName();
        }
        
        /* -------------------------------------------------------- */
        public ServletContext getServletContext()
        {
            return ((ServletHandler)_httpHandler).getServletContext();
        }

        /* -------------------------------------------------------- */
        public String getInitParameter(String param)
        {
            return ServletHolder.this.getInitParameter(param);
        }
    
        /* -------------------------------------------------------- */
        public Enumeration getInitParameterNames()
        {
            return ServletHolder.this.getInitParameterNames();
        }
    }
}





