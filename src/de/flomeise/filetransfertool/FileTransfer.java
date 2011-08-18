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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.swing.JFileChooser;
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

	public FileTransfer(Socket socket, boolean isRecipient) {
		this.socket = socket;
		this.isRecipient = isRecipient;
		pw = new ProgressWindow(this);
		pw.setMaxProgress(10000);
		pw.setVisible(true);
		if(isRecipient) {
			if(JOptionPane.showConfirmDialog(null, socket.getInetAddress().getAddress() + " tries to establish a connection to you. Allow?", "Incoming connection", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				try {
					socket.close();
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public FileTransfer(String address, int port, boolean isRecipient) throws IOException {
		socket = new Socket(address, port);
		this.isRecipient = isRecipient;
		pw = new ProgressWindow(this);
		pw.setMaxProgress(10000);
		pw.setVisible(true);
		if(isRecipient) {
			if(JOptionPane.showConfirmDialog(null, socket.getInetAddress().getAddress() + " tries to establish a connection to you. Allow?", "Incoming connection", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
				try {
					socket.close();
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public ProgressWindow getProgressWindow() {
		return pw;
	}

	public void send(String file) {
		send(new File(file));
	}

	public boolean send(File file) {
		try {
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			DataInputStream dis = new DataInputStream(is);
			DataOutputStream dos = new DataOutputStream(os);
			long offset = 0;

			writeFileName(dos, file.getName());
			writeFileSize(dos, file.length());
			writeMD5Hash(dos, file);
			writeBufferSize(dos, Integer.parseInt(FileTransferTool.getProperty("buffer_size")));
			writeEnd(dos);

			String msg;
			while((msg = dis.readUTF()).equals("end")) {
				switch(msg) {
					case "accept":
						if(dis.readBoolean() == false) {
							pw.printToTextbox("Transfer of file " + file.getName() + " refused!\n");
							return false;
						}
						break;
					case "offset":
						offset = dis.readLong();
						break;
				}
			}

			int tryCount = 1;

			while(true) {
				writeFile(dos, file, offset);
				if(pw.isAborted()) {
					return false;
				}

				if(dis.readBoolean() == false) {
					pw.printToTextbox("Transfer failed, starting retry " + tryCount + "\n");
					tryCount++;
				} else {
					return true;
				}
			}

		} catch(IOException ex) {
			ex.printStackTrace();
		} finally {
			pw.dispose();
		}
		return false;
	}

	private void writeEnd(DataOutputStream dos) throws IOException {
		dos.writeUTF("end");
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

	private void writeAccept(DataOutputStream dos, boolean b) throws IOException {
		dos.writeUTF("accept");
		dos.writeBoolean(b);
	}

	private void writeFile(OutputStream os, File file, long offset) throws FileNotFoundException, IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeUTF("content");
		try(InputStream is = new FileInputStream(file)) {
			if(is.skip(offset) < offset) {
				JOptionPane.showMessageDialog(pw, "Could not skip to offset!");
				throw new IOException("Could not skip to offset!");
			}
			byte[] buffer = new byte[Integer.parseInt(FileTransferTool.getProperty("buffer_size"))];
			long bytesReadAll = offset, filesize = file.length();
			int bytesRead = 0;
			long startTime = System.currentTimeMillis();
			while((bytesRead = is.read(buffer)) != -1) {
				if(pw.isAborted()) {
					return;
				}

				try {
					os.write(buffer, 0, bytesRead);
				} catch(IOException e) {
					JOptionPane.showMessageDialog(pw, "Peer has aborted the transfer!");
					throw new IOException("Peer has aborted the transfer!");
				}
				bytesReadAll += bytesRead;
				double progress = ((double) bytesReadAll / (double) filesize) * 10000;
				pw.setProgress((int) Math.round(progress));
				double speed = Math.round((double) (bytesReadAll / 1024) / (System.currentTimeMillis() - startTime) * 100) / 100;
				pw.setLabel(FileUtils.byteCountToDisplaySize(bytesReadAll) + "/" + FileUtils.byteCountToDisplaySize(filesize) + " (" + bytesReadAll + "/" + filesize + ") @ " + speed + " kb/s");
			}
		}
	}

	public void receive() {
		InputStream is = null;
		try {
			is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			DataInputStream dis = new DataInputStream(is);
			DataOutputStream dos = new DataOutputStream(os);
			
			String saveDir = FileTransferTool.getProperty("save_dir");
			boolean first = true;

			while(true) {
				String filename = "";
				int bufferSize = -1;
				byte[] md5 = null;
				long filesize = -1, offset = 0;
				String msg;
				while(!(msg = dis.readUTF()).equals("end")) {
					switch(msg) {
						case "filename":
							filename = dis.readUTF();
							break;
						case "filesize":
							filesize = dis.readLong();
							break;
						case "md5":
							dis.read(md5);
							break;
						case "buffersize":
							bufferSize = dis.readInt();
							break;
					}
				}

				if(filename.equals("") || filesize == -1 || bufferSize == -1 || md5 == null) {
					writeAccept(dos, false);
					return;
				}
				
				File file = new File(saveDir, filename);
				if(first) {	
					if(FileTransferTool.getProperty("save_dialog").equals("true")) {
						file = new File(FileOpenDialog.open(JFileChooser.SAVE_DIALOG, file));
						saveDir = file.getParent();
						first = false;
					}
				}
				
				if(file.isDirectory()) {
					writeAccept(dos, false);
					JOptionPane.showMessageDialog(pw, "You can't overwrite a directory!");
					return;
				}

				if(file.exists()) {
					int result = JOptionPane.showConfirmDialog(pw, "The file you are trying to download already exists! Press yes to overwrite, no to resume a previous transfer or cancel to abort the transfer.", "Problem", JOptionPane.YES_NO_CANCEL_OPTION);
					if(result == JOptionPane.YES_OPTION) {
						file.delete();
					} else if(result == JOptionPane.NO_OPTION) {
						offset = file.length();
					} else if(result == JOptionPane.CANCEL_OPTION) {
						return;
					}
				}
				
				writeAccept(dos, true);
				writeOffset(dos, offset);
				writeEnd(dos);
				
				try(FileOutputStream fos = new FileOutputStream(file)) {
					byte[] buffer = new byte[bufferSize];
					int bytesRead = 0;
					long bytesReadAll = offset;
					long startTime = System.currentTimeMillis();
					do {
						if(pw.isAborted() == true) {
							if(JOptionPane.showConfirmDialog(pw, "Would you really like to abort the transfer?", "Abort", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
								return;
							}
						}

						bytesReadAll += (bytesRead = is.read(buffer));
						fos.write(buffer, 0, bytesRead);
						
						double progress = ((double) bytesReadAll / (double) filesize) * 10000;
						pw.setProgress((int) Math.round(progress));
						double speed = Math.round((double) (bytesReadAll / 1024) / (System.currentTimeMillis() - startTime) * 100) / 100;
						pw.setLabel(FileUtils.byteCountToDisplaySize(bytesReadAll) + "/" + FileUtils.byteCountToDisplaySize(filesize) + " (" + bytesReadAll + "/" + filesize + ") @ " + speed + " kb/s");
					} while(bytesReadAll < filesize);
				}
			}
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	public void exit() {
		pw.dispose();
		try {
			socket.close();
		} catch(IOException ex) {
			ex.printStackTrace();
		}
	}

}
