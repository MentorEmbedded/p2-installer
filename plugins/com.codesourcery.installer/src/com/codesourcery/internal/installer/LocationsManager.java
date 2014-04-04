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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;

/**
* This class manages the created installation directories for all products.  It 
* manages directories shared between multiple products using a reference count 
* and stores the information in the home directory.  The format of this
* information is:
* <location>,<reference count>
*
* Example:
* Product 1 is installed to: /home/products/product1
* The directories that were created during installation:
*   /home/products
*   /home/products/product1
*
* The stored locations contains:
*   /home/products, 1
*   /home/products/product1, 1  
*
* Product 2 is installed to: /home/products/product2
* 
* The directories that were created during installation:
*   /home/products/product2
* 
* The stored locations contains:
*   /home/products/product1, 1
*   /home/products/product2, 1
*   /home/products, 2
*
* The directory  /home/products reference count was set to 2 because it was a 
* location created by product1, but also used in the path for product 2.  i.e. 
* product 2 installation would have created the directory if product 1 installation
* had not created it.
*
* Product 1 is uninstalled.  
* The directory /homes/products/product1 is removed since that is the product 
* location and it has only a single reference.  It removes a reference to 
* /home/products, but does not remove the directory since there is still a 
* reference to it.
*
* The stored locations contains:
*   /home/products/product2, 1
*   /home/products, 1
*
* Product 2 is uninstalled.
* The directory /home/products/product2 and /home/products are removed since 
* they have a single reference.
*/
public class LocationsManager {
	/** Install locations filename */
	private static final String INSTALL_LOCATIONS_FILENAME = ".locations";
	/** Default instance */
	private static LocationsManager instance = new LocationsManager();
	/** Install locations */
	private ArrayList<InstallLocation> installLocations = new ArrayList<InstallLocation>();
	/** Install location */
	private IPath installLocation;

	/**
	 * Constructor
	 */
	private LocationsManager() {
	}

	/**
	 * Returns the default instance.
	 * 
	 * @return Default instance
	 */
	public static LocationsManager getDefault() {
		return instance;
	}

	/**
	 * Sets the install location and creates initial directory.
	 * The directories for any previous install location will be deleted.
	 * 
	 * @param path Install location
	 * @throws CoreException on failure
	 */
	public void setInstallLocation(IPath path) throws CoreException {
		Installer.getDefault().getInstallDescription().setRootLocation(path);
		
		// Location changed
		if ((path == null) || !path.equals(installLocation)) {
			// Remove old location
			if (installLocation != null) {
				RepositoryManager.getDefault().stopAgent();
				deleteInstallLocation(installLocation);
			}
			this.installLocation = path;
		
			// Create new location
			if (installLocation != null) {
				createInstallLocation(installLocation);
				// Create new P2 agent
				// Note, this will result in agent files being created
				RepositoryManager.getDefault().createAgent(Installer.getDefault().getInstallDescription().getInstallLocation());
			}
		}
	}
	
	/**
	 * Returns an install location for a path.
	 * 
	 * @param path Path
	 * @return Install location or <code>null</code> if there is not
	 * install location for the path.
	 */
	private InstallLocation getInstallLocation(IPath path) {
		InstallLocation location = null;
		for (InstallLocation installLocation : installLocations) {
			if (path.equals(installLocation.getPath())) {
				location = installLocation;
				break;
			}
		}
		
		return location;
	}
	
	/**
	 * Adds an install location to be tracked for removal.
	 * If the location has already been added, then the reference to the
	 * location will be increased.
	 * 
	 * @param path Path for install location
	 */
	private void addInstallLocation(IPath path) {
		InstallLocation location = getInstallLocation(path);
		// New location
		if (location == null) {
			location = new InstallLocation(path);
			installLocations.add(location);
		}
		// Reference existing location
		else {
			location.addReference();
		}
	}

	/**
	 * Creates the directories for an install location.
	 * 
	 * @param path Path to install location
	 */
	public void createInstallLocation(IPath path) {
		String[] segments = path.segments();
		IPath location = new Path(path.getDevice(), "/");

		for (String segment : segments) {
			location = location.append(segment);
		
			// If references existing install location, increment reference count
			InstallLocation installLocation = getInstallLocation(location);
			if (installLocation != null) {
				installLocation.addReference();
			}
			
			File directory = location.toFile();
			// If directory is created, add install location for it
			if (!directory.exists()) {
				directory.mkdir();
				addInstallLocation(location);
			}
		}
	}
	
	/**
	 * Deletes an install location.
	 * 
	 * @param path Path to install location
	 */
	public void deleteInstallLocation(IPath path) {
		sortLocations();
		
		InstallLocation[] locations = installLocations.toArray(new InstallLocation[installLocations.size()]);
		for (InstallLocation location : locations) {
			if (location.getPath().toFile().exists()) {
				// If the location is a parent of the product location, remove the
				// reference to it.
				if (location.getPath().isPrefixOf(path)) {
					location.removeReference();
					if (!location.hasReferences()) {
						installLocations.remove(location);
						deleteDirectory(location.getPath());
					}
				}
			}
			else {
				installLocations.remove(location);
			}
		}
	}
	
	/**
	 * Removes a directory and all children.
	 * 
	 * @param path Path to directory
	 */
	public void deleteDirectory(IPath path) {
		FileCollector collector = new FileCollector(path);
		collector.deleteFiles(null);
		File directory = path.toFile();
		if (directory.exists())
			directory.delete();
	}
	
