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

import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;

import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.Log;
import com.codesourcery.internal.installer.ui.InstallWizard;
import com.codesourcery.internal.installer.ui.BusyAnimationControl;

/**
 * Page to show installation progress.
 * This page does not support console.
 */
public class ProgressPage extends InstallWizardPage {
	/** Install progress monitor part */
	private ProgressMonitorPart progressMonitorPart;
	/** Installing message */
	private String installingMessage;
	/** Animate control */
	private BusyAnimationControl animateCtrl;

	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param installingMessage Installing message
	 */
	public ProgressPage(String pageName, String title, String installingMessage) {
		super(pageName, title);
		this.installingMessage = installingMessage;
		
		setInstallPage(true);
	}

	/**
	 * Returns the progress monitor part.
	 * 
	 * @return Progress monitor part
	 */
	public ProgressMonitorPart getProgressMonitorPart() {
		return progressMonitorPart;
	}
	
	/**
	 * Returns the installing message.
	 * 
	 * @return Installing message
	 */
	public String getInstallingMessage() {
		return installingMessage;
	}

	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		
		area.setLayout(new GridLayout(2, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		// Installing label
		FormattedLabel installingLabel = new FormattedLabel(area, SWT.WRAP);
		installingLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		installingLabel.setText(getInstallingMessage());
		
		// Spacing
		Label spacer = new Label(area, SWT.NONE);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		// Installing animation
		animateCtrl = new BusyAnimationControl(area, SWT.NONE);
		animateCtrl.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		
		// Progress monitor part
		progressMonitorPart = new InstallProgressMonitorPart(area, new GridLayout());
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		progressMonitorPart.setLayoutData(data);
		progressMonitorPart.setVisible(true);

		return area;
	}
	
	/**
	 * Called to confirm installation cancel.
	 * 
	 * @return <code>true</code> to cancel.
	 */
	protected boolean canCancel() {
		InstallWizard wizard = (InstallWizard)getWizard();
		return wizard.showCancelConfirmation();
	}
	
	/**
	 * An installation progress monitor part that confirms
	 * Cancellation.
	 */
	private class InstallProgressMonitorPart extends ProgressMonitorPart
	{
		/**
		 * Constructor
		 * 
		 * @param parent Parent
		 * @param layout Layout
		 */
		public InstallProgressMonitorPart(Composite parent, Layout layout) {
			super(parent, layout, false);
			
			// Replace the cancel handler with one that can
			// prompt for confirmation.
		    fCancelListener = new Listener() {
		        public void handleEvent(Event e) {
		        	if (canCancel()) {
			            setCanceled(true);
			            if (fCancelComponent != null) {
							fCancelComponent.setEnabled(false);
						}
		        	}
		        }
		    };
		}
		
		@Override
		public void beginTask(String name, int totalWork) {
			super.beginTask(name, totalWork);

			// Start animation
			animateCtrl.animate(true);
			Log.getDefault().log(name);
		}

		@Override
		public void setTaskName(String name) {
			super.setTaskName(name);
			
			Log.getDefault().log(name);
		}

		@Override
		public void subTask(String name) {
			super.subTask(name);
			
			Log.getDefault().log(name);
		}

		@Override
		public void done() {
			// Stop animation
			animateCtrl.animate(false);
			super.done();
		}
	}
}
