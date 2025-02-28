// ========================================================================
// Copyright (c) 1999-2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: SecurityHandler.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import org.mortbay.http.BasicAuthenticator;
import org.mortbay.http.ClientCertAuthenticator;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.util.Code;

/* ------------------------------------------------------------ */
/** Handler to enforce SecurityConstraints.
 *
 * @version $Id: SecurityHandler.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
 * @author Greg Wilkins (gregw)
 */
public class SecurityHandler extends AbstractHttpHandler
{   
    /* ------------------------------------------------------------ */
    private String _authMethod=SecurityConstraint.__BASIC_AUTH;
    private String _realmName ;
    private boolean _realmForced=false;

    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return _authMethod;
    }
    
    /* ------------------------------------------------------------ */
    public void setAuthMethod(String method)
    {
        if (isStarted() && _authMethod!=null && !_authMethod.equals(method))
            throw new IllegalStateException("Handler started");
        _authMethod = method;
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        if (getHttpContext().getAuthenticator()==null)
        {
            // Find out the Authenticator.
            if (SecurityConstraint.__BASIC_AUTH.equalsIgnoreCase(_authMethod))
                getHttpContext().setAuthenticator(new BasicAuthenticator());
            else if (SecurityConstraint.__CERT_AUTH.equalsIgnoreCase(_authMethod))
                getHttpContext().setAuthenticator(new ClientCertAuthenticator());
            else
                Code.warning("Unknown Authentication method:"+_authMethod);
        }
        
        super.start();
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        getHttpContext().checkSecurityContstraints(pathInContext,request,response);
    }

}

