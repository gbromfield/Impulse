package com.grb.impulse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.grb.util.property.Property;


public interface Transform {    
	final static Log ConnectionLogger = LogFactory.getLog(Transform.class.getName() + "." + "connections");

	/**
	 * Gets the transform's name.
	 * 
	 * @return the transform's name.
	 */
	public String getTransformName();

	/**
	 * Gets the transform's instance name.
	 * 
	 * @return the transform's instance name.
	 */
	public String getInstanceName();

	/**
     * General initialization of a transform's logging, connections, and properties. 
     * Overriding subclasses should call super.init() first. 
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception;

	/**
     * Starts the transform.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception;

	/**
	 * Disposes of the transform when no longer needed.
	 * 
	 * @throws Exception
	 */
	public void dispose() throws Exception;

	/**
	 * Gets the property with the given name.
	 * 
	 * @param propName Name of the property to get.
	 * @return the property with the given name.
	 */
	public Property<?> getProperty(String propName);

	/**
	 * Get the Log.
	 * 
	 * @return the Log.
	 */
	public Log getLog();
}
