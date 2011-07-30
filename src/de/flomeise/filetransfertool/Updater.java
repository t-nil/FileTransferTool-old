/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.flomeise.filetransfertool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;

/**
 * A self-updater framework
 * @author Flohw
 */
public class Updater {
	private Updater() {
	}

	/**
	 * Updates the program
	 * @param updateFileAddress the address of the file where the update information is stored
	 * @param updateFileName the name of the file where the update information is stored
	 * @param currentVersion the current program version
	 * @return the file object related to the running jar
	 */
	public static void update(String updateFileAddress, String programName, Version currentVersion) {
		BufferedReader reader = null;
		InputStream is = null;
		OutputStream os = null;
		File currentJar;
		String line;
		try {
			try {
				URI uri = Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI();
				System.err.println("uri = " + uri);
				currentJar = new File(uri);
				System.err.println("currentJar = " + currentJar);
				if(!currentJar.exists() || currentJar.isDirectory()) {
					throw new URISyntaxException(currentJar.getCanonicalPath(), "doesn't exist or is an directory");
				}
			} catch(URISyntaxException ex) {
				Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
				JOptionPane.showMessageDialog(null, "Could not resolve running jar path");
				return;
			}
			
			is = new URL(updateFileAddress + programName + ".lst").openStream();
			reader = new BufferedReader(new InputStreamReader(is));
			Version highestVersion = new Version(0, 0, 0);
			line = null;
			
			while((line = reader.readLine()) != null) {
				Version v = new Version(line);
				if(v.compareTo(highestVersion) > 0) {
					highestVersion = v;
				}
			}
			reader.close();
			is.close();
			
			if(highestVersion.compareTo(currentVersion) > 0) {
				JOptionPane.showMessageDialog(null, "Update found! Current version = " + currentVersion + ", server version = " + highestVersion + ". Updating...");
				File updateFolder = new File(currentJar.getParentFile(), "update");
				FileUtils.forceMkdir(updateFolder);
				File newJar = new File(updateFolder, currentJar.getName());
				copyStream(new URL(updateFileAddress + highestVersion.toString() + ".jar").openStream(), new FileOutputStream(newJar));
				
				is = new URL(updateFileAddress + programName + "_libs.lst").openStream();
				reader = new BufferedReader(new InputStreamReader(is));
				line = null;
				
				while((line = reader.readLine()) != null) {
					File library = new File(currentJar.getParentFile(), "libs" + File.pathSeparator + line);
					if(!library.exists()) {
						copyStream(new URL(updateFileAddress + line).openStream(), new FileOutputStream(library));
					}
				}
			} else {
				//JOptionPane.showMessageDialog(null, "No update found!");
			}
		} catch(MalformedURLException ex) {
			Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
			JOptionPane.showMessageDialog(null, "Malformed URL for update information file");
		} catch(IOException ex) {
			Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
			JOptionPane.showMessageDialog(null, "I/O Error occurred while updating or the update information file has a wrong format");
		} finally {
			try {
				is.close();
				os.close();
				reader.close();
			} catch(Exception e) {
			}
		}
	}
	
	private static void copyStream(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[8192];
		int bytesRead = 0;
				
		while((bytesRead = is.read(buffer)) != -1) {
			os.write(buffer, 0, bytesRead);
		}
		
		is.close();
		os.close();
	}
}