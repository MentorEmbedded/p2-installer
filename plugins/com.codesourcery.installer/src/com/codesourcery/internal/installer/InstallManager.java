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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallManager;
import com.codesourcery.installer.IInstallManifest;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallModule;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.IProductRange;
import com.codesourcery.installer.InstallPageTitle;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.LaunchItem.LaunchItemType;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.actions.InstallIUAction;

/**
 * This class manages install operations.
 * Call {@link #setInstallLocation(IPath)} to set the install location.
 */
public class InstallManager implements IInstallManager {
	/** Cleanup progress work */
	private static final int CLEANUP_PROGRESS = 10;
	/** Uninstall setup progress work */
	private static final int UNINSTALL_SETUP_PROGRESS = 10;
	/** Product progress work */
	private static final int PRODUCT_PROGRESS = 100;
	/** Install registry filename */
	private static final String INSTALL_REGISTRY_FILENAME = ".registry";
	/** Installed product to update with installation */
	private IInstalledProduct installedProduct;
	
	/**
	 * Locations manager
	 */
	private LocationsManager locationsManager = new LocationsManager();
	/**
	 * Install description
	 */
	private IInstallDescription installDescription;
	/**
	 * Install manifest
	 */
	private IInstallManifest installManifest;
	/**
	 * Install modules
	 */
	IInstallModule[] modules;
	/**
	 * Install location
	 */
	private IPath installLocation;
	/**
	 * Install mode
	 */
	private InstallMode installMode;
	/**
	 * Install registry
	 */
	private InstallRegistry installRegistry = new InstallRegistry();

	/**
	 * Constructor
	 */
	public InstallManager() {
		// Create default new install manifest
		installManifest = new InstallManifest();
		// Load install locations
		locationsManager.load();
		// Install registry
		installRegistry = new InstallRegistry();
		IPath path = Installer.getDefault().getDataFolder().append(INSTALL_REGISTRY_FILENAME);
		if (path.toFile().exists()) {
			try {
				installRegistry.load(path);
			} catch (CoreException e) {
				Installer.log(e);
			}
		}
		
	}
	
