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
package com.codesourcery.internal.installer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallModule;
import com.codesourcery.installer.IInstallVerifier;
import com.codesourcery.installer.Installer;

/**
 * Registry of contributions
 */
public class ContributorRegistry {
	/** Modules extension identifier */
	private static final String MODULES_EXTENSION_ID = "modules"; //$NON-NLS-1$
	/** Module extension element */
	private static final String ELEMENT_MODULE = "module"; //$NON-NLS-1$
	/** Actions extension identifier */
	private static final String ACTIONS_EXTENSION_ID = "actions"; //$NON-NLS-1$
	/** Action extension element */
	private static final String ELEMENT_ACTION = "action"; //$NON-NLS-1$
	/** Verifiers extension identifier */
	private static final String VERIFIERS_EXTENSION_ID = "verifiers"; //$NON-NLS-1$
	/** Verifier extension element */
	private static final String ELEMENT_VERIFIER = "verifier"; //$NON-NLS-1$
	/** Class extension attribute */
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	/** Icon extension identifier */
	private static final String ICON_EXTENSION_ID = "icon"; //$NON-NLS-1$
	/** Icon extension element */
	private static final String ELEMENT_ICON = "icon"; //$NON-NLS-1$
	/** Image extension attribute */
	private static final String ATTRIBUTE_IMAGE = "image"; //$NON-NLS-1$

	/** Default instance */
	private static ContributorRegistry registry = null;

	/** Registered modules */
	private IInstallModule[] modules;
	/** Registered actions providers */
	private ActionDescription[] actions;
	/** Registered install verifiers */
	private IInstallVerifier[] verifiers;
	/** Icon image */
	private Image iconImage;

	/**
	 * Returns the default instance.
	 * 
	 * @return Registry
	 */
	public static ContributorRegistry getDefault() {
		if (registry == null)
			registry = new ContributorRegistry();

		return registry;
	}
	
