/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.flomeise.filetransfertool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Flohw
 */
public class FileTransferTool {
	/**
	 * the application name to be displayed in the about dialog
	 */
	public static final String NAME = "FileTransferTool";
	/**
	 * the author to be displayed in the about dialog
	 */
	public static final String AUTHOR = "Florian \"r0Xx4H-7331\" Meißner";
	/**
	 * the name of the config file
	 */
	public static final String PROPERTIES_FILE = "filetransfertool.properties";
	private static Properties properties, pDefaults;
	private static MainWindow mainWindow;
	private static boolean initialized = false;
	private static int connectionCount = 0;

	/**
	 * 
	 * @param mw
	 */
	public static void init(MainWindow mw) {
		if(initialized) {
			return;
		}
		mainWindow = mw;
		loadProperties();
	}

	/**
	 * 
	 */
	public static void loadProperties() {
		pDefaults = new Properties();
		pDefaults.setProperty("port", "1337");
		pDefaults.setProperty("buffer_size", "1048576");
		pDefaults.setProperty("save_dir", System.getProperty("user.home"));
		pDefaults.setProperty("save_dialog", "false");
		try {
			properties = new Properties(pDefaults);
			properties.load(new FileInputStream(new File(PROPERTIES_FILE)));
		} catch(IOException ex) {
			Logger.getLogger(FileTransferTool.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * 
	 */
	public static void listen() {
		try {
			ServerSocket server = new ServerSocket(Integer.parseInt(properties.getProperty("port")));
			while(true) {
				final Socket client = server.accept();
				ExecutorService pool = Executors.newCachedThreadPool();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						new FileTransfer(client, true).receive();
					}

				});
			}
		} catch(IOException ex) {
			Logger.getLogger(FileTransferTool.class.getName()).log(Level.SEVERE, null, ex);
			JOptionPane.showMessageDialog(mainWindow, "Could not bind to port, no incoming connections possible!!!");
		}
	}

	/**
	 * 
	 * @return
	 */
	public static int getDefaultPort() {
		return Integer.parseInt(getProperty("port"));
	}

	/**
	 * 
	 */
	public static void saveProperties() {
		try {
			properties.store(new FileOutputStream(new File(PROPERTIES_FILE)), NAME);
		} catch(IOException ex) {
			Logger.getLogger(FileTransferTool.class.getName()).log(Level.SEVERE, null, ex);
			JOptionPane.showMessageDialog(mainWindow, "Error while saving to properties file!");
		}
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	public static synchronized String getProperty(String s) {
		return properties.getProperty(s);
	}

	/**
	 * 
	 * @param s1
	 * @param s2
	 */
	public static synchronized void setProperty(String s1, String s2) {
		properties.setProperty(s1, s2);
	}

	/**
	 * 
	 * @return
	 */
	public static synchronized int getConnectionCount() {
		return connectionCount;
	}

	/**
	 * 
	 */
	public static synchronized void incrementConnectionCount() {
		connectionCount++;
	}

	/**
	 * 
	 */
	public static synchronized void decrementConnectionCount() {
		connectionCount--;
	}

	//now the non-static methods for actual sending/receiving
	public FileTransferTool(String address, int port) {
	}

	/**
	 * 
	 * @param client
	 */
//	public void receive(Socket client) {
//		incrementConnectionCount();
//		FileOutputStream osfile = null;
//		ProgressWindow pw = new ProgressWindow(this);
//		pw.setMaxProgress(10000);
//		pw.setVisible(true);
//		try {
//			pw.printToTextbox("Initializing...");
//			InputStream is = client.getInputStream();
//			OutputStream os = client.getOutputStream();
//			DataInputStream dis = new DataInputStream(is);
//			DataOutputStream dos = new DataOutputStream(os);
//			Version v;
//			if(!(v = new Version(dis.readUTF())).equals(VERSION)) {
//				dos.writeBoolean(false);
//				JOptionPane.showMessageDialog(pw, "Different client version, source has " + v + ", you have " + VERSION);
//				return;
//			}
//			dos.writeBoolean(true);
//			pw.printToTextbox("DONE\n");
//			String filename = dis.readUTF();
//			File file = new File(properties.getProperty("save_dir"), filename);
//			long filesize = dis.readLong();
//			byte[] md5 = new byte[16];
//			dis.read(md5);
//			int result = JOptionPane.showConfirmDialog(pw, client.getInetAddress().getHostAddress() + " is trying to send you " + filename + " (Size: " + filesize + "). Would you like to accept the transfer?", "Incoming transfer", JOptionPane.YES_NO_OPTION);
//			if(result == JOptionPane.YES_OPTION) {
//				if(properties.getProperty("save_dialog").equals("true")) {
//					file = new File(FileOpenDialog.open(JFileChooser.SAVE_DIALOG, file));
//				}
//				osfile = new FileOutputStream(file);
//				dos.writeBoolean(true);
//			} else {
//				dos.writeBoolean(false);
//				return;
//			}
//			pw.printToTextbox("Retrieving " + filename);
//			int bufferSize = dis.readInt();
//			byte[] buffer = new byte[bufferSize];
//			int bytesRead = 0, bytesReadAll = 0;
//			long startTime = System.currentTimeMillis();
//			do {
//				if(isAborted() == true) {
//					if(JOptionPane.showConfirmDialog(pw, "Would you really like to abort the transfer?", "Abort", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
//						return;
//					}
//				}
//
//				bytesReadAll += (bytesRead = is.read(buffer));
//				if(bytesReadAll == -1) {
//					JOptionPane.showMessageDialog(pw, "Peer has aborted the transfer!");
//					return;
//				}
//				osfile.write(buffer, 0, bytesRead);
//				double progress = ((double) bytesReadAll / (double) filesize) * 10000;
//				pw.setProgress((int) Math.round(progress));
//				double speed = Math.round((double) (bytesReadAll / 1024) / (System.currentTimeMillis() - startTime) * 100) / 100;
//				pw.setLabel(FileUtils.byteCountToDisplaySize(bytesReadAll) + "/" + FileUtils.byteCountToDisplaySize(filesize) + " (" + bytesReadAll + "/" + filesize + ") @ " + speed + " kb/s");
//			} while(bytesReadAll < filesize);
//			if(MD5.hashesEqual(md5, MD5.getHash(file))) {
//				dos.writeBoolean(true);
//				JOptionPane.showMessageDialog(pw, "File transfer succeeded!");
//			} else {
//				dos.writeBoolean(false);
//				JOptionPane.showMessageDialog(pw, "File transfer failed - hashes differ!");
//			}
//		} catch(IOException ex) {
//			Logger.getLogger(FileTransferTool.class.getName()).log(Level.SEVERE, null, ex);
//		} finally {
//			try {
//				osfile.close();
//				client.close();
//			} catch(Exception e) {
//			}
//			pw.dispose();
//			decrementConnectionCount();
//		}
//
//	}
}