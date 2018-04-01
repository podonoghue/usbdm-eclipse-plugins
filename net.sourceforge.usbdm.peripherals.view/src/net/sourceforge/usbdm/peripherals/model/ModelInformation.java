package net.sourceforge.usbdm.peripherals.model;

import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;
import net.sourceforge.usbdm.peripheralDatabase.RegisterException;

public class ModelInformation {
   private String       formatString;
   private Peripheral   peripheral;
   private Cluster      cluster;
   private Register     register;
   private Field        field;
   private int          clusterIndex;
   private int          registerIndex;
   
   public ModelInformation(Peripheral peripheral) {
      this.formatString    = null;
      this.peripheral      = peripheral;
      this.cluster         = null;
      this.register        = null;
      this.field           = null;
      this.clusterIndex    = -1;
      this.registerIndex   = -1;
   }

   public ModelInformation(ModelInformation other) {
      this.formatString    = other.formatString;
      this.peripheral      = other.peripheral;
      this.cluster         = other.cluster;
      this.register        = other.register;
      this.field           = other.field;
      this.clusterIndex    = other.clusterIndex;
      this.registerIndex   = other.registerIndex;
   }

   public void setFormatString(String formatString) {
      this.formatString = formatString;
   }
   public void setPeripheral(Peripheral peripheral) {
      this.peripheral = peripheral;
   }
   public void setCluster(Cluster cluster) {
      this.cluster = cluster;
   }
   public void setRegister(Register register) {
      this.register = register;
   }
   public void setField(Field field) {
      this.field = field;
   }
   public void setClusterIndex(int clusterIndex) {
      this.clusterIndex = clusterIndex;
   }
   public void setRegisterIndex(int registerIndex) {
      this.registerIndex = registerIndex;
   }
   public Peripheral getPeripheralName() {
      return peripheral;
   }
   public Cluster getCluster() {
      return cluster;
   }
   public Register getRegister() {
      return register;
   }
   public int getClusterIndex() {
      return clusterIndex;
   }
   public int getRegisterIndex() {
      return registerIndex;
   }
   
   public String getClusterName() throws RegisterException {
      if (clusterIndex <0) {
         return cluster.getName();
      }
      else {
         return cluster.getArraySubscriptedName(clusterIndex);
      }
   }

   /**
    * Gets the register name but uses the surrounding cluster's index
    * Used to 'unwrap' a cluster containing only a single register
    * 
    * @return
    * @throws Exception
    */
   public String getUnwrappedRegisterName() throws RegisterException {
      if (clusterIndex <0) {
         return register.getName();
      }
      else {
         // XXX Check this
//         return cluster.getArraySubscriptedName(register.getBaseName(), clusterIndex);
         return cluster.getArraySubscriptedName(clusterIndex);
      }
   }

   public String getRegisterName() throws RegisterException {
      if (registerIndex <0) {
         return register.getName();
      }
      else {
         return register.getArraySubscriptedName(registerIndex);
      }
   }

   public String getFieldName() {
      if (registerIndex <0) {
         return field.getName();
      }
      else {
         return field.getName(registerIndex);
      }
   }

   public long getRegisterAddress() {
      long address = getClusterAddress();
      address += register.getAddressOffset();
      if (registerIndex>0) {
         address += registerIndex*register.getElementSizeInBytes();
      }
      return address;
   }
   
   public long getClusterAddress() {
      long address = peripheral.getBaseAddress();
      if (cluster != null) {
         address += cluster.getAddressOffset();
         if (clusterIndex>0) {
            address += clusterIndex*cluster.getElementSizeInBytes();
         }
      }
      return address;
   }
   
   public String getDescription() {
      String description = "";
      if (field != null) {
         if (clusterIndex >= 0) {
            description = field.getCDescription(clusterIndex, registerIndex);
         }
         else if (registerIndex >= 0) {
            description = field.getCDescription(registerIndex);
         }
         else if (register != null) {
            description = field.getCDescription();
         }
      }
      else {
         if ((clusterIndex >= 0) && (register != null)) {
            description = register.getCDescription(clusterIndex, registerIndex);
         }
         else if (registerIndex >= 0) {
            description = register.getCDescription(registerIndex);
         }
         else if (register != null) {
            description = register.getCDescription();
         }
      }
      return description;
   }
}
