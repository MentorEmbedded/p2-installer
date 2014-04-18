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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.codesourcery.installer.IInstallAction;
import com.codesourcery.installer.IInstallDescription;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;

/**
 * This manifest records all actions performed for products in an installation.
 */
public class InstallManifest {
	/** File version */
	private static final String VERSION = "1.0";
	
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
	
	/** Location attribute */
	private static final String ATTRIBUTE_LOCATION = "location";
	/** Install location attribute */
	private static final String ATTRIBUTE_INSTALL_LOCATION = "installLocation";
	/** Name attribute */
	private static final String ATTRIBUTE_NAME = "name";
	/** Version attribute */
	private static final String ATTRIBUTE_VERSION = "version";
	/** Identifier attribute */
	private static final String ATTRIBUTE_ID = "id";

	/** Installed products */
	private ArrayList<IInstallProduct> products = new ArrayList<IInstallProduct>();
	/** Install manifest file */
	private File file;

	/**
	 * Loads an install manifest for the location specified in an install
	 * description.
	 * 
	 * @param installDescription Install description
	 * @return Install manifest or <code>null</code> not manifest is found.
	 * @throws CoreException on failure to load the install manifest
	 */
	public static InstallManifest loadManifest(IInstallDescription installDescription) throws CoreException {
		InstallManifest manifest = null;
		IPath uninstallLocation = installDescription.getRootLocation().append(IInstallConstants.UNINSTALL_DIRECTORY);
		if (uninstallLocation != null) {
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
	}
	
	/**
	 * Returns the products contained in the
	 * installation.
	 * 
	 * @return Products
	 */
	public IInstallProduct[] getProducts() {
		return products.toArray(new InstallProduct[products.size()]);
	}
	
	/**
	 * Adds a product to the installation.
	 * 
	 * @param product Product to add
	 */
	public void addProduct(IInstallProduct product) {
		products.add(product);
	}
	
	/**
	 * Removes a product from the installation.
	 * 
	 * @param product Product to remove
	 */
	public void removeProduct(IInstallProduct product) {
		products.remove(product);
	}
	
	/**
	 * Returns a product.
	 * 
	 * @param productId Product identifier
	 * @return Product or <code>null</code> if no product with the specified
	 * identifier exists in the manifest
	 */
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
	
	/**
	 * Convenience method to save the manifest
	 * to an existing file.
	 * 
	 * @throws CoreException on failure
	 */
	public void save() throws CoreException {
		save(file);
	}
	
	/**
	 * Saves the manifest to a file.
	 * 
	 * @param path File
	 * @throws CoreException on failure
	 */
	public void save(File file) throws CoreException {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document = docBuilder.newDocument();
			
			// Root element
			Element installElement = document.createElement(ELEMENT_INSTALL);
			document.appendChild(installElement);
			// File version
			installElement.setAttribute(ATTRIBUTE_VERSION, VERSION);

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
				productElement.setAttribute(ATTRIBUTE_VERSION, product.getVersion());
				// Product location
				productElement.setAttribute(ATTRIBUTE_LOCATION, product.getLocation().toOSString());
				// Product install location
				productElement.setAttribute(ATTRIBUTE_INSTALL_LOCATION, product.getInstallLocation().toOSString());
				
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
			}
			
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
            
            this.file = file;
		}
		catch (Exception e) {
			Installer.fail(InstallMessages.Error_SaveManifest, e);
		}
	}

	/**
	 * Loads the manifest from a file.
	 * 
	 * @param path File
	 * @throws CoreException on failure
	 */
	public void load(File file) throws CoreException {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document = docBuilder.parse(file);

			NodeList productNodes = document.getElementsByTagName(ELEMENT_PRODUCT);
			for (int productIndex = 0; productIndex < productNodes.getLength(); productIndex++) {
				Node productNode = productNodes.item(productIndex);
				if (productNode.getNodeType() == Node.ELEMENT_NODE) {
					Element productElement = (Element)productNode;
					
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
							}
						}
					}

					InstallProduct product = new InstallProduct(
							productElement.getAttribute(ATTRIBUTE_ID),
							productElement.getAttribute(ATTRIBUTE_NAME),
							productElement.getAttribute(ATTRIBUTE_VERSION),
							new Path(productElement.getAttribute(ATTRIBUTE_LOCATION)),
							new Path(productElement.getAttribute(ATTRIBUTE_INSTALL_LOCATION)),
							actions.toArray(new IInstallAction[actions.size()]));
					addProduct(product);
				}
			}
			
			this.file = file;
		}
		catch (Exception e) {
			Installer.fail(InstallMessages.Error_LoadManifest, e);
		}
	}
}
