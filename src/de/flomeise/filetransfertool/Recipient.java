/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.flomeise.filetransfertool;

/**
 *
 * @author Flohw
 */
public class Recipient {
	private String name, address;
	private int port;

	/**
	 * 
	 * @param name
	 * @param address
	 * @param port
	 */
	public Recipient(String name, String address, int port) {
		this.name = name;
		this.address = address;
		this.port = port;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	@Override
	public String toString() {
		return getName();
	}

}
