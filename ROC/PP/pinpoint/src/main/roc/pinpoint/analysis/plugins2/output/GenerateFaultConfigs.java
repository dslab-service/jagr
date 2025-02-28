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
package roc.pinpoint.analysis.plugins2.output;

// marked for release 1.0

import java.util.*;

import roc.pinpoint.analysis.*;
import roc.pinpoint.analysis.structure.*;
import roc.pinpoint.injection.FaultTrigger;


/**
 * @author emrek
 *
 */
public class GenerateFaultConfigs implements Plugin, RecordCollectionListener {

    public static final String INPUT_COLLECTION_NAME_ARG = "inputCollection";
    public static final String OUTPUT_COLLECTION_NAME_ARG = "outputCollection";
    public static final String DEFINING_ATTRS_ARG = "definingAttrs";

    PluginArg[] args = {
	new PluginArg( INPUT_COLLECTION_NAME_ARG,
		       "input collection.  this plugin will look for components in this record collection",
		       PluginArg.ARG_RECORDCOLLECTION,
		       true,
		       null ),
	new PluginArg( OUTPUT_COLLECTION_NAME_ARG,
		       "output collection.  this plugin will save String records(the xml fault configs) into this collection",
		       PluginArg.ARG_RECORDCOLLECTION,
		       true,
		       null ),
	new PluginArg( DEFINING_ATTRS_ARG,
		       "defining attributes of a component",
		       PluginArg.ARG_LIST,
		       true,
		       null )
    };

    RecordCollection inputRecordCollection;
    RecordCollection outputRecordCollection;
    Collection definingAttrs;

    HashSet alreadyProcessed;

    public PluginArg[] getPluginArguments() {
	return args;
    }

    /**
     * @see roc.pinpoint.analysis.Plugin#start(java.lang.String, java.util.Map, roc.pinpoint.analysis.AnalysisEngine)
     */
    public void start(String id, Map args, AnalysisEngine engine)
        throws PluginException {

        inputRecordCollection =
	    (RecordCollection) args.get(INPUT_COLLECTION_NAME_ARG);
	outputRecordCollection =
	    (RecordCollection) args.get(OUTPUT_COLLECTION_NAME_ARG);
        definingAttrs = (Collection)args.get( DEFINING_ATTRS_ARG );

	alreadyProcessed = new HashSet();

	inputRecordCollection.registerListener( this );
    }

    /**
     * @see roc.pinpoint.analysis.Plugin#stop()
     */
    public void stop() throws PluginException {
        inputRecordCollection.unregisterListener(this);
	alreadyProcessed = null;
    }

    /**
     * @see roc.pinpoint.analysis.RecordCollectionListener#addedRecords(java.lang.String, java.util.List)
     */
    public synchronized void addedRecord(String collectionName, Record rec) {
	    Component c = (Component)rec.getValue();

	    Map id = IdentifiableHelper.ReduceMap( c.getId(), definingAttrs );

	    if( alreadyProcessed.contains( id ))
		return;

	    outputFaultConfig( id );
	    alreadyProcessed.add( id );

    }

    void outputFaultConfig( Map id ) {
	String fc1 = 
	    "<faults>" + 
	    "<name>AutoGenerated Config for " + id.toString() + "</name>\n" +
	    "<faultTrigger>" +
	    "<faulttype>";

	String fc2 = 
	    "</faulttype>" +
	    "<component>";

	String fc3 = 
	    "</component>" +
	    "</faultTrigger>" +
	    "</faults>";


	String xmlComponentId = "";
	String componentId = "";

	Iterator iter = id.keySet().iterator();
	while( iter.hasNext() ) {
	    String key = (String)iter.next();

	    xmlComponentId += "<attr key=\"" + key + "\">" + 
		(String)id.get( key ) + "</attr>\n";
	    componentId += key + "_" + (String)id.get( key );
	}


	for( int i=0; i < FaultTrigger.FAULT_TYPE_STRINGS.length; i++ ) {
	    String fc = 
		fc1 + 
		FaultTrigger.FAULT_TYPE_STRINGS[i] +
		fc2 +
		xmlComponentId +
		fc3;
	    
	    Record rec = new Record( fc );
	    outputRecordCollection.setRecord( componentId + "-" + FaultTrigger.FAULT_TYPE_STRINGS[i], rec );
	}

    }
    

    /**
     * @see roc.pinpoint.analysis.RecordCollectionListener#removedRecords(java.lang.String, java.util.List)
     */
    public void removedRecords(String collectionName, List items) {
    }

}