	/**
	 * Deletes the directories for a product installation.
	 * 
	 * @param product Product
	 * @param monitor Progress monitor
	 * @param CoreException on failure to remove the product location
	 */
	public void deleteProductLocation(IInstallProduct product, IProgressMonitor monitor) throws CoreException {
		IStatus removeStatus = Status.OK_STATUS;
		
		// Remove product directory
		IPath productPath = product.getLocation();
		InstallLocation productLocation = getInstallLocation(productPath);
		if (productLocation != null) {
			installLocations.remove(productLocation);
			// Remove all files that can be removed (are not locked)
			removeStatus = deleteProductDirectory(productLocation.getPath(), monitor);
			// Schedule remaining files to be removed after installer exits
			ShutdownHandler.getDefault().addDirectoryToRemove(productLocation.getPath().toOSString(), false);
		}

		sortLocations();

		// Schedule directories to be deleted only if they are empty
		InstallLocation[] locations = installLocations.toArray(new InstallLocation[installLocations.size()]);
		for (InstallLocation location : locations) {
			if (location.getPath().toFile().exists()) {
				// If the location is a parent of the product location, remove the
				// reference to it.
				if (location.getPath().isPrefixOf(productPath)) {
					location.removeReference();
					if (!location.hasReferences()) {
						installLocations.remove(location);
					}
				}
				ShutdownHandler.getDefault().addDirectoryToRemove(location.getPath().toOSString(), true);
			}
			else {
				installLocations.remove(location);
			}
		}
		
		// Some files failed to remove
		if (!removeStatus.isOK()) {
			Installer.log(removeStatus.getMessage());
		}
	}

	/**
	 * Sorts install locations by deepest path first so that leaf directories
	 * are removed before parent directories.
	 */
	private void sortLocations() {
		Collections.sort(installLocations, 	new Comparator<InstallLocation>() {
			@Override
			public int compare(InstallLocation arg0, InstallLocation arg1) {
				int arg0Length = arg0.getPath().toOSString().length();
				int arg1Length = arg1.getPath().toOSString().length();
				if (arg0Length > arg1Length) {
					return -1;
				}
				else if (arg0Length < arg1Length) {
					return 1;
				}
				else {
					return 0;
				}
			}
		});
	}

	/**
	 * Deletes a product directory.
	 * 
	 * @param path Product directory
	 * @param monitor Progress monitor
	 * @return <code>IStatus.OK</code> on success, <code>IStatus.ERROR</code>
	 * on failure and the message will contain files that could not be removed. 
	 */
	private IStatus deleteProductDirectory(IPath path, IProgressMonitor monitor) {
		ArrayList<String> filesNotRemoved = new ArrayList<String>();

		// Collect files to be removed
		final File uninstallDirectory = new File(path.append(IInstallConstants.UNINSTALL_DIRECTORY).toOSString());
		FileCollector collector = new FileCollector(path, new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				// Exclude the uninstall directory as it is locked on Windows and
				// will be removed when uninstall exits.
				return !uninstallDirectory.getAbsolutePath().equals(pathname.getAbsolutePath());
			}
		});
		File[] files = collector.collectFiles();
		
		// Remove files
		monitor.beginTask(NLS.bind(InstallMessages.Removing0, ""), files.length);
		for (File file : files) {
			try {
				if (file.isDirectory()) {
					IPath filePath = new Path(file.getAbsolutePath()).removeFirstSegments(path.segmentCount()).setDevice("");
					monitor.setTaskName(NLS.bind(InstallMessages.Removing0, filePath.toOSString()));
				}
				if (!file.delete())
					filesNotRemoved.add(file.getAbsolutePath());
			}
			catch (Exception e) {
				filesNotRemoved.add(file.getAbsolutePath());
				Installer.log(e);
			}
			monitor.worked(1);
		}
		monitor.done();

		// Some files could not be removed
		if (filesNotRemoved.size() > 0) {
			// Report files that could not be removed
			StringBuffer message = new StringBuffer(InstallMessages.Error_FilesNotRemoved);
			int count = 0;
			for (String fileNotRemoved : filesNotRemoved) {
				// Only report up to ten files
				if (++count > 10) {
					message.append('\n');
					message.append(NLS.bind(InstallMessages.More0, filesNotRemoved.size() - 10));
					break;
				}
				message.append('\n');
				message.append("  ");
				message.append(fileNotRemoved);
			}
			return new Status(IStatus.ERROR, Installer.ID, 0, message.toString(), null);
		}
		// All files were removed
		else {
			return Status.OK_STATUS;
		}
	}
	
	/**
	 * Saves install locations.
	 */
	public void save() {
		IPath path = Installer.getDefault().getDataFolder().append(INSTALL_LOCATIONS_FILENAME);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(path.toOSString()));
			for (InstallLocation location : installLocations) {
				writer.write(location.getPath().toOSString());
				writer.write(',');
				writer.write(Integer.toString(location.getReferenceCount()));
				writer.newLine();
			}
		}
		catch (Exception e) {
			Installer.log(e);
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
	
	/**
	 * Loads install locations.
	 */
	public void load() {
		IPath path = Installer.getDefault().getDataFolder().append(INSTALL_LOCATIONS_FILENAME);
		
		installLocations.clear();
		if (path.toFile().exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(path.toOSString()));
				String line;
				while ((line = reader.readLine()) != null) {
					String[] parts = line.split(",");
					if (parts.length == 2) {
						try {
							InstallLocation location = new InstallLocation(new Path(parts[0]));
							int count = Integer.parseInt(parts[1]);
							location.setReferenceCount(count);
							installLocations.add(location);
						}
						catch (Exception e) {
							Installer.log(e);
						}
					}
				}			
			}
			catch (Exception e) {
				Installer.log(e);
			}
			finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// Ignore
					}
				}
			}
		}
	}
}
