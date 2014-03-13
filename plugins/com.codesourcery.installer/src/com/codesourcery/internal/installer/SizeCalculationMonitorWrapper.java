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
import org.eclipse.core.runtime.ProgressMonitorWrapper;

/**
 * Progress monitor wrapper for the ISizeCalculationMonitor.
 */
public class SizeCalculationMonitorWrapper extends ProgressMonitorWrapper
		implements ISizeCalculationMonitor {

	protected SizeCalculationMonitorWrapper(IProgressMonitor monitor) {
		super(monitor);
	}

	@Override
	public void done(long installSize) {
		IProgressMonitor monitor = getWrappedProgressMonitor();
		if (monitor instanceof ISizeCalculationMonitor) {
			((ISizeCalculationMonitor) monitor).done(installSize);
		}
	}

}
