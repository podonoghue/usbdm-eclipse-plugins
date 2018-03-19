package net.sourceforge.usbdm.peripheralDatabase;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;


public class PeripheralDatabaseMerger {


   // Where to write files
   private Path   xmlRootPath   = Paths.get("");

   public final static String PERIPHERAL_FOLDER = "peripherals";
   public final static String VECTOR_FOLDER     = "vectorTables";
   public final static String XML_EXTENSION     = ".svd.xml";

   public void setXmlRootPath(File file) {
      xmlRootPath = file.toPath();
   }

   public Path getXmlRootPath() {
      return xmlRootPath;
   }

   /**
    * Returns path to peripheral file
    * 
    * @param basename base name of peripheral file
    * @return
    */
   public Path getDevicePath(String basename) {
      return getXmlRootPath().resolve(basename+XML_EXTENSION);
   }

   /**
    * Returns path to peripheral file
    * 
    * @param basename base name of peripheral file
    * @return
    */
   public Path getPeripheralPath(String basename) {
      return getXmlRootPath().resolve(PERIPHERAL_FOLDER).resolve(basename+XML_EXTENSION);
   }

   /**
    * Returns path to vector table file
    * 
    * @param basename base name of vector table file
    * @return
    */
   public Path getVectorTablePath(String basename) {
      return getXmlRootPath().resolve(VECTOR_FOLDER).resolve(basename+XML_EXTENSION);
   }

   private final HashSet<String> uniqueFilenames = new HashSet<String>();

   /**
    * Create file name for a peripheral
    * 
    * @param uniqueFilenames  Table used to prevent name collisions
    * @param peripheral Peripheral to create file name for
    * 
    * @return Name based on peripheral. Names are unique.
    * 
    * @throws Exception
    */
   private String getPeripheralFilename(Peripheral peripheral) throws Exception {
      if (ModeControl.isRenameSimSource() && (peripheral.getName().equals("SIM") && peripheral.getUsedBy().size() == 1)) {
         peripheral.setSourceFilename("SIM_" + peripheral.getUsedBy().get(0));
      }
      String filename = peripheral.getSourceFilename();
      if (filename == null) {
         // Create filename
         String name;
         if (peripheral.getDerivedFrom() != null) {
            name = peripheral.getName()+"_from_"+peripheral.getDerivedFrom().getName();
         }
         else {
            name = peripheral.getName();
         }
         filename = name + "_" + peripheral.getUsedBy().get(0);
         peripheral.setSourceFilename(filename);
      }
      if (uniqueFilenames.contains(filename)) {
         throw new Exception("Peripheral should have unique ID set");
      }
      return filename;
   }

   int uniqueVectorTableIdNumber = 0;
   HashSet<String> usedVectorTableFilenames = new HashSet<String>();

   /**
    * Create file name for a vectorTable
    * 
    * @param vectorTable
    * 
    * @return Name based on vectorTable. Names are unique.
    * 
    * @throws Exception
    */
   private String getVectorTableFilename(VectorTable vectorTable) throws Exception {

      String name = vectorTable.getName();
      if ((name == null) || (name.length() == 0)) {
         name = currentDeviceName+"_VectorTable";
         vectorTable.setName(name);
      }
      if (usedVectorTableFilenames.contains(name)) {
         throw new Exception("Non-unique vector table name");
      }
      return name;
   }

   /**
    * This writes each peripheral to a separate file with an unique name
    * 
    * @throws FileNotFoundException
    * @throws Exception
    */
   public void writePeripheralsToSVD() throws Exception {
      Iterator<Entry<String, ArrayList<Peripheral>>> it = peripheralMap.entrySet().iterator();
      // Make directory for peripherals
      getXmlRootPath().resolve(PERIPHERAL_FOLDER).toFile().mkdir();
      while (it.hasNext()) {
         Entry<String, ArrayList<Peripheral>> pairs = it.next();
         for (Peripheral peripheral : pairs.getValue()) {
            PrintWriter writer = new PrintWriter(getPeripheralPath(getPeripheralFilename(peripheral)).toFile());
            // Written with no owner so defaults are NOT inherited 
            peripheral.writeSVD(writer, false, null);
            writer.close();
         }
      }
   }

