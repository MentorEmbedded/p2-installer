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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.codesourcery.installer.IInstallComponent;
import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallConstraint;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallValues;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleListPrompter;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.IInstallSummaryProvider;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.IInstallPlan;
import com.codesourcery.internal.installer.IInstallRepositoryListener;
import com.codesourcery.internal.installer.IInstallerImages;
import com.codesourcery.internal.installer.InstallManager;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.RepositoryManager;
import com.codesourcery.internal.installer.ui.DetailTree;
import com.codesourcery.internal.installer.ui.DetailTree.ImageType;
import com.codesourcery.internal.installer.ui.DetailTreeItem;
import com.codesourcery.internal.installer.ui.SpinnerProgress;
import com.codesourcery.internal.installer.ui.UIUtils;

/**
 * Page to show components for the install.  Optional components can be selected.
 * This page supports console.
 */
public class ComponentsPage extends InstallWizardPage implements IInstallSummaryProvider, IInstallConsoleProvider, IInstallRepositoryListener, ICheckStateListener {
	/** Component name column */
	private static final int COLUMN_NAME = 0;
	/** Component version column */
	private static final int COLUMN_VERSION = 1;
	
	/** Message label */
	protected Label messageLabel;
	/** Console list */
	protected ConsoleListPrompter<IInstallComponent> consoleList;
	/** Description font */
	protected Font descriptionFont;
	/** Button area */
	protected Composite buttonArea;
	/** Select all button */
	protected Button selectAllButton;
	/** Deselect all button */
	protected Button deselectAllButton;
	/** Status area containing the install size label and progress bar */
	protected StatusPanel statusArea;
	/** Last calculated install size */
	//protected long installSize;
	/** Install description */
	private IInstallDescription installDescription;
	/** Job to compute install plan */
	private InstallPlanJob installPlanJob = new InstallPlanJob();
	/** Components tree */
	private DetailTree tree;
	/** <code>true</code> if update is required */
	private boolean needsUpdate = true;
	/** Status for selection problems */
	private IStatus selectionStatus;
	/** Install plan */
	private IInstallPlan installPlan;
	/** Label provider */
	private ComponentLabelProvider labelProvider;
	/** Warning console prompter */
	private ConsoleYesNoPrompter warningConsolePrompter;
	/** Last computed available space */
	private long lastAvailableSpace = -1;
	
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

