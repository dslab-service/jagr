package roc.pinpoint.injection;

import java.io.IOException;
import java.io.File;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import swig.util.StringHelper;
import swig.util.XMLException;
import swig.util.XMLHelper;
import swig.util.XMLStructs;

public class FaultTrigger {

    public static int FT_NOFAULT = 0;
    public static int FT_THROWRUNTIMEEXCEPTION = 1;
    public static int FT_INFINITELOOP = 2;
    public static int FT_HALTJVM = 3;

    // marks the boundary between faults generatable by
    // the "FaultTrigger.GenerateFault()" function and those
    // that must be generated by the j2ee-caller.
    protected static int IS_AUTOMATIC_FAULT = 3;

    public static int FT_THROWEXPECTEDEXCEPTION = 4;
    public static int FT_NULLCALL = 5;

    public static int MAX_FT = FT_NULLCALL;

    public static String[] FAULT_TYPE_STRINGS = new String[] {
	"no fault", "runtime exception", "infinite loop",
	"halt", "expected exception", "null call"
    };

    int faultType;

    Set triggerComponents;  // a set of component definitions.  if we see any of these components, should trigger a fault.

    public FaultTrigger() {
	faultType = FT_NOFAULT;
	triggerComponents = new HashSet();
    }

    public FaultTrigger( int faultType, Set triggerComponents ) {
	this.faultType = faultType;
	this.triggerComponents = triggerComponents;
    }

    public static FaultTrigger ParseFaultTrigger( Element ftXML ) 
	throws XMLException {
	
	FaultTrigger ret = new FaultTrigger();

	String sFaultType = XMLHelper.GetChildText( ftXML, "faulttype" );
	ret.faultType = -1;
	for( int i=0; i<FAULT_TYPE_STRINGS.length; i++ ) {
	    if( FAULT_TYPE_STRINGS[i].equals( sFaultType )) {
		ret.faultType = i;
		break;
	    }
	}

	if( ret.faultType == -1 ) {
	    // ACK HUGE ERROR
	    //System.err.println( "FaultTrigger.java: [config file error] unrecognized fault type: " + sFaultType );
	}

	NodeList nlComponents = XMLHelper.GetChildrenByTagName( ftXML,
								"component" );
	for( int i=0; i<nlComponents.getLength(); i++ ) {
	    Element eComponentMap = (Element)nlComponents.item(i);
	    Map componentMap = XMLStructs.ParseMap( eComponentMap, "attr" );
	    //	    System.err.println( "faulttrigger: read component: " + componentMap );

	    ret.triggerComponents.add( componentMap );
	}
	
	return ret;
    }


    public boolean matches( Map currcomponent ) {

	//System.err.println( "faulttrigger: testing " + currcomponent );
	
	Iterator iter = triggerComponents.iterator();
	while( iter.hasNext() ) {
	    Map t = (Map)iter.next();
	    if( isMapSubset( currcomponent, t )) {

		//System.err.println( "TRIGGER MATCHES!!!!! " + t );
		return true;
	    }
	    else {
		//System.err.println( "faulttrigger: did NOT match: " + t );
	    }
	}

	//System.err.println( "faulttrigger: done testing" );
	
	return false;
    }

    /**
     * returns true if 'right' is a subset of 'left'
     */
    boolean isMapSubset( Map left, Map right ) {
	if( right.size() > left.size() ) {
	    // can't be a subset
	    return false;
	}
	 
	Iterator iter = right.entrySet().iterator();
	while( iter.hasNext() ) {
	    Map.Entry entry = (Map.Entry)iter.next();
	    Object leftValue = left.get( entry.getKey() );
	    Object rightValue = entry.getValue();
	    try {
		if(( leftValue == null && rightValue != null ) ||
		   ( leftValue != null && rightValue == null ) ||
		   ( !leftValue.equals( rightValue ))) {
		    return false;
		}
	    }
	    catch( ClassCastException e ) {
		// equals failed
		return false;
	    }
	}

	// passed all the tests
	return true;
    }

}


