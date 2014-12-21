package net.sourceforge.usbdm.peripheralDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class PeripheralDatabaseMerger {

   // Where to write files
   IPath xmlRootPath   = Path.EMPTY;
   String xmlExtension = ".svd.xml";
   
   public void setXmlExtension(String xmlExtension) {
      this.xmlExtension = xmlExtension;
   }
   
   public void setXmlRootPath(File file ) {
      xmlRootPath = new Path(file.getAbsolutePath());
   }
   
   public IPath getXmlRootPath() {
      return xmlRootPath;
   }
   
   public IPath getXmlFilepath(String fileName) {
      IPath path = xmlRootPath.append(fileName+xmlExtension);
//      System.out.println("PeripheralDatabaseMerger.getXmlFilepath() => \"" + path.toOSString() + "\"");
      return path;
   }

   private final String peripheralFolder = "peripherals";
   private final String vectorFolder     = "vectorTables";
   
   /**
    * Create file name for a peripheral
    * 
    * @param suffix
    * @param uniqueIds 
    * @param name       null to return perent directory
    * @return
    * @throws Exception 
    */
   private IPath getPeripheralFilepath(HashSet<String> uniqueIds, Peripheral peripheral) throws Exception {
      String name;
      if (peripheral == null) {
         return getXmlFilepath(peripheralFolder).removeFileExtension().removeFileExtension();
      }
      if (peripheral.getDerivedFrom() != null) {
         name = peripheral.getName()+"_from_"+peripheral.getDerivedFrom().getName();
      }
      else {
         name = peripheral.getName();
      }
      String uniqueId = peripheral.getSourceFilename();
      assert ((uniqueId != null) || (uniqueIds != null)) : "Peripheral should have unique ID set";
      if ((uniqueId == null) && (uniqueIds == null)) {
         throw new Exception("Peripheral should have unique ID set");
      }
      int index = 0;
      if (uniqueIds != null) {
         // Generate new uniqueId as needed
         while ((uniqueId == null) || uniqueIds.contains(uniqueId)) {
            uniqueId = name+"_"+index++;
         }
      }
      peripheral.setSourceFilename(uniqueId);
      return getXmlFilepath(peripheralFolder+Path.SEPARATOR+uniqueId);
   }

   int uniqueVectorTableIdNumber = 0;
   HashSet<String> usedVectorTableFilenames = new HashSet<String>();
   
   /**
    * Create file name for a vectorTable
    * 
    * @param suffix
    * @param uniqueIds 
    * @param name       null to return perent directory
    * @return
    * @throws Exception 
    */
   private IPath getVectorTableFilepath(VectorTable vectorTable) throws Exception {
      String name;
      if (vectorTable == null) {
         return getXmlFilepath(vectorFolder).removeFileExtension().removeFileExtension();
      }
      name = vectorTable.getName();
      if ((name == null) || (name.length() == 0)) {
         name = currentDeviceName+"_VectorTable";
      }
      if (usedVectorTableFilenames.contains(name)) {
         name = name + uniqueVectorTableIdNumber++;
      }
      vectorTable.setName(name);
      return getXmlFilepath(vectorFolder+Path.SEPARATOR+name);
   }

   /**
    * This writes each peripheral to a separate file with an unique name
    * 
    * @throws FileNotFoundException
    * @throws Exception
    */
   public void writePeripheralsToSVD() throws FileNotFoundException, Exception {
      Iterator<Entry<String, ArrayList<Peripheral>>> it = peripheralMap.entrySet().iterator();
      if (it.hasNext()) {
         // Make directory for peripherals
         getPeripheralFilepath(null, null).toFile().mkdir();
      }
      while (it.hasNext()) {
         Entry<String, ArrayList<Peripheral>> pairs = it.next();
         for (Peripheral peripheral : pairs.getValue()) {
            PrintWriter writer = new PrintWriter(getPeripheralFilepath(null, peripheral).toFile());
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
   public void writeVectorTablesToSVD() throws FileNotFoundException, Exception {
      getVectorTableFilepath(null).toFile().mkdir();
      for (VectorTable vectorTable : vectorTableList) {
         PrintWriter writer = new PrintWriter(getVectorTableFilepath(vectorTable).toFile());
         vectorTable.writeSVDInterruptEntries(writer, true);
         writer.close();
      }
   }
   
   HashMap<String, ArrayList<Peripheral>> peripheralMap   = new HashMap<String, ArrayList<Peripheral>>(200);
   ArrayList<VectorTable>                 vectorTableList = new ArrayList<VectorTable>();

   private IPath addVectortableToList(DevicePeripherals device) throws Exception {
      VectorTable newVectorTable = device.getVectorTable();
      int index;
      for(index=0; index<vectorTableList.size(); index++) {
         VectorTable vectorTable = vectorTableList.get(index);
         if (newVectorTable.equals(vectorTable)) {
            vectorTable.addUsedBy(device.getName());
            return getVectorTableFilepath(vectorTable);
         }
      }
      // First time the vector table is used - clear references
      newVectorTable.clearUsedBy();
      newVectorTable.addUsedBy(device.getName());

      // Add to list of know peripherals
      vectorTableList.add(newVectorTable);
      
      return getVectorTableFilepath(newVectorTable);      
   }
   
   /**
    * Adds a peripheral to the list of shared peripherals
    * 
    * @param  device
    * @param  newPeripheral
    * 
    * @return Path of SVD file representing the (shared) peripheral
    * @throws Exception 
    */
   private IPath addPeripheralToMap(DevicePeripherals device, Peripheral newPeripheral) throws Exception {
      if (newPeripheral.getDerivedFrom() != null) {
         return null;
      }
      // TODO - Where common peripherals are factored out
      ArrayList<Peripheral> peripheralList = peripheralMap.get(newPeripheral.getName());
      if (peripheralList == null) {
         peripheralList = new  ArrayList<Peripheral>(20);
         peripheralMap.put(newPeripheral.getName(), peripheralList);
      }
      int index;
      HashSet<String> uniqueIds = new HashSet<String>();
      for(index=0; index<peripheralList.size(); index++) {
         Peripheral peripheral = peripheralList.get(index);
         uniqueIds.add(peripheral.getSourceFilename());
         if (newPeripheral.equivalent(peripheral)) {
            peripheral.addUsedBy(device.getName());
            return getPeripheralFilepath(null, peripheral);
         }
      }
      // First time the device is used - clear references
      newPeripheral.clearUsedBy();
      newPeripheral.addUsedBy(device.getName());

      // Add to list of know peripherals
      peripheralList.add(newPeripheral);
      
      return getPeripheralFilepath(uniqueIds, newPeripheral);      
   }

   private final String preambleStart =  
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
         "<!DOCTYPE device\n" + 
         "[\n"
       ;

   private final String preambleEnd =  
         "]>\n"
         ;
   
   private final String systemEntityFormatString = "<!ENTITY %-12s SYSTEM \"%s\">\n";
   private String currentDeviceName;

   public final static String VECTOR_TABLE_ENTITY = "VECTOR_TABLE";
   
   /**
    * Writes a description of the device to a SVD file.
    * Adds device peripherals to the list of shared peripherals.
    * Uses ENTITY references for the (shared) peripherals used by the device.
    * 
    * @param device
    * @throws FileNotFoundException
    * @throws Exception
    */
   public void writeDeviceToSVD(DevicePeripherals device) throws FileNotFoundException, Exception {
      
      this.currentDeviceName = device.getName();
      IPath devicePath = getXmlFilepath(device.getName());
      PrintWriter writer = new PrintWriter(devicePath.toFile());
      writer.print(preambleStart);

      device.optimise();
      device.sortPeripheralsByName();
      
      IPath filename = addVectortableToList(device).makeRelativeTo(getXmlRootPath());
      writer.print(String.format(systemEntityFormatString, VECTOR_TABLE_ENTITY, filename));
      
      for (Peripheral peripheral : device.getPeripherals()) {
         if (peripheral.getDerivedFrom() == null) {
            // Look up shared peripheral reference - adds new one as needed
            IPath peripheralPath = addPeripheralToMap(device, peripheral).makeRelativeTo(getXmlRootPath());
            writer.print(String.format(systemEntityFormatString, peripheral.getName(), peripheralPath));
         }
//         else {
//            StringWriter sWriter = new StringWriter();
//            PrintWriter pWriter = new PrintWriter(sWriter);
//            peripheral.writeSVD(pWriter, false, device);
//            String entity = SVD_XML_BaseParser.escapeString(sWriter.toString());
//            writer.print(String.format(entityFormatString, peripheral.getName(), entity));
//         }
      }
      writer.print(preambleEnd);
      device.writeSVD(writer, false);
      writer.close();
   }

}
