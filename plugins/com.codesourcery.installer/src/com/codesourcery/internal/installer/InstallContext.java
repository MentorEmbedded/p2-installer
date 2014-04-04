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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallModule;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.LaunchItem.LaunchItemType;
import com.codesourcery.installer.ui.InstallWizardPage;

/**
 * Provides the install context.
 */
public class InstallContext implements IInstallContext {
	/** Cleanup progress work */
	private static int CLEANUP_PROGRESS = 10;
	/** Uninstall setup progress work */
	private static int UNINSTALL_SETUP_PROGRESS = 10;
	/** Product progress work */
	private static int PRODUCT_PROGRESS = 100;
	
	/**
	 * Install description
	 */
	private IInstallDescription installDescription;
	/**
	 * Install manifest
	 */
	private InstallManifest installManifest;
	/**
	 * Install modules
	 */
	IInstallModule[] modules;
	
	/**
	 * Constructor for installation context.
	 * 
	 * @param installDescription Install description
	 */
	public InstallContext(IInstallDescription installDescription) {
		this.installDescription = installDescription;
		
		initializeManifest();
	}
	
	/**
	 * Initializes the install manifest, loading from
	 * install location if available.
	 */
	private void initializeManifest() {
		try {
			installManifest = new InstallManifest();
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	/**
	 * Constructor for uninstallation context.
	 * 
	 * @param installManifest Install manifest
	 */
	public InstallContext(InstallManifest installManifest) {
		this.installManifest = installManifest;
	}

	@Override
	public boolean isInstall() {
		return (getInstallDescription() != null);
	}

	@Override
	public boolean isUpgrade() {
		boolean upgrade = false;
		
		if (getInstallDescription() != null) {
			// Check for existing version of product
			IInstallProduct[] existingProducts = getInstallManifest().getProducts();
			for (IInstallProduct existingProduct : existingProducts) {
				// Product is already installed
				if (existingProduct.getId().equals(getInstallDescription().getProductId())) {
					upgrade = true;
					break;
				}
			}
		}
		
		return upgrade;
	}

	/* (non-Javadoc)
	 * @see com.codesourcery.internal.installer.IInstallContext#getDescription()
	 */
	public IInstallDescription getInstallDescription() {
		return installDescription;
	}
	
	/* (non-Javadoc)
	 * @see com.codesourcery.internal.installer.IInstallContext#getManifest()
	 */
	public InstallManifest getInstallManifest() {
		return installManifest;
	}

	/**
	 * Loads the install manifest from the
	 * install location if available.
	 */
	private void loadManifest() {
		try {
			InstallManifest existingManifest = InstallManifest.loadManifest(getInstallDescription());
			if (existingManifest != null) {
				installManifest = existingManifest;
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
	
	@Override
	public void uninstall(IInstallProduct[] products, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, 
				products.length * PRODUCT_PROGRESS + products.length * PRODUCT_PROGRESS);

		InstallMode mode = new InstallMode(false);
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

			// Remove product directory
			removeProductDirectory(product, progress.newChild(PRODUCT_PROGRESS));
		}
		
		if (!mode.isRootUninstall()) {
			// Update manifest
			getInstallManifest().save();
		}
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
			LocationsManager.getDefault().deleteProductLocation(product, monitor);
		}
	}
	
	@Override
	public void install(IInstallData installData, IProgressMonitor monitor) throws CoreException {
		InstallMode mode = new InstallMode(true);
		
		// Set install folder
		String folder = (String)installData.getProperty(IInstallConstants.PROPERTY_INSTALL_FOLDER);
		if (folder != null) {
			IPath installPath = new Path(folder);
			getInstallDescription().setRootLocation(installPath);
		}

		// Load existing manifest if available
		loadManifest();
		
		// Check for existing version of product
		IInstallProduct upgradeProduct = null;
		if (getInstallManifest() != null) {
			upgradeProduct = getInstallManifest().getProduct(getInstallDescription().getProductId());
			if (upgradeProduct != null) {
				mode.setUpgrade(true);
				
				// Same version of the product is already installed, abort
				if (upgradeProduct.getVersion().equals(getInstallDescription().getProductVersion())) {
					String message = MessageFormat.format(InstallMessages.Error_AlreadyInstalled1, new Object[] {
							getInstallDescription().getProductName(),
							getInstallDescription().getProductVersion()
					});
					Installer.fail(message);
				}
			}
		}

		// Get install actions
		List<IInstallAction> actions = getInstallActions(installData, upgradeProduct);
		
		// Uninstall actions of previous product
		if (upgradeProduct != null) {
			for (IInstallAction action : upgradeProduct.getActions()) {
				if (action.uninstallOnUpgrade()) {
					if (isActionSupported(action)) {
						action.run(RepositoryManager.getDefault().getAgent(), 
								upgradeProduct, new InstallMode(false), new NullProgressMonitor());
					}
				}
				
				if (monitor.isCanceled())
					break;
			}
			// Remove product from manifest
			getInstallManifest().removeProduct(upgradeProduct);
		}

		// Install product
		InstallProduct product = new InstallProduct(
				getInstallDescription().getProductId(),
				getInstallDescription().getProductName(),
				getInstallDescription().getProductVersion(),
				getInstallDescription().getRootLocation(),
				getInstallDescription().getInstallLocation(),
				actions.toArray(new IInstallAction[actions.size()]));
		
		// Create product directories
		//LocationsManager.getDefault().createProductLocation(product);
		
		int totalActionWork = 0;
		for (IInstallAction action : product.getActions()) {
			totalActionWork += action.getProgressWeight();
		}
		
		SubMonitor progress = SubMonitor.convert(monitor, 
				totalActionWork + CLEANUP_PROGRESS + UNINSTALL_SETUP_PROGRESS);
				
		// Install
		int index;
		IInstallAction[] actionsToPerform = product.getActions();
		for (index = 0; index < actionsToPerform.length; index ++) {
			IInstallAction action = actionsToPerform[index];
			if (isActionSupported(action)) {
				action.run(RepositoryManager.getDefault().getAgent(), 
						product, mode, progress.newChild(action.getProgressWeight()));
			}
			if (monitor.isCanceled())
				break;
		}

		// Installation cancelled - clean up
		if (monitor.isCanceled()) {
			progress.setTaskName(InstallMessages.CleanupInstallation);
			
			if (getInstallManifest().getProducts().length == 0) {
				mode.setRootUninstall(true);
			}
			
			// Uninstall performed actions
			for (int rollbackIndex = 0; rollbackIndex <= index; rollbackIndex ++) {
				if (isActionSupported(actionsToPerform[rollbackIndex])) {
					actionsToPerform[rollbackIndex].run(RepositoryManager.getDefault().getAgent(), 
							product, mode, new NullProgressMonitor());
				}
			}
			
			// Remove product directory
			SubMonitor removeProgress = SubMonitor.convert(monitor, CLEANUP_PROGRESS);
			removeProductDirectory(product, removeProgress);
		}
		// Installation completed
		else {
			progress.worked(CLEANUP_PROGRESS);
			
			// Update install manifest
			getInstallManifest().addProduct(product);
	
			// Setup uninstall
			IPath uninstallLocation = getInstallDescription().getRootLocation().append(IInstallConstants.UNINSTALL_DIRECTORY);
			String[] uninstallFiles = getInstallDescription().getUninstallFiles();
			if (uninstallFiles != null) {
				// Update manifest
				IPath manifestPath = uninstallLocation.append(IInstallConstants.INSTALL_MANIFEST_FILENAME);
				
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
				// Save manifest
				getInstallManifest().save(manifestPath.toFile());
			}
		}
		
		progress.worked(UNINSTALL_SETUP_PROGRESS);
	}
	
	/**
	 * Returns the registered install modules that are specified in the
	 * install description.
	 * 
	 * @return Install modules
	 */
	protected IInstallModule[] getModules() {
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

	@Override
	public IInstallWizardPage[] getWizardPages() {
		ArrayList<IInstallWizardPage> pages = new ArrayList<IInstallWizardPage>();
		
		// Wizard page order
		String[] wizardPageNames = getInstallDescription().getWizardPagesOrder();
		// First pages to insert
		IInstallWizardPage[] firstPages = wizardPageNames != null ? 
				new IInstallWizardPage[wizardPageNames.length] : new IInstallWizardPage[0];
		// Remaining pages to insert
		ArrayList<IInstallWizardPage> remainingPages = new ArrayList<IInstallWizardPage>();
		// Loop through pages from all modules
		for (IInstallModule module : getModules()) {
			IInstallWizardPage[] modulePages = module.getInstallPages();
			// Module contributes pages
			if (modulePages != null) {
				for (IInstallWizardPage modulePage : modulePages) {
					// Verify page base class
					if (modulePage instanceof InstallWizardPage) {
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
	 * Returns the collection of install actions sorted according to their
	 * install phase, pre-install actions first, followed by install actions,
	 * followed by post-install actions.
	 * 
	 * @param installData Install data
	 * @param upgradeProduct Install product
	 * @return Sorted collection of install actions
	 */
	protected List<IInstallAction> getInstallActions(IInstallData installData, IInstallProduct upgradeProduct) {
		List <IInstallAction> actions = new ArrayList<IInstallAction>();
		for (IInstallModule module : getModules()) {
			IInstallAction[] moduleActions = module.getInstallActions(RepositoryManager.getDefault().getAgent(), installData, upgradeProduct);
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
		
		return actions;
	}
	
	/**
	 * Copies the installer to a location.
	 * 
	 * @param location Destination location
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	public void copyInstaller(IPath destinationLocation, IProgressMonitor monitor) throws CoreException {
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
		catch (Exception e) {
			Installer.log(e);
		}
	}

	/**
	 * Launches an item.
	 * 
	 * @param item Item to launch
	 */
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
}
