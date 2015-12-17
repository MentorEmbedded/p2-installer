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
package com.codesourcery.internal.installer.ui.pages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription.WizardNavigation;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.ui.ISetupWizardPage;

/**
 * Abstract setup page.
 */
public abstract class AbstractSetupPage extends InstallWizardPage implements ISetupWizardPage {
	/** <code>true</code> if page has been saved */
	private boolean saved = false;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param titleMessage or <code>null</code>
	 */
	protected AbstractSetupPage(String pageName, String titleMessage) {
		super(pageName, "");
		
		setPageNavigation(WizardNavigation.NONE);
	}

	/**
	 * Called to save the setup page data.  This method will only be called once.
	 * Subclasses should provide this method instead of {@link #saveInstallData(IInstallData)}.
	 * 
	 * @param data Install data
	 * @throws CoreException on failure
	 */
	protected abstract void saveSetup(IInstallData data) throws CoreException;
	
	/**
	 * Returns a plug-in image.
	 * 
	 * @param id Image identifier
	 * @return Image or <code>null</code> if id is <code>null</code> or installer
	 * is running console mode
	 */
	protected Image getImage(String id) {
		if ((id != null) && !isConsoleMode()) {
			return Installer.getDefault().getImageRegistry().get(id);
		}
		else {
			return null;
		}
	}
	
	/**
	 * Blends two RGB values using the provided ratio. 
	 * 
	 * @param c1 First RGB value
	 * @param c2 Second RGB value
	 * @param ratio Percentage of the first RGB to blend with 
	 * second RGB (0-100)
	 * 
	 * @return The RGB value of the blended color
	 */
	public static RGB blendRGB(RGB c1, RGB c2, int ratio) {
		ratio = Math.max(0, Math.min(255, ratio));

		int r = Math.max(0, Math.min(255, (ratio * c1.red + (100 - ratio) * c2.red) / 100));
		int g = Math.max(0, Math.min(255, (ratio * c1.green + (100 - ratio) * c2.green) / 100));
		int b = Math.max(0, Math.min(255, (ratio * c1.blue + (100 - ratio) * c2.blue) / 100));
		
		return new RGB(r, g, b);
	}
	
	@Override
	public boolean isSupported() {
		return true;
	}
	
	@Override
	public void saveInstallData(IInstallData data) throws CoreException {
		// Setup page only saves once
		if (!saved) {
			saveSetup(data);

			saved = true;
		}
	}
}
