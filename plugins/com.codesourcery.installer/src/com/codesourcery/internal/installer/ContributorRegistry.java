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
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallModule;
import com.codesourcery.installer.IInstallPlatformActions;
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
	/** Icon extension identifier */
	private static final String ICON_EXTENSION_ID = "icon"; //$NON-NLS-1$
	/** Icon extension element */
	private static final String ELEMENT_ICON = "icon"; //$NON-NLS-1$
	/** Image extension attribute */
	private static final String ATTRIBUTE_IMAGE = "image"; //$NON-NLS-1$
	/** Platform actions provider extension identifier */
	private static final String PLATFORM_ACTIONS_EXTENSION_ID = "platformActionsProvider"; //$NON-NLS-1$
	/** Provider extension element */
	private static final String ELEMENT_PROVIDER = "actionsProvider"; //$NON-NLS-1$
	/** Class extension attribute */
	private static final String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	/** Default instance */
	private static ContributorRegistry registry = null;

	/** Registered modules */
	private IInstallModule[] modules;
	/** Registered actions providers */
	private ActionDescription[] actions;
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
	 * Returns the registered platform actions if available.  If multiple
	 * platform actions are registered, the first is returned.
	 * 
	 * @return Platform actions or <code>null</code> if not available.
	 */
	public IInstallPlatformActions getPlatformActions() {
		IInstallPlatformActions actionsProvider = null;
		
		String plugin = Installer.getDefault().getContext().getBundle().getSymbolicName();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(plugin, PLATFORM_ACTIONS_EXTENSION_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equals(ELEMENT_PROVIDER)) {
					try {
						Object exe = element.createExecutableExtension(ATTRIBUTE_CLASS);
						if (exe instanceof IInstallPlatformActions) {
							actionsProvider = (IInstallPlatformActions)exe;
							break;
						}
						else {
							Installer.log("Extension must implement IInstallPlatformActions interface.");
						}
					} catch (CoreException e) {
						Installer.log(e);
					}
				}
			}
			if (actionsProvider != null) {
				break;
			}
		}
		
		return actionsProvider;
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
}
