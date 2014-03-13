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
import java.util.Iterator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.codesourcery.installer.IInstallConsoleProvider;
import com.codesourcery.installer.IInstallData;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IInstallVerifier;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.console.ConsoleYesNoPrompter;
import com.codesourcery.installer.ui.FormattedLabel;
import com.codesourcery.installer.ui.IInstallComponent;
import com.codesourcery.installer.ui.InstallWizardPage;
import com.codesourcery.internal.installer.AuthenticationService;
import com.codesourcery.internal.installer.ContributorRegistry;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.IInstallRepositoryListener;
import com.codesourcery.internal.installer.InstallManifest;
import com.codesourcery.internal.installer.InstallMessages;
import com.codesourcery.internal.installer.RepositoryManager;

/**
 * A page that retrieves add-on components from remote sites.
 */
public class AddonsPage extends InstallWizardPage implements IInstallConsoleProvider {
	/** Product name */
	private String productName;
	/** Button to skip add-ons */
	private Button skipAddonsButton;
	/** Button to get add-ons */
	private Button installAddonsButton;
	/** User name label */
	private Label usernameLabel;
	/** User name text */
	private Text usernameText;
	/** Password label */
	private Label passwordLabel;
	/** Password text */
	private Text passwordText;
	/** <code>true</code> to get add-ons */
	private boolean installAddons = true;
	/** User name for portal login */
	private String username = "";
	/** Password for portal login */
	private String password = "";
	/** Add-on repositories */
	private URI[] addonLocations;
	/** <code>true</code> if add-ons are complete */
	private boolean addonsComplete = false;
	/** Main area */
	private Composite area;
	/** Add-ons description */
	private String addonDescription;
	/** Install description */
	private IInstallDescription installDescription;
	
	/**
	 * Constructor
	 * 
	 * @param pageName Page name
	 * @param title Page title
	 */
	public AddonsPage(String pageName, String title, IInstallDescription installDescription) {
		super(pageName, title);
		
		this.installDescription = installDescription;
		this.productName = installDescription.getProductName();
		this.addonLocations = installDescription.getAddonRepositories();
		addonDescription = NLS.bind(InstallMessages.AddonsPage_Description, getProductName());
	}

	/**
	 * Sets a description to show for add-ons.  If one is not supplied, a
	 * standard message will be used.
	 * 
	 * @param addonDescription Description for add-ons
	 */
	public void setAddonsDescription(String addonDescription) {
		this.addonDescription = addonDescription;
	}
	
	/**
	 * Returns the description for add-ons.
	 * 
	 * @return Description for add-ons
	 */
	public String getAddonsDescription() {
		return addonDescription;
	}
	
	/**
	 * Returns the product name.
	 * 
	 * @return Product name
	 */
	protected String getProductName() {
		return productName;
	}
	
	/**
	 * Returns add-on repository locations.
	 * 
	 * @return Add-on locations
	 */
	protected URI[] getAddonLocations() {
		return addonLocations;
	}

	/**
	 * Returns if add-ons should be installed.
	 * 
	 * @return <code>true</code> to install add-ons
	 */
	public boolean getInstallAddons() {
		return installAddons;
	}
	
	/**
	 * Sets if add-ons should be installed.
	 * 
	 * @param installAddons <code>true</code> to install add-ons
	 */
	protected void setInstallAddons(boolean installAddons) {
		this.installAddons = installAddons;
	}

	/**
	 * Returns the user name.
	 * 
	 * @return User name
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Sets the user name.
	 * 
	 * @param username User name
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Returns the password.
	 * 
	 * @return Password
	 */
	public String getPassword() {
		return password;
	}
	
	/**
	 * Sets the password.
	 * 
	 * @param password Password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Sets add-ons complete.
	 * 
	 * @param addonsComplete <code>true</code> if add-ons are complete
	 */
	protected void setAddonsDone(boolean addonsComplete) {
		this.addonsComplete = addonsComplete;
	}
	
	/**
	 * Returns if add-ons are complete.
	 * 
	 * @return <code>true</code> if add-ons are complete
	 */
	protected boolean isAddonsDone() {
		return addonsComplete;
	}
	
	/**
	 * Returns the install description.
	 * 
	 * @return Install description
	 */
	public IInstallDescription getInstallDescription() {
		return installDescription;
	}
	
