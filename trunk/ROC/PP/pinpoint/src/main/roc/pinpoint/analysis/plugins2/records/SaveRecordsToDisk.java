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
package roc.pinpoint.analysis.plugins2.records;

// marked for release 1.0

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import swig.util.StringHelper;
import roc.pinpoint.analysis.*;


/**
 * This plugin saves the records in the input collection to an output file.
 * It saves the records as they enter the input collection, and whenever
 * they are modified.
 *
 * If the directory argument is set and the filename argument is not,
 * this plugin will write one file per record.
 *
 * @author emrek
 *
 */
public class SaveRecordsToDisk implements Plugin, RecordCollectionListener {

    public static final String INPUT_COLLECTION_NAME_ARG = "inputCollection";
    public static final String FILENAME_ARG = "filename";
    public static final String DIRECTORY_ARG = "directory";
    public static final String ID_ARG = "id";


    PluginArg[] args = {
	new PluginArg( INPUT_COLLECTION_NAME_ARG,
		       "input collection.  this plugin will save records that get placed in the collection specified by this argument.",
		       PluginArg.ARG_RECORDCOLLECTION,
		       true,
		       "observations"),
	new PluginArg( FILENAME_ARG,
		       "output file.  this plugin will save records to the file name specified in this argument",
		       PluginArg.ARG_STRING,
		       true,
		       "log.observations" ),
	new PluginArg( ID_ARG,
		       "id something or other for dynamic filenames - hack",
		       PluginArg.ARG_STRING,
		       false,
		       null ),
	new PluginArg( DIRECTORY_ARG,
		       "output directory.  if specified, this plugin will save each record to its own file (named by the recordid of the record) in this directory",
		       PluginArg.ARG_STRING,
		       false,
		       null )
    };

    String filename;
    String directory;

    RecordCollection inputRecordCollection;
    ObjectOutputStream oos;

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
        filename = (String) args.get(FILENAME_ARG);
	directory = (String)args.get(DIRECTORY_ARG );
	id = (String)args.get( ID_ARG );

	if( id != null ) {
	    String k = filename + id;
	    k = StringHelper.ReplaceAll( k, File.separator, "_" );
	    k = StringHelper.ReplaceAll( k, " ", "_" );
	    k = StringHelper.ReplaceAll( k, "(", "_" );
	    k = StringHelper.ReplaceAll( k, ")", "_" );
	    k = StringHelper.ReplaceAll( k, "{", "_" );
	    k = StringHelper.ReplaceAll( k, "}", "_" );
	    k = StringHelper.ReplaceAll( k, "[", "_" );
	    k = StringHelper.ReplaceAll( k, "]", "_" );
	    filename = k;
	    if( directory != null ) {
		filename = directory + File.separator + filename;
		directory = null;
	    }
	}

        try {
	    if( directory == null ) {
		oos =
		    new ObjectOutputStream( new FileOutputStream(new File(filename)));
	    }
        }
        catch (FileNotFoundException e) {
            throw new PluginException(
                "Unable to open or create output file " + filename);
        }
        catch (IOException e) {
            throw new PluginException(
                "I/O Exception while opening output file " + filename);
        }


	inputRecordCollection.registerListener( this );
    }

    /**
     * @see roc.pinpoint.analysis.Plugin#stop()
     */
    public void stop() throws PluginException {
        inputRecordCollection.unregisterListener(this);
        try {
            oos.close();
        }
        catch( IOException e ) {
            throw new PluginException( "I/O Exception while closing output file " + filename );
        }
    }

    /**
     * @see roc.pinpoint.analysis.RecordCollectionListener#addedRecords(java.lang.String, java.util.List)
     */
    public void addedRecord(String collectionName, Record rec) {
	//System.err.println( "SaveRecordsToDisk.addedRecords: begin..." );

            try {
		if( oos != null ) {
		    oos.writeObject( rec );
		    oos.flush();
		    oos.reset();
		}
		else {
		    String k = filename + rec.getAttribute("key").toString();
		    k = StringHelper.ReplaceAll( k, File.separator, "_" );
		    k = StringHelper.ReplaceAll( k, " ", "_" );
		    k = StringHelper.ReplaceAll( k, "(", "_" );
		    k = StringHelper.ReplaceAll( k, ")", "_" );
		    k = StringHelper.ReplaceAll( k, "{", "_" );
		    k = StringHelper.ReplaceAll( k, "}", "_" );
		    k = StringHelper.ReplaceAll( k, "[", "_" );
		    k = StringHelper.ReplaceAll( k, "]", "_" );
		    File f = new File( directory + File.separator + k );
		    //System.err.println( "SaveRecordToDisk.addedRecords: writing to " + f );
		    ObjectOutputStream oostmp = new ObjectOutputStream( new FileOutputStream( f ));
		    oostmp.writeObject( rec );
		    oostmp.flush();
		    oostmp.close();
		}
            }
            catch( IOException e ) {
                e.printStackTrace();
            }

	//System.err.println( "SaveRecordsToDisk.addedRecords: end." );
    }

    /**
     * @see roc.pinpoint.analysis.RecordCollectionListener#removedRecords(java.lang.String, java.util.List)
     */
    public void removedRecords(String collectionName, List items) {
    }

}