	/**
	 * Disposes of the install manager.
	 */
	public void dispose() {
		// Save install locations
		getLocationsManager().save();
		// Save install registry
		try {
			IPath path = Installer.getDefault().getDataFolder().append(INSTALL_REGISTRY_FILENAME);
			getInstallRegistry().save(path);
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	@Override
	public void setInstallDescription(IInstallDescription installDescription) {
		this.installDescription = installDescription;
	}
	
	@Override
	public IInstallDescription getInstallDescription() {
		return installDescription;
	}
	
	@Override
	public void setInstallManifest(IInstallManifest installManifest) {
		this.installManifest = installManifest;
	}
	
	@Override
	public IInstallManifest getInstallManifest() {
		return installManifest;
	}
	
	/**
	 * Returns the locations manager.
	 * 
	 * @return Locations manager
	 */
	private LocationsManager getLocationsManager() {
		return locationsManager;
	}
	
	/**
	 * Returns the install registry.
	 * 
	 * @return Install registry
	 */
	private InstallRegistry getInstallRegistry() {
		return installRegistry;
	}
	
	@Override
	public void setInstallLocation(IPath path) throws CoreException {
		getInstallDescription().setRootLocation(path);
		
		// Load existing manifest if available
		loadManifest();

		// Location changed
		if ((path == null) || !path.equals(installLocation)) {
			// Remove old location
			if (installLocation != null) {
				RepositoryManager.getDefault().stopAgent();
				if (!getInstallMode().isUpdate() && !getInstallMode().isUpgrade()) {
					getLocationsManager().deleteInstallLocation(installLocation);
				}
			}
			this.installLocation = path;
		
			// Create new location
			if (installLocation != null) {
				if (!getInstallMode().isUpdate() && !getInstallMode().isUpgrade()) {
					getLocationsManager().createInstallLocation(installLocation);
				}
				// Create new P2 agent
				// Note, if this is not an existing installation, it will result 
				// in agent files being created in the location.
				RepositoryManager.getDefault().createAgent(getInstallDescription().getInstallLocation());
			}
		}
	}
	
	@Override
	public IPath getInstallLocation() {
		return installLocation;
	}

	/**
	 * Sets the install mode.
	 * 
	 * @param installMode Install mode
	 */
	public void setInstallMode(InstallMode installMode) {
		this.installMode = installMode;
	}
	
	@Override
	public IInstallMode getInstallMode() {
		return installMode;
	}
	
	@Override
	public void install(IInstallData installData, IProgressMonitor monitor)
			throws CoreException {
		// Installation mode
		InstallMode mode = (InstallMode)Installer.getDefault().getInstallManager().getInstallMode();
		
		// Check for existing version of product
		IInstallProduct product = getExistingProduct(getInstallManifest());

		// Get install actions
		IInstallAction[] actions = getInstallActions(installData);

		// Remove existing product if required
		if (!mode.isUpdate()) {
			if (product != null) {
				// Uninstall actions of existing product that require it
				for (IInstallAction action : product.getActions()) {
					if (action.uninstallOnUpgrade()) {
						if (isActionSupported(action)) {
							action.run(RepositoryManager.getDefault().getAgent(), 
									product, new InstallMode(false), new NullProgressMonitor());
						}
					}
					
					if (monitor.isCanceled())
						break;
				}
				// Remove existing product
				getInstallManifest().removeProduct(product);
				product = null;
			}
		}

		// Create new product
		if (product == null) {
			product = new InstallProduct(
					getInstallDescription().getProductId(),
					getInstallDescription().getProductName(),
					getInstallDescription().getProductVersionString(),
					getInstallDescription().getRootLocation(),
					getInstallDescription().getInstallLocation());
		}

		// Compute action ticks
		int totalActionWork = 0;
		for (IInstallAction action : actions) {
			if (isActionSupported(action)) {
				totalActionWork += action.getProgressWeight();
			}
		}

		monitor.beginTask("", totalActionWork + CLEANUP_PROGRESS + UNINSTALL_SETUP_PROGRESS);
		
		// Install
		int index;
		for (index = 0; index < actions.length; index ++) {
			IInstallAction action = actions[index];
			if (isActionSupported(action)) {
				// Run action
				SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, action.getProgressWeight());
				action.run(RepositoryManager.getDefault().getAgent(), 
						product, mode, subMonitor);
				
				// Add the action to the product unless the installation mode
				// is update or it is the install IU's action
				if (!getInstallMode().isUpdate() || !(action instanceof InstallIUAction)) {
					product.addAction(action);
				}
			}
			if (monitor.isCanceled())
				break;
		}

		// Installation cancelled - clean up
		if (monitor.isCanceled()) {
			monitor.setTaskName(InstallMessages.CleanupInstallation);
			
			if (getInstallManifest().getProducts().length == 0) {
				mode.setRootUninstall(true);
			}
			
			// Uninstall performed actions
			for (int rollbackIndex = 0; rollbackIndex <= index; rollbackIndex ++) {
				if (isActionSupported(actions[rollbackIndex])) {
					actions[rollbackIndex].run(RepositoryManager.getDefault().getAgent(), 
							product, mode, new NullProgressMonitor());
				}
			}
			
			// Remove product directory
			SubMonitor removeProgress = SubMonitor.convert(monitor, CLEANUP_PROGRESS);
			removeProductDirectory(product, removeProgress);
		}
		// Installation completed
		else {
			monitor.worked(CLEANUP_PROGRESS);
			
			// Update install manifest
			getInstallManifest().addProduct(product);
			// Update install registry
			if (getInstallDescription().getUseInstallRegistry()) {
				getInstallRegistry().addProduct(new InstalledProduct(product.getId(), product.getName(), product.getVersionString(), product.getLocation()));
			}
	
			// Install manifest path
			IPath uninstallLocation = getInstallDescription().getRootLocation().append(IInstallConstants.UNINSTALL_DIRECTORY);
			IPath manifestPath = uninstallLocation.append(IInstallConstants.INSTALL_MANIFEST_FILENAME);
			
			// Setup uninstaller
			try {
				if (!mode.isUpdate()) {
					String[] uninstallFiles = getInstallDescription().getUninstallFiles();
					if (uninstallFiles != null) {
						// If there is an existing uninstaller, remove it to ensure
						// the latest version is included.
						if (uninstallLocation.toFile().exists()) {
							try {
								FileUtils.deleteDirectory(uninstallLocation.toFile());
							} catch (IOException e) {
								Installer.fail(InstallMessages.Error_CopyInstaller, e);
							}
						}
						// Copy installer
						if (!manifestPath.toFile().exists()) {
							copyInstaller(uninstallLocation,  new NullProgressMonitor());
						}
					}
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
			
			// Save manifest
			getInstallManifest().save(manifestPath.toFile());
		}
		
		monitor.worked(UNINSTALL_SETUP_PROGRESS);
		monitor.done();
	}

	@Override
	public void uninstall(IInstallProduct[] products, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 
				products.length * PRODUCT_PROGRESS + products.length * PRODUCT_PROGRESS);

		InstallMode mode = (InstallMode)Installer.getDefault().getInstallManager().getInstallMode();
		
		// Removing all products
		if (getInstallManifest().getProducts().length == products.length) {
			mode.setRootUninstall(true);
		}
		
		if (!mode.isRootUninstall()) {
			// Update manifest
			getInstallManifest().save();
		}

		// Run product actions
		for (IInstallProduct product : products) {
			RepositoryManager.getDefault().createAgent(product.getInstallLocation());
			
			int work = PRODUCT_PROGRESS / product.getActions().length;
			for (IInstallAction action : product.getActions()) {
				if (isActionSupported(action)) {
					action.run(RepositoryManager.getDefault().getAgent(), product, mode, progress.newChild(work));
				}
				if (monitor.isCanceled())
					break;
			}
			
			if (monitor.isCanceled())
				break;
		
			// Remove product from manifest
			getInstallManifest().removeProduct(product);
			// Remove product from install registry
			getInstallRegistry().removeProduct(product.getId());

			// Remove product directory
			removeProductDirectory(product, progress.newChild(PRODUCT_PROGRESS));
		}
		
		if (!mode.isRootUninstall()) {
			// Update manifest
			getInstallManifest().save();
		}
	}

	@Override
	public void launch(LaunchItem item) throws CoreException {
		IPath installLocation = getInstallDescription().getRootLocation();

		try {
			String program;
			// Executable item
			if (item.getType() == LaunchItemType.EXECUTABLE) {
				IPath toRun = installLocation.append(item.getPath());
				if (!toRun.toFile().exists())
					Installer.fail(InstallMessages.Error_FileNotFound + toRun.toOSString());
				
				// Add paths to environment and launch.
				ProcessBuilder pb = new ProcessBuilder();
				String[] paths = installDescription.getEnvironmentPaths();
				
				if (paths != null) {
					Map <String, String> env = pb.environment();
					String pathKey = "PATH";
					String pathVar = env.get(pathKey);
					
					if (pathVar == null) {
						pathVar = "";
					}
					
					for (String path : paths) {
						IPath resolvedPath = installDescription.getRootLocation().append(path);
						pathVar = resolvedPath.toString() + File.pathSeparatorChar + pathVar;
					}
					env.put(pathKey, pathVar);
				}
				
				program = toRun.toOSString();
				pb.command(program);
				pb.start();
			}
			// File item
			else if (item.getType() == LaunchItemType.FILE) {
				IPath toRun = installLocation.append(item.getPath());
				if (!toRun.toFile().exists())
					Installer.fail(InstallMessages.Error_FileNotFound + toRun.toOSString());
				
				program = "file://" + toRun.toOSString();
				Program.launch(program);
			}
			// HTML item
			else if (item.getType() == LaunchItemType.HTML){
				program = item.getPath();
				Program.launch(program);
			}
			else {
				throw new NullPointerException(InstallMessages.Error_NoLaunchItemType);
			}
		} 
		catch (Exception e) {
			Installer.fail(NLS.bind(InstallMessages.Error_LaunchFailed0, item.getPath()), e);
		}
		// SWT Program.launch() can throw an UnsatisfiedLinkError if it is
		// unable to launch the file.
		catch (UnsatisfiedLinkError e) {
			Installer.fail(NLS.bind(InstallMessages.Error_LaunchFailed0, item.getPath()), e);
		}
	}

	/**
	 * Returns the wizard pages from all install modules.  This method ensures
	 * that wizard pages with the same name are not returned.
	 * 
	 * @return Wizard pages
	 */
	protected IInstallWizardPage[] getModulePages() {
		// Filter duplicated named pages, maintain order
		LinkedHashMap<String, IInstallWizardPage> pages = new LinkedHashMap<String, IInstallWizardPage>();
		for (IInstallModule module : getModules()) {
			IInstallWizardPage[] modulePages = module.getInstallPages(getInstallMode());
			if (modulePages != null) {
				for (IInstallWizardPage modulePage : modulePages) {
					pages.put(modulePage.getName(), modulePage);
				}
			}
		}
		return pages.values().toArray(new IInstallWizardPage[pages.size()]);
	}
	
	@Override
	public IInstallWizardPage[] getWizardPages() {
		if (getInstallDescription() == null)
			return new IInstallWizardPage[0];
		
		ArrayList<IInstallWizardPage> pages = new ArrayList<IInstallWizardPage>();
		
		// Wizard page order
		String[] wizardPageNames = getInstallDescription().getWizardPagesOrder();
		// First pages to insert
		IInstallWizardPage[] firstPages = wizardPageNames != null ? 
				new IInstallWizardPage[wizardPageNames.length] : new IInstallWizardPage[0];
		// Remaining pages to insert
		ArrayList<IInstallWizardPage> remainingPages = new ArrayList<IInstallWizardPage>();

		// Page titles
		InstallPageTitle[] pageTitles = getInstallDescription().getPageTitles();

		IInstallWizardPage[] modulePages = getModulePages();
		// Loop through pages from all modules
		for (IInstallWizardPage modulePage : modulePages) {
			// Verify page base class
			if (modulePage instanceof InstallWizardPage) {
				InstallWizardPage page = (InstallWizardPage)modulePage;
				
				// Set page title if available
				if (pageTitles != null) {
					for (InstallPageTitle pageTitle : pageTitles) {
						if (pageTitle.getPageName().equals(modulePage.getName())) {
							page.setPageLabel(pageTitle.getPageTitle());
							break;
						}
					}
				}

				// Check if the page is found in the order
				int pos = -1;
				if (wizardPageNames != null) {
					for (int index = 0; index < wizardPageNames.length; index ++) {
						String modulePageName = modulePage.getName();
						if ((modulePageName != null) && modulePageName.equals(wizardPageNames[index])) {
							pos = index;
							break;
						}
					}
				}
				// First page
				if (pos != -1) {
					firstPages[pos] = modulePage;
				}
				// Remaining page
				else {
					remainingPages.add(modulePage);
				}
			}
			else {
				Installer.log(modulePage.getName() + " does not extend InstallWizardPage.");
			}
		}
		// Add first pages
		for (IInstallWizardPage page : firstPages) {
			if (page != null) {
				pages.add(page);
			}
		}
		// Add remaining pages
		for (IInstallWizardPage page : remainingPages) {
			pages.add(page);
		}
		
		return pages.toArray(new IInstallWizardPage[pages.size()]);
	}

	/**
	 * Returns all wizard pages that are currently supported.
	 * 
	 * @return Supported wizard pages
	 */
	public IInstallWizardPage[] getSupportedWizardPages() {
		ArrayList<IInstallWizardPage> supportedPages = new ArrayList<IInstallWizardPage>();
		IWizardPage[] pages = getWizardPages();
		for (IWizardPage page : pages) {
			if (page instanceof IInstallWizardPage) {
				IInstallWizardPage wizardPage = (IInstallWizardPage)page;
				if (wizardPage.isSupported()) {
					supportedPages.add(wizardPage);
				}
			}
		}
		
		return supportedPages.toArray(new IInstallWizardPage[supportedPages.size()]);
	}
	
	/**
	 * Loads the install manifest from the
	 * install location if available.
	 */
	private void loadManifest() {
		try {
			if (getInstallDescription() != null) {
				InstallManifest existingManifest = InstallManifest.loadManifest(getInstallDescription());
				if (existingManifest != null) {
					setInstallManifest(existingManifest);
					
					if (installMode != null) {
						// If patch then setup update mode
						if (installMode.isPatch()) {
							installMode.setUpdate();
						}
						// Else if install then set update or upgrade based
						// on the existing version
						else {
							// Check for existing version of product
							IInstallProduct existingProduct = existingManifest.getProduct(getInstallDescription().getProductId());
							if (existingProduct != null) {
								// Update same version
								if (existingProduct.getVersionString().equals(getInstallDescription().getProductVersionString())) {
									installMode.setUpdate();
								}
								// Upgrade different version
								else {
									installMode.setUpgrade();
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	/**
	 * Returns if an action is supported on the platform.
	 * 
	 * @param action Action
	 * @return <code>true</code> if the action is supported
	 */
	private boolean isActionSupported(IInstallAction action) {
		return action.isSupported(Platform.getOS(), Platform.getOSArch());
	}
	
	/**
	 * Schedules the product directory to be removed on shutdown.
	 * If all products are removed, the root installation directory is
	 * also removed.
	 * 
	 * @param product Product
	 * @param monitor Progress monitor
	 * @throws CoreException on failure to remove product directory
	 */
	private void removeProductDirectory(IInstallProduct product, IProgressMonitor monitor) throws CoreException {
		if (getInstallManifest().getProducts().length == 0) {
			getLocationsManager().deleteProductLocation(product, monitor);
		}
	}

	/**
	 * Returns the registered install modules that are specified in the
	 * install description.
	 * 
	 * @return Install modules
	 */
	private IInstallModule[] getModules() {
		if (modules == null) {
			String[] ids = installDescription.getModuleIDs();
			List<String> idList = (ids != null && ids.length > 0) ? Arrays.asList(ids) : null;
			modules = ContributorRegistry.getDefault().getModules(idList);
			// Initialize modules
			for (IInstallModule module : modules) {
				module.init(getInstallDescription());
			}
		}
		
		return modules;
	}
	
	/**
	 * Returns the collection of install actions sorted according to their
	 * install phase, pre-install actions first, followed by install actions,
	 * followed by post-install actions.
	 * 
	 * @param installData Install data
	 * @return Sorted install actions
	 */
	private IInstallAction[] getInstallActions(IInstallData installData) {
		List <IInstallAction> actions = new ArrayList<IInstallAction>();
		for (IInstallModule module : getModules()) {
			IInstallAction[] moduleActions = module.getInstallActions(RepositoryManager.getDefault().getAgent(), installData, getInstallMode());
			if (moduleActions != null) {
				actions.addAll(Arrays.asList(moduleActions));
			}
		}
		
		// Sort the actions according to their install phase attributes.
		Collections.sort(actions, new Comparator<IInstallAction>() {
			@Override
			public int compare(IInstallAction o1, IInstallAction o2) {
				return o1.getInstallPhase().ordinal() - o2.getInstallPhase().ordinal();
			}
		});
		
		return actions.toArray(new IInstallAction[actions.size()]);
	}
	
	/**
	 * Copies the installer to a location.
	 * 
	 * @param location Destination location
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	private void copyInstaller(IPath destinationLocation, IProgressMonitor monitor) throws CoreException {
		try {
			File uninstallDirectory = destinationLocation.toFile();
			if (!uninstallDirectory.exists()) {
				uninstallDirectory.mkdirs();
			}
			
			String[] uninstallFiles = getInstallDescription().getUninstallFiles();
			if (uninstallFiles != null) {
				for (String uninstallFile : uninstallFiles) {
					String destinationFileName = uninstallFile;
					String srcFileName = uninstallFile;
					
					// Parse file name for ":" if renaming of destination file is desired.
					if (uninstallFile.contains(":")) {
						srcFileName = uninstallFile.substring(0, uninstallFile.indexOf(":"));
						destinationFileName = uninstallFile.substring(uninstallFile.indexOf(":") + 1);
					}
					
					IPath destPath = destinationLocation.append(destinationFileName);
					File srcFile = Installer.getDefault().getInstallFile(srcFileName);
					if (srcFile.exists()) {
						File destFile = destPath.toFile();
						
						if (srcFile.isDirectory()) {
							FileUtils.copyDirectory(srcFile, destFile);
						}
						else {
							FileUtils.copyFile(srcFile, destFile);
						}
						
						// Set permissions
						destFile.setExecutable(srcFile.canExecute(), false);
						destFile.setReadable(srcFile.canRead(), false);
						destFile.setWritable(srcFile.canWrite(), false);
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log("Failed to copy installer.  This could be because you are running from the Eclipse workbench and the exported RCP binary files are not available.");
		}
	}

	@Override
	public void setInstalledProduct(IInstalledProduct product) throws CoreException {
		this.installedProduct = product;
		if (product != null) {
			setInstallLocation(product.getInstallLocation());
		}
	}
	
	@Override
	public IInstalledProduct getInstalledProduct() {
		return installedProduct;
	}
	
	@Override
	public IInstalledProduct getInstalledProduct(String id) {
		return getInstallRegistry().getProduct(id);
	}
	
	@Override
	public IInstalledProduct[] getInstalledProducts() {
		return getInstallRegistry().getProducts();
	}

	@Override
	public IInstalledProduct[] getInstalledProducts(IProductRange[] ranges, boolean uniqueLocations) {
		ArrayList<IInstalledProduct> products = new ArrayList<IInstalledProduct>();
		HashMap<IPath, IInstalledProduct> locations = new HashMap<IPath, IInstalledProduct>();
		
		for (IProductRange range : ranges) {
			IInstalledProduct product = getInstallRegistry().getProduct(range.getId());
			if (product != null) {
				// Any product version or product version is in range
				if ((range.getVersionRange() == null) || range.getVersionRange().isIncluded(product.getVersion())) {
					if (uniqueLocations)
						locations.put(product.getInstallLocation(), product);
					else
						products.add(product);
				}
			}
			
		}
		
		if (uniqueLocations) {
			Collection<IInstalledProduct> values = locations.values();
			return values.toArray(new IInstalledProduct[values.size()]);
		}
		else {
			return products.toArray(new IInstalledProduct[products.size()]);
		}
	}

	@Override
	public IInstallProduct getExistingProduct(IInstallManifest manifest) {
		IInstallProduct product = null;
		
		if (manifest != null) {
			// If patch then find existing product that matches
			if (getInstallMode().isPatch()) {
				// If an installed product has been set, look up its product
				// in the manifest
				if (getInstalledProduct() != null) {
					product = getInstallManifest().getProduct(getInstalledProduct().getId());
				}
				// Else find a product match in the manifest
				else {
					IProductRange[] ranges = getInstallDescription().getRequires();
					IInstallProduct[] existingProducts = null;
					// If any product is applicable
					if (ranges == null) {
						existingProducts = getInstallManifest().getProducts();
					}
					// Find products in range
					else {
						existingProducts = getInstallManifest().getProducts(ranges);
					}
					// Use first product that matches
					if ((existingProducts != null) && (existingProducts.length > 0)) {
						product = existingProducts[0];
					}
				}
			}
			// Else get existing product in manifest for product being installed
			else {
				product = getInstallManifest().getProduct(getInstallDescription().getProductId());
			}
		}
		
		return product;
	}
}
