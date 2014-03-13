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

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.ui.IInstallComponent;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.IInstallRepositoryListener;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.RepositoryManager;
import com.codesourcery.internal.installer.SizeCalculationMonitorAdapter;
import com.codesourcery.internal.installer.ui.SpinnerProgress;
import com.codesourcery.internal.installer.ui.UIUtils;

/**
 * Page to show components for the install.  Optional components can be selected.
 * This page supports console.
 */
public class ComponentsPage extends InstallWizardPage implements IInstallSummaryProvider, IInstallConsoleProvider, IInstallRepositoryListener {
	/** Component name column */
	private static final int COLUMN_NAME = 0;
	/** Component version column */
	private static final int COLUMN_VERSION = 1;
	
	/** Margin around components area */
	private static final int COMPONENTS_MARGIN = 2;
	/** Indent of component description */
	private static final int COMPONENT_DESCRIPTION_INDENT = 16;
	/** Component header height */
	private static final int COMPONENT_HEADER_HEIGHT = 45;
	
	/** Selected components */
	protected ArrayList<IInstallComponent> selectedComponents = new ArrayList<IInstallComponent>();
	/** Message label */
	protected Label messageLabel;
	/** Console list */
	protected ConsoleListPrompter<IInstallComponent> consoleList;
	/** Expansion bar */
	protected ExpandBar componentsBar;
	/** Required components expansion item */
	protected ExpandItem requiredList;
	/** Required components panel */
	protected ComponentsPanel requiredComponentsPanel;
	/** Optional components expansion item */
	protected ExpandItem optionalList;
	/** Optional components panel */
	protected ComponentsPanel optionalComponentsPanel;
	/** Title font */
	protected Font titleFont;
	/** Dimmed color #1 */
	protected Color dimColor1;
	/** Dimmed color #2 */
	protected Color dimColor2;
	/** Button area */
	protected Composite buttonArea;
	/** Select all button */
	protected Button selectAllButton;
	/** Deselect all button */
	protected Button deselectAllButton;
	/** Components scrolled container */
	protected ScrolledComposite componentContainer;
	/** Label to display the install size */
	protected Label installSizeLabel;
	/** Status area containing the install size label and progress bar */
	protected Composite statusArea;
	/** Progress bar for the size calculation */
	protected SpinnerProgress progressBar;
	/** Last calculated install size */
	protected long installSize;
	/** Install description */
	private IInstallDescription installDescription;
	
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 * @param installDescription Install description
	 */
	@SuppressWarnings("synthetic-access")
	public ComponentsPage(String pageName, String title, IInstallDescription installDescription) {
		super(pageName, title);
		this.installDescription = installDescription;
	}
	
	/**
	 * Returns if required components should be sorted.
	 * 
	 * @return <code>true</code> to sort required components
	 */
	public boolean getSortRequiredComponents() {
		return installDescription.getSortRequiredComponents();
	}
	
	/**
	 * Returns if optional components should be sorted.
	 * 
	 * @return <code>true</code> to sort optional components
	 */
	public boolean getSortOptionalComponents() {
		return installDescription.getSortOptionalComponents();
	}
	
	/**
	 * Returns if components version should be displayed.
	 * 
	 * @return <code>true</code> to display components version
	 */
	public boolean getHideComponentsVersion() {
		return installDescription.getHideComponentsVersion();
	}
	
	@Override
	public void dispose() {
		RepositoryManager.getDefault().removeRepositoryListener(this);
		
		if (titleFont != null) {
			titleFont.dispose();
		}
		if (dimColor1 != null) {
			dimColor1.dispose();
		}
		if (dimColor2 != null) {
			dimColor2.dispose();
		}
		
		super.dispose();
	}

	/**
	 * Returns the selected components.
	 * 
	 * @return Selected components
	 */
	protected IInstallComponent[] getSelectedComponents() {
		return selectedComponents.toArray(new IInstallComponent[selectedComponents.size()]);
	}
	