	@Override
	public Control createContents(Composite parent) {
		area = new Composite(parent, SWT.NONE);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		area.setLayout(new GridLayout(2, false));

		// Description
		FormattedLabel descriptionLabel = new FormattedLabel(area, SWT.WRAP);
		descriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		descriptionLabel.setText(getAddonsDescription());

		// Separator
		Label separator = new Label(area, SWT.NONE);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		GridData data;
		
		// Skip add-ons button
		skipAddonsButton = new Button(area, SWT.RADIO | SWT.WRAP);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		data.horizontalIndent = getDefaultIndent();
		skipAddonsButton.setLayoutData(data);
		skipAddonsButton.setText(InstallMessages.AddonsPage_SkipAddons);
		skipAddonsButton.setSelection(!getInstallAddons());
		skipAddonsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Set to skip add-ons
				setInstallAddons(false);
				// Update the page
				updatePage();
				
				validateInstallLocation();
			}
		});

		// Install add-ons button
		installAddonsButton = new Button(area, SWT.RADIO | SWT.WRAP);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		data.horizontalIndent = getDefaultIndent();
		installAddonsButton.setLayoutData(data);
		installAddonsButton.setText(InstallMessages.AddonsPage_InstallAddons0);
		installAddonsButton.setSelection(getInstallAddons());
		installAddonsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Set to install add-ons
				setInstallAddons(true);
				// Update the page
				updatePage();
				
				validateInstallLocation();
			}
		});

		if (getInstallDescription().getAddonsRequireLogin()) {
			// User name label
			usernameLabel = new Label(area, SWT.NONE);
			data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
			data.horizontalIndent = getDefaultIndent() + getDefaultIndent();
			usernameLabel.setLayoutData(data);
			usernameLabel.setText(InstallMessages.AddonsPage_Username);
			// User name entry
			usernameText = new Text(area, SWT.BORDER);
			usernameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			usernameText.setText(getUsername());
			usernameText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					setUsername(usernameText.getText());
					updatePage();
				}
			});
	
			// Password label
			passwordLabel = new Label(area, SWT.NONE);
			data = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
			data.horizontalIndent = getDefaultIndent() + getDefaultIndent();
			passwordLabel.setLayoutData(data);
			passwordLabel.setText(InstallMessages.AddonsPage_Password);
			// Password entry
			passwordText = new Text(area, SWT.BORDER | SWT.PASSWORD);
			passwordText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			passwordText.setText(getPassword());
			passwordText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					setPassword(passwordText.getText());
					updatePage();
				}
			});
			// For password, select all on focus
			passwordText.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					passwordText.selectAll();
				}
			});
		}

		// Update page initially
		updatePage();
		// Validate install location
		validateInstallLocation();
		
		return area;
	}

	/**
	 * Validates the install location.
	 */
	private void validateInstallLocation() {
		try {
			// Check if this an upgrade
			boolean upgrade = false;
			InstallManifest manifest = InstallManifest.loadManifest(installDescription);
			if (manifest != null) {
				IInstallProduct product = manifest.getProduct(installDescription.getProductId());
				upgrade = (product != null);
			}

			// If it is an upgrade and user chose not to install add-ons
			// show a warning that existing add-ons will be removed
			if (!getInstallAddons() && upgrade) {
				IPath agentLocation = installDescription.getInstallLocation().append(IInstallConstants.P2_DIRECTORY);
				if (agentLocation.toFile().exists()) {
					String message = NLS.bind(InstallMessages.AddonsPage_ExistingAddonsRemoved0, installDescription.getProductName());
					showStatus(new IStatus[] { new Status(IStatus.INFO, Installer.ID, message) });
				}
				else {
					hideStatus();
				}
			}
			else {
				hideStatus();
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
	}
	
	@Override
	public void setActive(IInstallData data) {
		super.setActive(data);
		
		validateInstallLocation();
	}

	/**
	 * Updates the page
	 */
	protected void updatePage() {
		boolean complete = true;

		if (getInstallDescription().getAddonsRequireLogin()) {
			// Enable user name and password only if
			// add-ons are selected to be installed.
			usernameLabel.setEnabled(getInstallAddons());
			usernameText.setEnabled(getInstallAddons());
			passwordLabel.setEnabled(getInstallAddons());
			passwordText.setEnabled(getInstallAddons());
	
			// Must provide user name and password if
			// add-ons are selected for install.
			if (getInstallAddons()) {
				if (getUsername().trim().isEmpty()) {
					complete = false;
				}
				else if (getPassword().trim().isEmpty()) {
					complete = false;
				}
			}
		}

		// Set page complete
		setPageComplete(complete);
	}
	
	@Override
	public void saveInstallData(IInstallData data) {
		data.setProperty(IInstallConstants.PROPERTY_INSTALL_ADDONS, new Boolean(getInstallAddons()));
	}

	/**
	 * Validates add-on credentials.
	 * 
	 * @return <code>IStatus.OK</code> if valid, otherwise error or
	 * warning status.
	 */
	private IStatus validateLogin(String username, String password) {
		IStatus status = Status.OK_STATUS;
		
		// Check login with verifiers
		IInstallVerifier[] verifiers = ContributorRegistry.getDefault().getInstallVerifiers();
		for (IInstallVerifier verifier : verifiers) {
			IStatus verifyStatus = verifier.verifyCredentials(username, password);
			if (!verifyStatus.isOK()) {
				status = verifyStatus;
				break;
			}
		}

		return status;
	}
	
	/**
	 * Copied from ServiceHelper because we need to obtain services
	 * before p2 has been started.
	 */
	public static Object getService(BundleContext context, String name) {
		if (context == null)
			return null;
		ServiceReference<?> reference = context.getServiceReference(name);
		if (reference == null)
			return null;
		Object result = context.getService(reference);
		context.ungetService(reference);
		return result;
	}
	
	/**
	 * This method search for any components found in an existing product.
	 * If the component is found, it is set to install by default.
	 * If the component is not found or there is no existing product, the
	 * component is set to not install by default.
	 * 
	 * @param components Components to set
	 */
	private void setupAddonComponents(IInstallComponent[] components) {
		// Set component to not install by default
		for (IInstallComponent component : components) {
			// Set to not install by default (it can be selected on Components page)
			component.setInstall(false);
			// Mark as add-on
			component.setAddon(true);
		}
		
		IProvisioningAgent agent = null;
		try {
			// Look for existing P2 in product directory
			IPath agentLocation = installDescription.getInstallLocation().append(IInstallConstants.P2_DIRECTORY);
			if (agentLocation.toFile().exists()) {
				// Existing product found
				if (agentLocation != null) {
					IProvisioningAgentProvider provider = (IProvisioningAgentProvider) getService(Installer.getDefault().getContext(), IProvisioningAgentProvider.SERVICE_NAME);
					agent = provider.createAgent(agentLocation.toFile().toURI());		
					IProfileRegistry profileRegistry = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
					IProfile profile = profileRegistry.getProfile(installDescription.getProfileName());
					// Query for existing IU groups
					IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();
					IQueryResult<IInstallableUnit> queryResult = profile.query(query, null);
					Iterator<IInstallableUnit> iter = queryResult.iterator();
					// Set every component found in existing IU groups to install by default
					while (iter.hasNext()) {
						IInstallableUnit existingUnit = iter.next();
						for (IInstallComponent component : components) {
							if (existingUnit.getId().equals(component.getInstallUnit().getId())) {
								component.setInstall(true);
								break;
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.log(e);
		}
		finally {
			// Stop agent
			if (agent != null) {
				agent.stop();
			}
		}
	}
	
	/**
	 * Retrieves the add-ons and adds them as optional components.
	 * 
	 * @return <code>true</code> on success, <code>false</code> on failure
	 */
	private boolean retrieveAddons() {
		final boolean[] result = new boolean[] { true };
		
		// Set credentials
		AuthenticationService.getDefault().setUserName(getUsername());
		AuthenticationService.getDefault().setPassword(getPassword());

		// Load add-on repositories
		for (URI addonLocation : getAddonLocations()) {
			// Listen for add-on repository load
			IInstallRepositoryListener listener = new IInstallRepositoryListener() {
				@Override
				public void repositoryStatus(RepositoryStatus status) {
					if (status == IInstallRepositoryListener.RepositoryStatus.loadingCompleted) {
						synchronized(this) {
							notifyAll();
						}
					}
				}

				@Override
				public void repositoryError(URI location, String errorMessage) {
					result[0] = false;
					Installer.log(errorMessage);
				}

				@Override
				public void repositoryLoaded(URI location,
						IInstallComponent[] components) {
					// Initialize components install status
					setupAddonComponents(components);
				}
			};
			// Load add-on repositories
			synchronized(listener) {
				RepositoryManager.getDefault().addRepositoryListener(listener);
				RepositoryManager.getDefault().loadRepositories(new URI[] { addonLocation }, true);
				try {
					listener.wait();
				} catch (InterruptedException e) {
					// Ignore
				}
				RepositoryManager.getDefault().removeRepositoryListener(listener);
			}
			
			// Error loading repository
			if (!result[0])
				break;
		}
		
		return result[0];
	}
	
	@Override
	public boolean validate() {
		final IStatus[] status = new IStatus[] { Status.OK_STATUS };

		// Add-ons are selected for install and have not been retrieved
		if (getInstallAddons() && !isAddonsDone()) {
			// Hide any status
			hideStatus();

			runOperation(InstallMessages.AddonsPage_Login, new Runnable() {
				@Override
				public void run() {
					IStatus loginStatus = Status.OK_STATUS;

					// Validate credentials
					if (getInstallDescription().getAddonsRequireLogin()) {
						loginStatus = validateLogin(getUsername(), getPassword());
					}
					
					if (loginStatus.isOK()) {
						// Update busy message
						getShell().getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								showBusy(InstallMessages.AddonsPage_CheckingAddons);
							}
						});
						
						// Retrieve add-ons
						try {
							if (!retrieveAddons()) {
								status[0] = new Status(IStatus.WARNING, Installer.ID, InstallMessages.Error_Addons);
							}
						}
						catch (Exception e) {
							Installer.log(e);
						}
					}
					else {
						status[0] = new Status(IStatus.ERROR, Installer.ID, 
								NLS.bind(InstallMessages.Error_Login0, loginStatus.getMessage()));
					}
				}
			});

			// Error or warning
			if (status[0] != Status.OK_STATUS) {
				showStatus(new IStatus[] { status[0] });
			}
			// If add-ons done, disable
			if (isAddonsDone()) {
				Control[] controls = area.getChildren();
				for (Control control : controls) {
					control.setEnabled(false);
				}
			}
		}
		
		//return (status[0].getSeverity() != IStatus.ERROR);
		return status[0].isOK();
	}

	int consolePromptState = 0;
	
	@Override
	public String getConsoleResponse(String input) {
		String response = null;
		ConsoleYesNoPrompter addonsPrompter = new ConsoleYesNoPrompter(
				getAddonsDescription(), 
				InstallMessages.AddonsPage_ConsoleInstallAddons, 
				getInstallAddons());
		
		if (input == null) {
			consolePromptState = 0;
		}
		
		// Prompt if add-ons should be installed
		if (consolePromptState == 0) {
			consolePromptState++;
			response = addonsPrompter.getConsoleResponse(input);
		}
		// Prompt input
		else if (consolePromptState == 1) {
			response = addonsPrompter.getConsoleResponse(input);
			// If install add-ons, prompt for user name
			if (addonsPrompter.getResult()) {
				// Add-ons require login
				if (getInstallDescription().getAddonsRequireLogin()) {
					consolePromptState++;
					response = InstallMessages.AddonsPage_ConsoleUsername;
				}
				// Add-ons do not require login
				else {
					consolePromptState = 4;
				}
			}
			// Skip add-ons
			else {
				consolePromptState = 5;
			}
			
		}
		// User name input
		else if (consolePromptState == 2) {
			consolePromptState++;
			setUsername(input);
			// Prompt for password
			response = InstallMessages.AddonsPage_ConsolePassword;
		}
		// Password input
		else if (consolePromptState == 3) {
			consolePromptState++;
			setPassword(input);
			// Validate log-in
			System.out.println(InstallMessages.AddonsPage_Login);
			IStatus loginStatus = validateLogin(getUsername(), getPassword());
			if (!loginStatus.isOK()) {
				String message = NLS.bind(InstallMessages.Error_Login0, loginStatus.getMessage()); 
				response = message + "\n\n" + InstallMessages.AddonsPage_ConsoleUsername;
				// Re-prompt for user name
				consolePromptState = 2;
			}
		}
		
		// Retrieve add-ons
		if (consolePromptState == 4) {
			consolePromptState ++;
			System.out.println(InstallMessages.AddonsPage_CheckingAddons);
			if (!retrieveAddons()) {
				consolePromptState = 1;
				System.out.println(InstallMessages.Error_Addons);
				response = addonsPrompter.getConsoleResponse(null);
			}
		}
		
		return response;
	}
}
