/**
    Copyright (C) 2004 Emre Kiciman and Stanford University

    This file is part of Pinpoint

    Pinpoint is free software; you can distribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    Pinpoint is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Pinpoint; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
**/
package roc.pinpoint.analysis;

// marked for release 1.0

/**
 * AnalysisEngineListeners are notified when events occur in the
 * AnalysisEngine, such as plugins being loaded or unloaded, or
 * record collections being created and removed.
 */
public interface AnalysisEngineListener {

    void pluginLoaded( AnalysisEngine engine, String id );

    void pluginUnloaded( AnalysisEngine engine, String id );

    void recordCollectionCreated( AnalysisEngine engine, String id );

    void recordCollectionRemoved( AnalysisEngine engine, String id );

}
