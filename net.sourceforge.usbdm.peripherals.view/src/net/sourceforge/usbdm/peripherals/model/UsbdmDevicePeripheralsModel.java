package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;
import java.util.HashSet;

import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripherals.view.GdbCommonInterface;

/**
 *  Object used to represent the peripheral data from target
 *  Contains a model for the view and related information.
 */
public class UsbdmDevicePeripheralsModel {

   /**
    * Creates the tree entries for the bit-fields within the register
    * 
    * @param registerModel The model that represents the register and owns the bit-fields
    * @param register      The register to obtain bit-field information from
    */
   private void createFieldModels(RegisterModel registerModel, Register register) {
      
      ArrayList<Field> fieldList = register.getFields();
      if (fieldList != null) {
         for (Field field : fieldList) {
            if (!field.isHidden()) {
               FieldModel fieldModel = new FieldModel(registerModel, field);
               fieldModel.setEnumeratedDescription(field.getEnumerations());
            }
         }
      }
   }

   /**
    * Creates the tree entries for the bit-fields within the register
    * 
    * @param registerModel The model that represents the register and owns the bit-fields
    * @param register      The register to obtain bit-field information from
    * @throws Exception 
    */
   private void createFieldModels(RegisterModel registerModel, Register register, int index) throws Exception {
      
      ArrayList<Field> fieldList = register.getFields();
      if (fieldList != null) {
         for (Field field : fieldList) {
            if (!field.isHidden()) {
               FieldModel fieldModel = new FieldModel(registerModel, field, index);
               fieldModel.setEnumeratedDescription(field.getEnumerations());
            }
         }
      }
   }

   /**
    * Creates the tree entry or entries for the register.  
    * Note there may be multiple as a register entry may represent multiple similar registers.
    * 
    * @param peripheralModel The model that represents the peripheral and owns the registers
    * @param register        The register to obtain register information from
    * @throws Exception 
    */
   private void createRegisterModels(PeripheralModel peripheralModel, Register register) throws Exception {
      if (register.isHidden()) {
         return;
      }
      if (register.getDimension() == 0) {
         // Simple register
         RegisterModel registerModel = new RegisterModel(peripheralModel, register);
         createFieldModels(registerModel, register);
      }
      else {
         // The register represents multiple registers
         for (int regIndex=0; regIndex<register.getDimension(); regIndex++) {
            RegisterModel registerModel = new RegisterModel(peripheralModel, register, regIndex);
            createFieldModels(registerModel, register, regIndex);
         }
      }
   }
   
   /**
    * Adds a set of entries for a Cluster of registers
    * 
    * @param device      The model that represents the device (entire tree) that owns the peripherals
    * @param peripheral  The peripheral to obtain register information from
    * 
    * @throws Exception 
    */
   private void createCluster(PeripheralModel peripheralModel, Cluster cluster) throws Exception {
      String nameFormat = cluster.getNameMacroFormat();
      if (cluster.getDimension()>0) {
         for(int clusterIndex=0; clusterIndex<cluster.getDimension(); clusterIndex++) {
            for (Register register : cluster.getRegisters()) {
               if (register.isHidden()) {
                  continue;
               }
               String name;
               name = nameFormat.replaceAll("@f", register.getName());
               name = name.replaceAll("@i", String.format("%d", clusterIndex));
               name = name.replaceAll("@a", cluster.getBaseName());
               name = name.replaceAll("@p", "");//peripheralModel.getName());
               if (register.getDimension()>0) {
                  for (int registerIndex=0; registerIndex<register.getDimension(); registerIndex++) {
                     RegisterModel registerModel = new RegisterModel(peripheralModel, cluster, clusterIndex, register, registerIndex);
                     registerModel.setName(register.format(name, registerIndex));
                     createFieldModels(registerModel, register);
                  }
               }
               else {
                  RegisterModel registerModel = new RegisterModel(peripheralModel, cluster, clusterIndex, register);
                  registerModel.setName(name);
                  createFieldModels(registerModel, register);
               }
            }
         }
      }
      else {
         for (Register register : cluster.getRegisters()) {
//            String name;
//            name = register.getName();
            RegisterModel registerModel = new RegisterModel(peripheralModel, cluster, register);
//            registerModel.setName(name);
            createFieldModels(registerModel, register);
         }
      }
   }
   
