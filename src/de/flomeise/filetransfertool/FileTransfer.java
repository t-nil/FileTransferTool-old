/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.flomeise.filetransfertool;

import com.twmacinta.util.MD5;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Flohw
 */
public class FileTransfer {
	private Socket socket;
	private ProgressWindow pw;
	private boolean isRecipient;
	private volatile boolean aborted = false;

	public FileTransfer(Socket socket, boolean isRecipient) {
		this.socket = socket;
		this.isRecipient = isRecipient;
		FileTransferTool.incrementConnectionCount();
	}

	public FileTransfer(String address, int port, boolean isRecipient) throws IOException {
		socket = new Socket(address, port);
		this.isRecipient = isRecipient;
		FileTransferTool.incrementConnectionCount();
	}

	public ProgressWindow getProgressWindow() {
		return pw;
	}

	public void send(String file) {
		send(new File(file));
	}

	public void send(String file, long offset) {
		send(new File(file), offset);
	}

	public void send(File file) {
		send(file, 0);
	}

	private boolean send(File file, long offset) {
		Socket server = null;
		pw = new ProgressWindow(this);
		pw.setMaxProgress(10000);
		pw.setVisible(true);
		try {
			InputStream is = server.getInputStream();
			OutputStream os = server.getOutputStream();
			DataInputStream dis = new DataInputStream(is);
			DataOutputStream dos = new DataOutputStream(os);

			writeFileName(dos, file.getName());
			writeFileSize(dos, file.length());
			writeOffset(dos, offset);
			writeMD5Hash(dos, file);
			writeBufferSize(dos, Integer.parseInt(FileTransferTool.getProperty("buffer_size")));
			writeFile(os, file);

			if(dis.readBoolean() == false) {
				JOptionPane.showMessageDialog(pw, "File transfer failed!");
				return false;
			} else {
				JOptionPane.showMessageDialog(pw, "File transfer suceeded!");
				return true;
			}
		} catch(IOException ex) {
			Logger.getLogger(FileTransfer.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				server.close();
			} catch(Exception e) {
			}
			pw.dispose();
			FileTransferTool.decrementConnectionCount();
		}
		return false;
	}

	private void writeFileCount(DataOutputStream dos, int i) throws IOException {
		dos.writeUTF("filecount");
		dos.writeInt(i);
	}

	private void writeFileName(DataOutputStream dos, String s) throws IOException {
		dos.writeUTF("filename");
		dos.writeUTF(s);
	}

	private void writeFileSize(DataOutputStream dos, long l) throws IOException {
		dos.writeUTF("filesize");
		dos.writeLong(l);
	}

	private void writeOffset(DataOutputStream dos, long l) throws IOException {
		dos.writeUTF("offset");
		dos.writeLong(l);
	}

	private void writeMD5Hash(DataOutputStream dos, File f) throws IOException {
		pw.printToTextbox("Calculating MD5 hash: ");
		dos.writeUTF("md5");
		dos.write(MD5.getHash(f));
		pw.printToTextbox("DONE\n");
	}

	private void writeBufferSize(DataOutputStream dos, int i) throws IOException {
		dos.writeUTF("buffersize");
		dos.writeInt(i);
	}

	private void writeFile(OutputStream os, File file) throws FileNotFoundException, IOException {
		InputStream is = new FileInputStream(file);
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF("content");
		try {
			is = new FileInputStream(file);
			byte[] buffer = new byte[Integer.parseInt(FileTransferTool.getProperty("buffer_size"))];
			long bytesReadAll = 0, filesize = file.length();
			int bytesRead = 0;
			long startTime = System.currentTimeMillis();
			while((bytesRead = is.read(buffer)) != -1) {
				if(aborted == true) {
					return;
				}

				try {
					os.write(buffer, 0, bytesRead);
				} catch(IOException e) {
					JOptionPane.showMessageDialog(pw, "Peer has aborted the transfer!");
					return;
				}
				bytesReadAll += bytesRead;
				double progress = ((double) bytesReadAll / (double) filesize) * 10000;
				pw.setProgress((int) Math.round(progress));
				double speed = Math.round((double) (bytesReadAll / 1024) / (System.currentTimeMillis() - startTime) * 100) / 100;
				pw.setLabel(FileUtils.byteCountToDisplaySize(bytesReadAll) + "/" + FileUtils.byteCountToDisplaySize(filesize) + " (" + bytesReadAll + "/" + filesize + ") @ " + speed + " kb/s");
			}
		} finally {
			is.close();
		}
	}

	void setAborted() {
		aborted = true;
	}

	void receive() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

}
