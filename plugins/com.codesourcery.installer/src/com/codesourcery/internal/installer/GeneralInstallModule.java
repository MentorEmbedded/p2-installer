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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import com.codesourcery.installer.AbstractInstallModule;
import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.IProductRange;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LicenseDescriptor;
import com.codesourcery.installer.LinkDescription;
import com.codesourcery.installer.ui.IInstallPageConstants;
import com.codesourcery.internal.installer.actions.InstallIUAction;
import com.codesourcery.internal.installer.actions.PathAction;
import com.codesourcery.internal.installer.actions.ShortcutAction;
import com.codesourcery.internal.installer.actions.UninstallLinkAction;
import com.codesourcery.internal.installer.actions.UnityShortcutAction;
import com.codesourcery.internal.installer.ui.pages.AddonsPage;
import com.codesourcery.internal.installer.ui.pages.ComponentsPage;
import com.codesourcery.internal.installer.ui.pages.InformationPage;
import com.codesourcery.internal.installer.ui.pages.InstallFolderPage;
import com.codesourcery.internal.installer.ui.pages.LicensePage;
import com.codesourcery.internal.installer.ui.pages.PathPage;
import com.codesourcery.internal.installer.ui.pages.SetupInstalledProductsPage;
import com.codesourcery.internal.installer.ui.pages.SetupPage;
import com.codesourcery.internal.installer.ui.pages.ShortcutsPage;
import com.codesourcery.internal.installer.ui.pages.WelcomePage;

/**
 * Install module that provides general wizard pages and actions.
 */
public class GeneralInstallModule extends AbstractInstallModule {
	@Override
	public IInstallAction[] getInstallActions(IProvisioningAgent agent, IInstallData data, IInstallMode installMode) {
		ArrayList<IInstallAction> actions = new ArrayList<IInstallAction>();

		// P2 IU action
		addIUAction(actions, data);

		if (!installMode.isUpdate()) {
			// Short-cut actions
			addShortcutActions(actions, data);
			
			// Add PATH environment action
			addPathAction(actions, data);
	
			// Uninstall links
			addUninstallLinkAction(actions);
		}
		
		return actions.toArray(new IInstallAction[actions.size()]);
	}

	/**
	 * Adds the P2 IU action.
	 * 
	 * @param actions Actions
	 * @param data Install data
	 */
	protected void addIUAction(List<IInstallAction> actions, IInstallData data) {
		// Units to add
		ArrayList<IInstallableUnit> unitsToAdd = new ArrayList<IInstallableUnit>();
		// Units to remove
		ArrayList<IInstallableUnit> unitsToRemove = new ArrayList<IInstallableUnit>();

		// Get the install plan
		RepositoryManager.getDefault().getInstallUnits(unitsToAdd, unitsToRemove);
		
		// Add P2 provision action
		actions.add(new InstallIUAction(
				getInstallDescription().getProfileName(),
				getInstallDescription().getProfileProperties(),
				getInstallDescription().getUpdateSites(),
				unitsToAdd.toArray(new IInstallableUnit[unitsToAdd.size()]),
				unitsToRemove.toArray(new IInstallableUnit[unitsToRemove.size()]),
				getInstallDescription().getProgressFindPatterns(),
				getInstallDescription().getProgressReplacePatterns()
				)
		);
	}
	
