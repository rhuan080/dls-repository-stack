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
package edu.ucar.dls.repository.indexing;

import edu.ucar.dls.repository.*;

import java.io.*;
import java.util.*;

import edu.ucar.dls.index.*;
import edu.ucar.dls.index.writer.*;
import java.text.*;

/**
 *  Interface for notifying watchers of indexing requests and prompts to configure and initialize.
 *
 * @author    John Weatherley
 */
public interface ItemIndexer extends EventListener {

	/**
	 *  Indicates the directory where ItemIndexer config files reside and a directory where peresistent files may
	 *  be stored. This method is called at start up time before any indexing events are fired. <p>
	 *
	 *  For the configDir: ItemIndexer instances may work with their own configuration file and find them here.
	 *  As a best practice it is recommended that the ItemIndexer use the name of its class in file names that it
	 *  uses for configuration. It is up to the ItemIndexer to process these files. <p>
	 *
	 *  For the persistanceDir: ItemIndexer instances may create persisance files here to save state between
	 *  sessions and invocations. The space is shared among all ItemsIndexers. Use class names in and
	 *  sub-directory names and/or files to disambiguate.
	 *
	 * @param  configDir       The directory where the config files reside
	 * @param  persistanceDir  The directory where persistent files may be stored
	 * @exception  Exception   Exception should be thrown if an error occurs
	 */
	public void setConfigDirectory(File configDir, File persistanceDir) throws Exception;


	/**
	 *  Indicates a request has been made for an indexing action to take place.
	 *
	 * @param  e              The indexing event indicating the action that should take place
	 * @exception  Exception  Exception should be thrown if an error occurs
	 */
	public void indexingActionRequested(IndexingEvent e) throws Exception;
}

