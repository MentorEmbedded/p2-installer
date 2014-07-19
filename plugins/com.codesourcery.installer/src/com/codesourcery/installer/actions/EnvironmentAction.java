/*******************************************************************************
 *  Copyright (c) 2014 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.installer.actions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.osgi.util.NLS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.InstallUtils;

public class EnvironmentAction extends AbstractInstallAction {
	/** Enivornment variable operations */
	public enum EnvironmentOperation {
		REPLACE,	// Replace environment variable
		PREPEND,	// Prepend to environment variable
		APPEND		// Append to environment variable
	}
	/** Action identifier */
	public static final String ID = "com.codesourcery.installer.EnvironmentAction";

	/** Variables element */
	private static final String ELEMENT_VARIABLES = "variables";
	/** Variable element */
	private static final String ELEMENT_VARIABLE = "variable";
	/** Variable name attribute */
	private static final String ATTRIBUTE_NAME = "name";
	/** Variable value attribute */
	private static final String ATTRIBUTE_VALUE = "value";
	/** Variable operation attribute */
	private static final String ATTRIBUTE_OPERATION = "operation";
	/** Variable delimiter attribute */
	private static final String ATTRIBUTE_DELIMITER = "delimiter";
	
	/** Windows user environment registry key */
	private static final String REG_USER_ENVIRONMENT = "HKEY_CURRENT_USER\\Environment";
	/** Profile file names. */  
	private static final String PROFILE_FILENAMES[] = {
		".bash_profile",
		".bash_login",
		".profile"
	};
	/** Index of PROFILE_FILENAMES to use when creating a new profile. */
	private static final int PROFILE_DEFAULT_INDEX = 2;

	/** Environment variables */
	private ArrayList<EnvironmentVariable> environmentVariables = new ArrayList<EnvironmentVariable>();

	/**
	 * Constructor
	 */
	public EnvironmentAction() {
		super(ID);
	}

	/**
	 * Reads a Windows environment variable.
	 * Only supported on the Windows platform.
	 * 
	 * @param variableName Variable name
	 * @return Variable value
	 * @throws UnsupportedOperationException if not supported
	 * @throws CoreException on failure to read the variable value
	 */
	public static String readWindowsEnvironmentVariable(String variableName) throws UnsupportedOperationException, CoreException {
		return Installer.getDefault().getInstallPlatform().getWindowsRegistryValue(REG_USER_ENVIRONMENT, variableName);	
	}
	
	/**
	 * Adds an environment variable to set.
	 * The variable will be replaced.
	 * 
	 * @param name Variable name
	 * @param value Variable value
	 */
	public void addVariable(String name, String value) {
		EnvironmentVariable variable = new EnvironmentVariable(name, EnvironmentOperation.REPLACE, null);
		variable.addValue(value);
		environmentVariables.add(variable);
	}
	
	/**
	 * Adds an environment variable to set.
	 * 
	 * @param name Variable name
	 * @param values Variable values
	 * @param operation Operation to perform (replace, prepend, or append)
	 * @param delimiter Delimiter to separate variables
	 */
	public void addVariable(String name, String[] values, EnvironmentOperation operation, String delimiter) {
		EnvironmentVariable variable = new EnvironmentVariable(name, operation, delimiter);
		for (String value : values) {
			variable.addValue(value);
		}
		environmentVariables.add(variable);
	}
	
	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product,
			IInstallMode mode, IProgressMonitor monitor) throws CoreException {

		try {
			if (mode.isInstall()) {
				monitor.beginTask(InstallMessages.EnvironmentAction_SettingEnvironmentVariables, 1);
				monitor.setTaskName(InstallMessages.EnvironmentAction_SettingEnvironmentVariables);
			}
			else {
				monitor.beginTask(InstallMessages.EnvironmentAction_RemovingEnvironmentVariables, 1);
				monitor.setTaskName(InstallMessages.EnvironmentAction_RemovingEnvironmentVariables);
			}
			
			// Windows
			if (Installer.isWindows()) {
				runWindows(mode, monitor);
			}
			// Linux
			else {
				runLinux(product, mode, monitor);
			}
			monitor.worked(1);
		}
		finally {
			monitor.done();
		}
	}
	
	/**
	 * Appends values to a buffer separating with a delimiter.
	 * 
	 * @param values Buffer
	 * @param newValue Value to append
	 * @param delimiter Delimiter to separate values
	 */
	private void appendValues(StringBuffer values, String newValue, String delimiter) {
		if ((values.length() > 0) && (delimiter != null))
			values.append(delimiter);
		values.append(newValue);
	}
	
	/**
	 * Handles Windows environment variables.
	 * 
	 * @param mode Install mode
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	private void runWindows(IInstallMode mode, IProgressMonitor monitor)
		throws CoreException {
		for (EnvironmentVariable environmentVariable : environmentVariables) {
			String existingValue = null;
			
			// Get existing value
			if ((environmentVariable.getOperation() != EnvironmentOperation.REPLACE)) { 
				try {
					existingValue = readWindowsEnvironmentVariable(environmentVariable.getName());
				}
				catch (CoreException e) {
					// Ignore
				}
			}
			
			StringBuffer valuesBuffer = new StringBuffer();
			// Install - prefix, append, or replace exiting value
			if (mode.isInstall()) {
				// Prepend or replace variable value
				if ((environmentVariable.getOperation() == EnvironmentOperation.PREPEND) || (environmentVariable.getOperation() == EnvironmentOperation.REPLACE)) {
					appendValues(valuesBuffer, environmentVariable.getValue(), environmentVariable.getDelimiter());
				}
				// Add existing variable value
				if (environmentVariable.getOperation() != EnvironmentOperation.REPLACE) {
					if (existingValue != null) {
						if (valuesBuffer.length() > 0) {
							valuesBuffer.append(environmentVariable.getDelimiter());
						}
						valuesBuffer.append(existingValue);
					}
				}
				// Append variable value
				if (environmentVariable.getOperation() == EnvironmentOperation.APPEND) {
					appendValues(valuesBuffer, environmentVariable.getValue(), environmentVariable.getDelimiter());
				}
				
				// Set new variable value
				Installer.getDefault().getInstallPlatform().setWindowsRegistryValue(REG_USER_ENVIRONMENT, environmentVariable.getName(), valuesBuffer.toString());
			}
			// Uninstall - remove variable value (prefix,append) or remove variable (replace)
			else {
				// Remove values
				if (existingValue != null) {
					String[] parts = InstallUtils.getArrayFromString(existingValue, environmentVariable.getDelimiter());
					ArrayList<String> existingValues = new ArrayList<String>(Arrays.asList(parts));
					String[] removeValues = InstallUtils.getArrayFromString(environmentVariable.getValue(), environmentVariable.getDelimiter());
					Iterator<String> iter = existingValues.iterator();
					while (iter.hasNext()) {
						String value = iter.next();
						for (String removeValue : removeValues) {
							if (removeValue.equals(value)) {
								iter.remove();
								break;
							}
						}
					}
					
					for (String value : existingValues) {
						appendValues(valuesBuffer, value, environmentVariable.getDelimiter());
					}
					
					// Set new variable value
					Installer.getDefault().getInstallPlatform().setWindowsRegistryValue(REG_USER_ENVIRONMENT, environmentVariable.getName(), valuesBuffer.toString());
				}
				// Remove variable
				else {
					Installer.getDefault().getInstallPlatform().deleteWindowsRegistryValue(REG_USER_ENVIRONMENT, environmentVariable.getName());
				}
			}
		}
	}

	/**
	 * Handles Linux path environment.
	 * 
	 * @param product Product
	 * @param mode Mode
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	private void runLinux(IInstallProduct product, IInstallMode mode, IProgressMonitor monitor)
			throws CoreException {
		// Path to .profile
		String homeDir = System.getProperty("user.home");
		if (homeDir == null)
			Installer.fail("Failed to get user home directory.");
		IPath homePath = new Path(homeDir);

		// Check for profile
		String profileFilename = null;
		File profileFile = null;
		for (String name : PROFILE_FILENAMES) {
			IPath profilePath = homePath.append(name);
			profileFile = profilePath.toFile();
			if (profileFile.exists()) {
				profileFilename = name;
				break;
			}
			else {
				Installer.log(name + " not found, skipping.");
			}
		}
		if (profileFilename == null) {
			// Create a new profile.
			profileFilename = PROFILE_FILENAMES[PROFILE_DEFAULT_INDEX];
			IPath newProfilePath = homePath.append(profileFilename);
			try {
				profileFile = newProfilePath.toFile();
				profileFile.createNewFile();
			} catch (IOException e) {
				Installer.log("Could not create profile " + newProfilePath);
				return;
			}
		}
		
		// Do not modify read-only profile
		if (!profileFile.canWrite()) {
			Installer.log("Profile was not modified because it is read-only.");
			return;
		}

		// File date suffix
		SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyyDDDHHmmss");
		String fileDateDesc = fileDateFormat.format(new Date());
		// Backup file path
		String backupName = profileFilename + fileDateDesc;
		IPath backupPath = homePath.append(backupName);
		File backupFile = backupPath.toFile();

		String line;
		// Install
		if (mode.isInstall()) {
			// Date description
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
			String dateDesc = dateFormat.format(new Date());

			// Make backup of .profile
			try {
				org.apache.commons.io.FileUtils.copyFile(profileFile, backupFile);
			} catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToBackupProfile, e);
			}
			
			// Write path extensions
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new FileWriter(profileFile, true));
				writer.newLine();
				// Write product block start
				writer.append(getProfileMarker(product, true));
				writer.newLine();
				writer.append("# Do NOT modify these lines; they are used to uninstall.");
				writer.newLine();
				line = MessageFormat.format("# New environment added by {0} on {1}.", new Object[] {
					product.getName(),
					dateDesc
				});
				writer.append(line);
				writer.newLine();
				line = MessageFormat.format("# The unmodified version of this file is saved in {0}.", backupPath.toOSString());
				writer.append(line);
				writer.newLine();
				
				monitor.setTaskName("Setting environment variables...");
				for (EnvironmentVariable environmentVariable : environmentVariables) {
					monitor.setTaskName(NLS.bind(InstallMessages.SettingEnvironment, environmentVariable.getName()));
					StringBuilder buffer = new StringBuilder();
					buffer.append(environmentVariable.getName());
					buffer.append('=');
					// Append variable
					if (environmentVariable.getOperation() == EnvironmentOperation.APPEND) {
						buffer.append("${");
						buffer.append(environmentVariable.getName());
						buffer.append("}");
						buffer.append(environmentVariable.getDelimiter());
					}
					boolean path = "PATH".equals(environmentVariable.getName());
					if (path)
						buffer.append('\"');
					buffer.append(environmentVariable.getValue());
					if (path)
						buffer.append('\"');
					if (environmentVariable.getOperation() == EnvironmentOperation.PREPEND) {
						buffer.append(environmentVariable.getDelimiter());
						buffer.append("${");
						buffer.append(environmentVariable.getName());
						buffer.append("}");
					}
					writer.append(buffer.toString());
					writer.newLine();
					writer.append("export ");
					writer.append(environmentVariable.getName());
					writer.newLine();
				}
				
				// Write product block end
				writer.append(getProfileMarker(product, false));
				writer.newLine();
				
			} catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToUpdateProfile, e);
			}
			finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}
		// Uninstall
		else {
			BufferedReader reader = null;
			BufferedWriter writer = null;
			boolean inProductBlock = false;
			try {
				reader = new BufferedReader(new FileReader(profileFile));
				writer = new BufferedWriter(new FileWriter(backupFile));
				while ((line = reader.readLine()) != null) {
					// Start of product path block
					if (line.startsWith(getProfileMarker(product, true))) {
						inProductBlock = true;
					}
					// End of product path block
					else if (line.startsWith(getProfileMarker(product, false))) {
						inProductBlock = false;
					}
					// If not in product path block, copy lines
					else if (!inProductBlock) {
						writer.write(line);
						writer.newLine();
					}
				}
			}
			catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToUpdateProfile, e);
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// Ignore
					}
				}
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}

			// Copy new profile
			try {
				org.apache.commons.io.FileUtils.copyFile(backupFile, profileFile);
			} catch (IOException e) {
				Installer.fail(InstallMessages.Error_FailedToUpdateProfile, e);
			}
			
			backupFile.delete();
		}
	}
	
	/**
	 * Returns the marker for beginning or ending of profile path block.
	 * 
	 * @param product Product
	 * @param start <code>true</code> for start of block marker,
	 * <code>false</code> for end of block marker.
	 * @return Marker
	 */
	private String getProfileMarker(IInstallProduct product, boolean start) {
		if (start)
			return MessageFormat.format("# Product Begin: {0}", new Object[] { product.getName() });
		else
			return MessageFormat.format("# Product End: {0}.", new Object[] { product.getName() });
	}
	
	@Override
	public void save(Document document, Element node) throws CoreException {
		Element element = document.createElement(ELEMENT_VARIABLES);
		node.appendChild(element);
		for (EnvironmentVariable environmentVariable : environmentVariables) {
			Element variableElement = document.createElement(ELEMENT_VARIABLE);
			element.appendChild(variableElement);
			variableElement.setAttribute(ATTRIBUTE_NAME, environmentVariable.getName());
			variableElement.setAttribute(ATTRIBUTE_VALUE, environmentVariable.getValue());
			variableElement.setAttribute(ATTRIBUTE_OPERATION, environmentVariable.getOperation().toString());
			variableElement.setAttribute(ATTRIBUTE_DELIMITER, environmentVariable.getDelimiter());
		}
	}

	@Override
	public void load(Element element) throws CoreException {
		environmentVariables.clear();
		NodeList variablesNodes = element.getElementsByTagName(ELEMENT_VARIABLES);
		for (int variablesIndex = 0; variablesIndex < variablesNodes.getLength(); variablesIndex++) {
			Node variablesNode = variablesNodes.item(variablesIndex);
			if (variablesNode.getNodeType() == Node.ELEMENT_NODE) {
				Element variablesElement = (Element)variablesNode;
				NodeList variableNodes = variablesElement.getElementsByTagName(ELEMENT_VARIABLE);
				for (int variableIndex = 0; variableIndex < variableNodes.getLength(); variableIndex++) {
					Node variableNode = variableNodes.item(variableIndex);
					if (variableNode.getNodeType() == Node.ELEMENT_NODE) {
						Element pathElement = (Element)variableNode;
						String name = pathElement.getAttribute(ATTRIBUTE_NAME);
						String value = pathElement.getAttribute(ATTRIBUTE_VALUE);
						EnvironmentOperation op = EnvironmentOperation.valueOf(pathElement.getAttribute(ATTRIBUTE_OPERATION));
						String delimiter = pathElement.getAttribute(ATTRIBUTE_DELIMITER);
						EnvironmentVariable variable = new EnvironmentVariable(name, op, delimiter);
						variable.addValue(value);
						environmentVariables.add(variable);
					}
				}
			}
		}
	}
	
	/**
	 * Environment variable
	 */
	private class EnvironmentVariable {
		/** Variable name */
		private String name;
		/** Variable value */
		private String value = "";
		/** Operation to perform */
		private EnvironmentOperation operation;
		/** Delimiter to separate variable values */
		private String delimiter;
		
		/**
		 * Constructor
		 * 
		 * @param name Variable name
		 * @param operation Operation to perform
		 * @param delimiter Delimiter to separate values
		 */
		public EnvironmentVariable(String name, EnvironmentOperation operation, String delimiter) {
			this.name = name;
			this.operation = operation;
			this.delimiter = delimiter;
		}
		
		/**
		 * Returns the variable name.
		 * 
		 * @return Name
		 */
		public String getName() {
			return name;
		}
		
		/**
		 * Adds a value to the variable.  Multiple values will be separated
		 * with the delimiter.
		 * 
		 * @param newValue Value to add
		 */
		public void addValue(String newValue) {
			if (!value.isEmpty() && (getDelimiter() != null))
				value += getDelimiter();
			value += newValue;
		}
		
		/**
		 * Returns the variable value.
		 * 
		 * @return Value
		 */
		public String getValue() {
			return value;
		}
		
		/**
		 * Returns the operation that will be performed.
		 * 
		 * @return Operation
		 */
		public EnvironmentOperation getOperation() {
			return operation;
		}
		
		/**
		 * Returns the delimiter to separate variable values.
		 * 
		 * @return Delimiter
		 */
		public String getDelimiter() {
			return delimiter;
		}
	}
}
