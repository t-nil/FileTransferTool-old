/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.flomeise.filetransfertool;

import com.csvreader.CsvReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
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
	public static void update(String updateFileAddress, String updateFileName, String currentJarName, Version currentVersion) {
		BufferedReader reader = null;
		InputStream is = null;
		OutputStream os = null;
		CsvReader csv = null;
		File currentJar;
		try {
//			try {
//				URI uri = Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI();
//				System.err.println("uri = " + uri);
//				currentJar = new File("FileTransferTool.jar").getAbsoluteFile();
//				System.err.println("currentJar = " + currentJar);
//				if(!currentJar.exists() || currentJar.isDirectory()) {
//					throw new URISyntaxException(currentJar.getCanonicalPath(), "doesn't exist or is an directory");
//				}
//			} catch(URISyntaxException ex) {
//				Logger.getLogger(Updater.class.getName()).log(Level.SEVERE, null, ex);
//				JOptionPane.showMessageDialog(null, "Could not resolve running jar path");
//				return null;
//			}

			currentJar = new File(currentJarName);
			URL updateFileURL = new URL(updateFileAddress + updateFileName);
			URLConnection updateFileURLConnection = updateFileURL.openConnection();
			csv = new CsvReader(updateFileURLConnection.getInputStream(), Charset.forName("ISO-8859-1"));
			Version highestVersion = new Version(0, 0, 0);
			String highestVersionFilename = null;
			while(csv.readRecord()) {
				Version v = new Version(csv.get(0));
				if(highestVersion.compareTo(v) < 0) {
					highestVersion = v;
					highestVersionFilename = csv.get(1);
				}
			}

			if(highestVersion.compareTo(currentVersion) > 0) {
				JOptionPane.showMessageDialog(null, "Update found! Current version = " + currentVersion + ", server version = " + highestVersion + ". Updating...");
				is = new URL(updateFileAddress + highestVersionFilename).openStream();
				File newJar = new File(currentJar.getCanonicalPath() + ".new");
				os = new FileOutputStream(newJar);
				byte[] buffer = new byte[8192];
				int bytesRead = 0;
				while((bytesRead = is.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				File tempFile = File.createTempFile("update", ".jar");
				FileUtils.copyFile(new File(currentJar.getCanonicalPath() + ".new"), tempFile);
				new ProcessBuilder("java", "-jar", tempFile.getCanonicalPath(), "update", currentJar.getCanonicalPath()).start();
				System.exit(0);
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
				csv.close();
			} catch(Exception e) {
			}
		}
	}
	
	public static void performUpdate(String path) {
	try {
			File jar = new File(path);
			File lock = new File(jar, "..\\lock");
			
			while(lock.exists())
				Thread.sleep(100);
			
			jar.delete();
			File newJar = new File(jar.getCanonicalPath() + ".new");
			newJar.renameTo(jar);
			JOptionPane.showMessageDialog(null, "Program successfully updated!");
			new ProcessBuilder("java", "-jar", jar.getCanonicalPath()).start();
			return;
		} catch(IOException ex) {
			Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
		} catch(InterruptedException ex) {
			Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
