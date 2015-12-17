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
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.codesourcery.installer.IInstallComponent;
import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.LicenseDescriptor;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.IInstallPages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.RepositoryManager;

/**
 * Page to show license agreement.
 * This page supports console.
 */
public class LicensePage extends InformationPage implements IInstallConsoleProvider {
	/** Available licenses */
	private LicenseDescriptor[] availableLicenses;
	/** Fixed licenses */
	private LicenseDescriptor[] licenses;
	/** License list */
	private TableViewer list;
	/** Accept licenses button */
	private Button acceptLicenseButton;
	/** Last license count */
	private int lastLicenseCount = 0;
	/** Main area */
	private Composite mainArea;
	/** Sash dividing license list from license information */
	private SashForm sash;
	/** License information control */
	private Control licenseInformationControl;
	/** Members used for retaining selection. */
	private LicenseDescriptor firstLicense;
	private IStructuredSelection lastLicenseSelection = StructuredSelection.EMPTY;
	
	/**
	 * Constructor
	 * 
	 * @param licenses Array of licenses or <code>null</code>
	 */
	public LicensePage(LicenseDescriptor[] licenses) {
		super (IInstallPages.LICENSE_PAGE, InstallMessages.LicensePageTitle, "");
		
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
	 * Returns available licenses.  This will include any licenses from files and any licenses from installable units
	 * for selected components.
	 * 
	 * @return Available licenses or <code>null</code>
	 */
	protected LicenseDescriptor[] getAvailableLicenses() {
		return availableLicenses;
	}
	
	/**
	 * Sets available licenses.
	 * 
	 * @param availableLicenses All licenses or <code>null</code>
	 */
	protected void setAvailableLicenses(LicenseDescriptor[] availableLicenses) {
		this.availableLicenses = availableLicenses;
	}
	
	@Override
	public Control createInformationArea(Composite parent) {
		// Main area
		mainArea = new Composite(parent, SWT.NONE);
		mainArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		mainArea.setLayout(new GridLayout());

		// Sash between license list and license information
		sash = new SashForm(mainArea, SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// License area
		Composite area = new Composite(sash, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.marginWidth = 0;
		area.setLayout(layout);

		// License list viewer
		list = new TableViewer(area, SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		list.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		list.setContentProvider(ArrayContentProvider.getInstance());
		// Enable tool-tips for viewer
		ColumnViewerToolTipSupport.enableFor(list);
		// License list label provider
		list.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				LicenseDescriptor descriptor = (LicenseDescriptor) element;
				return descriptor.getLicenseName();
			}

			@Override
			public String getToolTipText(Object element) {
				LicenseDescriptor descriptor = (LicenseDescriptor) element;
				return descriptor.getLicenseName();
			}
		});
		// Update license information on license selection changed
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

		// Create license information area
		licenseInformationControl = super.createInformationArea(sash);

		// Set license information area to occupy 2/3 of available horizontal space by default
		sash.setWeights(new int[] { 1, 3 });

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
	 * Returns the sash form containing the license list and license information.
	 * 
	 * @return Sash form
	 */
	private SashForm getSash() {
		return sash;
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
	 */
	private void updateLicenses() {
		ArrayList<LicenseDescriptor> licenses = new ArrayList<LicenseDescriptor>();

		if ((getLicenses() != null) && (getLicenses().length > 0)) {
			// Process the license descriptors
			for (LicenseDescriptor license : getLicenses()) {
				// License from text file
				if (license.getUnit() == null) {
					if ((license.getLicenseText() != null) && !license.getLicenseText().isEmpty()) {
						licenses.add(license);
					}
				}
				// License for installable unit
				else {
					IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents(false);
					for (IInstallComponent component : components) {
						// Component included in install
						if (component.getInstall()) {
							IInstallableUnit unit = component.getInstallUnit();
							if (unit != null) {
								if (unit.getId().equals(license.getUnit().getId())) {
									try {
										// Add the unit licenses
										Collection<ILicense> iuLicenses = unit.getLicenses(null);
										for (ILicense iuLicense : iuLicenses) {
											LicenseDescriptor licenseDesc = new LicenseDescriptor(iuLicense.getBody(), 
													(license.getLicenseName() != null) ? license.getLicenseName() : 
														unit.getProperty(IInstallableUnit.PROP_NAME, null));
											
											licenses.add(licenseDesc);
										}
									}
									catch (Exception e) {
										Installer.log(e);
									}
									break;
								}
							}
						}
					}
				}
			}
		}
		
		// Set all licenses
		setAvailableLicenses(licenses.toArray(new LicenseDescriptor[licenses.size()]));
		
		if (!isConsoleMode()) {
			if (!licenses.isEmpty())
				firstLicense = licenses.get(0);
			list.setInput(getAvailableLicenses());
		}
	}
	
	@Override
	public void setVisible(boolean visible) {
		// Update licenses
		updateLicenses();

		// No licenses
		if (getAvailableLicenses().length == 0) {
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
	
			int currentLicenseCount = list.getTable().getItemCount();
			
			// Check if Item count has been changed
			if (currentLicenseCount != lastLicenseCount) {
				// Single license
				if ( currentLicenseCount == 1) {
					acceptLicenseButton.setText(InstallMessages.AcceptLicense);
					setInformationTitle(InstallMessages.LicenseMessageSingleLicense);
					// Hide license list
					getSash().setMaximizedControl(licenseInformationControl);
				}
				// Multiple licenses
				else {
					acceptLicenseButton.setText(InstallMessages.AcceptLicenses);
					setInformationTitle(InstallMessages.LicenseMessageMultipleLicenses);
					// Show license list
					getSash().setMaximizedControl(null);
				}
	
				// Update License count
				lastLicenseCount = currentLicenseCount;
			}
		}

		super.setVisible(visible);
	}

	@Override
	public void setActive(IInstallData data) {
		super.setActive(data);

		// Update licenses for console mode
		updateLicenses();
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
		for (LicenseDescriptor license : getAvailableLicenses()) {
			String licenseText = NLS.bind(InstallMessages.LicensePageLicenseName1, license.getLicenseName(), license.getLicenseText());
			licenseInfo.append(licenseText);
		}
		return licenseInfo.toString();
	}

	@Override
	public boolean isSupported() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		return mode.isInstall();
	}
}
