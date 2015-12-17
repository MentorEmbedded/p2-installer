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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallComponent;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallManager;
import com.codesourcery.installer.IInstallManifest;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallModule;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallVerifier;
import com.codesourcery.installer.IInstallWizardPage;
import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.IProductRange;
import com.codesourcery.installer.InstallPageTitle;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LaunchItem;
import com.codesourcery.installer.LaunchItem.LaunchItemType;
import com.codesourcery.installer.UninstallMode;
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
	/** Mirror information filename */
	private static final String MIRROR_INFO_FILENAME = "mirror.info";
	/** Installed product to update with installation */
	private IInstalledProduct installedProduct;
	/** Cached wizard pages */
	private IInstallWizardPage[] wizardPages;
	/** Install verifiers*/
	private ListenerList installVerifiers = new ListenerList();
	/** Install data */
	private IInstallData installData;
	/** Start time */
	private Date startTime;
	
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
	/** Restart or re-login required or not. **/
	private boolean needsResetOrRelogin = false;

	/**
	 * Constructor
	 */
	public InstallManager() {
		// Create default new install manifest
		installManifest = new InstallManifest();
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
		
		installData = new InstallData();
	}
	
	/**
	 * Disposes of the install manager.
	 */
	public void dispose() {
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

		// Load install modules
		loadModules();
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
	 * Returns the install registry.
	 * 
	 * @return Install registry
	 */
	private InstallRegistry getInstallRegistry() {
		return installRegistry;
	}
	
	/**
	 * Verifies install components.
	 * 
	 * @return Error message or <code>null</code>
	 * @throws CoreException on error
	 */
	protected void verifyInstallComponents() throws CoreException {
		IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents(false);
		IStatus[] status = verifyInstallComponents(components);
		for (IStatus s : status) {
			if (s.getSeverity() == IStatus.ERROR) {
				throw new CoreException(new Status(IStatus.ERROR, Installer.ID, s.getMessage()));
			}
		}
	}

	@Override
	public void setInstallLocation(IPath path, IProgressMonitor monitor) throws CoreException {
		// Mirror has already been set
		if (getInstallMode().isMirror())
			return;
		
		if (monitor == null)
			monitor = new NullProgressMonitor();
		
		getInstallDescription().setRootLocation(path);
		
		// Load existing manifest if available
		loadManifest(path);

		// Location changed
		if ((path == null) || !path.equals(installLocation)) {
			if (installLocation != null) {
				// Stop the P2 agent
				RepositoryManager.getDefault().stopAgent();
				// Remove the old install location.  This will include any artifacts created by the P2 agent.
				removeInstallLocation();
			}
			this.installLocation = path;
		
			// Create new location
			if (installLocation != null) {
				if (getInstallMode().isInstall() && !getInstallMode().isUpdate() && !getInstallMode().isUpgrade()) {
					createInstallLocation(installLocation);
				}
				
				// Create the P2 agent.
				try {
					createAgent(getInstallDescription().getInstallLocation(), monitor);
				}
				catch (Exception e) {
					removeInstallLocation();

					this.installLocation = null;
					throw e;
				}
			}
		}
		
		if (installLocation != null) {
			// Verify install
			verifyInstall(RepositoryManager.getDefault().getAgent(), RepositoryManager.getDefault().getInstallProfile());
			// Verify install components
			verifyInstallComponents();
		}
	}

	@Override
	public void setMirrorLocation(IPath path, IProgressMonitor monitor)
			throws CoreException {
		// Install location has already been set
		if (getInstallLocation() != null)
			return;
		
		if (monitor == null)
			monitor = new NullProgressMonitor();

		// Set mirror install mode
		installMode.setMirror();

		// Set repository manager to create mirror of install
		RepositoryManager.getDefault().setCacheLocation(path);
		RepositoryManager.getDefault().setUpdateCache(true);
		RepositoryManager.getDefault().setCacheOnly(false);
		
		// Create default agent
		createAgent(null, monitor);
		
		verifyInstallComponents();
	}
	
	@Override
	public void setSourceLocation(IPath path) throws CoreException {
		MirrorDescription desc = new MirrorDescription();
		IPath mirrorInfo = path.append(MIRROR_INFO_FILENAME);
		File mirrorInfoFile = mirrorInfo.toFile();
		if (!mirrorInfoFile.exists()) {
			Installer.fail(InstallMessages.Error_WrongMirror);
		}
		desc.load(mirrorInfoFile);
		
		// Check that the mirror was made with this installer
		if (!getInstallDescription().getProductId().equals(desc.getProductId()) || 
			!getInstallDescription().getProductVersionString().equals(desc.getProductVersion())) {
			Installer.fail(InstallMessages.Error_WrongMirror);
		}
		
		// Set repository manager to install from mirror only
		RepositoryManager.getDefault().setCacheLocation(path);
		RepositoryManager.getDefault().setUpdateCache(false);
		RepositoryManager.getDefault().setCacheOnly(true);
	}

	/**
	 * Creates the directories for an install location and records the levels of directories created in the install
	 * manifest.
	 * 
	 * @param path Path to install location
	 * @throws CoreException on failure to create install location
	 */
	public void createInstallLocation(IPath path) throws CoreException {
		String[] segments = path.segments();
		IPath location = new Path(path.getDevice(), "/");

		ArrayList<String> createdDirectories = new ArrayList<String>();
		
		for (String segment : segments) {
			location = location.append(segment);
		
			File directory = location.toFile();
			// If directory does not exist then create it and increment the level
			// of directories that should be removed on uninstall.
			if (!directory.exists()) {
				directory.mkdir();
				if (!directory.exists()) {
					Installer.fail(InstallMessages.NoWritePermissions);
				}
				else {
					createdDirectories.add(directory.getName());
				}
			}
		}
		// Record the directories created
		String[] directories = createdDirectories.toArray(new String[createdDirectories.size()]);
		((InstallManifest)installManifest).setDirectories(directories);
	}

	/**
	 * Removes the directories for an install location.
	 * 
	 * @throws CoreException on failure to remove install location
	 */
	public void removeInstallLocation() throws CoreException {
		// Remove created install artifacts if not an update or upgrade of existing installation
		boolean removeInstallLocation = !getInstallMode().isUpdate() && !getInstallMode().isUpgrade();
		// Or if there are other products installed at location
		if ((getInstallManifest() != null) && (getInstallManifest().getProducts().length != 0)) {
			removeInstallLocation = false;
		}
		if (!removeInstallLocation) {
			return;
		}

		// Get install location to remove
		IPath path = getInstallLocation();
		if (path == null) {
			return;
		}

		String[] directories = (getInstallManifest() != null) ? ((InstallManifest)getInstallManifest()).getDirectories() : null; 

		try {
			// Remove directories
			if (directories != null) {
				// Delete created directories if they are empty
				for (int index = directories.length - 1; index >= 0; index --) {
					// Make sure parent directory name matches
					if (!path.lastSegment().equals(directories[index])) {
						break;
					}
					File dir = path.toFile();
					// Except for the first directory, delete only if it is empty
					if ((index != directories.length - 1) && dir.listFiles().length != 0)
						break;
					FileUtils.deleteDirectory(dir.toPath());
					path = path.removeLastSegments(1);
				}
			}
			// Otherwise remove files in directory
			else {
				File[] files = path.toFile().listFiles();
				for (File file : files) {
					if (file.isDirectory())
						FileUtils.deleteDirectory(file.toPath());
					else
						file.delete();
				}
			}
		}
		catch (Exception e) {
			Installer.fail("Failed to remove install location.", e);
		}
	}

	/**
	 * Creates and initializes the provisioning agent.
	 * 
	 * @param installLocation Install location
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	private void createAgent(IPath installLocation, IProgressMonitor monitor) throws CoreException {
		// Create new P2 agent
		// Note, if this is not an existing installation, it will result 
		// in agent files being created in the location.
		IProvisioningAgent agent = RepositoryManager.getDefault().createAgent(installLocation, monitor);

		// Let install modules perform any agent initialization
		if (agent != null) {
			for (IInstallModule module : getModules()) {
				try {
					module.initAgent(agent);
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
		}
		
		// Load repositories
		try {
			RepositoryManager.getDefault().loadInstallRepositories(monitor);
		}
		catch (Exception e) {
			RepositoryManager.getDefault().stopAgent();
			throw e;
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
	
	/**
	 * Logs a time stamp.
	 * 
	 * @param message Message for time stamp.
	 * @param date Time stamp
	 */
	private void logTimeStamp(String message, Date date) {
		try {
			final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Installer.log(message + ": " + format.format(date));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Logs start time.
	 * 
	 * @param message Message for time stamp.
	 */
	private void logStartTime(String message) {
		try {
			startTime = Calendar.getInstance().getTime();
			logTimeStamp(message, startTime);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Logs end time.
	 * 
	 * @param message Message for time stamp.
	 */
	private void logEndTime(String message) {
		try {
			Date endTime = Calendar.getInstance().getTime();
			logTimeStamp(message, endTime);
			
			long elapsed = endTime.getTime() - startTime.getTime();
			long secondsInMilliseconds = 1000;
			long minutesInMilliseconds = secondsInMilliseconds * 60;
			long hoursInMilliseconds = minutesInMilliseconds * 60;
			long daysInMilliseconds = hoursInMilliseconds * 24;
			
			long elapsedDays = elapsed / daysInMilliseconds;
			elapsed = elapsed % daysInMilliseconds;
			long elapsedHours = elapsed / hoursInMilliseconds;
			elapsed = elapsed % hoursInMilliseconds;
			long elapsedMinutes = elapsed / minutesInMilliseconds;
			elapsed = elapsed % minutesInMilliseconds;
			long elapsedSeconds = elapsed / secondsInMilliseconds;
			
			Installer.log("Total Time: " + 
					((elapsedDays > 0) ? Long.toString(elapsedDays) + " days " : "") +
					((elapsedHours > 0) ? Long.toString(elapsedHours) + " hours " : "") +
					((elapsedMinutes > 0) ? Long.toString(elapsedMinutes) + " minutes " : "") +
					((elapsedSeconds > 0) ? Long.toString(elapsedSeconds) + " seconds " : "")
					);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void install(IProgressMonitor monitor) throws CoreException {
		logStartTime("Install Started");

		if (Installer.getDefault().getInstallManager().getInstallMode().isMirror()) {
			doMirror(monitor);
		}
		else {
			doInstall(monitor);
		}
		
		logEndTime("Install Completed");
	}

	/**
	 * Performs a mirror operation.
	 * 
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	protected void doMirror(IProgressMonitor monitor) throws CoreException {
		if (monitor.isCanceled())
			return;

		// Units to add
		ArrayList<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();

		// Get the install plan
		RepositoryManager.getDefault().getAllInstallUnits(toAdd);
		// Peform the provision
		RepositoryManager.getDefault().provision(null, toAdd, null, false, monitor);
		
		// Save mirror information
		MirrorDescription desc = new MirrorDescription();
		desc.setProductId(getInstallDescription().getProductId());
		desc.setProductVersion(getInstallDescription().getProductVersionString());
		IPath infoPath = RepositoryManager.getDefault().getCacheLocation().append(MIRROR_INFO_FILENAME);
		desc.save(infoPath.toFile());
	}
	
	/**
	 * Performs an install operation.
	 * 
	 * @param monitor Progress monitor
	 * @throws CoreException on failure
	 */
	protected void doInstall(IProgressMonitor monitor) throws CoreException {
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
							if (action.needsRestartOrRelogin())
								needsResetOrRelogin = true;
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
					getInstallDescription().getProductUninstallName(),
					getInstallDescription().getRootLocation(),
					getInstallDescription().getInstallLocation());
			
			// Save if installation directories should be removed for the product
			UninstallMode uninstallMode = getInstallDescription().getUninstallMode();
			product.setProperty(IInstallProduct.PROPERTY_REMOVE_DIRS, 
					uninstallMode != null ? 
							Boolean.toString(getInstallDescription().getUninstallMode().getRemoveDirectories()) : 
							Boolean.FALSE.toString());
			// Save if product should be shown in uninstaller
			product.setProperty(IInstallProduct.PROPERTY_SHOW_UNINSTALL, 
					uninstallMode != null ? 
							Boolean.toString(getInstallDescription().getUninstallMode().getShowUninstall()) : 
							Boolean.FALSE.toString());
			// Save product additional uninstallation text (if any)
			String productUninstallText = getInstallDescription().getText(IInstallDescription.TEXT_UNINSTALL_ADDENDUM, null);
			if (productUninstallText != null) {
				product.setProperty(IInstallProduct.PROPERTY_UNINSTALL_TEXT, productUninstallText);
			}
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
				
				// Set reset or relogin if it is required for action.
				if (action.needsRestartOrRelogin())
					needsResetOrRelogin = true;
				
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
			
			// If not update, roll back performed actions.  For an update, the P2 provisioning operation would have been
			// cancelled and IU's rolled back.
			if (!getInstallMode().isUpdate()) {
				InstallMode rollbackMode = new InstallMode(mode);
				rollbackMode.setInstall(false);
				
				// Uninstall performed actions
				for (int rollbackIndex = 0; rollbackIndex <= index; rollbackIndex ++) {
					if (isActionSupported(actions[rollbackIndex])) {
						actions[rollbackIndex].run(RepositoryManager.getDefault().getAgent(), 
								product, rollbackMode, new NullProgressMonitor());
						
						// Set reset or relogin if it is required for action.
						if (actions[rollbackIndex].needsRestartOrRelogin())
							needsResetOrRelogin = true;
					}
				}
			}
			
			// Remove install location
			setInstallLocation(null, null);
		}
		// Installation completed
		else {
			monitor.worked(CLEANUP_PROGRESS);
			
			// Update install manifest
			getInstallManifest().addProduct(product);
			// Update install registry
			if (getInstallDescription().getUseInstallRegistry()) {
				getInstallRegistry().addProduct(
						new InstalledProduct(
								product.getId(), 
								product.getName(), 
								product.getVersionString(), 
								product.getLocation(), 
								getInstallDescription().getProductCategory()
								)
						);
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
								FileUtils.deleteDirectory(uninstallLocation.toFile().toPath());
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
			if (getInstallDescription().getUninstallMode() != null) {
				getInstallManifest().save(manifestPath.toFile());
			}
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
		
		// Remove the products from the registry. It is important that the products be removed before the actions
		// are run as InstallIUActions checks remaining products.
		for (IInstallProduct product : products) {
			// Remove product from manifest
			getInstallManifest().removeProduct(product);
			// Remove product from install registry
			getInstallRegistry().removeProduct(product.getId());
		}
		
		// Remove products actions
		for (IInstallProduct product : products) {
			RepositoryManager.getDefault().createAgent(product.getInstallLocation(), monitor);
			RepositoryManager.getDefault().loadInstallRepositories(monitor);
			
			if (monitor.isCanceled())
				break;

			// Remove product actions
			int work = PRODUCT_PROGRESS / product.getActions().length;
			for (IInstallAction action : product.getActions()) {
				if (isActionSupported(action)) {
					action.run(RepositoryManager.getDefault().getAgent(), product, mode, progress.newChild(work));
					
					// Set reset or re-login if it is required for any action.
					if (action.needsRestartOrRelogin())
						needsResetOrRelogin = true;
				}
				if (monitor.isCanceled())
					break;
			}
		}
		
		// Update manifest if there are still products installed
		if (getInstallManifest().getProducts().length > 0) {
			getInstallManifest().save();
			monitor.worked(PRODUCT_PROGRESS);
		}
		// Else remove product directory if all products have been uninstalled
		else {
			removeProductLocation(products[0], progress.newChild(PRODUCT_PROGRESS));
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
				
				ProcessBuilder pb = new ProcessBuilder();

				// Set up environment
				Map<String, String> environment = pb.environment();
				IInstallModule[] modules = getModules();
				for (IInstallModule module : modules) {
					try {
						module.setEnvironmentVariables(environment);
					}
					catch (Exception e) {
						Installer.log(e);
					}
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
			//RESTART item
			else if (item.getType() == LaunchItemType.RESTART) {
				if (Installer.isWindows()) {
					restart("shutdown -r");
				}
				else
					Installer.log(NLS.bind(InstallMessages.Error_UnsupportedOS, "Restart", System.getProperty("os.name")));
			}
			//LOGOUT item
			else if (item.getType() == LaunchItemType.LOGOUT) {
				if (Installer.isWindows()) {
					restart("shutdown -l");
				}
				else if (Installer.isLinux()) {
					final String user = System.getProperty("user.name");
					if (user == null) {
						Installer.log("Unable to obtain user.name");
					}
					else {
						restart("pkill -KILL -u " + user.toLowerCase());
					}
				}
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
	 * Runs a restart command at the end of the installation.
	 * 
	 * @param command Command to run
	 */
	private void restart(final String command) {
		ShutdownHandler.getDefault().addOperation(new Runnable() {
			@Override
			public void run() {
				try {
					Runtime.getRuntime().exec(command);
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
		});
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
	
	/**
	 * Returns if a wizard page is excluded.
	 * 
	 * @param wizardPage Wizard page
	 * @return <code>true</code> if wizard page is excluded
	 */
	private boolean isWizardPageExcluded(InstallWizardPage wizardPage) {
		boolean excluded = false;
		String[] excludedPages = getInstallDescription().getWizardPagesExcluded();
		if (excludedPages != null) {
			for (String excludedPage : excludedPages) {
				if (wizardPage.getName().equals(excludedPage)) {
					excluded = true;
					break;
				}
			}
		}
		
		return excluded;
	}
	
	@Override
	public IInstallWizardPage[] getWizardPages() {
		if (getInstallDescription() == null)
			return new IInstallWizardPage[0];
		
		if (wizardPages == null) {
			ArrayList<IInstallWizardPage> pages = new ArrayList<IInstallWizardPage>();
			
			// Wizard page order
			String[] wizardPagesOrder = getInstallDescription().getWizardPagesOrder();
			// First pages to insert
			IInstallWizardPage[] firstPages = wizardPagesOrder != null ? 
					new IInstallWizardPage[wizardPagesOrder.length] : new IInstallWizardPage[0];
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
					
					// Excluded page
					if (isWizardPageExcluded(page))
						continue;
					
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
					if (wizardPagesOrder != null) {
						for (int index = 0; index < wizardPagesOrder.length; index ++) {
							String modulePageName = modulePage.getName();
							if ((modulePageName != null) && modulePageName.equals(wizardPagesOrder[index])) {
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
			
			wizardPages = pages.toArray(new IInstallWizardPage[pages.size()]);
		}
		
		return wizardPages;
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
	 * 
	 * @param installLocation Install location
	 */
	private void loadManifest(IPath installLocation) {
		try {
			if (getInstallDescription() != null) {
				InstallManifest existingManifest = InstallManifest.loadManifest(installLocation);
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
	 * This method removes the created directories for a product installation.  Files in the product directory will be
	 * removed and the product directory will be scheduled for removal after the uninstaller exits.  Any parent
	 * directories created during the installation will be scheduled for removal if they are empty.
	 * Before a directory is scheduled for removal, it is scanned for existing installed products.  If any products are 
	 * found, those products will be adjusted so that they remove the directories instead when they are uninstalled 
	 * (regardless if that product created the directory during its installation).
	 * 
	 * Example:
	 *   Product 1 is installed to /parent/product1 and creates the 'parent' and 'product1' directories.
	 *   Product 2 is then installed to /parent/product2 and only creates the 'product2' directory.
	 *   
	 *   Product 1 records that it should remove two parent directories.  Product 2 records that it should only remove
	 *   one parent directory.
	 *   Uninstalling Product 1 will remove the 'product1' directory then adjust the Product 2 directories to remove
	 *   two parent directories when it is uninstalled.
	 * 
	 * @param product Product
	 * @param monitor Progress monitor
	 * @throws CoreException on failure to remove product directory
	 */
	public void removeProductLocation(IInstallProduct product, IProgressMonitor monitor) throws CoreException {
		if (getInstallManifest().getProducts().length == 0) {
			// If the product installation directories should be removed on uninstall
			String propRemoveDirs = product.getProperty(IInstallProduct.PROPERTY_REMOVE_DIRS);
			boolean removeDirs = ((propRemoveDirs == null) || Boolean.parseBoolean(propRemoveDirs));

			// Products directory
			IPath productPath = getInstallManifest().getInstallLocation();

			// Remove installation directories
			boolean removed = false;
			if (removeDirs) {
				// Created directories to remove
				String[] directories = (installManifest != null) ? ((InstallManifest)installManifest).getDirectories() : new String[0];

				IStatus removeStatus = Status.OK_STATUS;

				// Remove files in the product directory (except for uninstaller)
				removeStatus = deleteProductFiles(productPath, monitor);
				if (removeStatus.isOK()) {
					// Schedule the directory to be removed after the uninstaller has exited
					ShutdownHandler.getDefault().addDirectoryToRemove(productPath.toOSString(), false);
					removed = true;
			
					// Schedule created parent directories to be removed if they are empty
					IPath path = productPath.removeLastSegments(1);
					for (int index = directories.length - 2; index >= 0; index--) {
						String pathName = directories[index];
						// Verify parent directory is correct (installation was not moved)
						if (!path.lastSegment().equals(pathName)) {
							break;
						}
						
						// Find any nested products (skipping this product directory)
						InstallManifest[] nestedProducts = findProducts(path, productPath);
						// If nested products were found, adjust them so that the directories will be removed when
						// they are uninstalled instead and stop removing directories.
						if (nestedProducts.length != 0) {
							try {
								for (InstallManifest nestedProduct : nestedProducts) {
									ArrayList<String> nestedDirectories = new ArrayList<String>();
									// Added remaining parent directories that need to be removed
									for (int i = 0; i <= index; i++) {
										nestedDirectories.add(directories[i]);
									}
									
									// Get product directory (parent of uninstall directory containing manifest)
									IPath nestedProductDirectory = nestedProduct.getInstallLocation();
									String[] nestedSegments = nestedProductDirectory.removeFirstSegments(path.segmentCount()).segments();
									// Add the nested product directories that need to be removed
									for (String nestedSegment : nestedSegments) {
										nestedDirectories.add(nestedSegment);
									}

									// Save the nested product manifest
									nestedProduct.setDirectories(nestedDirectories.toArray(new String[nestedDirectories.size()]));
									nestedProduct.save();
								}
							}
							catch (Exception e) {
								Installer.log(e);
							}
							// Stop removing created parent directories
							break;
						}
						
						// Schedule the directory to be removed if empty
						ShutdownHandler.getDefault().addDirectoryToRemove(path.toOSString(), true);
						path = path.removeLastSegments(1);
					}
				}
				
				if (!removeStatus.isOK()) {
					Installer.fail(removeStatus.getMessage());
				}
			}
			
			// If product directory was not removed then just schedule the uninstall directory to be removed
			if (!removed) {
				IPath uninstallPath = productPath.append(IInstallConstants.UNINSTALL_DIRECTORY);
				if (uninstallPath.toFile().exists()) {
					// Make any read-only files writable so they can be removed
					try {
						FileUtils.setWritable(uninstallPath.toFile().toPath());
					} catch (IOException e) {
						Installer.log(e);
					}
					// Add directory for removal on shutdown
					ShutdownHandler.getDefault().addDirectoryToRemove(uninstallPath.toOSString(), false);
				}
			}
		}
	}

	/**
	 * Finds installed products in a directory.
	 * 
	 * @param directory Directory to search
	 * @param skipDirectory Directory to skip
	 * @return Installed product manifests or empty if no products were found.
	 */
	private InstallManifest[] findProducts(final IPath directory, final IPath skipDirectory) {
		final ArrayList<InstallManifest> products = new ArrayList<InstallManifest>();
		final boolean[] removed = new boolean[] { true };
		final java.nio.file.Path startPath = directory.toFile().toPath();
		final java.nio.file.Path skipPath = (skipDirectory != null) ? skipDirectory.toFile().toPath() : null;
		
		try {
			Files.walkFileTree(startPath, new SimpleFileVisitor<java.nio.file.Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						java.nio.file.Path dir, BasicFileAttributes attrs)
						throws IOException {
					
					// Skip product directory files
					if (skipPath != null) {
						return dir.equals(skipPath) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
					}
					else {
						return FileVisitResult.CONTINUE;
					}
				}
				
				@Override
				public FileVisitResult postVisitDirectory(
						java.nio.file.Path dir, IOException exc)
						throws IOException {

					FileVisitResult result = FileVisitResult.CONTINUE;
					try {
						// Load the other products manifest
						IPath productPath = new Path(dir.toFile().getAbsolutePath());
						InstallManifest manifest = InstallManifest.loadManifest(productPath);
						if (manifest != null) {
							products.add(manifest);
						}
					}
					catch (Exception e) {
						Installer.log(e);
						result = FileVisitResult.TERMINATE;
					}
					return result;
				
				}
			});
		}
		catch (Exception e) {
			Installer.log(e);
		}
		
		// If no nested products were found, schedule the directory to be removed if it is empty
		if (removed[0]) {
			ShutdownHandler.getDefault().addDirectoryToRemove(directory.toOSString(), true);
		}
		
		return products.toArray(new InstallManifest[products.size()]);
	}
	
	/**
	 * Deletes files in a product directory.  The uninstall directory and the
	 * product directory itself will not be removed.
	 * 
	 * @param path Product directory
	 * @param monitor Progress monitor
	 * @return <code>IStatus.OK</code> on success, <code>IStatus.ERROR</code>
	 * on failure and the message will contain files that could not be removed. 
	 */
	private IStatus deleteProductFiles(IPath path, IProgressMonitor monitor) {
		// Collect files to be removed
		final File uninstallDirectory = new File(path.append(IInstallConstants.UNINSTALL_DIRECTORY).toOSString());
		// Remove the files except for the uninstall directory (as it can't be removed on Windows while
		// the uninstaller is running).
		File[] filesNotRemoved = FileUtils.deleteFiles(path.toFile().toPath(), new java.nio.file.Path[] { uninstallDirectory.toPath() }, monitor);
		
		// Some files could not be removed
		if ((filesNotRemoved != null) && (filesNotRemoved.length > 0)) {
			// Report files that could not be removed
			StringBuffer message = new StringBuffer(InstallMessages.Error_FilesNotRemoved);
			int count = 0;
			for (File fileNotRemoved : filesNotRemoved) {
				// Only report up to ten files
				if (++count > 10) {
					message.append('\n');
					message.append(NLS.bind(InstallMessages.More0, filesNotRemoved.length - 10));
					break;
				}
				message.append('\n');
				message.append("  ");
				message.append(fileNotRemoved.getAbsolutePath());
			}
			return new Status(IStatus.ERROR, Installer.ID, 0, message.toString(), null);
		}
		// All files were removed
		else {
			return Status.OK_STATUS;
		}
	}
	
	/**
	 * Loads registered install modules that are specified in the install
	 * description.
	 */
	private void loadModules() {
		if (modules == null) {
			final String[] ids;
			String[] moduleIds = installDescription.getModuleIDs();
			// If no modules specified, use default module
			if (moduleIds == null) {
				ids = new String[] { GeneralInstallModule.ID };
			}
			else {
				ids = moduleIds;
			}
			
			IInstallData installData = getInstallData();
			
			List<String> idList = (ids != null && ids.length > 0) ? Arrays.asList(ids) : null;
			modules = ContributorRegistry.getDefault().getModules(idList);
			// Initialize modules
			for (IInstallModule module : modules) {
				module.init(getInstallDescription());
				module.setDataDefaults(installData);
			}
			
			// Set data defaults from install description
			IInstallDescription description = Installer.getDefault().getInstallManager().getInstallDescription();
			if (description != null) {
				Map<String, String> defaultData = description.getInstallDataDefaults();
				if (defaultData != null) {
					for (Entry<String, String> entry : defaultData.entrySet()) {
						installData.setProperty(entry.getKey(), entry.getValue());
					}
				}
			}
			
			// Sort modules according to order in install description
			Arrays.sort(modules, new Comparator<IInstallModule>() {
				@Override
				public int compare(IInstallModule arg0, IInstallModule arg1) {
					int arg0i = -1;
					int arg1i = -1;
					for (int index = 0; index < ids.length; index ++) {
						if (arg0.getId().equals(ids[index]))
							arg0i = index;
						if (arg1.getId().equals(ids[index]))
							arg1i = index;
					}
					
					if (arg0i < arg1i)
						return -1;
					else if (arg0i > arg1i)
						return 1;
					else
						return 0;
				}
			});
		}
	}

	/**
	 * Returns install modules.
	 * 
	 * @return Install modules
	 */
	private IInstallModule[] getModules() {
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
			// Get module actions
			IInstallAction[] moduleActions = module.getInstallActions(RepositoryManager.getDefault().getAgent(), installData, getInstallMode());
			if (moduleActions != null) {
				// Add actions that are not excluded
				for (IInstallAction moduleAction : moduleActions) {
					boolean addAction = true;
					String[] excludedActions = getInstallDescription().getExcludedActions();
					if (excludedActions != null) {
						for (String excludedAction : excludedActions) {
							if (moduleAction.getId().equals(excludedAction)) {
								addAction = false;
								break;
							}
						}
					}
					if (addAction) {
						actions.add(moduleAction);
					}
				}
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
							FileUtils.copyDirectory(srcFile.toPath(), destFile.toPath(), true);
						}
						else {
							FileUtils.copyFile(srcFile.toPath(), destFile.toPath(),true);
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log("Failed to copy installer.  This could be because you are running from the Eclipse workbench and the exported RCP binary files are not available.");
			Installer.log(e);
		}
	}

	@Override
	public void setInstalledProduct(IInstalledProduct product, IProgressMonitor monitor) throws CoreException {
		this.installedProduct = product;
		if (product != null) {
			setInstallLocation(product.getInstallLocation(), monitor);
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
	public IInstalledProduct[] getInstalledProductsByRange(IProductRange[] ranges, boolean uniqueLocations) {
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
	public IInstalledProduct[] getInstalledProductsByCategory(String category,
			boolean uniqueLocations) {
		ArrayList<IInstalledProduct> products = new ArrayList<IInstalledProduct>();
		HashMap<IPath, IInstalledProduct> locations = new HashMap<IPath, IInstalledProduct>();

		IInstalledProduct[] installedProducts = getInstallRegistry().getProducts();
		for (IInstalledProduct installedProduct : installedProducts) {
			if (category.equals(installedProduct.getCategory())) {
				if (uniqueLocations) {
					if (locations.get(installedProduct.getInstallLocation()) == null) {
						locations.put(installedProduct.getInstallLocation(), installedProduct);
						products.add(installedProduct);
					}
					else {
						products.add(installedProduct);
					}
				}
			}
		}
		
		return products.toArray(new IInstalledProduct[products.size()]);
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
				product = manifest.getProduct(getInstallDescription().getProductId());
			}
		}
		
		return product;
	}
	
	/**
	 * Returns whether restart or re-login required at the end of installation.
	 * 
	 * @return <code>true</code> if restart or re-login is required, Otherwise returns
	 * <code>false</code> 
	 */
	public boolean needsRestartOrRelogin() {
		return needsResetOrRelogin;
	}

	@Override
	public void addInstallVerifier(IInstallVerifier verifier) {
		installVerifiers.add(verifier);
	}

	@Override
	public void removeInstallVerifier(IInstallVerifier verifier) {
		installVerifiers.remove(verifier);
	}

	/**
	 * Verifies an installation folder.
	 * 
	 * @param installLocation Install location
	 * @return Status for the folder
	 */
	public IStatus[] verifyInstallLocation(IPath installLocation) {
		ArrayList<IStatus> status = new ArrayList<IStatus>();
		for (Object listener : installVerifiers.getListeners()) {
			try {
				IStatus verifyStatus = ((IInstallVerifier)listener).verifyInstallLocation(installLocation);
				if ((verifyStatus != null) && !verifyStatus.isOK()) {
					status.add(verifyStatus);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		return status.toArray(new IStatus[status.size()]);
	}
	
	/**
	 * Verifies an install.
	 * 
	 * @param agent Provisioning agent
	 * @param profile Install profile
	 * @throws CoreException if install is not valid
	 */
	public void verifyInstall(IProvisioningAgent agent, IProfile profile) throws CoreException {
		for (Object listener : installVerifiers.getListeners()) {
			IStatus status = ((IInstallVerifier)listener).verifyInstall(agent, profile);
			if (status.getSeverity() == IStatus.ERROR) {
				throw new CoreException(new Status(IStatus.ERROR, Installer.ID, status.getMessage()));
			}
		}
	}
	
	/**
	 * Verify the user supplied credentials to insure they are valid
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	public IStatus[] verifyCredentials(String username, String password) {
		ArrayList<IStatus> status = new ArrayList<IStatus>();
		for (Object listener : installVerifiers.getListeners()) {
			try {
				IStatus verifyStatus = ((IInstallVerifier)listener).verifyCredentials(username, password);
				if ((verifyStatus != null) && !verifyStatus.isOK()) {
					status.add(verifyStatus);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		return status.toArray(new IStatus[status.size()]);
	}

	/**
	 * Verifies loaded components.
	 * 
	 * @param components Components
	 * @return Status for components
	 */
	public IStatus[] verifyInstallComponents(IInstallComponent[] components) {
		ArrayList<IStatus> status = new ArrayList<IStatus>();
		for (Object listener : installVerifiers.getListeners()) {
			try {
				IStatus verifyStatus = ((IInstallVerifier)listener).verifyInstallComponents(components);
				if ((verifyStatus != null) && !verifyStatus.isOK()) {
					status.add(verifyStatus);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		return status.toArray(new IStatus[status.size()]);
	}

	/**
	 * Verifies component selection.
	 * 
	 * @param components Selected components
	 * @return Status for components selection
	 */
	public IStatus[] verifyInstallComponentSelection(IInstallComponent[] components) {
		ArrayList<IStatus> status = new ArrayList<IStatus>();
		for (Object listener : installVerifiers.getListeners()) {
			try {
				IStatus verifyStatus = ((IInstallVerifier)listener).verifyInstallComponentSelection(components);
				if ((verifyStatus != null) && !verifyStatus.isOK()) {
					status.add(verifyStatus);
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
		
		return status.toArray(new IStatus[status.size()]);
	}

	@Override
	public IInstallData getInstallData() {
		return installData;
	}

	@Override
	public boolean isLaunchItemAvailable(LaunchItem item) {
		boolean available = false;
		IPath installLocation = getInstallLocation();
		
		if (item.getType() == LaunchItemType.EXECUTABLE) {
			if (installLocation != null) {
				IPath path = installLocation.append(item.getPath());
				available = (path.toFile().exists());
			}
		}
		// File item
		else if (item.getType() == LaunchItemType.FILE) {
			if (installLocation != null) {
				IPath path = installLocation.append(item.getPath());
				available = path.toFile().exists();
			}
		}
		// HTML item
		else if (item.getType() == LaunchItemType.HTML){
			URI uri = null;
			try {
				uri = new URI(item.getPath());
			} catch (URISyntaxException e) {
				Installer.log(e);
			}
			if (uri != null && uri.getScheme().toLowerCase().equals("file")) {
				File file = new File(uri.toString());
				available = file.exists();
			}
			else {
				available = true;
			}
		}
		//RESTART and LOGOUT items
		else if (item.getType() == LaunchItemType.RESTART || item.getType() == LaunchItemType.LOGOUT) {
			available = true;
		}
		
		return available;
	}
}
