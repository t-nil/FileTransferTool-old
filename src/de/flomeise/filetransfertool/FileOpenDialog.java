package de.flomeise.filetransfertool;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JFileChooser;

/**
 * 
 * @author Yannick
 */
public class FileOpenDialog {
	private static File lastOpened;

	/**
	 * 
	 * @param type
	 * @param current
	 * @return
	 */
	public static String open(int type, File current) {
		return open(null, type, current);
	}

	/**
	 * Shows the file open dialog
	 * @param parent
	 * @param type
	 * @param current
	 * @return
	 */
	public static String open(java.awt.Component parent, int type, File current) {
		final JFileChooser chooser = new JFileChooser("Verzeichnis wählen");
		chooser.setDialogType(type);
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		if(lastOpened == null) {
			lastOpened = new File(FileTransferTool.getProperty("save_dir"));
		}

		if(current == null) {
			chooser.setSelectedFile(lastOpened);
		} else {
			chooser.setSelectedFile(current);
		}

		chooser.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if(e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)
				   || e.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
					final File f = (File) e.getNewValue();
				}
			}

		});

		chooser.setVisible(true);
		final int result;
		if(type == JFileChooser.SAVE_DIALOG) {
			result = chooser.showSaveDialog(parent);
		} else {
			result = chooser.showOpenDialog(parent);
		}

		if(result == JFileChooser.APPROVE_OPTION) {
			File inputVerzFile = chooser.getSelectedFile();
			lastOpened = new File(inputVerzFile, "..");
			String inputVerzStr = inputVerzFile.getPath();
			return inputVerzStr;
		}
		chooser.setVisible(false);
		return null;
	}

}
