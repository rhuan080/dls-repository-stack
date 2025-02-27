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
package edu.ucar.dls.schemedit.vocab.integrity;

import edu.ucar.dls.schemedit.vocab.*;



import edu.ucar.dls.schemedit.SchemEditUtils;
import edu.ucar.dls.xml.Dom4jUtils;
import edu.ucar.dls.xml.schema.SchemaHelper;
import edu.ucar.dls.xml.schema.DefinitionMiner;
import edu.ucar.dls.xml.schema.StructureWalker;
import edu.ucar.dls.xml.schema.SchemaReader;
import edu.ucar.dls.xml.schema.SchemaHelperException;
import edu.ucar.dls.xml.schema.SchemaNodeMap;
import edu.ucar.dls.xml.schema.SchemaNode;
import edu.ucar.dls.xml.schema.GlobalDef;
import edu.ucar.dls.xml.schema.GenericType;
import edu.ucar.dls.xml.schema.SimpleType;
import edu.ucar.dls.xml.schema.ComplexType;

import edu.ucar.dls.xml.XPathUtils;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.*;
import java.lang.*;

import org.dom4j.*;


/**
 *  Command line routine that checks fields files for well-formedness, and ensures that the xpaths associated
 *  with the field files exist within the given metadata framework.
 *
 * @author     ostwald <p>
 *
 *      $Id: FieldFilesChecker.java,v 1.4 2012/03/15 18:50:19 ostwald Exp $
 * @version    $Id: FieldFilesChecker.java,v 1.4 2012/03/15 18:50:19 ostwald Exp $
 */
class FieldFilesChecker {
	/* private HashMap map = null; */
	static boolean debug = false;
	static boolean verbose = false;
	
	
	private Element fieldFilesElement = null;
	
	URI schemaUri;
	private SchemaHelper schemaHelper = null;
	

	public String frameworkName;
	public String version;
	
	SchemaPaths schemaPaths = null;
	int filesRead = 0;
	
	/* error codes */
	public static final int READER_ERROR = 0;
	public static final int ILLEGAL_PATH = 1;
	public static final int MISSING_PATH = 2;
	public static final int DUPLICATE_PATH = 3;
	public static final int MISSING_VOCAB = 4;
	public static final int DUPLICATE_VOCAB = 5;
	

	ErrorList missingPaths = null;
	ErrorList multiPaths = null;
	
	private URI fileListURI = null;
	File listingFile = null;
	Document listingDoc = null;
	boolean initialized = false;
	ErrorManager em = null;
	
	public static void main (String [] args) throws Exception {
		String framework = "http://ns.nsdl.org/ncs/lar";
		
		// String schema = "http://www.dlese.org/Metadata/collection/1.0.00/collection.xsd";
		String schema = "http://ns.nsdl.org/ncs/lar/1.00/schemas/lar.xsd";
		FieldFilesChecker.setDebug(true);
		FieldFilesChecker.setVerbose(true);
		FieldFilesChecker checker = null;
		try {
			checker = new FieldFilesChecker (new File (framework), new URI (schema));
		} catch (Exception e) {
			prtln ("checker error: " + e.getMessage());
			return;
		}
		checker.doCheck();
		checker.doReport();
	}
	
