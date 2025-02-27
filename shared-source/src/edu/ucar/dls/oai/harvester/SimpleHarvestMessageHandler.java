/*
	Copyright 2017 Digital Learning Sciences (DLS) at the
	University Corporation for Atmospheric Research (UCAR),
	P.O. Box 3000, Boulder, CO 80307

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package edu.ucar.dls.oai.harvester;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 *  Outputs harvest status messages to standard output.
 *
 * @author    John Weatherley
 * @see       Harvester
 */
public class SimpleHarvestMessageHandler implements HarvestMessageHandler, Serializable {
	/**
	 *  A status message indicating an event that took place during the harvest, such as a
	 *  request made to the data provider.
	 *
	 * @param  msg  A harvest status message generated by the harvester.
	 */
	public void statusMessage(String msg) {
		System.out.println(msg);
	}

	/**
	 *  A status message indicating the number of records currenlty harvested and the number
	 *  of resumption tokens issued.
	 *
	 * @param  recordCount      Number of recrods currently harvested.
	 * @param  resumptionCount  Number of resumption tokens currently issued.
	 * @see                     #getNumRecordsForStatusNotification()
	 */
	public void statusMessage(int recordCount, int resumptionCount) {
		System.out.println("Records harvested: " + recordCount + ", resumption tokens issued: " + resumptionCount);
	}


	/**
	 *  Gets the number of records the Harveser should harvest between sending statusMessage
	 *  notifications to this HarvestMessageHandler.
	 *
	 * @return    The numRecordsForStatusNotification value.
	 * @see       #statusMessage(String msg)
	 */
	public int getNumRecordsForStatusNotification() {
		return 50;
	}


	/**
	 *  A message generated by the harvester when an <a
	 *  href="http://www.openarchives.org/OAI/2.0/openarchivesprotocol.htm#ErrorConditions">
	 *  OAI protocol error</a> has occured.
	 *
	 * @param  oaiError      The OAI error code, for example "noRecordsMatch".
	 * @param  errorMessage  The accompanying message returned by the data provider, if any.
	 * @param  supportedGranularity  Supported granularity [days, seconds] or null
	 * @param  deletedRecordSupport  Deleted record support [no, transient, persistent] or null	 		 
	 */
	public void oaiErrorMessage(String oaiError, String errorMessage, String supportedGranularity, String deletedRecordSupport) {
		System.out.println("OAI code '" + oaiError + "' was returned by the data provider. Message: " + errorMessage);
	}


	/**
	 *  A serios error that occured during the harvest, preventing it from completing. For
	 *  example an http 500 error or a parsing error.
	 *
	 * @param  errorMessage  Description of the error.
	 */
	public void errorMessage(String errorMessage) {
		System.out.println("Error: " + errorMessage);
	}

	/**
	 *  Sets the harvest attributes for this harvest.
	 *
	 * @param  from   The from date or null if none used
	 * @param  until  The until date or null if none used
	 */	
	public void setHarvestAttributes(Date from, Date until) {
		// Currently, nothing done - could report these to the output...	
	}
	
	/**
	 *  A final report detailing the result of a successful harvest. This method is only
	 *  called if the harvest completes successfully with no errors.
	 *
	 * @param  recordCount      The total number of records harvested.
	 * @param  resumptionCount  Number of resumption tokens issued.
	 * @param  baseURL          The baseURL that was harvested.
	 * @param  set              The set that was harvested, or an empty string if none.
	 * @param  startTime        The time the harvest began.
	 * @param  endTime          The time the harvest was completed.
	 * @param  zipFilePathName  The full path to the harvest zip file, or null if none.
	 * @param  supportedGranularity  Supported granularity [days, seconds]
	 * @param  deletedRecordSupport  Deleted record support [no, transient, persistent]	 	 
	 */
	public void completedHarvestMessage(int recordCount,
	                                    int resumptionCount,
	                                    String baseURL,
	                                    String set,
	                                    long startTime,
	                                    long endTime,
										String zipFilePathName,
										String supportedGranularity,
	                                    String deletedRecordSupport) {
		String start = new SimpleDateFormat("h:mm:ss a zzz, EEE MMM d, yyyy ").format(new Date(startTime));
		String end = new SimpleDateFormat("h:mm:ss a zzz, EEE MMM d, yyyy ").format(new Date(endTime));
		if (set != null && set.length() > 0)
			set = ", set " + set;
		String msg = "Harvest of " + baseURL + set + " is complete. Total of " + recordCount +
			" records harvested. Total of " + resumptionCount + " resumption tokens were issued. " +
			"Harvest started at " + start + " and completed at " + end + ".";
		if(zipFilePathName != null)
			msg = msg + " Files saved to zip file at: " + zipFilePathName;
		System.out.println(msg);	
	}
}