		// Label provider
		labelProvider = new ComponentLabelProvider();
	}

	/**
	 * Returns the install description.
	 * 
	 * @return Install description
	 */
	private IInstallDescription getInstallDescription() {
		return installDescription;
	}
	
	/**
	 * Returns if the size status area should be shown.
	 * 
	 * @return <code>true</code> if size status area should be shown
	 */
	private boolean getShowSizeStatus() {
		return (getInstallDescription().getInstallSizeFormat() != null);
	}
	
	/**
	 * Returns the components label provider.
	 * 
	 * @return Label provider
	 */
	private ComponentLabelProvider getLabelProvider() {
		return labelProvider;
	}
	
	@Override
	public void dispose() {
		RepositoryManager.getDefault().removeRepositoryListener(this);
		
		if (descriptionFont != null) {
			descriptionFont.dispose();
		}
		
		super.dispose();
	}

	@SuppressWarnings("synthetic-access")
	@Override
	public Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayout areaLayout = new GridLayout();
		areaLayout.verticalSpacing = 0;
		areaLayout.marginWidth = 0;
		areaLayout.marginHeight = 0;
		area.setLayout(areaLayout);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		// Create bold font
		FontData[] fd = getFont().getFontData();
		fd[0].setHeight(fd[0].getHeight() - 1);
		descriptionFont = new Font(getShell().getDisplay(), fd[0]);
		
		// Message label
		messageLabel = new Label(area, SWT.WRAP);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 1, 1));

		// Components tree
		tree = new DetailTree(area, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP | SWT.DOUBLE_BUFFERED);
		tree.setFont(getFont());
		tree.setDescriptionFont(descriptionFont);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		// Set images
		tree.setImage(ImageType.COLLAPSED, Installer.getDefault().getImageRegistry().get(IInstallerImages.TREE_COLLAPSE));
		tree.setImage(ImageType.EXPANDED, Installer.getDefault().getImageRegistry().get(IInstallerImages.TREE_EXPAND));
		tree.setImage(ImageType.UNCHECKED, Installer.getDefault().getImageRegistry().get(IInstallerImages.TREE_UNCHECKED));
		tree.setImage(ImageType.CHECKED, Installer.getDefault().getImageRegistry().get(IInstallerImages.TREE_CHECKED));
		tree.setImage(ImageType.NOCHECK, Installer.getDefault().getImageRegistry().get(IInstallerImages.TREE_NOCHECK));
		
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
		
		// Status area
		if (getShowSizeStatus()) {
			statusArea = new StatusPanel(area, SWT.NONE);
			statusArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		}
		
		// Listen to component changes
		RepositoryManager.getDefault().addRepositoryListener(this);

		// Update page
		updatePage();
		
		return area;
	}
	
	/**
	 * Returns the components tree.
	 * 
	 * @return Tree
	 */
	private DetailTree getTree() {
		return tree;
	}
	
	/**
	 * Selects/deselects all optional components.
	 * 
	 * @param select <code>true</code> to select all, <code>false</code> to
	 * deselect all
	 */
	private void selectAllOptional(boolean select) {
		getTree().setAllChecked(select);
		updateButtons();
		updateInstallPlan();
		validateSelection(null);
}

	/**
	 * Returns the components title.
	 * 
	 * @return Title text
	 */
	private String getComponentsTitle() {
		return Installer.getDefault().getInstallManager().getInstallMode().isUpdate() ?
				InstallMessages.ComponentsPage_ComponentsLabelUpdate:
				InstallMessages.ComponentsPage_ComponentsLabelInstall;	
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		// Update page in case component attributes changed
		if (visible) {
			updatePage();
			// Update install plan for new components
			updateInstallPlan();
		}
	}

	/**
	 * Adds a new component.
	 * 
	 * @param component Component to add
	 * @param parent Parent item or <code>null</code>
	 */
	private void addComponent(IInstallComponent component, DetailTreeItem parent) {
		// If component is included
		if (component.isIncluded()) {
			// Create item, optional components get a check-box
			DetailTreeItem item;
			// Member of other item
			if (parent == null) {
				item = new DetailTreeItem(getTree(), component.isOptional() ? SWT.CHECK : SWT.NONE);
			}
			// Root item
			else {
				item = new DetailTreeItem(parent, component.isOptional() ? SWT.CHECK : SWT.NONE);
			}
			item.setData(component);
			item.setText(getLabelProvider().getText(component));
			String description = component.getDescription();
			if (description != null) {
				item.setDescription(component.getDescription());
			}			
			
			// Set component installed
			item.setChecked(component.getInstall());
			
			// Component group
			if (component.hasMembers()) {
				IInstallComponent[] groupComponents = component.getMembers();
				for (IInstallComponent groupComponent : groupComponents) {
					addComponent(groupComponent, item);
				}
			}
		}
	}

	/**
	 * Sets the page to update the next time it is displayed.
	 * 
	 * @param needsUpdate <code>true</code> to update page
	 */
	private void setUpdatePage(boolean needsUpdate) {
		this.needsUpdate = needsUpdate;
	}
	
	/**
	 * Returns if the page should update the next time it is displayed.
	 * 
	 * @return <code>true</code> if the page will be updated
	 */
	private boolean getUpdatePage() {
		return needsUpdate;
	}

	/**
	 * Adds components to the page.
	 */
	private void addComponents() {
		// Remove existing components
		getTree().removeAll();

		// First set of components to show
		ArrayList<IInstallComponent> first = new ArrayList<IInstallComponent>();
		// Second set of components to show
		ArrayList<IInstallComponent> second = new ArrayList<IInstallComponent>();

		// Add components.  By default, order the required components first
		IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents(true);
		for (IInstallComponent component : components) {
			if (component.isOptional()) {
				second.add(component);
			}
			else {
				first.add(component);
			}
		}
		// Option to order optional components firs is set
		if (getInstallDescription().getShowOptionalComponentsFirst()) {
			ArrayList<IInstallComponent> swap = first;
			first = second;
			second = swap;
		}
		// Add first set of components
		for (IInstallComponent component : first) {
			addComponent(component, null);
		}
		// Add second set of components
		for (IInstallComponent component : second) {
			addComponent(component, null);
		}
	}
	
	/**
	 * Updates the page.
	 */
	private void updatePage() {
		// No update required
		if (!getUpdatePage())
			return;
		setUpdatePage(false);
		
		setPageComplete(true);
		messageLabel.setText(getComponentsTitle());

		// Stop listening for checked events
		getTree().removeCheckStateListener(this);

		// Add components
		addComponents();
		
		// Set expanded components
		IVersionedId[] expandedRoots = getInstallDescription().getWizardExpandedRoots();
		if ((expandedRoots != null) && (expandedRoots.length != 0)) {
			DetailTreeItem[] items = getTree().getAllItems();
			for (DetailTreeItem item : items) {
				if (item.hasChildren()) {
					IInstallComponent component = (IInstallComponent)item.getData();
					for (IVersionedId expandedRoot : expandedRoots) {
						if (component.getInstallUnit().getId().equals(expandedRoot.getId())) {
							item.expandAll();
							break;
						}
					}
				}
			}
		}
		
		// Listen for checked events
		getTree().addCheckStateListener(this);
		// Update buttons
		updateButtons();
	}
	
	@Override
	protected void autoUpdate() {
		if (installPlan != null) {
			long availableSpace = installPlan.getAvailableSpace();
			// If available space changed
			if (availableSpace != lastAvailableSpace) {
				lastAvailableSpace = availableSpace;
				// Update to clear any warning for insufficient installation space
				updateStatus();
				// Update size status to reflect changes in available space
				updateSizeStatus();
			}
		}
	}

	/**
	 * Updates the install plan.
	 */
	private void updateInstallPlan() {
		// Cancel current job if running
		if (installPlanJob.getState() != Job.NONE) {
			installPlanJob.cancel();
		}
		// Schedule plan job
		installPlanJob.schedule();
	}
	
	/**
	 * Updates the enabled/disable state of buttons.
	 */
	private void updateButtons() {
		boolean allSelected = true;
		boolean oneSelected = false;
		
		DetailTreeItem[] items = getTree().getAllItems();
		for (DetailTreeItem item : items) {
			IInstallComponent component = (IInstallComponent)item.getData();
			if (component.isOptional()) {
				if (item.isChecked()) {
					oneSelected = true;
				}
				else {
					allSelected = false;
				}
			}
		}

		// No items
		if (items.length == 0) {
			selectAllButton.setEnabled(false);
			deselectAllButton.setEnabled(false);
		}
		// Select all is enabled if all are not selected
		// Deselect all is enabled if at least one is selected
		else {
			selectAllButton.setEnabled(!allSelected);
			deselectAllButton.setEnabled(oneSelected);
		}
	}

	@Override
	public String getInstallSummary() {
		StringBuffer output = new StringBuffer();
		output.append(InstallMessages.ComponentsPage_Summary_Installed);
		output.append('\n');
		consoleList = getConsolePrompter();
		consoleList.setCheckedString(
				"[" + InstallMessages.Yes + "]", 
				"[" + InstallMessages.No +" ]");
		output.append(consoleList.toString(false, false, false));
		output.append('\n');
		
		return output.toString();
	}

	/**
	 * Sets the component install states from the checked items.
	 */
	private void saveInstallState() {
		if (!isConsoleMode()) {
			DetailTreeItem[] items = getTree().getAllItems();
			for (DetailTreeItem item : items) {
				IInstallComponent component = (IInstallComponent)item.getData();
				component.setInstall(item.isChecked());
			}
		}
	}

	/**
	 * Sets the checked items according to component install states.
	 */
	private void updateInstallState() {
		if (!isConsoleMode()) {
			DetailTreeItem[] items = getTree().getAllItems();
			for (DetailTreeItem item : items) {
				IInstallComponent component = (IInstallComponent)item.getData();
				item.setChecked(component.getInstall());
			}
		}
	}
	
	@Override
	public void saveInstallData(IInstallData data) throws CoreException {
		// Save install states
		saveInstallState();
		
		// Save install size
		if (installPlan != null) {
			data.setProperty(IInstallValues.INSTALL_SIZE, Long.toString(installPlan.getSize()));
		}
	}
	
	/**
	 * Traverses install components and member components and adds them to a list.
	 * 
	 * @param items List for items
	 * @param component Component to add
	 */
	private void addConsoleItem(ArrayList<IInstallComponent> items, IInstallComponent component) {
		// Do not add excluded components
		if (!component.isIncluded())
			return;
		
		items.add(component);
		if (component.hasMembers()) {
			for (IInstallComponent member : component.getMembers()) {
				addConsoleItem(items, member);
			}
		}
	}

	/**
	 * Returns the console name for an install component.
	 * 
	 * @param component Install component
	 * @return Console name
	 */
	private String getConsoleName(IInstallComponent component) {
		StringBuffer name = new StringBuffer();
		// Indent for parents
		IInstallComponent parent = component.getParent();
		while (parent != null) {
			name.append("  ");
			parent = parent.getParent();
		}
		// Append component information
		name.append(getLabelProvider().getColumnText(component, COLUMN_NAME));
		return name.toString();
	}

	/**
	 * Returns the console prompter.
	 * 
	 * @return Console prompter
	 */
	private ConsoleListPrompter<IInstallComponent> getConsolePrompter() {
		ConsoleListPrompter<IInstallComponent> prompter = new ConsoleListPrompter<IInstallComponent>(getComponentsTitle());
		
		// Add console items
		ArrayList<IInstallComponent> items = new ArrayList<IInstallComponent>();
		IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents(true);
		for (IInstallComponent component : components) {
			addConsoleItem(items, component);
		}
		
		// Add items
		for (IInstallComponent item : items) {
			// Item is member of other item
			if (item.getParent() != null) {
				int parentIndex = prompter.getItemIndex(item.getParent());
				prompter.addItem(parentIndex, getConsoleName(item), item, item.getInstall(),
						item.isOptional());
			}
			// Root item
			else {
				prompter.addItem(getConsoleName(item), item, item.getInstall(),
						item.isOptional());
			}
		}
		
		return prompter;
	}
	
	@Override
	public String getConsoleResponse(String input)
			throws IllegalArgumentException {
		String response = null;

		// Warning prompter active
		if (warningConsolePrompter != null) {
			response = warningConsolePrompter.getConsoleResponse(input);
			// Continue
			if (warningConsolePrompter.getResult()) {
				return null;
			}
			// Do not continue
			else {
				warningConsolePrompter = null;
				input = null;
			}
		}
		
		// Initial response
		if (input == null) {
			// Create console items list
			consoleList = getConsolePrompter();
		}

		// Get response
		response = consoleList.getConsoleResponse(input);
		
		// Save install selections
		ArrayList<IInstallComponent> selectedComponents = new ArrayList<IInstallComponent>();
		consoleList.getSelectedData(selectedComponents);
		
		//Traverse through all install components and mark only selected components for installation.
		IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents(false);
		for (IInstallComponent component : components) {
			if (component.isOptional()) {
				component.setInstall(false);
				if (selectedComponents.contains(component))
					component.setInstall(true);
			}
		}

		// Report any selection error
		String status = validateConstraints();
		if (status != null) {
			System.out.println(status);
		}

		// Validate result
		if (response == null) {
			// Compute plan
			final IInstallPlan plan = RepositoryManager.getDefault().computeInstallPlan(null);
			
			if (plan != null) {
				// Error in plan
				if (plan.getStatus().getSeverity() == IStatus.ERROR) {
					response = MessageFormat.format("ERROR: {0}\n{1}", plan.getErrorMessage(), consoleList.toString());
				}
				// Show warnings and/or information and prompt to continue
				else if (plan.getStatus().getSeverity() == IStatus.WARNING) {
					String message = plan.getErrorMessage();
					if (message != null) {
						warningConsolePrompter = new ConsoleYesNoPrompter(plan.getErrorMessage(), 
								InstallMessages.Continue, true);
						response = warningConsolePrompter.getConsoleResponse(null);
					}
				}
			}
		}
		
		// Validate selections
		if (response == null) {
			status = validateConstraints();
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
	 * Updates the page status.
	 */
	private void updateStatus() {
		ArrayList<IStatus> status = new ArrayList<IStatus>();
		// Selection status
		if (selectionStatus != null) {
			status.add(selectionStatus);
		}
		// Provision status
		if (installPlan != null) {
			IStatus installStatus = installPlan.getStatus();
			if ((installStatus != null) && !installStatus.isOK()) {
				status.add(new Status(
						installStatus.getSeverity(), 
						installStatus.getPlugin(), 
						installPlan.getErrorMessage(), 
						installStatus.getException()));
			}
		}
		
		// Checker status
		InstallManager manager = (InstallManager)Installer.getDefault().getInstallManager();
		IStatus[] checkerStatus = manager.verifyInstallComponentSelection(getCheckedComponents());
		for (IStatus s : checkerStatus) {
			status.add(s);
		}
		
		IStatus[] totalStatus = status.toArray(new IStatus[status.size()]);

		// Set status
		setStatus((totalStatus.length > 0) ? totalStatus : null);
		if (!isConsoleMode()) {
			if (totalStatus.length > 0) {
				showStatus(totalStatus);
			}
			else {
				hideStatus();
			}
		}

		// Set complete if no errors
		setPageComplete(!hasStatusError());
	}
	
	/**
	 * Formats a comma delimited components list.
	 * 
	 * @param components Components
	 * @return Components list
	 */
	private String formatComponentList(IInstallComponent[] components) {
		StringBuffer names = new StringBuffer();
		for (IInstallComponent component : components) {
			if (names.length() > 0) {
				names.append(", ");
			}
			names.append(getLabelProvider().getText(component));
		}
		
		return names.toString();
	}

	/**
	 * Returns the currently checked install components.
	 * 
	 * @return Install components
	 */
	private IInstallComponent[] getCheckedComponents() {
		DetailTreeItem[] items = getTree().getCheckedItems();
		IInstallComponent[] components = new IInstallComponent[items.length];
		for (int index = 0; index < components.length; index ++) {
			components[index] = (IInstallComponent)items[index].getData();
		}
		
		return components;
	}

	/**
	 * Returns the install components.
	 * 
	 * @return Install components
	 */
	private IInstallComponent[] getInstallComponents() {
		ArrayList<IInstallComponent> toInstall = new ArrayList<IInstallComponent>(); 
		IInstallComponent[] components = RepositoryManager.getDefault().getInstallComponents(false);
		for (IInstallComponent component : components) {
			if (component.getInstall() && component.isIncluded()) {
				toInstall.add(component);
			}
		}
		
		return toInstall.toArray(new IInstallComponent[toInstall.size()]);
	}

	/**
	 * Validates component constraints.
	 * 
	 * @return Constraint problem message or <code>null</code> if all constraints
	 * are met.
	 */
	public String validateConstraints() {
		saveInstallState();
		
		String error = null;
		// Get constraints
		IInstallConstraint[] constraints = getInstallDescription().getInstallConstraints();
		if (constraints != null) {
			// Get selected components
			IInstallComponent[] checkedComponents = getInstallComponents();
			// Verify constraints
			for (IInstallConstraint constraint : constraints) {
				if (!constraint.validate(checkedComponents)) {
					error = getConstraintError(constraint);
					break;
				}
			}
		}
		
		return error;
	}

	/**
	 * Sets the installed state of components.
	 * 
	 * @param components Components to set
	 * @param install <code>true</code> to install
	 */
	public void setInstallState(IInstallComponent[] components, boolean install) {
		for (IInstallComponent component : components) {
			component.setInstall(install);
			if (component.hasMembers()) {
				setInstallState(component.getMembers(), install);
			}
		}
		
		updateInstallState();
	}

	/**
	 * Attempts to handle constraint problems when a component state changes
	 * by selecting or de-selecting components.
	 * 
	 * @param item Component item that changed state
	 */
	public void handleConstraints(DetailTreeItem item) {
		// Selected components
		IInstallComponent[] checkedComponents = getCheckedComponents();

		// Install constraints
		IInstallConstraint[] constraints = getInstallDescription().getInstallConstraints();
		if (constraints != null) {
			// Check each constraint
			for (IInstallConstraint constraint : constraints) {
				// Constraint failed
				if (!constraint.validate(checkedComponents)) {
					switch(constraint.getConstraint()) {
					// One of a set of components must be selected
					case ONE_OF:
						// Nothing can be done
						break;
					// One component requires others
					case REQUIRES:
						IInstallComponent[] targets = constraint.getTargets();
						IInstallComponent component = (IInstallComponent)item.getData();
						// If component is set to install but requires other components.
						// Set the other components to install.
						if (component.equals(constraint.getSource()) || component.isMemberOf(constraint.getSource())) {
							setInstallState(targets, true);
						}
						// Component is not set to be installed, but is required by other
						// components.  Set other components to not install.
						else {
							setInstallState(new IInstallComponent[] { constraint.getSource() }, false);
						}
						break;
					// Only of of a set of components can be selected
					case ONLY_ONE:
						// Set other components to not install
						ArrayList<IInstallComponent> notInstall = new ArrayList<IInstallComponent>();
						IInstallComponent source = (IInstallComponent)item.getData();
						for (IInstallComponent target : constraint.getTargets()) {
							if (!source.equals(target) && !source.isMemberOf(target)) {
								notInstall.add(target);
							}
						}
						setInstallState(notInstall.toArray(new IInstallComponent[notInstall.size()]), false);
						break;
					}
				}
			}
		}
	}
	
	/**
	 * Returns an error message for a failed constraint.
	 * 
	 * @param constraint Constraint
	 * @return Error message
	 */
	private String getConstraintError(IInstallConstraint constraint) {
		String error = null;
		
		if (constraint != null) {
			switch (constraint.getConstraint()) {
			// One of a set of components must be selected
			case ONE_OF:
				error = NLS.bind(InstallMessages.Error_ConstraintOneRequired0, 
						formatComponentList(constraint.getTargets()));
				break;
			// One component requires others
			case REQUIRES:
				String sourceName = getLabelProvider().getText(constraint.getSource());
				error = NLS.bind(InstallMessages.Error_ConstraintRequires1, sourceName, formatComponentList(constraint.getTargets()));
				break;
			// Only one of a set of components can be selected
			case ONLY_ONE:
				error = NLS.bind(InstallMessages.Error_Constraint0, formatComponentList(constraint.getTargets()));
				break;
			}
		}
		
		return error;
	}

	/**
	 * Validates the current selection and shows a status.
	 * 
	 * @param item Tree item or <code>null</code>
	 */
	private void validateSelection(DetailTreeItem item) {
		boolean statusChanged = false;
		String error = validateConstraints();
		if (error != null) {
			// If constraints are not satisfied, attempt to correct the selection
			if (item != null) {
				handleConstraints(item);
			}
			// Check constraints again
			String recheckError = validateConstraints();
			// Constraints still have errors
			if (recheckError != null) {
				selectionStatus = new Status(IStatus.ERROR, Installer.ID, recheckError);
				statusChanged = true;
			}
			// Constraints were correct, show information that action was taken
			else {
				selectionStatus = new Status(IStatus.INFO, Installer.ID, error);
				statusChanged = true;
			}
		}
		// Clear any error status, but leave any information status to inform
		// user that some action was taken to correct the constraint.
		else if ((selectionStatus != null) && !(selectionStatus.getSeverity() == IStatus.INFO)) {
			selectionStatus = null;
			statusChanged = true;
		}
		
		// Update status
		if (statusChanged) {
			updateStatus();
		}
	}
	
	@Override
	public void checkStateChanged(CheckStateChangedEvent event) {
		DetailTreeItem item = (DetailTreeItem)event.getElement();
		IInstallComponent component = (IInstallComponent)item.getData();
		if (component.isOptional()) {
			// Updated install state
			component.setInstall(item.isChecked());
			
			// Update install space for new component selection
			updateInstallPlan();
			// Update button state
			updateButtons();
			// Validate selection
			validateSelection(item);
		}
	}

	@Override
	public boolean validate() {
		// Check constraint errors
		String constraintError = validateConstraints();
		if (constraintError != null) {
			selectionStatus = new Status(IStatus.ERROR, Installer.ID, constraintError);
		}
		else {
			selectionStatus = null;
		}
		// Update status
		updateStatus();
		
		return (selectionStatus == null);
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
					setUpdatePage(true);
					updatePage();
					validate();
				}
			}
		});
	}

	@Override
	public void installComponentsChanged() {
		// Mark the page to be updated when it is displayed
		setUpdatePage(true);
	}

	@Override
	public void repositoryError(URI location, String errorMessage) {
	}

	@Override
	public void installComponentChanged(IInstallComponent component) {
		// Mark the page to be updated when it is displayed
		setUpdatePage(true);
	}

	@Override
	public boolean isSupported() {
		IInstallMode mode = Installer.getDefault().getInstallManager().getInstallMode();
		return mode.isInstall();
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
					// Optional component
					if (component.isOptional()) {
						image = null;
					}
					// Required component
					else {
						image = null;
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
					if (getInstallDescription().getShowComponentVersions()) {
						text += " (" + component.getInstallUnit().getVersion().toString() + ")";
					}
				}
				// Version column
				else if (columnIndex == COLUMN_VERSION) {
					text = component.getInstallUnit().getVersion().toString();
				}
			}
			
			return text;
		}
	}

	/**
	 * Updates the size status text.
	 */
	private void updateSizeStatus() {
		if (getShowSizeStatus()) {
			if (installPlan != null) {
				String msg;
				if (installPlan.getSize() == -1) {
					msg = MessageFormat.format(InstallMessages.RequiredSizeFormat0, 
							UIUtils.formatBytes(installPlan.getRequiredSize()));
				}
				else {
					msg = MessageFormat.format(getInstallDescription().getInstallSizeFormat(),
							UIUtils.formatBytes(installPlan.getSize()),
							UIUtils.formatBytes(installPlan.getRequiredSize()),
							UIUtils.formatBytes(installPlan.getAvailableSpace()));
				}
				
				statusArea.setText(msg);
			}
		}
		
		// Start automatically updating so the available space is re-computed as the file system is changed
		startAutoUpdate();
	}
	
	/**
	 * A job to compute the install plan.
	 */
	private class InstallPlanJob extends Job {
		/**
		 * Constructor
		 */
		public InstallPlanJob() {
			super("InstallPlanJob");
			setSystem(true);
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// Show progress
			getShell().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					if (getShowSizeStatus()) {
						statusArea.setProgressVisible(true);
					}
				}
			});
			// Compute plan
			if (Installer.getDefault().getInstallManager().getInstallMode().isMirror()) {
				installPlan = RepositoryManager.getDefault().computeCacheSize(monitor);
			}
			else {
				installPlan = RepositoryManager.getDefault().computeInstallPlan(monitor);
			}
			
			// Hide progress and update install plan size and status
			if ((installPlan != null) && !(getShell() == null) && !getShell().isDisposed()) {
				getShell().getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						statusArea.setProgressVisible(false);
						// Update size information
						updateSizeStatus();
						updateStatus();
					}
				});
			}
			
			return Status.OK_STATUS;
		}
	}

	/**
	 * Panel to show size information.
	 */
	private class StatusPanel extends Composite {
		/** Size progress */
		private SpinnerProgress progressBar;
		/** Size label */
		private FormattedLabel installSizeLabel;

		/**
		 * Constructor
		 * 
		 * @param parent Parent for panel
		 * @param style Style flags
		 */
		public StatusPanel(Composite parent, int style) {
			super(parent, style);
			
			GridLayout layout = new GridLayout(1, true);
			layout.marginTop = 8;
			layout.marginWidth = 2;
			layout.marginBottom = layout.horizontalSpacing = layout.verticalSpacing = 0;
			setLayout(layout);

			progressBar = new SpinnerProgress(this, SWT.NONE);
			progressBar.setText(InstallMessages.ComponentsPage_ComputingSize);
			progressBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 1, 1));
			installSizeLabel = new FormattedLabel(this, SWT.NONE);
			installSizeLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 1, 1));
			
			setProgressVisible(false);
		}
		
		/**
		 * Sets the visibility of the progress bar.
		 * 
		 * @param visible <code>true</code> to set progress bar visible
		 */
		public void setProgressVisible(final boolean visible) {
			progressBar.setVisible(visible);
			progressBar.setProgress(visible);
			int dimensionsProgress = (visible) ? SWT.DEFAULT : 0;
			int dimensionsLabel = (visible) ? 0 : SWT.DEFAULT;
			((GridData)progressBar.getLayoutData()).widthHint = dimensionsProgress;
			((GridData)progressBar.getLayoutData()).heightHint = dimensionsProgress;
			
			installSizeLabel.setVisible(!visible);
			((GridData)installSizeLabel.getLayoutData()).widthHint = dimensionsLabel;
			((GridData)installSizeLabel.getLayoutData()).heightHint = dimensionsLabel;
			
			layout(true);
		}
		
		/**
		 * Sets the install size label.
		 * @param text
		 */
		public void setText(final String text) {
			installSizeLabel.setText(text);
			layout(true);
		}
	}
}
