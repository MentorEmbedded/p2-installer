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
package com.codesourcery.internal.installer.ui;

import java.text.DecimalFormat;

/**
 * UI Utilities.
 *
 */
public class UIUtils {
	/**
	 * Displays the given in a human-readable format.
	 * @param size
	 * @return The formatted string.
	 */
	public static String formatBytes(long size) {
		if (size <= 0) {
			return size + " B"; //$NON-NLS-1$
		}
		String[] units = new String[] { "B", "KB", "MB", "GB", "TB" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups]; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