   /**
    * Creates the tree entries for a peripheral
    * 
    * @param device      The model that represents the device (entire tree) that owns the peripherals
    * @param peripheral  The peripheral to obtain register information from
    * 
    * @throws Exception 
    */
   private void createPeripheralModel(DeviceModel device, Peripheral peripheral, GdbCommonInterface gdbInterface) throws Exception {
      
      // Create peripheral mode
      PeripheralModel peripheralModel = new PeripheralModel(device, peripheral, gdbInterface);
      
      // Add registers
      for (Cluster cluster : peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            createRegisterModels(peripheralModel, (Register)cluster);
         }
         else {
            createCluster(peripheralModel, cluster);
         }
      }
   }

   private HashSet<String> excludedPeripherals = null;
   
   /** 
    * Indicates devices in the database which are NOT to be displayed
    * 
    * @param name Device name to check
    * @return true => should be excluded from display
    */
   private boolean isExcludedPeripheral(String name) {
      if (excludedPeripherals == null) {
         excludedPeripherals = new HashSet<String>();
         excludedPeripherals.add("AIPS0");
         excludedPeripherals.add("AIPS1");
         excludedPeripherals.add("AXBS");
         excludedPeripherals.add("ETF");
      }
      return excludedPeripherals.contains(name);
   }
   
   /**
    * Creates the tree entries for a peripheral
    * 
    * @param devicenameOrFilename    Device SVD file path or device name to use to locate peripheral description
    * @throws Exception 
    */
   private DeviceModel createDeviceModel(String devicenameOrFilename, GdbCommonInterface gdbInterface) {
      
      if (devicenameOrFilename == null) {
         return null;
      }
      
      // Get description of all peripherals for device
      DevicePeripherals devicePeripherals = createDevicePeripheralsDatabase(devicenameOrFilename);
      if (devicePeripherals == null) {
         // Return empty model
         return null;
      }
      
      DeviceModel deviceModel = new DeviceModel(devicePeripherals.getName()) ;
      
      try {
         BaseModel.setLittleEndian(devicePeripherals.getCpu().getEndian().equalsIgnoreCase("little"));

         // Add the peripherals
         for (Peripheral peripheral : devicePeripherals.getPeripherals()) {
            if (isExcludedPeripheral(peripheral.getName())) {
               continue;
            }
            createPeripheralModel(deviceModel, peripheral, gdbInterface);     
         }
      } catch (Exception e) {
         System.err.println("Error in UsbdmDevicePeripheralsModel.createDeviceModel(), reason : "+e.getMessage());
      }
      return deviceModel;
   }
   
   /**
    * Create the peripheral database
    * 
    * @param devicenameOrFilename Path to SVD file or device name (standard locations are searched)
    * 
    * @return Database created
    * @throws Exception 
    */
   private static DevicePeripherals createDevicePeripheralsDatabase(String devicenameOrFilename) {
      
      return DevicePeripherals.createDatabase(devicenameOrFilename);
   }

   private DeviceModel        model        = null;
   private GdbCommonInterface gdbInterface = null;
   
   /**
    * Constructor
    * 
    * Loads device model using path to SVD file or device name and associates GDB session
    * 
    * @param devicenameOrFilename Path to SVD file or device name (standard locations are searched)
    * @param gdbInterface
    */
   public UsbdmDevicePeripheralsModel(String devicenameOrFilename, GdbCommonInterface gdbInterface) {
      this.gdbInterface = gdbInterface;

      setDevice(devicenameOrFilename);
   }
   
   /**
    * Loads device model using path to SVD file or device name
    * 
    * @param devicenameOrFilename Path to SVD file or device name (standard locations are searched)
    */
   public void setDevice(String devicenameOrFilename) {
//      System.err.println("UsbdmDevicePeripheralsModel.setDevice() : \'" + devicenameOrFilename + "\'");
      try {
         model = createDeviceModel(devicenameOrFilename, gdbInterface);
      } catch (Exception e) {
         try {
            model = createDeviceModel(null, gdbInterface);
//            System.err.println("UsbdmDevicePeripheralsModel.setDevice() : Failed - creating empty model");
         } catch (Exception e2) {
            model = null;
            e.printStackTrace();
         }
         e.printStackTrace();
      }
      if (model == null) {
         System.err.println("UsbdmDevicePeripheralsModel.setDevice() : model == null!");
      }
   }
   
   public String getDeviceName() {
      if (model == null) {
         return "No Model";
      }
      return model.getName();
   }
   
   public DeviceModel getModel() {
      return model;
   }
   
   public GdbCommonInterface getGdbInterface() {
      return gdbInterface;
   }
   
}
