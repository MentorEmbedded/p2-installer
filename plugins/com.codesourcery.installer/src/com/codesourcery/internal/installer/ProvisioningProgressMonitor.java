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
import org.eclipse.osgi.util.NLS;

import com.codesourcery.installer.Installer;

/**
 * Delegating progress monitor used during installation
 * to filter p2 progress messages.
 */
public class ProvisioningProgressMonitor extends ProgressMonitorWrapper {
	/** IU name macro */
	private static String MACRO_IU_NAME = "$IU_NAME";
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
	/** Start time */
	private long startTime;
	/** Total work amount */
	private double totalWork;
	/** Worked amount */
	private double worked;
	/** Task name */
	private String taskName;
	/** <code>true</code> to append time estimate to task name */
	private boolean showRemainingTime = false;

	/**
	 * Constructor
	 * 
	 * @param delegateProgressMonitor Progress monitor for delegation
	 */
	public ProvisioningProgressMonitor(IProgressMonitor delegateProgressMonitor) {
		super(delegateProgressMonitor);
		this.delegateProgressMonitor = delegateProgressMonitor;
	}

	/**
	 * Sets the progress monitor filter.
	 * 
	 * @param find Regular expressions for find patterns.
	 * @param replace Corresponding regular expressions for replace patterns.
	 */
	public void setFilter(String[] find, String[] replace) {
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
	 * Set whether a remaining time estimate should be appended to the task name.
	 * 
	 * @param showTimeEstimate <code>true</code> to show remaining time estimate
	 */
	public void setShowRemainingTime(boolean showTimeEstimate) {
		this.showRemainingTime = showTimeEstimate;
	}
	
	/**
	 * @return <code>true</code> if remaining time estimate will be appended to the task name
	 */
	public boolean getShowRemainingTime() {
		return showRemainingTime;
	}
	
	/**
	 * Called to filter progress messages before
	 * sending them to the delegated progress monitor.
	 * 
	 * @param name Name
	 * @return Filtered name
	 */
	protected String filterName(String name) {
		// No replacement
		if (findPatterns == null) {
			return name;
		}
		// Pattern replacements
		else {
			// Loop through find patterns
			for (int index = 0; index < findPatterns.length; index ++) {
				Matcher match = findPatterns[index].matcher(name);
				// Match found
				if ((match != null) && match.find()) {
					String replacePattern = replacePatterns[index];
					// Replace pattern contains IU name macro
					int i1 = replacePattern.indexOf(MACRO_IU_NAME + "(");
					if (i1 > 0) {
						String iuName = null;
						try {
							int i2 = replacePattern.indexOf(')', i1);
							if (i2 > 0) {
								String groupPart = replacePattern.substring(i1 + MACRO_IU_NAME.length() + 1, i2);
								if (groupPart.startsWith("$")) {
									groupPart = groupPart.substring(1);
									int group = Integer.parseInt(groupPart);
									if (group != -1) {
										String iuId = match.group(group);
										if (iuId != null) {
											iuName = RepositoryManager.getDefault().queryIuName(iuId);
											if (iuName != null) {
												replacePattern = replacePattern.substring(0, i1) + iuName + replacePattern.substring(i2 + 1);
											}
										}
									}
								}
							}
						}
						catch (Exception e) {
							Installer.log(e);
						}
						
						if (iuName == null) {
							replacePattern = null;
						}
					}
					
					if (replacePattern != null) {
						String progress = match.replaceFirst(replacePattern);
						if (!currentMessage.equals(progress)) {
							currentMessage = progress;
						}
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
		this.taskName = name;
		this.totalWork = totalWork;
		this.startTime = System.currentTimeMillis();
		
		Log.getDefault().log(name);
	}

	@Override
	public void setTaskName(String name) {
		super.setTaskName(filterName(name));
		this.taskName = name;
		
		Log.getDefault().log(LOG_PREFIX + name);
	}

	@Override
	public void subTask(String name) {
		super.subTask(filterName(name));
		
		Log.getDefault().log(LOG_PREFIX + name);
	}

	/**
	 * Appends a time segment to the buffer.
	 * 
	 * @param buffer Buffer
	 * @param time Time amount
	 * @param notation Time notation (hours, minutes, seconds, etc.)
	 */
	private void appendTimeSegment(StringBuilder buffer, long time, String notation) {
		if (time > 0) {
			if (buffer.length() != 0) {
				buffer.append(", ");
			}
			buffer.append(Long.toString(time));
			buffer.append(' ');
			buffer.append(notation);
		}
	}
	
	@Override
	public void internalWorked(double work) {
		super.internalWorked(work);
		
		// Append remaining time
		if (getShowRemainingTime()) {
			try {
				worked += work;
				if (worked > totalWork) {
					worked = totalWork;
				}
				
				long currentTime = System.currentTimeMillis();
				long remainingTime = (long)((currentTime - startTime) / worked * (totalWork - worked));
				
				long estimatedSeconds = (remainingTime / 1000) % 60;
				long estimatedMinutes = (remainingTime / (1000 *60)) % 60;
				long estimatedHours = (remainingTime / (1000 * 60 * 60)) % 24;
				
				// Don't show if more than a day
				if (estimatedHours < 24) {
					StringBuilder buffer = new StringBuilder();
					appendTimeSegment(buffer, estimatedHours, InstallMessages.Hours);
					appendTimeSegment(buffer, estimatedMinutes, InstallMessages.Minutes);
					// Only show seconds if no more hours or minutes remaining
					if ((estimatedHours == 0) && (estimatedMinutes == 0)) {
						appendTimeSegment(buffer, estimatedSeconds, InstallMessages.Seconds);
					}
					if (buffer.length() > 0) {
						String formattedTaskName = NLS.bind(InstallMessages.RemainingTime1, taskName, buffer.toString());
						super.setTaskName(formattedTaskName);
					}
					else {
						super.setTaskName(taskName);
					}
					
				}
			}
			catch (Exception e) {
				Installer.log(e);
			}
		}
	}
	
	
}
