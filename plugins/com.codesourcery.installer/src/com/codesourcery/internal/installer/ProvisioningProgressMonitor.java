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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;

import com.codesourcery.installer.Installer;

/**
 * Delegating progress monitor used during installation
 * to filter p2 progress messages.
 */
public class ProvisioningProgressMonitor extends ProgressMonitorWrapper {
	/** Log message prefix */
	private static final String LOG_PREFIX = "P2: ";
	
	/** Delegated progress monitor */
	private IProgressMonitor delegateProgressMonitor;
	/** Current progress message */
	private String currentMessage = "";
	/** Progress regular expression find patterns */
	private Pattern[] findPatterns;
	/** Progress replace patterns */
	private String[] replacePatterns;

	/**
	 * Constructor
	 * 
	 * @param delegateProgressMonitor Progress monitor for delegation
	 * @param find Regular expression find patterns
	 * @param replace Regular expression replace patterns
	 */
	public ProvisioningProgressMonitor(IProgressMonitor delegateProgressMonitor, 
			String[] find, String[] replace) {
		super(delegateProgressMonitor);
		this.delegateProgressMonitor = delegateProgressMonitor;
		try {
			if (find != null) {
				findPatterns = new Pattern[find.length];
				for (int index = 0; index < findPatterns.length; index ++) {
					findPatterns[index] = Pattern.compile(find[index]);
				}
			}
			replacePatterns = replace;
		}
		catch (Exception e) {
			findPatterns = null;
			replacePatterns = null;
			Installer.log(e);
		}
	}

	/**
	 * Called to filter progress messages before
	 * sending them to the delegated progress monitor.
	 * 
	 * @param name Name
	 * @return Filtered name
	 */
	protected String filterName(String name) {
		if (findPatterns == null) {
			return name;
		}
		else {
			for (int index = 0; index < findPatterns.length; index ++) {
				Matcher match = findPatterns[index].matcher(name);
				if ((match != null) && match.find()) {
					String progress = match.replaceFirst(replacePatterns[index]);
					if (!currentMessage.equals(progress)) {
						currentMessage = progress;
					}
					
					break;
				}
			}
			
			return currentMessage;
		}
	}

	/**
	 * Returns the delegated progress monitor.
	 * 
	 * @return Progress monitor
	 */
	protected IProgressMonitor getProgressMonitor() {
		return delegateProgressMonitor;
	}
	
	@Override
	public void beginTask(String name, int totalWork) {
		super.beginTask(filterName(name), totalWork);
		
		Log.getDefault().log(name);
	}

	@Override
	public void setTaskName(String name) {
		super.setTaskName(filterName(name));
		
		Log.getDefault().log(LOG_PREFIX + name);
	}

	@Override
	public void subTask(String name) {
		super.subTask(filterName(name));
		
		Log.getDefault().log(LOG_PREFIX + name);
	}
}