	/**
	 * Returns the selected component names.
	 * 
	 * @return Selected component names
	 */
	public String[] getSelectedComponentNames() {
		IInstallComponent[] components = getSelectedComponents();
		String[] names = new String[components.length];
		for (int index = 0; index < names.length; index ++) {
			names[index] = components[index].getName();
		}
		
		return names;
	}

	/**
	 * Returns the title font.
	 * 
	 * @return Title font
	 */
	private Font getTitleFont() {
		return titleFont;
	}
	
	/**
	 * Returns the first dimmed color.
	 * 
	 * @return Dimmed color
	 */
	private Color getDimColor1() {
		return dimColor1;
	}
	
	/**
	 * Returns the second dimmed color.
	 * 
	 * @return Dimmed color
	 */
	private Color getDimColor2() {
		return dimColor2;
	}
	
	@SuppressWarnings("synthetic-access")
	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayout areaLayout = new GridLayout();
		areaLayout.marginHeight = 0;
		areaLayout.marginWidth = 0;
		areaLayout.verticalSpacing = 0;
		area.setLayout(areaLayout);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Create bold font
		FontData[] fd = getFont().getFontData();
		fd[0].setStyle(SWT.BOLD);
		titleFont = new Font(getShell().getDisplay(), fd[0]);
		
		// Create dimmed colors
		RGB dimRGB = blendRGB(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB(), 
				getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
				45);
		dimColor1 = new Color(getShell().getDisplay(), dimRGB);
		dimRGB = blendRGB(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(), 
				getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB(), 
				75);
		dimColor2 = new Color(getShell().getDisplay(), dimRGB);
		
