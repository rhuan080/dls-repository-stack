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
package edu.ucar.dls.harvest.processors.record;

import javax.xml.bind.annotation.XmlRootElement;

import org.dom4j.DocumentException;

import edu.ucar.dls.harvest.Config;
import edu.ucar.dls.harvest.exceptions.HarvestException;
import edu.ucar.dls.harvest.tools.XMLDocumentValidator;
import edu.ucar.dls.harvest.workspaces.Workspace;

/**
 * Processor that validates a record schema, For efficiency we use a 
 * XMLDocumentValidator then when constructed will cache the xsd while 
 * validating the first record. From then on out its using cached versions
 * for the rest
 */

@XmlRootElement(name="RecordSchemaValidator")
public class RecordSchemaValidator extends RecordProcessor {
	private XMLDocumentValidator xmlDocumentValidator = null;

	/**
	 * initialize the processor by constructing a XMLDocumentValidator
	 * first so it will be cached for all records
	 */

	public void initialize(Workspace workspace) throws HarvestException
	{
		super.initialize(workspace);
		this.xmlDocumentValidator  = new XMLDocumentValidator();
	}
	
	/**
	 * Run the record through the XMLDocumentValildator. Appending errors
	 * or warnings to the processor if it didn't validate
	 * @param recordId record id as a string
	 * @param record record xml as a string
	 * @param workspace the workspace that is being used for harvesting
	 */
	public String run(String documentId, String record) throws HarvestException {
		int resultValue=XMLDocumentValidator.NOT_VALID_RESULT;
		try {
			resultValue = this.xmlDocumentValidator.validateString(record, true, true);
		} catch (DocumentException e) {
			throw new HarvestException(Config.Exceptions.PROGRAMMER_ERROR_CODE,
					"Could not parse XML record during schema validation, Should not "+
					"have been caught here. Something is coded wrong in the validataor.",e);
		}
		if(resultValue==XMLDocumentValidator.NOT_VALID_RESULT || 
				resultValue==XMLDocumentValidator.NOT_WELL_FORMED_RESULT  )
		{
			String errorMsg = xmlDocumentValidator.outputBuffer.toString();
			this.addError(documentId, errorMsg);
		}
		else if(resultValue==XMLDocumentValidator.VALID_W_WARNINGS_RESULT)
		{
			this.addWarning(documentId, xmlDocumentValidator.outputBuffer.toString());
		}
		return null;
	}
}
