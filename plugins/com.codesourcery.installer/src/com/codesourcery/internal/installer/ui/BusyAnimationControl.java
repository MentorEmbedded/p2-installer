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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.codesourcery.installer.Installer;
import com.codesourcery.internal.installer.IInstallerImages;

/**
 * Animated control that shows a computer with spinning progress dots.
 */
public class BusyAnimationControl extends AnimateControl {
	/**
	 * Constructor
	 * 
	 * @param parent Parent
	 * @param style Style
	 */
	public BusyAnimationControl(Composite parent, int style) {
		super(parent, style, new Image[] {
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT0),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT1),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT2),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT3),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT4),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT5),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT6),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT7),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT8),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT9),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT10),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT11),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT12),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT13),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT14),	
				Installer.getDefault().getImageRegistry().get(IInstallerImages.WAIT1),	
		});
	}
}
