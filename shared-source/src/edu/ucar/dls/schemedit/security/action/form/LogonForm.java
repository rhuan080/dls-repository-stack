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
package edu.ucar.dls.schemedit.security.action.form;


import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import edu.ucar.dls.schemedit.SchemEditUtils;


/**
 * Form bean for the user profile page.  This form has the following fields,
 * with default values in square brackets:
 * <ul>
 * <li><b>password</b> - Entered password value
 * <li><b>username</b> - Entered username value
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2009/03/20 23:33:57 $
 */

public final class LogonForm extends ActionForm {


    // --------------------------------------------------- Instance Variables


    /**
     * The password.
     */
    private String password = null;


    /**
     * The username.
     */
    private String username = null;


	private String dest = null;
	
	private String requiredRole = null;
	
    // ----------------------------------------------------------- Properties


    /**
     * Return the password.
     */
    public String getPassword() {

	return (this.password);

    }


    /**
     * Set the password.
     *
     * @param password The new password
     */
    public void setPassword(String password) {

        this.password = password;

    }


    /**
     * Return the username.
     */
    public String getUsername() {

		return (this.username);

    }


    /**
     * Set the username.
     *
     * @param username The new username
     */
    public void setUsername(String username) {

        this.username = username;

    }

	public String getDest () {
		return dest;
	}
	
	public void setDest (String dest) {
		this.dest = dest;
	}
	
	public String getRequiredRole () {
		return requiredRole;
	}
	
	public void setRequiredRole (String role) {
		requiredRole = role;
	}
	
    // --------------------------------------------------------- Public Methods


    /**
     * Reset all properties to their default values.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {

        this.password = null;
        this.username = null;

    }


    /**
     * Validate the properties that have been set from this HTTP request,
     * and return an <code>ActionErrors</code> object that encapsulates any
     * validation errors that have been found.  If no errors are found, return
     * <code>null</code> or an <code>ActionErrors</code> object with no
     * recorded error messages.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
		// System.out.println ("\nLogonForm.validate()");							
									 
        ActionErrors errors = new ActionErrors();
        if ((username == null) || (username.length() < 1))
            errors.add("error", new ActionError("error.username.required"));
        if ((password == null) || (password.length() < 1))
            errors.add("error", new ActionError("error.password.required"));

        return errors;

    }


}
