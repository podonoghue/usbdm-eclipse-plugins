/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Constructor changes etc                                                           | V4.10.6.250
===============================================================================================================
*/
package net.sourceforge.usbdm.peripherals.model;

import java.util.ArrayList;
import java.util.HashSet;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;
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
    * @throws Exception 
    */
   private static void createFieldModels(RegisterModel registerModel, Register register, ModelInformation pInformation) throws Exception {
      ModelInformation information = new ModelInformation(pInformation);
      ArrayList<Field> fieldList = register.getFields();
      if (fieldList != null) {
         for (Field field : fieldList) {
            if (field.isHidden()) {
               continue;
            }
            information.setField(field);
            FieldModel fieldModel = new FieldModel(registerModel, field, information);
            fieldModel.setEnumeratedDescription(field.getEnumerations());
         }
      }
   }

   /**
    * Creates the tree entry or entries for the register.  <br>
    * Note there may be multiple as a register may be an array.
    * 
    * @param  peripheralModel The model that represents the peripheral and owns the registers
    * @param  register        The register to obtain register information from
    * @param  pInformation    Format for register to this level
    * 
    * @throws Exception
    */
   private static void createRegisterModels(PeripheralModel peripheralModel, Register register, ModelInformation pInformation) throws Exception {
      if (register.isHidden()) {
         return;
      }
      ModelInformation information = new ModelInformation(pInformation);
      information.setRegister(register);
      
      if (register.getDimension() == 0) {
         // Simple register
         RegisterModel registerModel = new RegisterModel(peripheralModel, information);
         createFieldModels(registerModel, register, information);
      }
      else {
         // The register is an array - Create individual registers for each element
         for (int regIndex=0; regIndex<register.getDimension(); regIndex++) {
            information.setRegisterIndex(regIndex);
            RegisterModel registerModel = new RegisterModel(peripheralModel, information);
            createFieldModels(registerModel, register, information);
         }
      }
   }
   
   /**
    * Adds a set of entries for a Cluster of registers
    * 
    * @param peripheral 
    * @param device      The model that represents the device (entire tree) that owns the peripherals
    * @param peripheral  The peripheral to obtain register information from
    * @param  pInformation    Format for register to this level
    * 
    * @throws Exception 
    */
   private static void createClusterModels(PeripheralModel peripheralModel, Cluster cluster, ModelInformation pInformation) throws Exception {
      ModelInformation clusterInfo = new ModelInformation(pInformation);
      clusterInfo.setCluster(cluster);
      
      if (cluster.getDimension()>0) {
         // Cluster which is an array
         for (int clusterIndex=0; clusterIndex<cluster.getDimension(); clusterIndex++) {
            clusterInfo.setClusterIndex(clusterIndex);
            Register singleVisibleRegister = cluster.getSingleVisibleRegister();
            if ((singleVisibleRegister != null) && (singleVisibleRegister.getDimension() == 0)) {
               // Omit cluster wrapper for a single, simple register
               clusterInfo.setRegister(singleVisibleRegister);
               RegisterModel registerModel = new RegisterModel(peripheralModel, clusterInfo);
               registerModel.setName(clusterInfo.getUnwrappedRegisterName());
               createFieldModels(registerModel, singleVisibleRegister, clusterInfo);
               continue;
            }
            ClusterModel clusterModel = new ClusterModel(peripheralModel, clusterInfo);
            for (Register register : cluster.getRegisters()) {
               if (register.isHidden()) {
                  continue;
               }
               ModelInformation registerInfo = new ModelInformation(clusterInfo);
               registerInfo.setRegister(register);
               if (register.getDimension()>0) {
                  for (int registerIndex=0; registerIndex<register.getDimension(); registerIndex++) {
                     registerInfo.setRegisterIndex(registerIndex);
                     RegisterModel registerModel = new RegisterModel(clusterModel, registerInfo);
                     createFieldModels(registerModel, register, registerInfo);
                  }
               }
               else {
                  registerInfo.setRegisterIndex(-1);
                  RegisterModel registerModel = new RegisterModel(clusterModel, registerInfo);
                  createFieldModels(registerModel, register, registerInfo);
               }
            }
         }
      }
      else {
         // A simple cluster
         for (Register register : cluster.getRegisters()) {
            clusterInfo.setRegister(register);
            RegisterModel registerModel = new RegisterModel(peripheralModel, clusterInfo);
            registerModel.setName(registerModel.getName());
            createFieldModels(registerModel, register, clusterInfo);
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
   private static void createPeripheralModel(DeviceModel device, Peripheral peripheral, GdbCommonInterface gdbInterface) throws Exception {
      // Create peripheral mode
      PeripheralModel   peripheralModel = new PeripheralModel(device, peripheral, gdbInterface);
      ModelInformation  information     = new ModelInformation(peripheral);
      
      // Add registers
      for (Cluster cluster : peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            createRegisterModels(peripheralModel, (Register)cluster, information);
         }
         else {
            createClusterModels(peripheralModel, cluster, information);
         }
      }
   }

   private static HashSet<String> excludedPeripherals = null;
   
   /** 
    * Indicates devices in the database which are NOT to be displayed
    * 
    * @param name Device name to check
    * @return true => should be excluded from display
    */
   private static boolean isExcludedPeripheral(String name) {
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
   public static DeviceModel createDeviceModel(DevicePeripherals devicePeripherals, GdbCommonInterface gdbInterface) throws Exception {

      if (devicePeripherals == null) {
         // Return empty model
         return null;
      }
      DeviceModel deviceModel = new DeviceModel(devicePeripherals.getName()) ;
      if (gdbInterface != null) {
         gdbInterface.setLittleEndian(devicePeripherals.getCpu().getEndian().equalsIgnoreCase("little"));
      }
      String cpuName = devicePeripherals.getCpu().getName();
      if (cpuName.startsWith("CM")) {
         deviceModel.setTargetType(InterfaceType.T_ARM);
      }
      else if (cpuName.startsWith("CFV1")) {
         deviceModel.setTargetType(InterfaceType.T_CFV1);
      }
      else if (cpuName.startsWith("CFV2") || cpuName.startsWith("CFV3") || cpuName.startsWith("CFV4")) {
         deviceModel.setTargetType(InterfaceType.T_CFVX);
      }
      else {
         deviceModel.setTargetType(null);
      }

      // Add the peripherals
      for (Peripheral peripheral : devicePeripherals.getPeripherals()) {
         if (isExcludedPeripheral(peripheral.getName())) {
            continue;
         }
         createPeripheralModel(deviceModel, peripheral, gdbInterface);     
      }
      return deviceModel;
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
    * Constructor
    * 
    * Loads device model using path to SVD file or device name and associates GDB session
    * 
    * @param devicenameOrFilename Path to SVD file or device name (standard locations are searched)
    * @param gdbInterface
    */
   public UsbdmDevicePeripheralsModel(DevicePeripherals devicePeripherals, GdbCommonInterface gdbInterface) {
      this.gdbInterface = gdbInterface;
      setDevice(devicePeripherals);
   }
   
   /**
    * Loads device model using path to SVD file or device name
    * 
    * @param devicenameOrFilename Path to SVD file or device name (standard locations are searched)
    */
   public void setDevice(String devicenameOrFilename) {
      setDevice(DevicePeripherals.createDatabase(devicenameOrFilename));
   }
   
   /**
    * Loads device model using path to SVD file or device name
    * 
    * @param devicenameOrFilename Path to SVD file or device name (standard locations are searched)
    */
   public void setDevice(DevicePeripherals devicePeripherals) {
      DeviceModel model = null;
      try {
         model = createDeviceModel(devicePeripherals, gdbInterface);
      } catch (Exception e) {
         e.printStackTrace();
      }
      this.model = model;
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