	public FieldFilesChecker (File frameworkDir, URI schemaUri ) throws Exception {
		
		prtln ("\n FieldFilesChecker instantiated");

		this.schemaUri = schemaUri;
		this.version = frameworkDir.getName();
		this.frameworkName = frameworkDir.getParentFile().getName();
		prtln (Utils.line (20));
		prtln ("\nChecking Version (" + version + ") ...");
		
		this.em = new ErrorManager ();
		
		prtln ("schemaUri: " + schemaUri.toString());
		this.schemaHelper = getSchemaHelper (schemaUri);
		this.schemaPaths = new SchemaPaths(schemaHelper);
					
		this.listingFile = new File (frameworkDir + "/build/fields-list.xml");
		if (!listingFile.exists()) {
/* 			prtln ("ERROR: fieldsFileListing not found at " + listingFile);
			return; */
			throw new Exception ("fieldsFileListing not found at " + listingFile);
		}
		
		listingDoc = null;
		prtln ("\t reading listing file ...");
		try {
			fileListURI = listingFile.toURI();
			listingDoc = SchemEditUtils.getLocalizedXmlDocument(fileListURI);
		} catch (Exception e) {
			prtln ("msg returned from SchemEditUtils.getLocalizedXmlDocument: " + e.getMessage());
			throw new Exception("ERROR: File listing document (\"" + listingFile + "\") either does not exist or cannot be parsed as XML");
		}
		
		
		// prtln("Processing fields file listing at : " + listingUri.toString());

		Node filesNode = listingDoc.selectSingleNode("/metadataFieldsInfo/files");
		if (filesNode == null) {
			throw new Exception("no filesNode found");
		}
		
		this.fieldFilesElement = (Element) filesNode;
		
		prtln("\nFieldFilesChecker instantiated at " + Utils.getTimeStamp() + "\n");
		prtln("FieldFiles Listing: " + listingFile.toString());
		prtln("Schema: " + schemaUri.toString());
		prtln("");
		
		initialized = true;

	}
	
	void initializeErrorLists () {

/* 		readerErrors = new ErrorList ("Reader Errors", 
			"Field files that either do not exist or could not be read as XML");
			
		illegalPaths = new ErrorList ("Illegal Paths", 
			"Field files defining an xpath that is not schema-legal"); */
			
		missingPaths = new ErrorList ("Missing Paths",
			"Schema xpaths not defined in any field file");
			
		multiPaths = new ErrorList ("Duplicate Paths",
			"Xpaths contained in mulitiple fields files");
			
/* 		missingVocabs = new ErrorList ("Missing Vocabs",
			"Field Files in which not all Schema VOCAB TERMS are defined");
			
		multiVocabs = new ErrorList ("Duplicate Vocab",
			"Field Files containing defining a VOCAB TERM more than once"); */
	}
	
