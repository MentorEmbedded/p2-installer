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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LicenseDescriptor;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.IInstallPageConstants;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.RepositoryManager;

/**
 * Page to show license agreement.
 * This page supports console.
 */
public class LicensePage extends InformationPage implements IInstallConsoleProvider {
	/** All licenses */
	private LicenseDescriptor[] allLicenses;
	/** Fixed licenses */
	private LicenseDescriptor[] licenses;
	/** License list */
	private ListViewer list;
	/** Accept licenses button */
	private Button acceptLicenseButton;
	/** Last license count */
	private int lastLicenseCount = 0;
	/** Main area */
	private Composite mainArea;
	
	/**
	 * Members used for retaining selection.
	 */
	private LicenseDescriptor firstLicense;
	private IStructuredSelection lastLicenseSelection = StructuredSelection.EMPTY;
	
	/**
	 * Constructor
	 * 
	 * @param licenses Array of licenses or <code>null</code>
	 */
	public LicensePage(LicenseDescriptor[] licenses) {
		super (IInstallPageConstants.LICENSE_PAGE, InstallMessages.LicensePageTitle, "");
		
		this.licenses = licenses;
		
		setInformationTitle(InstallMessages.LicenseMessageMultipleLicenses);
		// Enable scrolling
		setScrollable(true);
		// Set incomplete until license is accepted
		setPageComplete(false);
	}

	/**
	 * Returns the licenses.
	 * 
	 * @return Licenses or <code>null</code>
	 */
	public LicenseDescriptor[] getLicenses() {
		return licenses;
	}
	
	/**
	 * Returns all licenses, including any from installable units.
	 * 
	 * @return All licenses or <code>null</code>
	 */
	protected LicenseDescriptor[] getAllLicenses() {
		return allLicenses;
	}
	
	/**
	 * Sets all licenses.
	 * 
	 * @param allLicenses All licenses or <code>null</code>
	 */
	protected void setAllLicenses(LicenseDescriptor[] allLicenses) {
		this.allLicenses = allLicenses;
	}
	
	@Override
	public Control createInformationArea(Composite parent) {
		// Main area
		mainArea = new Composite(parent, SWT.NONE);
		mainArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainArea.setLayout(new GridLayout());
		
		// License area
		Composite area = new Composite(mainArea, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		area.setLayout(layout);
		area.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// License selection area
		Composite left = new Composite(area, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		left.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_VERTICAL);
		left.setLayoutData(gd);

		// Create license information area
		super.createInformationArea(area);

		// License list viewer
		list = new ListViewer(left, SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		list.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		list.setContentProvider(ArrayContentProvider.getInstance());
		
		list.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				LicenseDescriptor descriptor = (LicenseDescriptor) element;
				return descriptor.getLicenseName();
			}
		});
		
		list.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				lastLicenseSelection = (IStructuredSelection) event.getSelection();
				if(lastLicenseSelection.isEmpty())
					return;
				
