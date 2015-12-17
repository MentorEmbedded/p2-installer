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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.planner.IPlanner;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.AbstractInstallModule;
import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallDescription.MirrorMode;
import com.codesourcery.installer.IInstallPlatform.ShortcutFolder;
import com.codesourcery.installer.IInstallValues;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.IProductRange;
import com.codesourcery.installer.IRepositoryLocation;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LicenseDescriptor;
import com.codesourcery.installer.LinkDescription;
import com.codesourcery.installer.UninstallMode;
import com.codesourcery.installer.ui.IInstallPages;
import com.codesourcery.internal.installer.actions.InstallIUAction;
import com.codesourcery.internal.installer.actions.PathAction;
import com.codesourcery.internal.installer.actions.ShortcutAction;
import com.codesourcery.internal.installer.actions.UninstallLinkAction;
import com.codesourcery.internal.installer.actions.UnityShortcutAction;
import com.codesourcery.internal.installer.ui.pages.ComponentsPage;
import com.codesourcery.internal.installer.ui.pages.InformationPage;
import com.codesourcery.internal.installer.ui.pages.InstallFolderPage;
import com.codesourcery.internal.installer.ui.pages.LicensePage;
import com.codesourcery.internal.installer.ui.pages.MirrorPage;
import com.codesourcery.internal.installer.ui.pages.PathPage;
import com.codesourcery.internal.installer.ui.pages.SetupInstalledProductsPage;
import com.codesourcery.internal.installer.ui.pages.SetupPage;
import com.codesourcery.internal.installer.ui.pages.ShortcutsPage;
import com.codesourcery.internal.installer.ui.pages.WelcomePage;

/**
 * Install module that provides general wizard pages and actions.
 */
public class GeneralInstallModule extends AbstractInstallModule {
	/** Module identifier */
	public static final String ID = "com.codesourcery.installer.module.default";
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void init(IInstallDescription installDescription) {
		super.init(installDescription);
		
		Installer.getDefault().getInstallManager().addInstallVerifier(new GeneralInstallVerifier());
	}
	
	@Override
	public void initAgent(IProvisioningAgent agent) {
		// Use ordered planner
		if (Installer.getDefault().getInstallManager().getInstallDescription().getOrderPlanner()) {
			agent.registerService(IPlanner.SERVICE_NAME, new OrderedPlanner(agent));
		}
	}