	/**
	 *  Process a set of FieldsFiles (read from an xml file) and perform an integrity check
	 on each fieldsFile.
	 *
	 * @exception  Exception  NOT YET DOCUMENTED
	 */
	void doCheck () throws Exception {
		
		if (!initialized) {
			// throw exception so this reporter will be removed from the master list and no reports will be attempted
			throw new Exception ("not initialized!");
		}
		
		filesRead = 0;
		
		initializeErrorLists();
		
		// load each of the files in the fields file listing
		for (Iterator i = fieldFilesElement.elementIterator(); i.hasNext(); ) {
			Node fileNode = (Node) i.next();
			String fileSpec = fileNode.getText();
			String fileName = XPathUtils.getLeaf(fileSpec);
			FieldInfoReader reader = null;
			String xpath;
			try {
				// URI myUri = baseUri.resolve(fileName);
				URI myUri = FieldInfoMap.getFieldsFileUri(fileListURI, fileSpec);
				try {
					reader = new FieldInfoReader(myUri);
				} catch (Exception e) {
					// readerErrors.add(new ReaderError(myUri, e.getMessage()));
					FileError error = new FileError (READER_ERROR, myUri);
					error.description = e.getMessage();
					em.add (error);
					
					continue;
				}

				filesRead++;

				try {
					vocabCheck (reader, fileSpec);
				} catch (Exception e) {
					prtln ("VocabCheck error: " + e.getMessage());
					e.printStackTrace();
				}

				try {
					xpath = reader.getPath();
				} catch (Throwable pathEx) {
					// illegalPaths.add(new PathError(myUri, "path not found"));
					FileError error = new FileError (READER_ERROR, myUri);
					error.description = "path not found";
					em.add (error);
					
					continue;
				}

				if (!schemaPaths.isLegalPath(xpath)) {
					/* throw new Exception ("ERROR: Fields file at " + myUri.toString() + " contains an illegal path: " + xpath); */
					// illegalPaths.add(new PathError(myUri, xpath));
					em.add (new FileError (ILLEGAL_PATH, myUri, xpath));
				}
				else {
					schemaPaths.markAsSeen(myUri, xpath);
				}
				
			} catch (Throwable t) {
				prtln(t.getMessage());
				t.printStackTrace();
				return;
			}
		}
	}

	
/* 	void vocabCheckDebugger (String fileSpec) throws Exception {
		
		if (!initialized) {
			// throw exception so this reporter will be removed from the master list and no reports will be attempted
			throw new Exception ("not initialized!");
		}
		
		prtln (Utils.box ("vocabCheck DEBUGGER - " + XPathUtils.getLeaf (fileSpec)));
		
		filesRead = 0;
		
		initializeErrorLists();
		
		FieldInfoReader reader = null;
		String xpath;
		try {
			// URI myUri = baseUri.resolve(fileSpec);
			URI myUri = NewFieldInfoMap.getFieldsFileUri(fileListURI, fileSpec);
			try {
				reader = new FieldInfoReader(myUri);
			} catch (Exception e) {
				// readerErrors.add(new ReaderError(myUri, e.getMessage()));
				prtln (e.getMessage());
				return;
			}

			filesRead++;

			try {
				xpath = reader.getPath();
			} catch (Throwable pathEx) {
				// illegalPaths.add(new PathError(myUri, "path not found"));
				prtln ("path not found");
				return;
			}

			if (!schemaPaths.isLegalPath(xpath)) {
				// throw new Exception ("ERROR: Fields file at " + myUri.toString() + " contains an illegal path: " + xpath);
				// illegalPaths.add(new PathError(myUri, xpath));
			}
			else {
				schemaPaths.markAsSeen(myUri, xpath);
			}
			
			try {
				vocabCheck (reader, fileSpec);
			} catch (Exception e) {
				prtln ("VoabCheck error: " + e.getMessage());
				
			}

		} catch (Throwable t) {
			prtln(t.getMessage());
			t.printStackTrace();
			return;
		}
	} */

		
	/**
	 * We use the FieldInfoReader to extract vocab terms defined in fieldsFile
	 * Use the reader to obtain schemaNode for this field. This is how we
	 * access the schema-defined information for this field.
	 * If we can't find the schemaNode something is pretty wrong -Bail
	 
	* three cases <ol>
	<li> simple Enumeration (GenericType.isEnumerationType()) -> genericType.getEnumerationValues
	<li> - comboUnion Type (SimpleType.isComboUnionType() -> genericType.getEnumerationValues 
		(?will this work with comboUnions?)
	<li> - derivedTextOnlyModel (that extends either an enumeration or comboUnion)
		ComplexType.isDerivedTextOnlyModel -> work with ComplexType.getExtensionType
		</ol>
	 
	 * NOTEs: 
	 - throwing an Exception from this method causes the vocabCheck to bail.
	 - this method will be called on fields that do not have vocabs, so we can't
	 treat the failure to find vocab fields as an error
	 - ASSUMPTION: vocabs terms are always defined in the schema as ENUMERATIONs.
	*/
	List getSchemaVocabTerms (FieldInfoReader reader) throws Exception {
		// prtln (Utils.underline("getSchemaVocabTerms()"));
		String errorMsg, msg;
		String path;
		SchemaNode schemaNode;
		try {
			path = reader.getPath();
			prtln ("path: " + path);
			if (path == null || path.length() == 0)
				throw new Exception ("path not found");
			schemaNode = schemaHelper.getSchemaNode(path);
			if (schemaNode == null)
				throw new Exception ("schemaNode not found");
		} catch (Exception e) {
			errorMsg = "vocabCheck: error: " + e.getMessage();
			throw new Exception (errorMsg);
		}
		
		
		GlobalDef globalDef = schemaNode.getTypeDef();
		if (globalDef == null) {
			errorMsg = "No type information is associated with this field in the schema?!";
			prtln (errorMsg);
			throw new Exception (errorMsg);
		}
		
		// trap non-fatal "errors" and return an empty list
		List schemaTerms;
		try {
			if (!globalDef.isTypeDef()) {
				msg = "GlobalDef found at " + path + "(" + globalDef.getQualifiedInstanceName() + " cannot have an enumeration";
				throw new Exception (msg);
			}

			// Case 3 - ComplexType && isDerivedTextOnlyModel
			if (globalDef.isComplexType()) {
				ComplexType complexType = (ComplexType) globalDef;
				if (!complexType.isDerivedTextOnlyModel())
					throw new Exception ("globalDef is complexType but not derivedTextOnlyModel - cannot define enumeration");
				globalDef = complexType.getExtensionType();
				prtln ("working with extension base: " + globalDef.getQualifiedInstanceName());
			}
			
			if (!globalDef.isTypeDef())
				throw new Exception ("globalDef is NOT typeDef (" + globalDef.getQualifiedInstanceName());
			
			GenericType typeDef = (GenericType) globalDef;
			
			if (!globalDef.isSimpleType())
				// now we need to test for a model that EXTENDS a simpleType ...
				throw new Exception ("globalDef is NOT simpleType (" + globalDef.getQualifiedInstanceName());
			
			SimpleType simpleType = (SimpleType) globalDef;
			
			if (! (simpleType.isEnumeration() || simpleType.isComboUnionType()))
				throw new Exception ("NOT an enumeration or comboUnion");
				
			schemaTerms = ((GenericType)typeDef).getEnumerationValues();

			if (schemaTerms == null) {
				msg = "getEnumerationValues returned NULL (shouldn't happen)";
				throw new Exception (msg);
				// if we want to let the rest of the vocab check proceed ...
				// terms = new ArrayList();
			}
		} catch (Exception e) {
			prtln (e.getMessage());
			return new ArrayList ();
		}
		return schemaTerms;
	}
		
