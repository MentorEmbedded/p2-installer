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

import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallContext;
import com.codesourcery.internal.installer.InstallOperation;

/**
 * Install operation that shows a GUI wizard.
 * The display must be created before this operation is run.
 */
public class GUIInstallOperation extends InstallOperation {
	private boolean started = false;
	private boolean stopped = false;
	
	@Override
	public synchronized void run(IInstallContext context) {
		if (stopped || started)
			return;
		started = true;
		
		// Initialize images
		Installer.getDefault().initializeImages();

		// Open wizard dialog
		InstallWizard wizard = new InstallWizard(context);
		InstallWizardDialog dialog = new InstallWizardDialog(wizard);
		dialog.open();
		
		// Write status
		writeStatus(wizard.getStatus());
	}
}
