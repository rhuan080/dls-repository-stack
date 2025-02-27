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
package edu.ucar.dls.standards.asn;

import org.dom4j.Element;

import edu.ucar.dls.xml.Dom4jUtils;
import edu.ucar.dls.xml.XPathUtils;
import edu.ucar.dls.util.strings.FindAndReplace;
import java.util.regex.*;

import java.util.*;

/**
 *  Extends AsnStandard to provide custom "description" for CommCore standards,
 *  which have "statementNotation" (e.g., "F-BF.4") defined for leaf nodes.
 *
 * @author    Jonathan Ostwald
 */
public class CommonCoreStandard extends AsnStandard {

	private static boolean debug = false;


	/**
	 *  Constructor for the CommonCoreStandard object
	 *
	 * @param  asnDoc    the parent standards document
	 * @param  asnStmnt  the standard statement that provides data for this AsnStandard object
	 */
	public CommonCoreStandard(AsnStatement asnStmnt, AsnDocument asnDoc) {
		super(asnStmnt, asnDoc);
	}

	Pattern numPat = Pattern.compile("[\\d]+\\. ",
		Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

	/**
	 *  If there is a statementNotation defined for this standard, remove the
	 *  leading number since it is redundant with a portion of the statementNotation
	 *
	 * @return    The description value
	 */
	public String getDescription() {
		String desc = this.getAsnStatement().getDescription();
		if (this.isLeaf()) {
			return numPat.matcher(desc).replaceFirst("");
		}
		else {
			return desc;
		}
	}


	/**
	 *  NOT YET DOCUMENTED
	 *
	 * @param  s  NOT YET DOCUMENTED
	 */
	private static void prtln(String s) {
		if (debug) {
			System.out.println("CO Bmk: " + s);
		}
	}
}