	/**
	* Collect the vocab terms defined by a field file.
	*/
	List getFieldFileTerms (FieldInfoReader reader) {
		
		String vocabXPath = "/metadataFieldInfo/field/terms/termAndDeftn/@vocab";
		List vocabNodes = reader.getDocument().selectNodes (vocabXPath);
		List terms = new ArrayList ();
		for (Iterator i=vocabNodes.iterator();i.hasNext();) {
			Node vocabNode = (Node)i.next();
			terms.add (vocabNode.getText());
		}
		return terms;
	}
			
	/**
	* Check for proper tocab term definitions
	* - all schema Vocab terms must be defined in the field file
	* - report duplicate term definitions (terms must be defined only once
	*/
	void vocabCheck (FieldInfoReader reader, String fileSpec) throws Exception {
		// prtln (Utils.line (30));
		String fileName = XPathUtils.getLeaf (fileSpec);
		prtln ("");
		prtln (Utils.underline ("vocabCheck() with " + fileName));
		SchemaNode schemaNode = null;
		String path = "";
		
		// List fieldFileTerms = reader.getTermList(); // will never be null
		List fieldFileTerms = getFieldFileTerms(reader); // will never be null
		List schemaTerms;
		try {
			schemaTerms = getSchemaVocabTerms(reader);
		} catch (Exception e) {
			prtln ("vocabCheck aborted: " + e.getMessage());
			return;
		}
		
		// Debugging
		if (debug && verbose) {
			prtln (fileName + ": Checking fieldFileTerms against schemaTerms");
			
			prtln (fieldFileTerms.size() + " terms defined in fields file");
			for (Iterator i = fieldFileTerms.iterator();i.hasNext();) {
				prtln ("\t" + (String)i.next());
			}
			
			prtln (schemaTerms.size() + " vocab terms defined in schema");
			for (Iterator i = schemaTerms.iterator();i.hasNext();) {
				prtln ("\t" + (String)i.next());
			}
		}
		// FINALLY, we do the check
		
		// Are each of the schemaTerms represented in  fieldFileTerms?
		if (!fieldFileTerms.containsAll(schemaTerms)) {
			// List missingTerms = new ArrayList();
			for (Iterator i=schemaTerms.iterator(); i.hasNext();) {
				String term = (String)i.next();
				if (!fieldFileTerms.contains(term)) {
					// missingTerms.add (term);
					em.add (new FileError (MISSING_VOCAB, reader.uri, term));
				}
			}
			
			// missingVocabs.add (new MissingVocab (reader.uri, missingTerms));
		}
		else if (verbose) {
			prtln ("fieldFileTerms contains all enumeration values");
		}
		
		
		// Do any fieldFileTerms occurr more than once?
		Counter counter = new Counter();
		for (Iterator i = fieldFileTerms.iterator();i.hasNext();) {
			counter.inc ((String)i.next());
		}
		if (counter.hasDups()) {
			// List dupTerms = new ArrayList ();
			String msg = "The following terms were defined more than once in the fields file";
			for (Iterator i = counter.getDups().iterator(); i.hasNext();) {
				String term = (String)i.next();
				em.add (new FileError (DUPLICATE_VOCAB, reader.uri, term));
				msg += "\n\t" + term;
				// dupTerms.add (term);
			}
			prtln (msg);
			// multiVocabs.add (new MultiVocab (reader.uri, dupTerms));
		}
	}

