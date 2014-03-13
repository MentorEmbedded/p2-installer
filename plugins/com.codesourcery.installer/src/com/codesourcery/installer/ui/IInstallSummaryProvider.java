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
package com.codesourcery.installer.ui;

/**
 * Provides installation summary information.
 * Install wizard pages should implement this interface if they want to provide
 * information that is displayed on the summary page.
 */
public interface IInstallSummaryProvider {
	/**
	 * Returns installation summary information.
	 * 
	 * @return Summary information text.  The text can indicate items that 
	 * should be bold using using <code>&lt;b&gt;</code> and 
	 * <code>&lt;/b&gt;</code>.  The text can indicate items that should be 
	 * italic using <code>&lt;i&gt;</code> and <code>&lt;/i&gt;</code>.
	 */
	public String getInstallSummary();
}
