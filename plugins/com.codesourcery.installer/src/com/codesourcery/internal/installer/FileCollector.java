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
import java.io.FileFilter;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.Installer;

/**
 * This class will collect all files in a directory.
 */
public class FileCollector {
	/** Root path for files */
	private IPath directory;
	/** Collected files */
	private ArrayList<File> fileStore;
	/** Filter or <code>null</code> */
	private FileFilter filter;
	
	/**
	 * Constructor
	 * 
	 * @param directory Directory to collect all files
	 */
	public FileCollector(IPath directory) {
		this(directory, null);
	}
	
	/**
	 * Constructor
	 * 
	 * @param directory Directory to collect files
	 * @param filter File filter
	 */
	public FileCollector(IPath directory, FileFilter filter) {
		this.directory = directory;
		this.filter = filter;
	}
	
	/**
	 * Returns the path to the directory to collect files.
	 * 
	 * @return Path
	 */
	public IPath getDirectory() {
		return directory;
	}
	
	/**
	 * Returns the file filter.
	 * 
	 * @return Filter or <code>null</code>
	 */
	public FileFilter getFilter() {
		return filter;
	}
	
	/**
	 * Collects all files.  Files of a directory will be ordered first before
	 * the directory.  The root directory is not included.
	 * 
	 * @return Files
	 */
	public File[] collectFiles() {
		fileStore = new ArrayList<File>();
		File directory = new File(getDirectory().toOSString());
		collect(directory);
		
		return fileStore.toArray(new File[fileStore.size()]);
	}

	/**
	 * Collects and deletes files.
	 * 
	 * @param monitor Progress monitor or <code>null</code>
	 * @return Files that could not be deleted
	 */
	public File[] deleteFiles(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		
		ArrayList<String> filesNotRemoved = new ArrayList<String>();
		File[] files = collectFiles();
		monitor.beginTask(NLS.bind(InstallMessages.Removing0, ""), files.length);
		for (File file : files) {
			try {
				if (file.exists()) {
					if (file.isDirectory()) {
						IPath filePath = new Path(file.getAbsolutePath()).removeFirstSegments(getDirectory().segmentCount()).setDevice("");
						monitor.setTaskName(NLS.bind(InstallMessages.Removing0, filePath.toOSString()));
					}
					if (!file.delete())
						filesNotRemoved.add(file.getAbsolutePath());
				}
			}
			catch (Exception e) {
				filesNotRemoved.add(file.getAbsolutePath());
				Installer.log(e);
			}
			monitor.worked(1);
		}
		monitor.done();
		
		return filesNotRemoved.toArray(new File[filesNotRemoved.size()]);
	}
	
	/**
	 * Recursively collects all files.
	 * 
	 * @param root Root file
	 */
	private void collect(File root) {
		File[] files;
		if (getFilter() != null)
			files = root.listFiles(getFilter());
		else
			files = root.listFiles();
		
		for (File file : files) {
			// If directory, collect files for directory
			if (file.isDirectory()) {
				collect(file);
			}
			// Add the file
			fileStore.add(file);
		}
	}
}