	/**  NOT YET DOCUMENTED */
	void doReport() {
		
		report (Utils.line ("="));
		report (Utils.underline("** " + frameworkName + " - " + version + " **"));
 		// report("FieldFiles Listing:" + listingFile.toString());
		report("Schema Location: " + schemaUri.toString());
		report("Files Read: " + filesRead + "/" + fieldFilesElement.elements().size());
		
		/* Missing paths - defined in schema but not in fields file */
		missingPaths.errors = schemaPaths.getMissingPaths();
		report (missingPaths.report());

		/* xpaths defined more than once */
		multiPaths.errors = schemaPaths.getMultiPaths();
		report (multiPaths.report());
		
		report (em.report());
		
	}
	
	SchemaHelper getSchemaHelper (URI schemaUri) throws Exception {
		SchemaHelper schemaHelper = null;
		String scheme = schemaUri.getScheme();
		try {
			if (scheme.equals("file"))
				schemaHelper = new SchemaHelper(new File(schemaUri.getPath()));
			else if (scheme.equals("http"))
				schemaHelper = new SchemaHelper(schemaUri.toURL());
			else
				throw new Exception("ERROR: Unrecognized scheme (" + scheme + ")");

			if (schemaHelper == null)
				throw new Exception();
		} catch (Exception e) {
			if (e.getMessage() != null && e.getMessage().trim().length() > 0)
				throw new Exception("Unable to instantiate SchemaHelper at " + schemaUri + ": " + e.getMessage());
			else
				throw new Exception("Unable to instantiate SchemaHelper at " + schemaUri);
		}
		return schemaHelper;
	}

	public static void setVerbose (boolean v) {
		verbose = v;
	}
	
	public static void setDebug (boolean d) {
		debug = d;
	}
	
	/**
	 *  Output a line of text to standard out, with datestamp, if debug is set to true.
	 *
	 * @param  s  The String that will be output.
	 */
	private static void prtln(String s) {
		if (debug)
			System.out.println(s);
	}

	private static void report (String s) {
		System.out.println(s);
	}
	
	abstract class ICError {
		public String location;
		Object data;
		public String fileName = "n/a";
		public boolean fatal;

		ICError (Object data) {
			this ("", data);
		}
		
		ICError (URI uri, Object data) {
			this (uri.toString(), data);
			if (uri != null)
				this.fileName = XPathUtils.getLeaf (uri.toString ());
		}
		
		/**
		 *  Constructor for the ICError object
		 *
		 * @param  location  URI of fieldsFile
		 * @param  data      An Oject to hold information about this error
		 */
		ICError (String location, Object data) {
			this.location = location;
			this.data = data;

		}
		
		abstract String report ();
	}
	
	public class FileError  {
		
		static final String UNKNOWN = "_unknown_";
		
		public int error_code;
		public URI fieldsFileUri;
		public String data;
		public String description;
		public String fileName;
	
		public FileError (int error_code, URI fieldsFileUri) {
			this (error_code, fieldsFileUri, UNKNOWN);
		}
		
		public FileError (int error_code, URI fieldsFileUri, String data) {
			this.error_code = error_code;
			this.fieldsFileUri = fieldsFileUri;
			this.data = data;
			if (fieldsFileUri != null)
				this.fileName = XPathUtils.getLeaf (fieldsFileUri.toString());
			else
				this.fileName = UNKNOWN;
		}
		
