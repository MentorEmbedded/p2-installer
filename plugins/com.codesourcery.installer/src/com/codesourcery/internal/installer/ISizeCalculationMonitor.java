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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Progress monitor which reports the result from the install 
 * size calculation.
 */
public interface ISizeCalculationMonitor extends IProgressMonitor {
	/**
	 * Called when the size has been computed.
	 * 
	 * @param installSize Installation size
	 */
	public void done(long installSize);
}