	/**
	 * Adds short-cut actions.
	 * 
	 * @param actions Actions
	 * @param data Install data
	 */
	protected void addShortcutActions(List<IInstallAction> actions, IInstallData data) {
		LinkDescription[] links = getInstallDescription().getLinks();
		
		// No short-cuts
		if (links == null || links.length == 0)
			return;
		
		for (LinkDescription link : links) {
			try {
				// Add program shortcuts?
				if (link.getFolder().equals(ShortcutFolder.PROGRAMS)) {
					Boolean addPrograms = (Boolean)data.getProperty(IInstallConstants.PROPERTY_PROGRAM_SHORTCUTS);
					if ((addPrograms != null) && (addPrograms.booleanValue() == false))
						continue;
				}
				// Add desktop shortcuts?
				if (link.getFolder().equals(ShortcutFolder.DESKTOP)) {
					Boolean addDesktop = (Boolean)data.getProperty(IInstallConstants.PROPERTY_DESKTOP_SHORTCUTS);
					if ((addDesktop != null) && (addDesktop.booleanValue() == false))
						continue;
				}
				// Add launcher shortcuts?
				if (link.getFolder().equals(ShortcutFolder.UNITY_DASH)) {
					Boolean addLauncher = (Boolean)data.getProperty(IInstallConstants.PROPERTY_LAUNCHER_SHORTCUTS);
					if (((addLauncher != null) && (addLauncher
							.booleanValue() == false))
							|| !Installer.getDefault().getInstallPlatform()
							.desktopIsUnity())
						continue;
				}

				IPath target = getInstallDescription().getRootLocation().append(link.getTarget());
				IPath iconPath = getInstallDescription().getRootLocation().append(link.getIconPath());
				IPath workingDirectory = target.removeLastSegments(1);
				IPath shortcutFolder;
				IPath removeFolder = null;
				IPath linkPath = new Path(link.getPath());
				
				boolean allUsers = getInstallDescription().getAllUsers();
				IPath baseFolder = Installer.getDefault().getInstallPlatform().getShortcutFolder(link.getFolder(), allUsers); 

				// Desktop short-cut
				if (link.getFolder() == ShortcutFolder.DESKTOP) {
					// Remove top short-cut folder
					if (!linkPath.isEmpty()) {
						removeFolder = baseFolder.append(getTopFolder(linkPath));
					}
					shortcutFolder = baseFolder.append(linkPath);
				}
				// Programs short-cut
				else {
					// User location available
					String value = (String)data.getProperty(IInstallConstants.PROPERTY_PROGRAM_SHORTCUTS_FOLDER);
					if (value != null) {
						IPath userPath = new Path(value);

						// On Windows, use start menu relative path
						if (Installer.isWindows()) {
							removeFolder = baseFolder.append(getTopFolder(userPath));
							shortcutFolder = baseFolder.append(userPath).append(link.getPath());
						}
						// On Linux, use full path
						else {
							removeFolder = getFirstNonExistentPath(userPath);
							shortcutFolder = userPath.append(link.getPath());
						}
					}
					// No user location (headless mode) - use default
					else {
						IPath folder = getInstallDescription().getLinksLocation().append(linkPath);
						shortcutFolder = baseFolder.append(folder);
						if (!folder.isEmpty())
							removeFolder = baseFolder.append(getTopFolder(folder));
					}
				}

				ShortcutAction shortcutAction = null;
				if (link.getFolder() == ShortcutFolder.UNITY_DASH
						|| (link.getFolder() == ShortcutFolder.DESKTOP && Installer
						.getDefault().getInstallPlatform()
						.desktopIsUnity())) {
					shortcutAction = new UnityShortcutAction(shortcutFolder, removeFolder, link.getName(),
							target, iconPath, workingDirectory, link.getArguments(),
							link.getFolder() == ShortcutFolder.UNITY_DASH);
				} else {
					shortcutAction = new ShortcutAction(shortcutFolder, removeFolder, link.getName(), target,
							iconPath, workingDirectory, link.getArguments());
				}
				actions.add(shortcutAction);
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
	}

	/**
	 * Adds PATH environment action.
	 * 
	 * @param actions Actions
	 * @param data Install data
	 */
	protected void addPathAction(List<IInstallAction> actions, IInstallData data) {
		Boolean addEnvironment = (Boolean)data.getProperty(IInstallConstants.PROPERTY_MODIFY_PATHS);
		if ((addEnvironment == null) || (addEnvironment.booleanValue() == true)) {
			// Add paths
			String[] paths = getInstallDescription().getEnvironmentPaths();
			if (paths != null) {
				String[] resolvedPaths = new String[paths.length];
				for (int index = 0; index < paths.length; index ++) {
					IPath path = getInstallDescription().getRootLocation().append(paths[index]);
					resolvedPaths[index] = path.toOSString();
				}
				actions.add(new PathAction(resolvedPaths));
			}
		}
	}
	
	/**
	 * Adds uninstall link action (Windows only).
	 * 
	 * @param actions Actions
	 */
	protected void addUninstallLinkAction(List<IInstallAction> actions) {
		if (Installer.isWindows()) {
			if (getInstallDescription().getUninstallerName() != null) {
				actions.add(new UninstallLinkAction(
						getInstallDescription().getRootLocation().append(IInstallConstants.UNINSTALL_DIRECTORY),
						getInstallDescription().getProductVendor(),
						getInstallDescription().getProductVersionString(),
						getInstallDescription().getProductHelp(),
						getInstallDescription().getUninstallerName()));
			}
		}
	}
	
	/**
	 * Given a full path, this method returns the path to the first
	 * directory that does not exist.
	 * 
	 * @param fullPath Full path
	 * @return Path to first directory in full path that does
	 * not exist.
	 */
	private IPath getFirstNonExistentPath(IPath fullPath) {
		IPath path = new Path("").makeAbsolute();
		for (String segment : fullPath.segments()) {
			path = path.append(segment);
			if (!path.toFile().exists())
				break;
		}
		
		return path;
	}
	
	/**
	 * Returns the top most folder of a path.
	 * 
	 * @return
	 */
	private IPath getTopFolder(IPath path) {
		if (!path.isEmpty())
			return new Path(path.segment(0));
		else
			return new Path("");
	}

	@Override
	public IInstallWizardPage[] getInstallPages(IInstallMode installMode) {
		ArrayList<IInstallWizardPage> pages = new ArrayList<IInstallWizardPage>();

		IInstallDescription installDescription = getInstallDescription();		
		String productName = installDescription.getProductName();

		// Setup page
		IInstallWizardPage setupPage = createSetupPage(installDescription);
		if (setupPage != null) {
			pages.add(setupPage);
		}
		
		// Fixed licenses
		LicenseDescriptor[] licenses = installDescription.getLicenses();

		// Welcome page
		IInstallWizardPage welcomePage = createWelcomePage(installDescription, installDescription.getProductName());
		pages.add(welcomePage);

		// License page
		// If IU license information will not be included, order the license page 
		// after the Welcome page.
		if ((licenses !=null) && (licenses.length > 0) && !installDescription.getLicenseIU()) {
			pages.add(new LicensePage(licenses));
		}
		
		// Information page
		String informationText = installDescription.getInformationText();
		if (informationText != null) {
			InformationPage informationPage = new InformationPage(IInstallPageConstants.INFORMATION_PAGE, InstallMessages.InformationPageTitle, informationText);
			informationPage.setScrollable(true);
			pages.add(informationPage);
		}
		
		// If install location has not already been set
		if (Installer.getDefault().getInstallManager().getInstallLocation() == null) {
			// Installation folder page
			String defaultFolder = null;
			IPath installLocation = installDescription.getRootLocation();
			if (installLocation != null) {
				defaultFolder = installLocation.toOSString();
			}
			IInstallWizardPage installFolderPage = createInstallFolderPage(installDescription, defaultFolder);
			pages.add(installFolderPage);
		}

		// Add-ons page
		URI[] addonRepositories = installDescription.getAddonRepositories();
		if ((addonRepositories != null) && (addonRepositories.length > 0)) {
			AddonsPage addonsPage = new AddonsPage(IInstallPageConstants.ADDONS_PAGE, InstallMessages.Addons, 
					installDescription);
			addonsPage.setAddonsDescription(installDescription.getAddonDescription());
			pages.add(addonsPage);
		}

		// Components page
		IInstallWizardPage componentsPage = createComponentPage(installDescription);
		if(componentsPage != null)
			pages.add(componentsPage);

		// If IU license information will be included, show page after the
		// Components page.
		if (installDescription.getLicenseIU()) {
			pages.add(new LicensePage(licenses));
		}
		
		// Shortcuts page
		LinkDescription[] links = installDescription.getLinks();
		if (links != null) {
			try {
				boolean allUsers = Installer.getDefault().getInstallManager().getInstallDescription().getAllUsers();
				IPath location = Installer.isWindows() ? installDescription.getLinksLocation() : 
					Installer.getDefault().getInstallPlatform().getShortcutFolder(ShortcutFolder.PROGRAMS, allUsers).append(installDescription.getLinksLocation());
				ShortcutsPage shortcutsPage = new ShortcutsPage(IInstallPageConstants.SHORTCUTS_PAGE, 
						InstallMessages.ShortcutsPageTitle, links, location);
				pages.add(shortcutsPage);
			}
			catch (CoreException e) {
				Installer.log(e);
			}
		}
		
		// PATH page
		String[] paths = installDescription.getEnvironmentPaths();
		if ((paths != null) && (paths.length > 0)) {
			PathPage pathPage = new PathPage(IInstallPageConstants.PATHS_PAGE, InstallMessages.PathPageTitle, 
					productName, true, paths);
			pages.add(pathPage);
		}
		
		return pages.toArray(new IInstallWizardPage[pages.size()]);
	}

	/**
	 * Creates the Setup page if required.
	 * 
	 * @param installDescription Install description
	 * @return Setup page or <code>null</code> if not required
	 */
	protected IInstallWizardPage createSetupPage(IInstallDescription installDescription) {
		IInstallWizardPage setupPage = null;
		
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		// Installing
		if (mode.isInstall()) {
			IProductRange[] range = installDescription.getRequires();
			
			// Get existing product if available
			String productId = getInstallDescription().getProductId();
			IInstalledProduct installedProduct = Installer.getDefault().getInstallManager().getInstalledProduct(productId);

			// If patch install
			if (mode.isPatch()) {
				IInstalledProduct[] products = null;
				// If range is not specified, choose from all products
				if (range == null) {
					products = Installer.getDefault().getInstallManager().getInstalledProducts();
				}
				// Choose from applicable products
				else {
					products = Installer.getDefault().getInstallManager().getInstalledProducts(range, true);
				}
				// Show setup page to pick product for update, otherwise the user
				// must browse for the product installation in the wizard
				if ((products != null) && (products.length > 0)) {
					setupPage = new SetupInstalledProductsPage("setupPage", products);
				}
			}
			// Update or upgrade
			else if (installedProduct != null) {
				setupPage = new SetupPage("setupPage", installedProduct);
			}
			// Else if product range is specified
			else if (range != null) {
				IInstalledProduct[] products = Installer.getDefault().getInstallManager().getInstalledProducts(range, true);
				setupPage = new SetupInstalledProductsPage("setupPage", products);
			}
		}
		
		return setupPage;
	}
	
	/***
	 * Creates Welcome page
	 * @param installDescription
	 * @param productName
	 * @return welcome page
	 */
	protected IInstallWizardPage createWelcomePage(IInstallDescription installDescription, String productName) {
		return new WelcomePage(IInstallPageConstants.WELCOME_PAGE, productName, installDescription.getWelcomeText());
	}
	
	/***
	 * Creates page to specify install folder.
	 * @param installDescription
	 * @param defaultFolder
	 * @return install folder page
	 */
	protected IInstallWizardPage createInstallFolderPage(IInstallDescription installDescription, String defaultFolder) {
		return new InstallFolderPage(IInstallPageConstants.INSTALL_FOLDER_PAGE, InstallMessages.InstallFolderPageTitle, defaultFolder, installDescription);
	}
	
	/***
	 * Creates page to list components
	 * @param installDescription
	 * @return components page
	 */
	protected IInstallWizardPage createComponentPage(IInstallDescription installDescription) {
		ComponentsPage componentsPage = new ComponentsPage(IInstallPageConstants.COMPONENTS_PAGE,
				InstallMessages.Components, installDescription);
		
		return componentsPage;
	}
}
