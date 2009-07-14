/*
 * Class ThreadMonitorEntry
 * Copyright (C) 2001  Juha Lindfors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * This package and its source code is available at www.jboss.org
 * $Id: ThreadMonitorEntry.java,v 1.1.1.1 2002/10/03 21:06:52 candea Exp $
 */     
package org.jboss.admin.dataholder;

// standard imports
import java.io.Serializable;
import javax.jms.Message;
import javax.jms.JMSException;

// non-standard class dependencies
import org.jboss.monitor.MetricsConstants;


/**
 * ...
 *
 * @author  <a href="mailto:jplindfo@helsinki.fi">Juha Lindfors</a>
 */
public class ThreadMonitorEntry implements MetricsConstants,
                                           Serializable {

    private String type        = "<undefined>";
    private int    threadCount = 0;
    private long   time        = 0;
    
/*
 *************************************************************************
 *
 *      CONSTRUCTORS
 *
 *************************************************************************
 */
 
    public ThreadMonitorEntry() {}

    public ThreadMonitorEntry(String type, long time, int threadCount) {
        setType(type);
        setTime(time);
        setThreadCount(threadCount);
    }

    public ThreadMonitorEntry(Message msg) throws JMSException {
        
        setType(msg.getStringProperty("TYPE"));
        setTime(msg.getJMSTimestamp());
        setThreadCount(msg.getIntProperty("ThreadCount"));
    }
    
/*
 *************************************************************************
 *
 *      PUBLIC INSTANCE METHODS
 *
 *************************************************************************
 */
 
    public void setType(String type) {
        if ((type == null) || ("").equals(type))
            return;
            
        this.type = type;
    }
    
    public String getType() {
        return type;
    }
    
    public void setTime(long time) {
        if (time < 0)
            return;
            
        this.time = time;
    }
    
    public long getTime() {
        return time;
    }
    
    public void setThreadCount(int count) {
        if (count < 0)
            return;
            
        this.threadCount = count;
    }
    
    public int getThreadCount() {
        return threadCount;
    }
    
    
/*
 *************************************************************************
 *
 *      METHOD OVERRIDES
 *
 *************************************************************************
 */
 
    public String toString() {
        return "Threads: " + getThreadCount();
    }

}