	/**
	 * Returns all registered install verifiers.
	 * 
	 * @return Install verifiers
	 */
	public IInstallVerifier[] getInstallVerifiers() {
		if (verifiers != null)
			return verifiers;

		Vector<IInstallVerifier> contributions = new Vector<IInstallVerifier>();
		// Loop through the registered modules
		String plugin = Installer.getDefault().getContext().getBundle().getSymbolicName();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(plugin, VERIFIERS_EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i1 = 0; i1 < extensions.length; i1++) {
			// Loop through the extension elements
			IConfigurationElement[] elements = extensions[i1].getConfigurationElements();
			for (int i2 = 0; i2 < elements.length; i2++) {
				try {
					// Verifier element
					IConfigurationElement confElement = elements[i2];
					if (!(confElement.getName().equals(ELEMENT_VERIFIER))) //$NON-NLS-1$
						continue;
	
					IInstallVerifier verifier = (IInstallVerifier)confElement.createExecutableExtension(ATTRIBUTE_CLASS);
					contributions.add(verifier);
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
		}

		verifiers = contributions.toArray(new IInstallVerifier[contributions.size()]);
		return verifiers;
	}
	
	/**
	 * Returns all registered modules. If moduleIDs is not null, the loaded 
	 * modules will be restricted to those specified by ID in the list.
	 * 
	 * @param moduleIDs The list of module IDs to include
	 * 
	 * @return Modules
	 */
	public IInstallModule[] getModules(List<String> moduleIDs) {
		if (modules != null)
			return modules;

		Vector<IInstallModule> contributors = new Vector<IInstallModule>();
		// Loop through the registered modules
		String plugin = Installer.getDefault().getContext().getBundle().getSymbolicName();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(plugin, MODULES_EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i1 = 0; i1 < extensions.length; i1++) {
			// Loop through the extension elements
			IConfigurationElement[] elements = extensions[i1].getConfigurationElements();
			for (int i2 = 0; i2 < elements.length; i2++) {
				// Module element
				IConfigurationElement confElement = elements[i2];
				if (!(confElement.getName().equals(ELEMENT_MODULE))) //$NON-NLS-1$
					continue;

				ModuleDescription desc = new ModuleDescription(confElement);
				if (moduleIDs != null && !moduleIDs.contains(desc.getId())) {
					continue;
				}

				try {
					contributors.add(desc.createModule());
				} catch (CoreException e) {
					Installer.log("Failed to create module extension.");
					Installer.log(e);
				}
			}
		}

		modules = contributors.toArray(new IInstallModule[0]);
		return modules;
	}

	/**
	 * Returns all registered install action descriptions.
	 * 
	 * @return Modules
	 */
	protected ActionDescription[] getActions() {
		if (actions != null)
			return actions;

		Vector<ActionDescription> contributors = new Vector<ActionDescription>();
		// Loop through the registered modules
		String plugin = Installer.getDefault().getContext().getBundle().getSymbolicName();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(plugin, ACTIONS_EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i1 = 0; i1 < extensions.length; i1++) {
			// Loop through the extension elements
			IConfigurationElement[] elements = extensions[i1].getConfigurationElements();
			for (int i2 = 0; i2 < elements.length; i2++) {
				// Action element
				IConfigurationElement confElement = elements[i2];
				if (!(confElement.getName().equals(ELEMENT_ACTION))) //$NON-NLS-1$
					continue;

				ActionDescription desc = new ActionDescription(confElement);
				contributors.add(desc);
			}
		}

		actions = contributors.toArray(new ActionDescription[0]);
		return actions;
	}
	
	/**
	 * Creates an install action.
	 * 
	 * @param id Action identifier
	 * @return Install action
	 * @throws CoreException on failure
	 */
	public IInstallAction createAction(String id) throws CoreException {
		IInstallAction action = null;
		
		ActionDescription[] actionDescriptions = getActions();
		for (ActionDescription actionDescription : actionDescriptions) {
			String actionId= actionDescription.getId();
			if (actionId.equals(id)) {
				action = actionDescription.createAction();
				break;
			}
		}

		return action;
	}
	
	/**
	 * Returns the title icon.
	 * 
	 * @return Title icon or <code>null</code> if no icon has been registered
	 */
	public Image getTitleIcon() {
		
		if (iconImage != null)
			return iconImage;
		
		String plugin = Installer.getDefault().getContext().getBundle().getSymbolicName();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(plugin, ICON_EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i1 = 0; i1 < extensions.length; i1++) {
			// Loop through the extension elements
			IConfigurationElement[] elements = extensions[i1].getConfigurationElements();
			for (int i2 = 0; i2 < elements.length; i2++) {
				// Action element
				IConfigurationElement confElement = elements[i2];
				if (!(confElement.getName().equals(ELEMENT_ICON))) //$NON-NLS-1$
					continue;
				else {
					Bundle bundle = Platform.getBundle(confElement.getContributor().getName());
					String iconPath = confElement.getAttribute(ATTRIBUTE_IMAGE);
					if (iconPath != null) {
						URL iconURL = FileLocator.find(bundle, new Path(iconPath), null);
						if (iconURL != null) {
							ImageDescriptor id = ImageDescriptor.createFromURL(iconURL);
							iconImage = id.createImage();
							return iconImage;
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Verifies an installation folder.
	 * 
	 * @param installLocation Install location
	 * @return Status for the folder
	 */
	public IStatus[] verifyInstallLocation(IPath installLocation) {

		ArrayList<IStatus> status = new ArrayList<IStatus>();
		// Check installation location with verifiers
		IInstallVerifier[] verifiers = ContributorRegistry.getDefault().getInstallVerifiers();
		for (IInstallVerifier verifier : verifiers) {
			IStatus verifyStatus = verifier.verifyInstallLocation(installLocation);
			if ((verifyStatus != null) && !verifyStatus.isOK()) {
				status.add(verifyStatus);
			}
		}
		
		return status.toArray(new IStatus[status.size()]);
	}
	
	/**
	 * Verify the user supplied credentials to insure they are valid
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	public IStatus[] verifyCredentials(String username, String password) {
		IInstallVerifier[] verifiers = ContributorRegistry.getDefault().getInstallVerifiers();
		ArrayList<IStatus> status = new ArrayList<IStatus>();
		
		for (IInstallVerifier verifier : verifiers) {
			IStatus verifyStatus = verifier.verifyCredentials(username, password);
			if ((verifyStatus != null) && !verifyStatus.isOK()) {
				status.add(verifyStatus);
			}
		}
		return status.toArray(new IStatus[status.size()]);
	}
}