	@Override
	public void setDataDefaults(IInstallData data) {
		// Initialize defaults
		data.setProperty(IInstallValues.CREATE_PROGRAM_SHORTCUTS, true);
		data.setProperty(IInstallValues.CREATE_DESKTOP_SHORTCUTS, true);
		data.setProperty(IInstallValues.CREATE_LAUNCHER_SHORTCUTS, true);
		data.setProperty(IInstallValues.SET_PATH, true);
	}

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
			addUninstallLinkAction(actions, data);
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
		InstallIUAction iuAction = new InstallIUAction(
				RepositoryManager.getDefault().getProfileId(),
				getInstallDescription().getProfileProperties(),
				getInstallDescription().getUpdateSites(),
				unitsToAdd.toArray(new IInstallableUnit[unitsToAdd.size()]),
				unitsToRemove.toArray(new IInstallableUnit[unitsToRemove.size()])
				);
		iuAction.setRemoveProfile(getInstallDescription().getRemoveProfile());
		actions.add(iuAction);
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
					if (!data.getBooleanProperty(IInstallValues.CREATE_PROGRAM_SHORTCUTS))
						continue;
				}
				// Add desktop shortcuts?
				if (link.getFolder().equals(ShortcutFolder.DESKTOP)) {
					if (!data.getBooleanProperty(IInstallValues.CREATE_DESKTOP_SHORTCUTS))
						continue;
				}
				// Add launcher shortcuts?
				if (link.getFolder().equals(ShortcutFolder.UNITY_DASH)) {
					if (!data.getBooleanProperty(IInstallValues.CREATE_LAUNCHER_SHORTCUTS) ||
							!Installer.getDefault().getInstallPlatform()
							.desktopIsUnity())
						continue;
				}

				// Handle {cmd} variable.
				IPath target;
				if (link.getTarget().equals("{cmd}")) {
					target = getShellPath();
				} else {
					target = getInstallDescription().getRootLocation().append(link.getTarget());
				}

				IPath iconPath = null;
				// Link includes icon
				if ((link.getIconPath() != null) && !link.getIconPath().isEmpty()) { 
					iconPath = getInstallDescription().getRootLocation().append(link.getIconPath());
					String iconExtension = iconPath.getFileExtension();
					// Only icon resources allowed on Windows
					if (Installer.isWindows() && (!iconExtension.equals("exe") && !iconExtension.equals("ico"))) {
						iconPath = null;
					}
				}				
				IPath workingDirectory = target.removeLastSegments(1);
				IPath shortcutFolder;
				IPath removeFolder = null;
				IPath linkPath = new Path(link.getPath());
				IPath baseFolder = Installer.getDefault().getInstallPlatform().getShortcutFolder(link.getFolder()); 

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
					String value = (String)data.getProperty(IInstallValues.PROGRAM_SHORTCUTS_FOLDER);
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
					// No user location (silent mode) - use default relative to 
					// install location
					else {
						IPath folder = getInstallDescription().getLinksLocation().append(linkPath);
						IPath installFolder = Installer.getDefault().getInstallManager().getInstallLocation();
						shortcutFolder = Installer.isWindows() ? baseFolder.append(folder) : installFolder.append(folder);
						removeFolder = getFirstNonExistentPath(shortcutFolder);
					}
				}

				// Ubuntu Unity short-cut
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
	 * Returns the path to the system shell.
	 * 
	 * @return shell path
	 * @throws Exception
	 */
	protected IPath getShellPath() throws Exception {
		if (!Installer.isWindows()) {
			throw new Exception("{cmd} property only supported for Windows");
		}
		
		String shellPath = System.getenv("ComSpec");
		
		if (shellPath == null) {
			throw new Exception("Can't resolve {cmd} property");
		}
		
		return new Path(shellPath);
	}
	
	/**
	 * Adds PATH environment action.
	 * 
	 * @param actions Actions
	 * @param data Install data
	 */
	protected void addPathAction(List<IInstallAction> actions, IInstallData data) {
		if (data.getBooleanProperty(IInstallValues.SET_PATH)) {
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
	 * @param data Install data
	 */
	protected void addUninstallLinkAction(List<IInstallAction> actions, IInstallData data) {
		if (Installer.isWindows()) {
			// Install size (in kilobytes)
			int installSize = -1;
			String size = data.getProperty(IInstallValues.INSTALL_SIZE);
			if (size != null) {
				try {
					long bytes = Long.parseLong(size);
					installSize = (int)(bytes / 1024);
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
			
			UninstallMode uninstallMode = getInstallDescription().getUninstallMode();
			if ((uninstallMode != null) && uninstallMode.getCreateAddRemove() && (getInstallDescription().getUninstallerName() != null)) {
				actions.add(new UninstallLinkAction(
						getInstallDescription().getRootLocation().append(IInstallConstants.UNINSTALL_DIRECTORY),
						getInstallDescription().getProductVendor(),
						getInstallDescription().getProductVersionString(),
						getInstallDescription().getProductHelp(),
						getInstallDescription().getUninstallerName(),
						installSize));
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
		IInstallWizardPage[] setupPages = createSetupPages(installDescription);
		for (IInstallWizardPage setupPage : setupPages) {
			pages.add(setupPage);
		}
		
		// Fixed licenses
		LicenseDescriptor[] licenses = installDescription.getLicenses();

		// Welcome page
		IInstallWizardPage welcomePage = createWelcomePage(installDescription, installDescription.getProductName());
		pages.add(welcomePage);

		// Determine if any IU licenses should be displayed
		boolean includeUnitLicenses = false;
		if (licenses != null) {
			for (LicenseDescriptor license : licenses) {
				if (license.getUnit() != null) {
					includeUnitLicenses = true;
					break;
				}
			}
		}
		
		// License page
		// If IU license information will not be included, order the license page 
		// after the Welcome page.
		if ((licenses !=null) && (licenses.length > 0) && !includeUnitLicenses) {
			pages.add(new LicensePage(licenses));
		}
		
		// Information page
		String informationText = installDescription.getText(IInstallDescription.TEXT_INFORMATION, null);
		if (informationText != null) {
			InformationPage informationPage = new InformationPage(IInstallPages.INFORMATION_PAGE, InstallMessages.InformationPageTitle, informationText);
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

		// Components page
		IInstallWizardPage componentsPage = createComponentPage(installDescription);
		if(componentsPage != null)
			pages.add(componentsPage);

		// If IU license information will be included, show page after the
		// Components page.
		if ((licenses !=null) && (licenses.length > 0) && includeUnitLicenses) {
			pages.add(new LicensePage(licenses));
		}
		
		// Shortcuts page
		LinkDescription[] links = installDescription.getLinks();
		if (links != null) {
			ShortcutsPage shortcutsPage = new ShortcutsPage(IInstallPages.SHORTCUTS_PAGE, 
					InstallMessages.ShortcutsPageTitle, links);
			pages.add(shortcutsPage);
		}
		
		// PATH page
		String[] paths = installDescription.getEnvironmentPaths();
		if ((paths != null) && (paths.length > 0)) {
			PathPage pathPage = new PathPage(IInstallPages.PATHS_PAGE, InstallMessages.PathPageTitle, 
					productName, paths);
			pages.add(pathPage);
		}
		
		return pages.toArray(new IInstallWizardPage[pages.size()]);
	}

	/**
	 * Creates the Setup page if required.
	 * 
	 * @param installDescription Install description
	 * @return Setup pages
	 */
	protected IInstallWizardPage[] createSetupPages(IInstallDescription installDescription) {
		ArrayList<IInstallWizardPage> setupPages = new ArrayList<IInstallWizardPage>();
		
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();

		// If no mirror repository has been set
		if (RepositoryManager.getDefault().getCacheLocation() == null) {
			boolean enableMirror;
			// Enable mirroring only for remote repositories
			if (installDescription.getMirrorMode() == MirrorMode.REMOTE_ONLY) {
				enableMirror = false;
				List<IRepositoryLocation> repositoryLocations = installDescription.getRepositoryLocations();
				for (IRepositoryLocation location : repositoryLocations) {
					URI[] metadataLocations = location.getMetadataLocations();
					for (URI metadataLocation : metadataLocations) {
						// Remote repository
						if (metadataLocation.getHost() != null) {
							enableMirror = true;
							break;
						}
					}
				}
			}
			// Enable mirroring
			else {
				enableMirror = installDescription.getMirrorMode() != MirrorMode.NONE;
			}
			
			// Add mirror page if required
			if (enableMirror) {
				MirrorPage mirrorPage = new MirrorPage(IInstallPages.MIRROR_PAGE, InstallMessages.MirrorPage_Name);
				setupPages.add(mirrorPage);
			}
		}
		
		String installedProductsTitle = installDescription.getText(IInstallDescription.TEXT_CATEGORY_INSTALL,
				NLS.bind(InstallMessages.SetupPage_Title0, Installer.getDefault().getInstallManager().getInstallDescription().getProductName()));

		// Installing
		if (mode.isInstall()) {
			// Product requirement range
			IProductRange[] range = installDescription.getRequires();
			// Product category
			String category = installDescription.getProductCategory();
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
					products = Installer.getDefault().getInstallManager().getInstalledProductsByRange(range, true);
				}
				// Show setup page to pick product for update, otherwise the user
				// must browse for the product installation in the wizard
				if ((products != null) && (products.length > 0)) {
					IInstallWizardPage setupPage = new SetupInstalledProductsPage(IInstallPages.SETUP_PAGE, installedProductsTitle, products, false);
					setupPages.add(setupPage);
				}
			}
			// Update or upgrade
			else if (installedProduct != null) {
				IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
				
				Version productVersion = description.getProductVersion();
				boolean update = (productVersion.equals(installedProduct.getVersion()));
				boolean supportsUpdate = description.getSupportsUpdate();
				boolean supportsUpgrade = description.getSupportsUpgrade();
				String updateTitle = description.getText(IInstallDescription.TEXT_EXISTING_INSTALL, null);
				
				// If no existing version text supplied, use default
				if (updateTitle == null) {
					updateTitle = NLS.bind(InstallMessages.SetupPrompter_Title1, installedProduct.getName(), installedProduct.getVersionText());
				}
				// Else replace version property in text
				else {
					updateTitle = updateTitle.replace("${" + IInstallDescription.PROP_EXISTING_VERSION + "}", installedProduct.getVersionText());
				}
				
				// Update
				if (update && supportsUpdate) {
					IInstallWizardPage setupPage = new SetupPage(IInstallPages.SETUP_PAGE, updateTitle, installedProduct);
					setupPages.add(setupPage);
				}
				// Upgrade
				else if (supportsUpgrade){
					Version minUpgradeVersion = getInstallDescription().getMinimumUpgradeVersion();
					// If the product meets the minimum supported upgrade version
					if ((minUpgradeVersion == null) || (installedProduct.getVersion().compareTo(minUpgradeVersion) >= 0)) {
						IInstallWizardPage setupPage = new SetupPage(IInstallPages.SETUP_PAGE, updateTitle, installedProduct);
						setupPages.add(setupPage);
					}
				}
			}
			// Category specified
			else if (category != null) {
				IInstalledProduct[] products = Installer.getDefault().getInstallManager().getInstalledProductsByCategory(category, true);
				// If a requirement on other products is also specified, filter the list
				if (range != null) {
					IInstalledProduct[] requiredProducts = Installer.getDefault().getInstallManager().getInstalledProductsByRange(range, true);
					ArrayList<IInstalledProduct> validProducts = new ArrayList<IInstalledProduct>();
					for (IInstalledProduct product : products) {
						for (IInstalledProduct requiredProduct : requiredProducts) {
							if (requiredProduct.getId().equals(product.getId())) {
								validProducts.add(product);
								break;
							}
						}
					}
					products = validProducts.toArray(new IInstalledProduct[validProducts.size()]);
				}
				if (products.length > 0) {
					IInstallWizardPage setupPage = new SetupInstalledProductsPage(IInstallPages.SETUP_PAGE, installedProductsTitle, products, true);
					setupPages.add(setupPage);
				}
			}
			// Else if product range is specified
			else if (range != null) {
				IInstalledProduct[] products = Installer.getDefault().getInstallManager().getInstalledProductsByRange(range, true);
				if (products.length > 0) {
					IInstallWizardPage setupPage = new SetupInstalledProductsPage(IInstallPages.SETUP_PAGE, installedProductsTitle, products, false);
					setupPages.add(setupPage);
				}
			}
		}
		
		return setupPages.toArray(new IInstallWizardPage[setupPages.size()]);
	}
	
	/***
	 * Creates Welcome page
	 * 
	 * @param installDescription Install description
	 * @param productName Product name
	 * @return Welcome page
	 */
	protected IInstallWizardPage createWelcomePage(IInstallDescription installDescription, String productName) {
		return new WelcomePage(IInstallPages.WELCOME_PAGE, productName, installDescription.getText(IInstallDescription.TEXT_WELCOME, null));
	}
	
	/***
	 * Creates page to specify install folder.
	 * 
	 * @param installDescription Install description
	 * @param defaultFolder Default path of installation directory
	 * @return Install folder page
	 */
	protected IInstallWizardPage createInstallFolderPage(IInstallDescription installDescription, String defaultFolder) {
		return new InstallFolderPage(IInstallPages.INSTALL_FOLDER_PAGE, InstallMessages.InstallFolderPageTitle, defaultFolder, installDescription);
	}
	
	/***
	 * Creates page to list components
	 * 
	 * @param installDescription Install description
	 * @return Components page
	 */
	protected IInstallWizardPage createComponentPage(IInstallDescription installDescription) {
		ComponentsPage componentsPage = new ComponentsPage(IInstallPages.COMPONENTS_PAGE,
				InstallMessages.Components, installDescription);
		
		return componentsPage;
	}

	@Override
	public void setEnvironmentVariables(Map<String, String> environment) {
		// Set up any PATH that may be required to launch programs
		String[] paths = getInstallDescription().getEnvironmentPaths();
		if (paths != null) {
			String pathKey = "PATH";
			String pathVar = environment.get(pathKey);
			
			if (pathVar == null) {
				pathVar = "";
			}
			
			for (String path : paths) {
				IPath resolvedPath = getInstallDescription().getRootLocation().append(path);
				pathVar = resolvedPath.toString() + File.pathSeparatorChar + pathVar;
			}
			environment.put(pathKey, pathVar);
		}
	}
}