				LicenseDescriptor ld = (LicenseDescriptor) lastLicenseSelection.getFirstElement();
				setInformation(ld.getLicenseText() == null ? "" : ld.getLicenseText());
			}
		});

		// License accept button
		Composite acceptComp = new Composite(mainArea, SWT.NONE);
		layout = new GridLayout();
		layout.marginWidth = 0;
		acceptComp.setLayout(layout);
		acceptComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createAcceptButton(acceptComp);
		
		return area;
		
	}
	
	/**
	 * Returns the main area composite.
	 * 
	 * @return Main area
	 */
	private Composite getMainArea() {
		return mainArea;
	}

	/**
	 * Create accept button
	 * @param parent Parent control
	 * @return Accept button
	 */
	protected Control createAcceptButton(Composite parent) {
		acceptLicenseButton = new Button(parent, SWT.CHECK);
		acceptLicenseButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));
		acceptLicenseButton.setText(InstallMessages.AcceptLicenses);
		acceptLicenseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setPageComplete(acceptLicenseButton.getSelection());
			}
		});

		return parent;
	}

	@Override
	public String getConsoleResponse(String input) throws IllegalArgumentException {
		String message = InstallMessages.LicensePageConsoleMessage + "\n\n" + getConsoleLicenseAgreements();
		ConsoleYesNoPrompter prompter = new ConsoleYesNoPrompter(message, InstallMessages.LicensePageConsolePrompt);
		String response = prompter.getConsoleResponse(input);
		if ((response == null) && !prompter.getResult()) {
			throw new IllegalArgumentException(InstallMessages.LicensePageLicenseNotAccepted);
		}
		return response;
	}

	/**
	 * Updates the licenses
	 * 
	 * @param data Install data
	 */
	private void updateLicenses(IInstallData data) {
		ArrayList<LicenseDescriptor> licenses = new ArrayList<LicenseDescriptor>();
		
		// Add fixed licenses
		LicenseDescriptor[] fixedLicenses = getLicenses();
		if (fixedLicenses != null) {
			for (LicenseDescriptor license : fixedLicenses) {
				// License is enabled?
				if (license.isEnabled())
					licenses.add(license);
			}
		}
		
		// Add IU licenses if enabled
		if (Installer.getDefault().getInstallManager().getInstallDescription().getLicenseIU() && (data != null)) {
			ArrayList<IInstallableUnit> toAdd = new ArrayList<IInstallableUnit>();
			ArrayList<IInstallableUnit> toRemove = new ArrayList<IInstallableUnit>();
			RepositoryManager.getDefault().getInstallUnits(toAdd, toRemove);
			for (IInstallableUnit unit : toAdd) {
				try {
					Collection<ILicense> iuLicenses = unit.getLicenses(null);
					for (ILicense iuLicense : iuLicenses) {
						LicenseDescriptor license = new LicenseDescriptor(iuLicense.getBody(), 
								unit.getProperty(IInstallableUnit.PROP_NAME, null));
						
						licenses.add(license);
					}
				}
				catch (Exception e) {
					Installer.log(e);
				}
			}
		}
		
		// Set all licenses
		setAllLicenses(licenses.toArray(new LicenseDescriptor[licenses.size()]));
		
		if (!isConsoleMode()) {
			if (!licenses.isEmpty())
				firstLicense = licenses.get(0);
			list.setInput(getAllLicenses());
		}
	}
	
	@Override
	public void setActive(IInstallData data) {
		super.setActive(data);

		// Update licenses
		updateLicenses(data);

		// Not running in console mode
		if (!isConsoleMode()) {
			// No licenses
			if (getAllLicenses().length == 0) {
				setInformationTitle(InstallMessages.LicensePageNoComponents);
				showStatus(new IStatus[] { new Status(IStatus.OK, Installer.ID, InstallMessages.ClickNext) });
				getMainArea().setVisible(false);
				setPageComplete(true);
			}
			// Licenses
			else {
				hideStatus();
				getMainArea().setVisible(true);
				setPageComplete(acceptLicenseButton.getSelection());
				
				list.setSelection(lastLicenseSelection);
				IStructuredSelection ns = (IStructuredSelection) list.getSelection();
				if(ns.isEmpty() && firstLicense != null)
					list.setSelection(new StructuredSelection(firstLicense));
		
				int currentLicenseCount = list.getList().getItemCount();
				
				// Check if Item count has been changed
				if (currentLicenseCount != lastLicenseCount) {
					Composite comp = list.getControl().getParent();
					boolean hideList = false;
					String acceptButtonText = InstallMessages.AcceptLicenses;
					String pageTitle = InstallMessages.LicenseMessageMultipleLicenses;
					
					/* If only one license agreement, change button text
					 * and hide List
					 */
					if ( currentLicenseCount == 1) {
						acceptButtonText = InstallMessages.AcceptLicense;
						pageTitle = InstallMessages.LicenseMessageSingleLicense;
						hideList = true;
					}
					
					// Update Accept button text and Page title
					setInformationTitle(pageTitle);
					acceptLicenseButton.setText(acceptButtonText);
					
					// Update visibility of list viewer
					((GridData)comp.getLayoutData()).exclude = hideList;
					comp.setVisible(!hideList);
					comp.getParent().layout();
		
					// Update License count
					lastLicenseCount = currentLicenseCount;
				}
			}
		}
	}

	/**
	 * Get list of license agreements to show for head-less installer. Licenses are listed in following way:
	 * License Name: Name of License 
	 * Text of License agreement
	 * 
	 * @return Formated string containing license agreement(s)
	 */
	private String getConsoleLicenseAgreements() {
		StringBuffer licenseInfo = new StringBuffer();
		for (LicenseDescriptor license : getAllLicenses()) {
			if (!license.isEnabled())
				continue;
			String licenseText = NLS.bind(InstallMessages.LicensePageLicenseName1, license.getLicenseName(), license.getLicenseText());
			licenseInfo.append(licenseText);
		}
		return licenseInfo.toString();
	}

	@Override
	public boolean isSupported() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		return (mode.isInstall() && (!mode.isUpdate() || mode.isPatch()));
	}
}
