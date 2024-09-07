package com.raven;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class FileUploaderLibrary {

    private JPanel fileContainer;
    private final List<File> files;
    private final Component deleteComponent;
    private final Component browseComponent;
    private int imageWidth;
    private int imageHeight;
    private int imageMargin = 5; // Margin between images

    public FileUploaderLibrary(JPanel fileContainer, Component deleteComponent, Component browseComponent) {
        this(fileContainer, deleteComponent, browseComponent, 67, 82); // Default size
    }

    public FileUploaderLibrary(JPanel fileContainer, Component deleteComponent, Component browseComponent, int imageWidth, int imageHeight) {
        this.fileContainer = fileContainer;
        this.deleteComponent = deleteComponent;
        this.browseComponent = browseComponent;
        this.files = new ArrayList<>();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;

        init();
    }

    private void init() {
        if (fileContainer == null) {
            fileContainer = new JPanel();
        }
        fileContainer.setLayout(new BorderLayout()); // Use BorderLayout for the container

        // Create scrollable panel with GridBagLayout for matrix-like arrangement
        JPanel scrollablePanel = new JPanel();
        scrollablePanel.setLayout(new GridBagLayout()); // Use GridBagLayout for matrix-like arrangement
        JScrollPane scrollPane = new JScrollPane(scrollablePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Adjust scroll speed
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
        verticalScrollBar.setUnitIncrement(20); // Adjust as needed
        verticalScrollBar.setBlockIncrement(100); // Adjust as needed
        horizontalScrollBar.setUnitIncrement(20); // Adjust as needed
        horizontalScrollBar.setBlockIncrement(100); // Adjust as needed

        fileContainer.add(scrollPane, BorderLayout.CENTER); // Add scroll pane to center of BorderLayout

        if (deleteComponent instanceof JButton) {
            ((JButton) deleteComponent).setEnabled(false);
            ((JButton) deleteComponent).addActionListener(e -> deleteSelectedFiles());
        } else if (deleteComponent instanceof JLabel) {
            deleteComponent.setForeground(Color.RED);
            deleteComponent.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteComponent.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    deleteSelectedFiles();
                }
            });
        }

        if (browseComponent instanceof JButton) {
            ((JButton) browseComponent).addActionListener(e -> chooseFiles());
        } else if (browseComponent instanceof JLabel) {
            browseComponent.setForeground(Color.BLUE);
            browseComponent.setCursor(new Cursor(Cursor.HAND_CURSOR));
            browseComponent.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    chooseFiles();
                }
            });
        }

        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        new DropTarget(fileContainer, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = e.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        droppedFiles.forEach(FileUploaderLibrary.this::addFile);
                        showFiles();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

private void chooseFiles() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setMultiSelectionEnabled(true);
    
    // Define file filters
    FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif", "bmp", "tiff");
    FileNameExtensionFilter otherFilter = new FileNameExtensionFilter("Document and Archive Files", "txt", "zip", "pdf", "doc", "docx", "xls", "xlsx");
    FileNameExtensionFilter allFilesFilter = new FileNameExtensionFilter("All Supported Files", "jpg", "jpeg", "png", "gif", "bmp", "tiff", "txt", "zip", "pdf", "doc", "docx", "xls", "xlsx");
    
    // Add filters to the chooser
    fileChooser.addChoosableFileFilter(imageFilter);
    fileChooser.addChoosableFileFilter(otherFilter);
    fileChooser.addChoosableFileFilter(allFilesFilter);
    
    // Set default filter
    fileChooser.setFileFilter(allFilesFilter);
    
    int result = fileChooser.showOpenDialog(fileContainer);
    if (result == JFileChooser.APPROVE_OPTION) {
        for (File file : fileChooser.getSelectedFiles()) {
            if (isSupportedFile(file)) {
                addFile(file);
            } else {
                JOptionPane.showMessageDialog(fileContainer, "Unsupported file type: " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        showFiles();
    }
}


    private void addFile(File file) {
        if (!files.contains(file)) {
            files.add(file);
        }
    }

    private void deleteSelectedFiles() {
        List<File> toRemove = new ArrayList<>();
        for (Component comp : fileContainer.getComponents()) {
            if (comp instanceof JScrollPane) {
                JPanel scrollablePanel = (JPanel) ((JScrollPane) comp).getViewport().getView();
                for (Component subComp : scrollablePanel.getComponents()) {
                    if (subComp instanceof FileLabel1) {
                        FileLabel1 label = (FileLabel1) subComp;
                        if (label.isSelected()) {
                            toRemove.add(label.getFile());
                        }
                    }
                }
            }
        }
        files.removeAll(toRemove);
        showFiles();
    }

private void showFiles() {
    for (Component comp : fileContainer.getComponents()) {
        if (comp instanceof JScrollPane) {
            JPanel scrollablePanel = (JPanel) ((JScrollPane) comp).getViewport().getView();
            scrollablePanel.removeAll(); // Remove all existing components

            // Reset GridBagConstraints
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = new Insets(imageMargin, imageMargin, imageMargin, imageMargin);
            gbc.anchor = GridBagConstraints.NORTHWEST;

            int x = 0;
            int y = 0;
            int containerWidth = scrollablePanel.getWidth();

            for (File file : files) {
                ImageIcon icon = null;

                if (isImageFile(file)) {
                    icon = createImageIcon(file);
                } else {
                    // Use a default icon from UIManager, no casting to ImageIcon
                    Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
                    if (fileIcon instanceof ImageIcon) {
                        icon = (ImageIcon) fileIcon;
                    }
                }

                FileLabel1 fileLabel;
                if (icon != null) {
                    fileLabel = new FileLabel1(icon, file);
                } else {
                    // Handle the case where no valid icon is found
                    fileLabel = new FileLabel1(null, file);
                    fileLabel.setText(file.getName()); // Set text for non-image files
                }

                fileLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                fileLabel.setPreferredSize(new Dimension(imageWidth, imageHeight));

                // Check if we need to move to a new row
                if (x + imageWidth > containerWidth) {
                    x = 0;
                    y++;
                }

                gbc.gridx = x++;
                gbc.gridy = y;

                scrollablePanel.add(fileLabel, gbc);
            }

            scrollablePanel.revalidate();
            scrollablePanel.repaint();
            break;
        }
    }
    if (deleteComponent instanceof JButton) {
        ((JButton) deleteComponent).setEnabled(!files.isEmpty());
    }
}

    private ImageIcon createImageIcon(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                return new ImageIcon(img.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

private boolean isImageFile(File file) {
    String[] validExtensions = { "jpg", "jpeg", "png", "gif", "bmp", "tiff" };
    String fileName = file.getName().toLowerCase();
    for (String ext : validExtensions) {
        if (fileName.endsWith(ext)) {
            return true;
        }
    }
    return false;
}

private boolean isSupportedFile(File file) {
    String[] validExtensions = { "jpg", "jpeg", "png", "gif", "bmp", "tiff", "txt", "zip", "pdf", "doc", "docx", "xls", "xlsx" }; // Add more as needed
    String fileName = file.getName().toLowerCase();
    for (String ext : validExtensions) {
        if (fileName.endsWith(ext)) {
            return true;
        }
    }
    return false;
}


public class FileLabel1 extends JLabel {
    private final File file;
    private boolean selected;

    public FileLabel1(ImageIcon icon, File file) {
        super(icon);
        this.file = file;
        this.selected = false;
        setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        setHorizontalAlignment(SwingConstants.CENTER); // Center the text and icon

        // If the file is not an image, display the default icon and name properly
        if (!isImageFile(file)) {
            Icon fileIcon = getDefaultFileIcon(file);
            if (fileIcon != null) {
                setIcon(fileIcon);  // Set the default icon for the file type
            }

            // Show the file name directly below the icon
            setText(getTruncatedFileName(file.getName()));
            setIconTextGap(10); // Gap between icon and text
        } else {
            // For image files, you might want to display the image thumbnail
            setText(file.getName());
            setHorizontalTextPosition(SwingConstants.CENTER);
            setVerticalTextPosition(SwingConstants.BOTTOM);
        }

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 1) { // Single click to select/deselect
                    toggleSelection();
                } else if (evt.getClickCount() == 2) { // Double click to open file
                    openFile();
                }
            }
        });
    }

    // Method to retrieve the default file icon (e.g., for text files)
    private Icon getDefaultFileIcon(File file) {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        return fileSystemView.getSystemIcon(file); // Get the system's default file icon
    }

    public File getFile() {
        return file;
    }

    public boolean isSelected() {
        return selected;
    }

    private void toggleSelection() {
        selected = !selected;
        setBorder(BorderFactory.createLineBorder(selected ? Color.RED : Color.BLACK, 2));
    }

    private void openFile() {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                if (file.exists()) {
                    desktop.open(file); // Open the file with the system default application
                } else {
                    JOptionPane.showMessageDialog(this, "File not found: " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error opening file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Desktop operations not supported on this system.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getTruncatedFileName(String fileName) {
        int maxLength = 15; // Set max visible length for the file name
        if (fileName.length() > maxLength) {
            return fileName.substring(0, maxLength) + "..."; // Truncate the file name
        }
        return fileName;
    }

    private boolean isImageFile(File file) {
        String[] validExtensions = { "jpg", "jpeg", "png", "gif", "bmp", "tiff" };
        String fileName = file.getName().toLowerCase();
        for (String ext : validExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}

}