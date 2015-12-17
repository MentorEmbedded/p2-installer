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

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallManifest;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.IProductRange;
import com.codesourcery.installer.Installer;

/**
 * Default implementation of {@link com.codesourcery.installer.IInstallManifest}.
 */
public class InstallManifest implements IInstallManifest {
	/** File version */
	private static final String FILE_VERSION = "1.2";
	
	/** Install element */
	private static final String ELEMENT_INSTALL = "install";
	/** Products element */
	private static final String ELEMENT_PRODUCTS = "products";
	/** Product element */
	private static final String ELEMENT_PRODUCT = "product";
	/** Actions element */
	private static final String ELEMENT_ACTIONS = "actions";
	/** Action element */
	private static final String ELEMENT_ACTION = "action";
	/** Units element */
	private static final String ELEMENT_UNITS = "units";
	/** Unit element */
	private static final String ELEMENT_UNIT = "unit";
	/** Properties element */
	private static final String ELEMENT_PROPERTIES = "properties";
	/** Property element */
	private static final String ELEMENT_PROPERTY = "property";
	
	/** Location attribute */
	private static final String ATTRIBUTE_LOCATION = "location";
	/** Install location attribute */
	private static final String ATTRIBUTE_INSTALL_LOCATION = "installLocation";
	/** Name attribute */
	private static final String ATTRIBUTE_NAME = "name";
	/** Uninstall Name attribute */
	private static final String ATTRIBUTE_UNINSTALL_NAME = "uninstallName";
	/** Value attribute */
	private static final String ATTRIBUTE_VALUE = "value";
	/** Version attribute */
	private static final String ATTRIBUTE_VERSION = "version";
	/** Identifier attribute */
	private static final String ATTRIBUTE_ID = "id";
	/** Data path attribute */
	private static final String ATTRIBUTE_DATA = "dataLocation";
	/** Parent directory names attribute */
	private static final String ATTRIBUTE_DIRECTORIES = "directories";

	/** Installed products */
	private ArrayList<IInstallProduct> products = new ArrayList<IInstallProduct>();
	/** Install manifest file */
	private File file;
	/** Loaded version */
	private String version = FILE_VERSION;
	/** Path to installer data directory */
	private IPath dataPath;
	/** Directories to remove during uninstallation */
	private String[] directories = new String[0];

	/**
	 * Loads an install manifest for the location specified in an install
	 * description.
	 * 
	 * @param installLocation Install location
	 * @return Install manifest or <code>null</code> not manifest is found.
	 * @throws CoreException on failure to load the install manifest
	 */
	public static InstallManifest loadManifest(IPath installLocation) throws CoreException {
		InstallManifest manifest = null;
		if (installLocation != null) {
			IPath uninstallLocation = installLocation.append(IInstallConstants.UNINSTALL_DIRECTORY);
			IPath manifestPath = uninstallLocation.append(IInstallConstants.INSTALL_MANIFEST_FILENAME);
			File manifestFile = manifestPath.toFile();
			// Loading existing manifest
			if (manifestFile.exists()) {
				manifest = new InstallManifest();
				manifest.load(manifestFile);
			}
		}
		
		return manifest;
	}

	/**
	 * Constructor
	 */
	public InstallManifest() {
		dataPath = Installer.getDefault().getDataFolder();
	}

	@Override
	public IInstallProduct[] getProducts() {
		return products.toArray(new InstallProduct[products.size()]);
	}

	@Override
	public void addProduct(IInstallProduct product) {
		if (!products.contains(product))
			products.add(product);
	}

	@Override
	public void removeProduct(IInstallProduct product) {
		products.remove(product);
	}

	@Override
	public IInstallProduct getProduct(String productId) {
		IInstallProduct product = null;
		IInstallProduct[] existingProducts = getProducts();
		for (IInstallProduct existingProduct : existingProducts) {
			// Product is already installed
			if (existingProduct.getId().equals(productId)) {
				product = existingProduct;
				break;
			}
		}
		
		return product;
	}

