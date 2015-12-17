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

import com.codesourcery.installer.IInstalledProduct;
import com.codesourcery.installer.Installer;

/**
 * Maintains the registry of installed products.
 */
public class InstallRegistry {
	/** File version */
	private static final String VERSION = "1.0";

	/** Install element */
	private static final String ELEMENT_REGISTRY = "registry";
	/** Products element */
	private static final String ELEMENT_PRODUCTS = "products";
	/** Product element */
	private static final String ELEMENT_PRODUCT = "product";
	/** Version attribute */
	private static final String ATTRIBUTE_VERSION = "version";
	/** Identifier attribute */
	private static final String ATTRIBUTE_ID = "id";
	/** name attribute */
	private static final String ATTRIBUTE_NAME = "name";
	/** Location attribute */
	private static final String ATTRIBUTE_LOCATION = "location";
	/** Category attribute */
	private static final String ATTRIBUTE_CATEGORY = "category";
	/** Installed products */
	private ArrayList<IInstalledProduct> products = new ArrayList<IInstalledProduct>();

	/**
	 * Constructor
	 */
	public InstallRegistry() {
	}
	
	/**
	 * Saves the registry to a file.
	 * 
	 * @param location Path to registry file
	 * @throws CoreException on failure
	 */
	public void save(IPath location) throws CoreException {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document = docBuilder.newDocument();

			// Root element
			Element rootElement = document.createElement(ELEMENT_REGISTRY);
			document.appendChild(rootElement);
			// File version
			rootElement.setAttribute(ATTRIBUTE_VERSION, VERSION);
			
			// Products root
			Element productsElement = document.createElement(ELEMENT_PRODUCTS);
			rootElement.appendChild(productsElement);
			
			// Products
			for (IInstalledProduct product : products) {
				Element productElement = document.createElement(ELEMENT_PRODUCT);
				// Product identifier
				productElement.setAttribute(ATTRIBUTE_ID, product.getId());
				// Product name
				productElement.setAttribute(ATTRIBUTE_NAME, product.getName());
				// Product version
				productElement.setAttribute(ATTRIBUTE_VERSION, product.getVersionText());
				// Product location
				productElement.setAttribute(ATTRIBUTE_LOCATION, product.getInstallLocation().toOSString());
				// Product location
				productElement.setAttribute(ATTRIBUTE_LOCATION, product.getInstallLocation().toOSString());
				// Product category
				if (product.getCategory() != null) {
					productElement.setAttribute(ATTRIBUTE_CATEGORY, product.getCategory());
				}
				
				productsElement.appendChild(productElement);
			}
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			StreamResult result = new StreamResult(location.toFile());
	 
			// Formatting
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
		}
		catch (Exception e) {
			Installer.fail("Error saving install registry.", e);
		}
	}
	
	/**
	 * Loads the registry from a file.
	 * 
	 * @param location Path to registry file
	 * @throws CoreException on failure
	 */
	public void load(IPath location) throws CoreException {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document document = docBuilder.parse(location.toFile());

			products.clear();
			NodeList productNodes = document.getElementsByTagName(ELEMENT_PRODUCT);
			for (int productIndex = 0; productIndex < productNodes.getLength(); productIndex++) {
				Node productNode = productNodes.item(productIndex);
				if (productNode.getNodeType() == Node.ELEMENT_NODE) {
					Element productElement = (Element)productNode;
					
					// Product location
					IPath productLocation = new Path(productElement.getAttribute(ATTRIBUTE_LOCATION));
					// Add product if it is found (was not removed manually)
					if (productLocation.toFile().exists()) {
						InstalledProduct product = new InstalledProduct(
								productElement.getAttribute(ATTRIBUTE_ID),
								productElement.getAttribute(ATTRIBUTE_NAME),
								productElement.getAttribute(ATTRIBUTE_VERSION),
								productLocation,
								productElement.getAttribute(ATTRIBUTE_CATEGORY));
						products.add(product);
					}
				}
			}
		}
		catch (Exception e) {
			Installer.fail("Error loading install registry.", e);
		}
	}

	/**
	 * Adds a new product to the registry.
	 * 
	 * @param product Product to add
	 */
	public void addProduct(IInstalledProduct product) {
		removeProduct(product.getId());
		products.add(product);
	}
	
	/**
	 * Removes a product from the registry.
	 * 
	 * @param productId Product to remove
	 */
	public void removeProduct(String productId) {
		IInstalledProduct product = getProduct(productId);
		if (product != null) {
			products.remove(product);
		}
	}
	
	/**
	 * Returns a product.  A product will only be returned if it exist in the
	 * registry and its install location exists.
	 * 
	 * @param productId Identifier of product
	 * @return Products
	 */
	public IInstalledProduct getProduct(String productId) {
		IInstalledProduct foundProduct = null;
		for (IInstalledProduct product : products) {
			if (product.getId().equals(productId)) {
				foundProduct = product;
				break;
			}
		}
		
		return foundProduct;
	}

	/**
	 * Returns products in the registry.
	 * 
	 * @return Products
	 */
	public IInstalledProduct[] getProducts() {
		return products.toArray(new IInstalledProduct[products.size()]);
	}
}
