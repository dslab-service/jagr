// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id: RequestLog.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
// ========================================================================

package org.mortbay.http;

import java.io.Serializable;
import org.mortbay.util.LifeCycle;

/* ------------------------------------------------------------ */
/** Abstract HTTP Request Log format
 * @version $Id: RequestLog.java,v 1.1.1.1 2003/03/07 08:26:05 emrek Exp $
 * @author Tony Thompson
 * @author Greg Wilkins
 */
public interface RequestLog
    extends LifeCycle,
            Serializable
{
    public void log(HttpRequest request,
                    HttpResponse response,
                    int responseLength);
}