	@Override
	public IInstallProduct[] getProducts(IProductRange[] ranges) {
		if (ranges == null)
			return null;
		
		ArrayList<IInstallProduct> foundProducts = new ArrayList<IInstallProduct>();
		for (IProductRange range : ranges) {
			IInstallProduct product = getProduct(range.getId());
			if (product != null) {
				if ((range.getVersionRange() == null) || range.getVersionRange().isIncluded(product.getVersion())) {
					foundProducts.add(product);
				}
			}
		}
		
		return foundProducts.toArray(new IInstallProduct[foundProducts.size()]);
	}

	@Override
	public IPath getDataPath() {
		return dataPath;
	}
	
	@Override
	public void save() throws CoreException {
		save(file);
	}

	/**
	 * Converts an absolute path into a path relative to this manifest file.
	 * 
	 * @param path Absolute path
	 * @return Relative path
	 */
	private IPath toRelativePath(IPath path) {
		if (path != null) {
			if (path.isAbsolute()) {
				path = path.makeRelativeTo(getPath().removeLastSegments(1));
			}
		}
		
		return path;
	}
	
	/**
	 * Converts a path relative to this manifest file into an absolute path.
	 * 
	 * @param path Relative path
	 * @return Absolute path
	 */
	private IPath fromRelativePath(IPath path) {
		IPath absPath = path;
		if (!path.isAbsolute()) {
			absPath = getPath().removeLastSegments(1).append(path);
		}
		
		return absPath;
	}
	