		// Message label
		messageLabel = new Label(area, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));

		// Scrolled composite container
		componentContainer = new ScrolledComposite(area, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		componentContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		componentContainer.setExpandHorizontal(true);
		componentContainer.setExpandVertical(true);
		
		// Create the components bar
		componentsBar = new ExpandBar(componentContainer, SWT.NONE );
		componentsBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		componentsBar.setSpacing(COMPONENTS_MARGIN);
		componentsBar.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		componentsBar.setBackgroundMode(SWT.INHERIT_DEFAULT);
		componentContainer.setContent(componentsBar);
		
		// Create required components section
		requiredList = new ExpandItem(componentsBar, SWT.NONE);
		requiredList.setText(InstallMessages.ComponentsPage_Required );
		requiredList.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.COMPONENT));
		// Create required components panel
		requiredComponentsPanel = new ComponentsPanel(componentsBar);
		requiredList.setControl(requiredComponentsPanel);
		
		// Create optional components section
		optionalList = new ExpandItem(componentsBar, SWT.NONE);
		optionalList.setText(InstallMessages.ComponentsPage_Optional);
		optionalList.setImage(Installer.getDefault().getImageRegistry().get(IInstallerImages.COMPONENT_OPTIONAL));
		// Create optional components panel
		optionalComponentsPanel = new ComponentsPanel(componentsBar);
		optionalList.setControl(optionalComponentsPanel);
		optionalComponentsPanel.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedComponents.clear();
				// Add required units
				for (IInstallComponent component : RepositoryManager.getDefault().getInstallComponents()) {
					if (!component.isOptional()) {
						selectedComponents.add(component);
					}
				}
				// Add selected optional units
				IStructuredSelection ssel = (IStructuredSelection)event.getSelection();
				Iterator<?> iter = ssel.iterator();
				while (iter.hasNext()) {
					selectedComponents.add((IInstallComponent)iter.next());
				}
				
				// Update install space for new component selection
				updateInstallSpace();
				// Update button state
				updateButtons();
				// Validate selection
				validate();
			}
		});
		
		// Button area
		buttonArea = new Composite(area, SWT.NONE);
		buttonArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		buttonArea.setLayout(new GridLayout(2, false));
		// Select all optional button
		selectAllButton = new Button(buttonArea, SWT.PUSH);
		selectAllButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		selectAllButton.setText(InstallMessages.ComponentsPage_SelectAllOptional);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAllOptional(true);
			}
		});
		// Deselect all optional button
		deselectAllButton = new Button(buttonArea, SWT.PUSH);
		deselectAllButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		deselectAllButton.setText(InstallMessages.ComponentsPage_DeselectAllOptional);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAllOptional(false);
			}
		});
		
		statusArea = new Composite(area, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		statusArea.setLayout(gl);
		statusArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		progressBar = new SpinnerProgress(statusArea, SWT.NONE);
		progressBar.setText(InstallMessages.ComponentsPage_ComputingSize);
		progressBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 1, 1));
		installSizeLabel = new Label(statusArea, SWT.NONE);
		installSizeLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 1, 1));
		
		setProgressVisible(false);
		
		// Listen to component changes
		RepositoryManager.getDefault().addRepositoryListener(this);

		// Update page
		updatePage();
		
		return area;
	}
	
	/**
	 * Selects/deselects all optional components.
	 * 
	 * @param select <code>true</code> to select all, <code>false</code> to
	 * deselect all
	 */
	private void selectAllOptional(boolean select) {
		if (optionalList != null) {
			ComponentsPanel panel = (ComponentsPanel)optionalList.getControl();
			// Select all
			if (select) {
				panel.setSelection(new StructuredSelection(panel.getComponents()));
			}
			// Deselect all
			else {
				panel.setSelection(new StructuredSelection(new IInstallComponent[0]));
			}
		}
	}

	/**
	 * Updates the page.
	 */
	private void updatePage() {
		// Width
		int minWidth = 0;
		// Height
		int minHeight = 0;
		
		// Components have been loaded
		if (!RepositoryManager.getDefault().isLoading()) {
			hideBusy();

			IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents();
			selectedComponents.clear();
			for (IInstallComponent component : components) {
				if (component.getInstall()) {
					selectedComponents.add(component);
				}
			}

			setPageComplete(true);
			messageLabel.setText(InstallMessages.ComponentsPage_ComponentsLabel);

			// Add required and optional components
			ArrayList<IInstallComponent> requiredComponents = new ArrayList<IInstallComponent>();
			ArrayList<IInstallComponent> optionalComponents = new ArrayList<IInstallComponent>();
			for (IInstallComponent component : components) {
				if (component.isOptional()) {
					optionalComponents.add(component);
				}
				else {
					requiredComponents.add(component);
				}
			}
			
			// Sort components by name
			Comparator<IInstallComponent> componentComparator = new Comparator<IInstallComponent>() {
				@Override
				public int compare(IInstallComponent arg0, IInstallComponent arg1) {
					return arg0.getName().compareTo(arg1.getName());
				}
				
			};
			// Sort required components for display
			if (getSortRequiredComponents()) {
				Collections.sort(requiredComponents, componentComparator);
			}
			// Sort optional components for display
			if (getSortOptionalComponents()) {
				Collections.sort(optionalComponents, componentComparator);
			}
			
			// Required components
			requiredComponentsPanel.setComponents(requiredComponents.toArray(new IInstallComponent[requiredComponents.size()]), false, getHideComponentsVersion());
			Point requiredSize = requiredComponentsPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			requiredList.setHeight(requiredSize.y);
			minWidth = requiredSize.x;
			// Expand the section
			requiredList.setExpanded(true);

			// Optional components
			optionalComponentsPanel.setComponents(optionalComponents.toArray(new IInstallComponent[optionalComponents.size()]), true, getHideComponentsVersion());
			Point optionalSize = optionalComponentsPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			optionalList.setHeight(optionalSize.y);
			if (optionalSize.x > minWidth)
				minWidth = optionalSize.x;
			// Expand the section
			optionalList.setExpanded(true);

			// Layout the components bar parent before computing sizes
			componentsBar.getParent().layout(true);
			// Compute minimum height to display all items
			// Note: Do not compute the size of componentsBar for minimum height.
			// Although, this works on Windows, it returns incorrect value on
			// GTK.  
			// It is possible for ExpandBar.getHeaderHeight() to return
			// a negative value.
			minHeight = requiredSize.y + COMPONENT_HEADER_HEIGHT + componentsBar.getSpacing() +
					optionalSize.y + COMPONENT_HEADER_HEIGHT + componentsBar.getSpacing();
		}
		// Components not yet loaded
		else {
			setPageComplete(false);
			showBusy(InstallMessages.ComponentsPage_LoadingInstallInformation);
		}
		
		updateInstallSpace();
		
		// Update buttons
		updateButtons();

		// Update the scrolled container size
		componentContainer.setMinHeight(minHeight);
		componentContainer.setMinWidth(minWidth);
	}

	/**
	 * Sets the visibility of the progress bar.
	 * @param visible
	 */
	private void setProgressVisible(final boolean visible) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				progressBar.setVisible(visible);
				progressBar.setProgress(visible);
				int dimensionsProgress = (visible) ? SWT.DEFAULT : 0;
				int dimensionsLabel = (visible) ? 0 : SWT.DEFAULT;
				((GridData)progressBar.getLayoutData()).widthHint = dimensionsProgress;
				((GridData)progressBar.getLayoutData()).heightHint = dimensionsProgress;
				
				installSizeLabel.setVisible(!visible);
				((GridData)installSizeLabel.getLayoutData()).widthHint = dimensionsLabel;
				((GridData)installSizeLabel.getLayoutData()).heightHint = dimensionsLabel;
				
				statusArea.layout(true);
			}
		});
	}
	
	/**
	 * Updates the install space status area.
	 */
	private void updateInstallSpace() {
		
		// selectedComponents not yet initialized.
		if (selectedComponents.isEmpty()) {
			setStatusText("");
			return;
		}
		
		new Thread() {
			public void run() {
				RepositoryManager.getDefault().startSizeCalculation(selectedComponents.toArray(new IInstallComponent[]{}), new SizeCalculationMonitorAdapter() {
					@Override
					public void beginTask(String name, int totalWork) {
						super.beginTask(name, totalWork);
						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								setProgressVisible(true);
							}
						});
					}
					@Override
					public void done(final long installSize) {
						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								ComponentsPage.this.installSize = installSize;
								setProgressVisible(false);
								String msg = MessageFormat
										.format(InstallMessages.ComponentsPage_0,
												UIUtils.formatBytes(installSize));
								setStatusText(msg);
							}
						});
					}
				});
			};
		}.start();
	}
	
	/**
	 * Sets the install size label.
	 * @param text
	 */
	private void setStatusText(final String text) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				installSizeLabel.setText(text);
				statusArea.layout(true);
			}
		});
	}
	
	/**
	 * Updates the enabled/disable state of buttons.
	 */
	private void updateButtons() {
		if (optionalList != null) {
			buttonArea.setEnabled(true);
			
			ComponentsPanel panel = (ComponentsPanel)optionalList.getControl();
			IStructuredSelection selection = (IStructuredSelection)panel.getSelection();
			Object[] selectedElements = selection.toArray();
			selectAllButton.setEnabled(selectedComponents.size() != RepositoryManager.getDefault().getInstallComponents().length);
			deselectAllButton.setEnabled(selectedElements.length != 0);
		}
		else {
			selectAllButton.setEnabled(false);
			deselectAllButton.setEnabled(false);
		}
	}
	
	/**
	 * Component label provider
	 */
	private class ComponentLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getImage(Object element) {
			return getColumnImage(element, 0);
		}

		@Override
		public String getText(Object element) {
			return getColumnText(element, 0);
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			Image image = null;
			
			if (element instanceof IInstallComponent) {
				IInstallComponent component = (IInstallComponent)element;
				if (columnIndex == 0) {
					// Add-on component
					if (component.isAddon()) {
						image = Installer.getDefault().getImageRegistry().get(IInstallerImages.COMP_ADDON_OVERLAY);
					}
					// Optional component
					else if (component.isOptional()) {
						image = Installer.getDefault().getImageRegistry().get(IInstallerImages.COMP_OPTIONAL_OVERLAY);
					}
					// Required component
					else {
						image = Installer.getDefault().getImageRegistry().get(IInstallerImages.COMP_REQUIRED_OVERLAY);
					}
				}
			}
			
			return image;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			String text = null;
			
			if (element instanceof IInstallComponent) {
				IInstallComponent component = (IInstallComponent)element;
				// Name column
				if (columnIndex == COLUMN_NAME) {
					text = component.getName();
				}
				// Version column
				else if (columnIndex == COLUMN_VERSION) {
					text = component.getInstallUnit().getVersion().toString();
				}
			}
			
			return text;
		}
	}

	@Override
	public String getInstallSummary() {
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(InstallMessages.SummaryPage_ComponentsLabel);
		String[] componentNames = getSelectedComponentNames();
		Arrays.sort(componentNames);
		for (String name : componentNames) {
			buffer.append("\n\t");
			buffer.append(name);
		}

		return buffer.toString() + "\n\n";
	}

	/**
	 * Returns a list of IVersionedId objects from the IInstallComponent list.
	 * @param components
	 * @return the IVersionedId list.
	 */
	protected IVersionedId[] componentsToVersionIds(IInstallComponent[] components) {
		if (components != null) {
			IVersionedId[] versions = new IVersionedId[components.length];
			for (int index = 0; index < versions.length; index ++) {
				versions[index] = new VersionedId(components[index].getInstallUnit().getId(), 
						components[index].getInstallUnit().getVersion());
			}
			
			return versions;
		}
		
		return null;
	}

	@Override
	public void saveInstallData(IInstallData data) {
		IInstallComponent[] components = getSelectedComponents();
		if (components != null) {
			IVersionedId[] versions = componentsToVersionIds(components);
			data.setProperty(IInstallConstants.PROPERTY_REQUIRED_ROOTS, versions);
		}
		
		data.setProperty(IInstallConstants.PROPERTY_INSTALL_SIZE, installSize);
	}

	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;

		// Initial response
		if (input == null) {
			// Wait for the load
			RepositoryManager.getDefault().waitForLoad();

			// Create console items list
			ComponentLabelProvider labelProvider = new ComponentLabelProvider();
			consoleList = new ConsoleListPrompter<IInstallComponent>(InstallMessages.ComponentsPage_ComponentsLabel);
			IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents();
			// Sort components by name
			Arrays.sort(components, new Comparator<IInstallComponent>() {
				@Override
				public int compare(IInstallComponent arg0,
						IInstallComponent arg1) {
					return arg0.getName().compareTo(arg1.getName());
				}
			});
			// Add components
			for (IInstallComponent component : components) {
				consoleList.addItem(labelProvider.getColumnText(component, COLUMN_NAME) + " - " + labelProvider.getColumnText(component, COLUMN_VERSION), //$NON-NLS-1$
						component,
						component.getInstall(),
						component.isOptional());
			}
		}

		// Get response
		response = consoleList.getConsoleResponse(input);
		// Set selected roots
		if (response == null) {
			consoleList.getSelectedData(selectedComponents);
			// Validate selections
			String status = getStatusMessage();
			// Selection error
			if (status != null) {
				response = MessageFormat.format("ERROR: {0}\n{1}",  //$NON-NLS-1$
						status,
						consoleList.toString());
			}
		}
		
		return response;
	}
	
	/**
	 * Blends c1 and c2 based in the provided ratio.
	 * 
	 * @param c1
	 *            first color
	 * @param c2
	 *            second color
	 * @param ratio
	 *            percentage of the first color in the blend (0-100)
	 * @return the RGB value of the blended color
	 * 
	 * copied from FormColors.java
	 */
	public RGB blendRGB(RGB c1, RGB c2, int ratio) {
		int r = blend(c1.red, c2.red, ratio);
		int g = blend(c1.green, c2.green, ratio);
		int b = blend(c1.blue, c2.blue, ratio);
		return new RGB(r, g, b);
	}

	/**
	 * Blends two color values
	 * 
	 * @param v1 First color value
	 * @param v2 Second color value
	 * @param ratio Ratio
	 * @return Blended color value
	 */
	private int blend(int v1, int v2, int ratio) {
		int b = (ratio * v1 + (100 - ratio) * v2) / 100;
		return Math.min(255, b);
	}

	/**
	 * Returns the status message for any selection error.
	 * 
	 * @return Status message or <code>null</code> if no error
	 */
	private String getStatusMessage() {
		String message = null;
		
		for (IInstallComponent selectedComponent : selectedComponents) {
			IInstallComponent[] requiredComponents = selectedComponent.getRequiredComponents();
			if (requiredComponents != null) {
				for (IInstallComponent requiredComponent : requiredComponents) {
					if (!selectedComponents.contains(requiredComponent)) {
						message = MessageFormat.format("{0} requires {1}.",  //$NON-NLS-1$
								selectedComponent.getName(),
								requiredComponent.getName());
						break;
					}
				}
			}
		}

		return message;
	}
	
	@Override
	public boolean validate() {
		String message = getStatusMessage();
		
		// Show status for problems in selection
		if (message != null) {
			IStatus status = new Status(IStatus.ERROR, Installer.ID, message);
			showStatus(new IStatus[] { status });
		}
		// Hide status if no problems
		else {
			hideStatus();
		}
		setPageComplete(message == null);
		
		return (message == null);
	}

	@Override
	public void repositoryStatus(final RepositoryStatus status) {
		getShell().getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				// Repositories loading
				if (status == IInstallRepositoryListener.RepositoryStatus.loadingStarted) {
					showBusy(InstallMessages.ComponentsPage_LoadingInstallInformation);
					setPageComplete(false);
				}
				// Repositories loaded
				else if (status == IInstallRepositoryListener.RepositoryStatus.loadingCompleted) {
					hideBusy();
					// Select all by default
					selectedComponents = new ArrayList<IInstallComponent>(Arrays.asList(RepositoryManager.getDefault().getInstallComponents()));
					updatePage();
					validate();
				}
			}
		});
	}

	@Override
	public void repositoryLoaded(URI location, IInstallComponent[] components) {
	}

	@Override
	public void repositoryError(URI location, String errorMessage) {
	}

	/**
	 * A panel that displays the name, version, and description for list of
	 * components.
	 */
	private class ComponentsPanel extends Composite implements ISelectionProvider {
		/** Components for panel */
		private IInstallComponent[] components;
		/** Selection listeners */
		private ListenerList selectionListeners = new ListenerList();
		/** Items */
		private ArrayList<Composite> items = new ArrayList<Composite>();
		/** Title buttons */
		private ArrayList<Button> titleButtons = new ArrayList<Button>();
		
		/**
		 * Constructor
		 * 
		 * @param parent Parent
		 */
		public ComponentsPanel(Composite parent) {
			super(parent, SWT.NONE);
			
			// Set colors
			Display display = getShell().getDisplay();
			setBackground(display.getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
			
			// Layout
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 2;
			layout.marginWidth  = 2;
			layout.verticalSpacing = 0;
			setLayout(layout);
		}

		/**
		 * Sets the components for the panel.
		 * 
		 * @param components Components
		 * @param optional <code>true</code> if components are optional
		 */
		public void setComponents(IInstallComponent[] components, boolean optional, boolean hideVersion) {
			this.components = components;
			
			for (Composite item : items) {
				item.dispose();
			}
			items.clear();
			titleButtons.clear();
			
			ComponentLabelProvider labelProvider = new ComponentLabelProvider();
			
			// Create items for each component
			for (IInstallComponent component : components) {
				// Component item area
				// |-name-|-version|
				// |--description--|
				Composite itemArea = new Composite(this, SWT.NONE);
				itemArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				GridLayout itemLayout = new GridLayout(1, false);
				itemLayout.marginWidth = 0;
				itemLayout.marginHeight = 2;
				itemLayout.verticalSpacing = 0;
				itemArea.setLayout(itemLayout);
				itemArea.setBackground(getBackground());
				items.add(itemArea);

				// Title area
				Composite titleArea = new Composite(itemArea, SWT.NONE);
				GridLayout titleLayout = new GridLayout(3, false);
				titleLayout.marginWidth = 0;
				titleLayout.marginHeight = 0;
				titleArea.setLayout(titleLayout);
				titleArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				titleArea.setBackground(getBackground());
				
				Label icon = new Label(titleArea, SWT.NONE);
				icon.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
				icon.setImage(labelProvider.getImage(component));
				
				String componentName = labelProvider.getColumnText(component, COLUMN_NAME);
				// If the components is optional, create a check-box so
				// it can be selected/unselected.
				if (optional) {
					final Button titleButton = new Button(titleArea, SWT.CHECK);
					titleButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));
					titleButton.setText(componentName);
					titleButton.setFont(getTitleFont());
					titleButton.setSelection(component.getInstall());
					titleButton.setData(component);
					titleButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							// Update listeners
							fireSelectionChanged();
						}
					});
					titleButton.setBackground(getBackground());
					titleButtons.add(titleButton);
				}
				// Required component
				else {
					Label titleLabel = new Label(titleArea, SWT.NONE);
					titleLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1));
					titleLabel.setText(componentName);
					titleLabel.setFont(getTitleFont());
					titleLabel.setBackground(getBackground());
					titleLabel.setForeground(getForeground());
				}
				
				// Component version
				if (!hideVersion) {
					Label versionLabel = new Label(titleArea, SWT.NONE);
					versionLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 1, 1));
					versionLabel.setText(component.getInstallUnit().getVersion().toString());
					versionLabel.setForeground(getDimColor2());
					versionLabel.setBackground(getBackground());
				}
				
				// Component description
				String description = component.getDescription();
				if (description != null) {
					Text descriptionText = new Text(itemArea, SWT.MULTI | SWT.WRAP );
					descriptionText.setEditable(false);
					GridData descriptionData = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
					descriptionData.horizontalIndent = COMPONENT_DESCRIPTION_INDENT;
					descriptionText.setLayoutData(descriptionData);
					descriptionText.setText(description);
					descriptionText.setForeground(getDimColor1());
					descriptionText.setBackground(getBackground());
				}
			}
			
			layout(true);
		}

		/**
		 * Fires a selection changed event to all listeners.
		 */
		private void fireSelectionChanged() {
			Object[] listeners = selectionListeners.getListeners();
			SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
			for (Object listener : listeners) {
				try {
					((ISelectionChangedListener)listener).selectionChanged(event);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * Returns the components.
		 * 
		 * @return Components
		 */
		public IInstallComponent[] getComponents() {
			return components;
		}

		@Override
		public void addSelectionChangedListener(
				ISelectionChangedListener listener) {
			selectionListeners.add(listener);
		}

		@Override
		public ISelection getSelection() {
			ArrayList<IInstallComponent> items = new ArrayList<IInstallComponent>();
			for (Button titleButton : titleButtons) {
				if (titleButton.getSelection()) {
					items.add((IInstallComponent)titleButton.getData());
				}
			}
			
			return new StructuredSelection(items.toArray(new IInstallComponent[items.size()]));
		}

		@Override
		public void removeSelectionChangedListener(
				ISelectionChangedListener listener) {
			selectionListeners.remove(listener);
		}

		@Override
		public void setSelection(ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				selectedComponents.clear();
				for (Button titleButton : titleButtons) {
					IInstallComponent component = (IInstallComponent)titleButton.getData();
					boolean selected = false;
					IStructuredSelection ssel = (IStructuredSelection)selection;
					Iterator<?> iter = ssel.iterator();
					while (iter.hasNext()) {
						if (component.equals(iter.next())) {
							selected = true;
							break;
						}
					}
					titleButton.setSelection(selected);
				}
				fireSelectionChanged();
			}
		}
	}
}