		String report () {
			if (error_code == READER_ERROR)
				return "";
			return data;
		}
	}
	
	class MultiPaths extends ICError {
		
		String xpath;
		List paths;
	
		MultiPaths (String xpath, Object paths) {
			super (xpath, paths);
			this.xpath = location;
			this.paths = (List)paths;
		}
		
		String report () {
			String s = "xpath: " + xpath;
			for (Iterator i=paths.iterator();i.hasNext();) {
				URI uri = (URI)i.next();
				s += "\n\t" + XPathUtils.getLeaf (uri.toString());
			}
			return s;
		}
	}
	
	class SimpleError extends ICError {
		
		String msg;
	
		SimpleError (String msg) {
			super ((Object)msg);
			this.msg = msg;
		}
		
		String report () {
			return msg;
		}
	}
	
	
	class ErrorList {
		String name;
		private List errors;
		String description;
		String indent = "  ";
		
		public ErrorList (String name, String description) {
			this (name, description, new ArrayList ());
		}
		
		public ErrorList (String name, String description, List errors) {
			this.name = name;
			this.description = description;
			this.errors = errors == null ? new ArrayList() : errors;
			
			prtln (Utils.box("ErrorList instantiated\n\tname: " + name + "\n\tdescription: " + description));
			
		}
		
		public void add (ICError error) {
			errors.add (error);
		}
		
		public boolean isEmpty () {
			return errors.isEmpty();
		}
		
		public int size () {
			return errors.size ();
		}
		
		public String report () {
			String header = name + " (" + errors.size() + ") - " + description;
			String s = "";
			
			if (verbose || !isEmpty ()) {
				// s += Utils.underline (header);
				s += "\n";
				s += Utils.overline (header);
				s += "\n";
			}
			
			if (!isEmpty()) {
				String items = "";
				for (Iterator i=errors.iterator(); i.hasNext();) {
					ICError error = (ICError)i.next();
					String [] lines = error.report ().split("\n");
					for (int j=0;j<lines.length;j++) {
						if (lines[j].length() > 0) {
							if (j == 0)
								items += "\n" + indent + "- " + lines[j];
							else
								items += "\n" + indent + "  " + lines[j];
						}
					}
				}
				s += items;
			}
			else if (verbose) {
				prtln ("");
				s += "\n\t- " + "no errors to report - ";
			}
			return s;
		}
	}
	
 	class Counter {
		HashMap map;
		boolean hasDup;
		
		public Counter () {
			map = new HashMap();
		}
		
		int count (String term) {
			Integer n = (Integer)map.get (term);
			if (n == null)
				return 0;
			else
				return n.intValue();
		}
		
		public void inc (String term) {
			int n = count(term);
			map.put (term, new Integer (n + 1));
			if (count(term) > 1) hasDup = true;
		}
		
		public boolean hasDups () {
			return hasDup;
		}
		
		
		public List getDups () {
			List dups = new ArrayList ();
			for (Iterator i=map.keySet().iterator();i.hasNext();) {
				String term = (String)i.next();
				if (count (term) > 1)
					dups.add (term);
			}
			return dups;
		}
	}
				
	
	class SchemaPaths {
		Map map = null;

		/**
		 *  Constructor for the SchemaPaths object
		 *
		 * @param  schemaNodeMap  NOT YET DOCUMENTED
		 */
		SchemaPaths(SchemaHelper schemaHelper) {
			map = new TreeMap();
			for (Iterator i = schemaHelper.getSchemaNodeMap().getKeys().iterator(); i.hasNext(); ) {
				String path = (String) i.next();
				map.put(path, new ArrayList());
			}
		}


		/**
		 *  Gets the legalPath attribute of the SchemaPaths object
		 *
		 * @param  xpath  NOT YET DOCUMENTED
		 * @return        The legalPath value
		 */
		boolean isLegalPath(String xpath) {
			if (xpath == null || xpath.trim().length() == 0)
				return false;
			return (map.containsKey(xpath));
		}