   /**
    * This writes each vector table to a separate file
    * 
    * @throws FileNotFoundException
    * @throws Exception
    */
   public void writeVectorTablesToSVD() throws Exception {
      getXmlRootPath().resolve(VECTOR_FOLDER).toFile().mkdir();
      for (VectorTable vectorTable : vectorTableList) {
         PrintWriter writer = new PrintWriter(getVectorTablePath(getVectorTableFilename(vectorTable)).toFile());
         vectorTable.writeSVDInterruptEntries(writer, true);
         writer.close();
      }
   }

   HashMap<String, ArrayList<Peripheral>> peripheralMap   = new HashMap<String, ArrayList<Peripheral>>(200);
   ArrayList<VectorTable>                 vectorTableList = new ArrayList<VectorTable>();

   private void addVectortableToList(DevicePeripherals device) throws Exception {
      VectorTable newVectorTable = device.getVectorTable();
      int index;
      for(index=0; index<vectorTableList.size(); index++) {
         VectorTable vectorTable = vectorTableList.get(index);
         if (newVectorTable.equals(vectorTable)) {
            // Add usage information
            vectorTable.addUsedBy(device.getName());
            // Remove redundant VT
            device.setVectorTable(vectorTable);
            return;
         }
      }
      // First time the vector table is used - clear references
      newVectorTable.clearUsedBy();
      newVectorTable.addUsedBy(device.getName());

      // Add to list of know peripherals
      vectorTableList.add(newVectorTable);
   }

   /**
    * Adds a peripheral to the list of shared peripherals
    * 
    * @param  device
    * @param  newPeripheral
    * 
    * @throws Exception 
    */
   void addPeripheralToMap(DevicePeripherals device, Peripheral newPeripheral) throws Exception {
      if (newPeripheral.getDerivedFrom() != null) {
         // Already derived - ignore
         return;
      }
      // TODO - Where common peripherals are factored out
      ArrayList<Peripheral> peripheralList = peripheralMap.get(newPeripheral.getName());
      if (peripheralList == null) {
         // First peripheral of that name
         peripheralList = new ArrayList<Peripheral>(20);
         peripheralMap.put(newPeripheral.getName(), peripheralList);
      }
      // Check if equivalent to an exiting peripheral
      for(int index=0; index<peripheralList.size(); index++) {
         Peripheral peripheral = peripheralList.get(index);
//         if (newPeripheral.getSourceFilename().equals("FGPIOA_MKE") && 
//               newPeripheral.getSourceFilename().equals(peripheral.getSourceFilename())) {
//            System.err.println("Checking, "+newPeripheral.getSourceFilename()+" <> "+peripheral.getSourceFilename());
//         }
         if (newPeripheral.equivalent(peripheral)) {
            // Found equivalent
            peripheral.addUsedBy(device.getName());
            newPeripheral.setFilename(peripheral.getFilename());
            return;
         }
         if (peripheral.getSourceFilename().equals(newPeripheral.getSourceFilename())) {
            newPeripheral.equivalent(peripheral);
            throw new Exception("Opps, "+newPeripheral.getSourceFilename()+" <> "+peripheral.getSourceFilename());
         }
      }
      // First time the device is used - clear references etc
      newPeripheral.clearUsedBy();
      newPeripheral.addUsedBy(device.getName());

      // Add unique file path
      newPeripheral.setFilename(getPeripheralFilename(newPeripheral));

      // Add to list of know peripherals
      peripheralList.add(newPeripheral);
   }

   private final String xmlPreamble =  
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<!DOCTYPE device>\n"
               ;

   private String currentDeviceName;

   public final static String VECTOR_TABLE_ENTITY = "VECTOR_TABLE";

   /**
    * Writes a description of the device to a SVD file.
    * Adds device peripherals to the list of shared peripherals.
    * Uses ENTITY references for the (shared) peripherals used by the device.
    * 
    * @param  device
    * @throws FileNotFoundException
    * @throws Exception
    */
   public void writeDeviceToSVD(DevicePeripherals device) throws Exception {

      currentDeviceName = device.getName();

      Path devicePath = getDevicePath(device.getName());

      PrintWriter writer = new PrintWriter(devicePath.toFile());
      writer.print(xmlPreamble);

      device.optimise();
      device.sortPeripheralsByName();

      // Look for shared vector tables
      addVectortableToList(device);

      for (Peripheral peripheral : device.getPeripherals()) {
         // Searches for shared peripheral reference and adds new one as needed
         addPeripheralToMap(device, peripheral);
      }
      device.writeSVD(writer, false, 20);
      writer.close();
   }

}