	@Override
	public void save(File file) throws CoreException {
        this.file = file;
		try {
			// Don't overwrite a previous version.  This allows for patching
			// an old installation without breaking the format used by a
			// a previous uninstaller.
			if (Installer.getDefault().getInstallManager().getInstallMode().isPatch() && 
					!FILE_VERSION.equals(version)) {
				return;
			}
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document = docBuilder.newDocument();
			
			// Root element
			Element installElement = document.createElement(ELEMENT_INSTALL);
			document.appendChild(installElement);
			// File version
			installElement.setAttribute(ATTRIBUTE_VERSION, FILE_VERSION);
			// Data path
			installElement.setAttribute(ATTRIBUTE_DATA, getDataPath().toOSString());
			// Directory levels
			installElement.setAttribute(ATTRIBUTE_DIRECTORIES, InstallUtils.getStringFromArray(directories, "/"));

			// Products root
			Element productsElement = document.createElement(ELEMENT_PRODUCTS);
			installElement.appendChild(productsElement);
			
			// Products
			IInstallProduct[] products = getProducts();
			for (IInstallProduct product : products) {
				Element productElement = document.createElement(ELEMENT_PRODUCT);
				// Product identifier
				productElement.setAttribute(ATTRIBUTE_ID, product.getId());
				// Product name
				productElement.setAttribute(ATTRIBUTE_NAME, product.getName());
				// Product version
				productElement.setAttribute(ATTRIBUTE_VERSION, product.getVersionString());
				// Product uninstall name
				productElement.setAttribute(ATTRIBUTE_UNINSTALL_NAME, product.getUninstallName());
				// Product location
				productElement.setAttribute(ATTRIBUTE_LOCATION, toRelativePath(product.getLocation()).toOSString());
				// Product install location
				productElement.setAttribute(ATTRIBUTE_INSTALL_LOCATION, toRelativePath(product.getInstallLocation()).toOSString());
				
				productsElement.appendChild(productElement);
				
				// Product actions root
				Element actionsElement = document.createElement(ELEMENT_ACTIONS);
				productElement.appendChild(actionsElement);
				// Product actions
				IInstallAction[] actions = product.getActions();
				for (IInstallAction action : actions) {
					Element actionElement = document.createElement(ELEMENT_ACTION);
					actionsElement.appendChild(actionElement);
					actionElement.setAttribute(ATTRIBUTE_ID, action.getId());
					
					action.save(document, actionElement);
				}
				
				// Product install units root
				Element unitsElement = document.createElement(ELEMENT_UNITS);
				productElement.appendChild(unitsElement);
				// Product install units
				for (IVersionedId unit : product.getInstallUnits()) {
					Element unitElement = document.createElement(ELEMENT_UNIT);
					unitsElement.appendChild(unitElement);
					unitElement.setAttribute(ATTRIBUTE_ID, unit.getId());
					unitElement.setAttribute(ATTRIBUTE_VERSION, unit.getVersion().getOriginal());
				}
				
				// Product properties
				Element propertiesElement = document.createElement(ELEMENT_PROPERTIES);
				productElement.appendChild(propertiesElement);
				Set<Entry<String, String>> properties = product.getProperties().entrySet();
				for (Entry<String, String> property : properties) {
					Element propertyElement = document.createElement(ELEMENT_PROPERTY);
					propertyElement.setAttribute(ATTRIBUTE_NAME, property.getKey());
					propertyElement.setAttribute(ATTRIBUTE_VALUE, property.getValue());
					propertiesElement.appendChild(propertyElement);
				}
			}
			
			file.getParentFile().mkdirs();
			if (file.exists())
				file.delete();

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(file);
	 
			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);
	 
			// Formatting
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
		}
		catch (Exception e) {
			Installer.fail(InstallMessages.Error_SaveManifest, e);
		}
	}

	@Override
	public void load(File file) throws CoreException {
		this.file = file;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document = docBuilder.parse(file);

			NodeList installNodes = document.getElementsByTagName(ELEMENT_INSTALL);
			version = ((Element)installNodes.item(0)).getAttribute(ATTRIBUTE_VERSION);
			
			String data = ((Element)installNodes.item(0)).getAttribute(ATTRIBUTE_DATA);
			if ((data != null) && !data.isEmpty()) {
				dataPath = new Path(data);
			}
			
			data = ((Element)installNodes.item(0)).getAttribute(ATTRIBUTE_DIRECTORIES);
			if ((data != null) && !data.isEmpty()) {
				directories = InstallUtils.getArrayFromString(data, "/");
			}

			NodeList productNodes = document.getElementsByTagName(ELEMENT_PRODUCT);
			for (int productIndex = 0; productIndex < productNodes.getLength(); productIndex++) {
				Node productNode = productNodes.item(productIndex);
				if (productNode.getNodeType() == Node.ELEMENT_NODE) {
					Element productElement = (Element)productNode;
					
					// Load actions
					ArrayList<IInstallAction> actions = new ArrayList<IInstallAction>();
					NodeList actionsNodes = productElement.getElementsByTagName(ELEMENT_ACTIONS);
					for (int actionsIndex = 0; actionsIndex < actionsNodes.getLength(); actionsIndex++) {
						Node actionsNode = actionsNodes.item(actionsIndex);
						if (actionsNode.getNodeType() == Node.ELEMENT_NODE) {
							Element actionsElement = (Element)actionsNode;
							
							NodeList actionNodes = actionsElement.getElementsByTagName(ELEMENT_ACTION);
							for (int actionIndex = 0; actionIndex < actionNodes.getLength(); actionIndex++) {
								Node actionNode = actionNodes.item(actionIndex);
								if (actionNode.getNodeType() == Node.ELEMENT_NODE) {
									try {
										Element actionElement = (Element)actionNode;
										String id = actionElement.getAttribute(ATTRIBUTE_ID);
										IInstallAction action = ContributorRegistry.getDefault().createAction(id);
										if (action != null) {
											try {
												action.load(actionElement);
												actions.add(action);
											}
											catch (Exception e) {
												Installer.log(e);
											}
										}
										else {
											Installer.log("Deprecated action not supported: " + id);
										}
									}
									catch (Exception e) {
										Installer.log(e);
									}
								}
							}
						}
					}
					
					String attrLocation = productElement.getAttribute(ATTRIBUTE_LOCATION);
					String attrInstallLocation = productElement.getAttribute(ATTRIBUTE_INSTALL_LOCATION);
					
					// Create product
					InstallProduct product = new InstallProduct(
							productElement.getAttribute(ATTRIBUTE_ID),
							productElement.getAttribute(ATTRIBUTE_NAME),
							productElement.getAttribute(ATTRIBUTE_VERSION),
							productElement.getAttribute(ATTRIBUTE_UNINSTALL_NAME),
							attrLocation != null ? fromRelativePath(new Path(attrLocation)) : null,
							attrInstallLocation != null ? fromRelativePath(new Path(attrInstallLocation)) : null);
					// Add product actions
					for (IInstallAction action : actions) {
						product.addAction(action);
					}
					// Add product
					addProduct(product);

					// Load install units
					NodeList unitsNodes = productElement.getElementsByTagName(ELEMENT_UNITS);
					for (int unitsIndex = 0; unitsIndex < unitsNodes.getLength(); unitsIndex++) {
						Node unitsNode = unitsNodes.item(unitsIndex);
						if (unitsNode.getNodeType() == Node.ELEMENT_NODE) {
							Element unitsElement = (Element)unitsNode;
							NodeList unitNodes = unitsElement.getElementsByTagName(ELEMENT_UNIT);
							for (int unitIndex = 0; unitIndex < unitNodes.getLength(); unitIndex++) {
								Node unitNode = unitNodes.item(unitIndex);
								if (unitNode.getNodeType() == Node.ELEMENT_NODE) {
									Element unitElement = (Element)unitNode;
									try {
										String id = unitElement.getAttribute(ATTRIBUTE_ID);
										String version = unitElement.getAttribute(ATTRIBUTE_VERSION);
										VersionedId unit;
										if ((version != null) && !version.trim().isEmpty()) {
											unit = new VersionedId(id, version);
										}
										else {
											unit = new VersionedId(id, Version.emptyVersion);
										}
										product.addInstallUnit(unit);
									}
									catch (Exception e) {
										Installer.log(e);
									}
								}
							}
						}
					}
					
					// Load product properties
					NodeList propertiesNodes = productElement.getElementsByTagName(ELEMENT_PROPERTIES);
					for (int propertiesIndex = 0; propertiesIndex < propertiesNodes.getLength(); propertiesIndex++) {
						Node propertiesNode = propertiesNodes.item(propertiesIndex);
						if (propertiesNode.getNodeType() == Node.ELEMENT_NODE) {
							Element propertiesElement = (Element)propertiesNode;
							NodeList propertyNodes = propertiesElement.getElementsByTagName(ELEMENT_PROPERTY);
							for (int propertyIndex = 0; propertyIndex < propertyNodes.getLength(); propertyIndex++) {
								Node propertyNode = propertyNodes.item(propertyIndex);
								if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
									Element propertyElement = (Element)propertyNode;
									try {
										String name = propertyElement.getAttribute(ATTRIBUTE_NAME);
										String value = propertyElement.getAttribute(ATTRIBUTE_VALUE);
										if (name != null) {
											product.setProperty(name, value);
										}
									}
									catch (Exception e) {
										Installer.log(e);
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Installer.fail(InstallMessages.Error_LoadManifest, e);
		}
	}
	
	/**
	 * Sets the names of the parent directories created during installation.
	 * 
	 * @param directories Directory names created
	 */
	public void setDirectories(String[] directories) {
		this.directories = directories;
	}
	
	/**
	 * Returns the names of the parent directories created during installation.
	 * 
	 * @return Directory name
	 */
	public String[] getDirectories() {
		return directories;
	}

	/**
	 * Returns the path to the manifest file.
	 * 
	 * @return Manifest file path
	 */
	private IPath getPath() {
		if (file != null) {
			return new Path(file.getAbsolutePath());
		}
		else {
			return null;
		}
	}

	@Override
	public IPath getInstallLocation() {
		IPath path = getPath();
		if ((path != null) && (path.segmentCount() > 2)) {
			return path.removeLastSegments(2);
		}
		else {
			return null;
		}
	}
}