		/**
		 *  NOT YET DOCUMENTED
		 *
		 * @param  xpath  NOT YET DOCUMENTED
		 * @param  uri    NOT YET DOCUMENTED
		 */
		void markAsSeen(URI uri, String xpath) {
			List locs = (List) map.get(xpath);
			if (locs != null) {
				locs.add(uri);
				map.put(xpath, locs);
			}
			else {
				prtln("add error: no xpath found for " + xpath);
			}
		}


		/**
		 *  Gets the locs attribute of the SchemaPaths object
		 *
		 * @param  xpath  NOT YET DOCUMENTED
		 * @return        The locs value
		 */
		List getLocs(String xpath) {
			return (List) map.get(xpath);
		}


		/**
		 *  Gets the unseenPaths attribute of the SchemaPaths object
		 *
		 * @return    The unseenPaths value
		 */
		List getMissingPaths() {
			List ret = new ArrayList();
			for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				List locs = getLocs(key);
				if (locs == null || locs.size() == 0) {
					ret.add(new SimpleError (key));
				}
			}
			return ret;
		}

		
		/**
		 *  Gets the multiples attribute of the SchemaPaths object
		 *
		 * @return    The multiples value
		 */
		List getMultiPaths() {
			Map multiplesMap = new TreeMap();
			for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				List locs = getLocs(key);
				if (locs.size() > 1) {
					multiplesMap.put(key, locs);
				}
			}
			List multiplesList = new ArrayList();
			for (Iterator i = multiplesMap.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry multiple = (Map.Entry)i.next();
				multiplesList.add (new MultiPaths ((String)multiple.getKey(), 
													(List)multiple.getValue()));
			}
			return multiplesList;
		}
		
/* 		Map getMultiPaths() {
			Map multiples = new TreeMap();
			for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
				String key = (String) i.next();
				List locs = getLocs(key);
				if (locs.size() > 1) {
					multiples.put(key, locs);
				}
			}
			return multiples;
		} */
		
	}

	List getSchemaVocabTermsOLD (FieldInfoReader reader) throws Exception {
		// prtln (Utils.underline("getSchemaVocabTerms()"));
		String errorMsg, msg;
		String path;
		SchemaNode schemaNode;
		try {
			path = reader.getPath();
			prtln ("path: " + path);
			if (path == null || path.length() == 0)
				throw new Exception ("path not found");
			schemaNode = schemaHelper.getSchemaNode(path);
			if (schemaNode == null)
				throw new Exception ("schemaNode not found");
		} catch (Exception e) {
			errorMsg = "vocabCheck: error: " + e.getMessage();
			throw new Exception (errorMsg);
		}
		
		
		GlobalDef globalDef = schemaNode.getTypeDef();
		if (globalDef == null) {
			errorMsg = "No type information is associated with this field in the schema?!";
			prtln (errorMsg);
			throw new Exception (errorMsg);
		}
		
		// trap non-fatal "errors" and return an empty list
		List schemaTerms;
		try {
			if (!globalDef.isTypeDef()) {
				msg = "GlobalDef found at " + path + "(" + globalDef.getQualifiedInstanceName() + " cannot have an enumeration";
				throw new Exception (msg);
			}
	
			GenericType typeDef = (GenericType)globalDef;
			if (!typeDef.isEnumerationType()) {
				// msg =  "schemaNode at " + path + " is \"" + typeDef.getName() + "\" (not an enumeration type)";
				msg =  "schemaNode is " +  typeDef.getQualifiedInstanceName() + "\" (not an enumeration type)";
				throw new Exception (msg);
			}
	
			// we know it is an enumeration, it should not return an empty list,
			// and an empty list would also be pretty strange ...
			schemaTerms = typeDef.getEnumerationValues();
			if (schemaTerms == null) {
				msg = "getEnumerationValues returned NULL (shouldn't happen)";
				throw new Exception (msg);
				// if we want to let the rest of the vocab check proceed ...
				// terms = new ArrayList();
			}
		} catch (Exception e) {
			prtln (e.getMessage());
			return new ArrayList ();
		}
		return schemaTerms;
	}
	
}
	
