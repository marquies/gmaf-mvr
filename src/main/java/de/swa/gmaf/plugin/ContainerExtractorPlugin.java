package de.swa.gmaf.plugin;

import java.io.*;
import java.net.URL;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.swa.gmaf.GMAF;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

/**
 * Plugin to handle container files (ZIP) by extracting and processing each contained file
 * using the existing ProcessFlow system
 */
public class ContainerExtractorPlugin implements GMAF_Plugin {
    private final Vector<Node> detectedNodes = new Vector<>();
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + File.separator + "container" + File.separator;

    @Override
    public boolean canProcess(String extension) {
        return extension.toLowerCase().endsWith("zip");
    }

    @Override
    public void process(URL url, File f, byte[] bytes, MMFG fv) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            // Create temp directory if it doesn't exist
            new File(TEMP_DIR).mkdirs();
            
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // Create temp file for the entry
                    File tempFile = new File(TEMP_DIR + entry.getName());
                    tempFile.getParentFile().mkdirs();
                    
                    // Extract the file
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    
                    // Process the extracted file using GMAF
                    try {
                        // Create a new GMAF instance to process this file
                        GMAF gmaf = new GMAF();
                        MMFG entryFv = gmaf.processAsset(tempFile);
                        
                        if (entryFv != null) {
                            // Add container context to the feature vector
                            Node containerNode = new Node("Container", fv);
                            containerNode.addChildNode(entryFv.getNodes().get(0));
                            fv.addNode(containerNode);
                            
                            // Collect all nodes from the processed file
                            detectedNodes.addAll(entryFv.getNodes());
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing container file entry: " + entry.getName());
                        e.printStackTrace();
                    } finally {
                        // Clean up temp file
                        tempFile.delete();
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            System.err.println("Error processing container file: " + f.getName());
            e.printStackTrace();
        }
    }

    @Override
    public boolean providesRecoursiveData() {
        return true;
    }

    @Override
    public boolean isGeneralPlugin() {
        return false;
    }

    @Override
    public Vector<Node> getDetectedNodes() {
        return detectedNodes;
    }
}
