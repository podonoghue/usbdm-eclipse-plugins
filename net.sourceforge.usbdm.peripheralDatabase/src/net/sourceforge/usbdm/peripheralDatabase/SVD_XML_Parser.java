/*
 Change History
+===================================================================================
| Revision History
+===================================================================================
| 16 Nov 13 | Added subfamily field                                       4.10.6.100
+===================================================================================
 */
package net.sourceforge.usbdm.peripheralDatabase;

import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author podonoghue
 *
 */
public class SVD_XML_Parser extends SVD_XML_BaseParser {

   public static String DERIVEDFROM_ATTRIB           = "derivedFrom";
   public static String PREFERREDACCESSWIDTH_ATTRIB  = "preferredAccessWidth";
   public static String BLOCKALIGNMENT_ATTRIB        = "forcedAccessWidth";
   public static String SOURCEFILE_ATTRIB            = "sourceFile";

   public static String ALTERNATEREGISTER_TAG    = "alternateRegister";
   public static String ACCESS_TAG               = "access";
   public static String ADDRESSBLOCK_TAG         = "addressBlock";
   public static String ADDRESSOFFSET_TAG        = "addressOffset";
   public static String ADDRESSUNITSBITS_TAG     = "addressUnitBits";
   public static String ALTERNATEGROUP_TAG       = "alternateGroup";
   public static String APPENDTONAME_TAG         = "appendToName";
   public static String BASEADDRESS_TAG          = "baseAddress";
   public static String BITOFFSET_TAG            = "bitOffset";
   public static String BITWIDTH_TAG             = "bitWidth";
   public static String BITRANGE_TAG             = "bitRange";
   public static String CLUSTER_TAG              = "cluster";
   public static String CPU_TAG                  = "cpu";
   public static String DESCRIPTION_TAG          = "description";
   public static String DIM_TAG                  = "dim";
   public static String DIMINCREMENT_TAG         = "dimIncrement";
   public static String DIMINDEX_TAG             = "dimIndex";
   public static String DISABLECONDITION_TAG     = "disableCondition";
   public static String DISPLAYNAME_TAG          = "displayName";
   public static String ENDIAN_TAG               = "endian";
   public static String ENUMERATEDVALUES_TAG     = "enumeratedValues";
   public static String ENUMERATEDVALUE_TAG      = "enumeratedValue";
   public static String FALSE_TAG                = "false";
   public static String FIELDS_TAG               = "fields";
   public static String FIELD_TAG                = "field";
   public static String FPUPRESENT_TAG           = "fpuPresent";
   public static String GROUPNAME_TAG            = "groupName";
   public static String ISDEFAULT_TAG            = "isDefault";
   public static String INTERRUPT_TAG            = "interrupt";
   public static String INTERRUPTS_TAG           = "interrupts";
   public static String LSB_TAG                  = "lsb";
   public static String MODIFIEDWRITEVALUES_TAG  = "modifiedWriteValues";
   public static String MPUPRESENT_TAG           = "mpuPresent";
   public static String MSB_TAG                  = "msb";
   public static String NAME_TAG                 = "name";
   public static String NVICPRIOBITS_TAG         = "nvicPrioBits";
   public static String OFFSET_TAG               = "offset";
   public static String PERIPHERAL_TAG           = "peripheral";
   public static String PERIPHERALS_TAG          = "peripherals";
   public static String PREPENDTONAME_TAG        = "prependToName";
   public static String READACTION_TAG           = "readAction";
   public static String REGISTERS_TAG            = "registers";
   public static String REGISTER_TAG             = "register";
   public static String RESERVED_TAG             = "RESERVED";
   public static String RESETVALUE_TAG           = "resetValue";
   public static String RESETMASK_TAG            = "resetMask";
   public static String REVISION_TAG             = "revision";
   public static String SIZE_TAG                 = "size";
   public static String TRUE_TAG                 = "true";
   public static String USAGE_TAG                = "usage";
   public static String VALUE_TAG                = "value";
   public static String VENDORSYSTICKCONFIG_TAG  = "vendorSystickConfig";
   public static String VENDOREXTENSIONS_TAG     = "vendorExtensions";
   public static String VERSION_TAG              = "version";
   public static String WRITECONSTRAINTS_TAG     = "writeConstraint";
   public static String WIDTH_TAG                = "width";
   public static String WRITECONSTRAINT_TAG      = "writeConstraint";
   
   public static String DEVICELIST_FILENAME      = "DeviceList";

   DevicePeripherals devicePeripherals = null;          
   
   /**
    * Parse a <enumeratedValue> element
    * @param field 
    * 
    * @param  enumeratedValue <enumeratedValue> element
    * 
    * @return Enumeration described
    *  
    * @throws Exception 
    */
   private Enumeration parseEnumeratedValue(Field field, Element enumeratedValue) throws Exception {

      Enumeration enumeration = new Enumeration();
      
      for (Node node = enumeratedValue.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
            enumeration.setName(element.getTextContent());
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            enumeration.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == VALUE_TAG) {
            enumeration.setValue(element.getTextContent());
         }
         else if (element.getTagName() == ISDEFAULT_TAG) {
            enumeration.setAsDefault();
         }
         else {
            throw new Exception("Unexpected field in ENUMERATEDVALUE', value = \'"+element.getTagName()+"\'");
         }
      }
      return enumeration;
   }
   
   /**
    * Parse a <enumeratedValues> element
    * 
    * @param  enumeratedValuesElement <enumeratedValues> element
    * 
    * @return Device described 
    * 
    * @throws Exception 
    */
   private void parseEnumeratedValues(Field field, Element enumeratedValuesElement) throws Exception {

      for (Node node = enumeratedValuesElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == ENUMERATEDVALUE_TAG) {
            field.addEnumeration(parseEnumeratedValue(field, element));
         }
         else {
            throw new Exception("Unexpected field in ENUMERATEDVALUES', value = \'"+element.getTagName()+"\'");
         }
      }
   }
   
   /**
    * Parse a <field> element
    * @param register 
    * 
    * @param  fieldElement <field> element
    * 
    * @return Device described
    *  
    * @throws Exception 
    */
   private Field parseField(Register register, Element fieldElement) throws Exception {

      Field field = new Field(register);

      // Inherit defaults
      field.setAccessType(register.getAccessType());

      for (Node node = fieldElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
            field.setName(element.getTextContent());
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            field.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == LSB_TAG) {
            field.setBitOffset(getIntElement(element));
         }
         else if (element.getTagName() == MSB_TAG) {
            field.setBitWidth(getIntElement(element)-field.getBitOffset()+1);
         }
         else if (element.getTagName() == BITOFFSET_TAG) {
            field.setBitOffset(getIntElement(element));
         }
         else if (element.getTagName() == BITWIDTH_TAG) {
            field.setBitWidth(getIntElement(element));
         }
         else if (element.getTagName() == BITRANGE_TAG) {
            String bitRange = element.getTextContent();
            if ((bitRange.charAt(0) != '[')||(bitRange.charAt(bitRange.length()-1) != ']')) {
               throw new Exception("Illegal BITRANGE in FIELD', value = \'"+bitRange+"\'");
            }
            bitRange = bitRange.substring(1, bitRange.length()-1);
            int commaIndex = bitRange.indexOf(':');
            if (commaIndex < 0) {
               throw new Exception("Illegal BITRANGE in FIELD', value = \'"+bitRange+"\'");
            }
            String end   = bitRange.substring(0, commaIndex);
            String start = bitRange.substring(commaIndex+1);
            int startBit = Integer.parseInt(start);
            int endBit   = Integer.parseInt(end);
            field.setBitOffset(startBit);
            field.setBitWidth(endBit-startBit+1);
         }
         else if (element.getTagName() == ACCESS_TAG) {
            field.setAccessType(getAccessElement(element));
         }
         else if (element.getTagName() == MODIFIEDWRITEVALUES_TAG) {
            //TODO: Implement modifiedWriteValues
         }
         else if (element.getTagName() == WRITECONSTRAINTS_TAG) {
            //TODO: Implement writeConstraint
         }
         else if (element.getTagName() == READACTION_TAG) {
            //TODO: Implement readAction
         }
         else if (element.getTagName() == ENUMERATEDVALUES_TAG) {
            parseEnumeratedValues(field, element);
         }
         else {
            throw new Exception("Unexpected field in FIELD', value = \'"+element.getTagName()+"\'");
         }
      }
      return field;
   }


   /**
    * Parse a <fields> element
    * 
    * @param  register Register to add fields to
    * @param  fieldsElement <fields> element
    * 
    * @throws Exception 
    */
   private void parseFields(Register register, Element fieldsElement) throws Exception {

      long bitsUsed = 0;
      for (Node node = fieldsElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == FIELD_TAG) {
            Field field = parseField(register, element);
            long bitsUsedThisField = 0;
            for (long i=field.getBitOffset(); i<(field.getBitOffset()+field.getBitwidth()); i++) {
               bitsUsedThisField |= 1L<<i;
            }
            if ((bitsUsed&bitsUsedThisField) != 0) {
               throw new Exception(String.format("Bit fields overlap in register \'%s\'", register.getName()));
            }
            bitsUsed |= bitsUsedThisField;
            if (field.getAccessType() == null) {
               field.setAccessType(register.getAccessType());
            }
            if (!field.getName().equals(RESERVED_TAG)) {
               register.addField(field);
            }
         }
         else {
            throw new Exception(String.format("Unexpected field in <fields> reg = \'%s\', field = \'%s\'", 
                  register.getName(), element.getTagName()));
         }
      }
   }

   /**
    * Parse a <register> element
    * @param peripheral 
    * 
    * @param  registerElement <register> element
    * 
    * @return Register described
    *  
    * @throws Exception 
    */
   private Register parseRegister(Peripheral peripheral, Cluster cluster, Element registerElement) throws Exception {

      Register register = new Register(peripheral);
      boolean derived = false;

      if (registerElement.hasAttribute(DERIVEDFROM_ATTRIB)) {
         Cluster referencedRegister = null;
         if (cluster != null) {
            referencedRegister = cluster.findRegister(Peripheral.getMappedPeripheralName(registerElement.getAttribute(DERIVEDFROM_ATTRIB)));
            if (referencedRegister == null) {
               // Try unmapped name
               referencedRegister = cluster.findRegister(registerElement.getAttribute(DERIVEDFROM_ATTRIB));
            }
         }
         if (referencedRegister == null) {
            referencedRegister = peripheral.findRegister(Peripheral.getMappedPeripheralName(registerElement.getAttribute(DERIVEDFROM_ATTRIB)));
            if (referencedRegister == null) {
               // Try unmapped name
               referencedRegister = peripheral.findRegister(registerElement.getAttribute(DERIVEDFROM_ATTRIB));
               if (referencedRegister == null) {
                  throw new Exception("Referenced register cannot be found: \""+registerElement.getAttribute(DERIVEDFROM_ATTRIB)+"\"");
               }
            }
         }
         register = (Register) ((Register) referencedRegister).clone();
         derived  = true;
      }
      else {
         register = new Register(peripheral);

         // Inherit default from device
         register.setWidth(peripheral.getWidth());
         register.setAccessType(peripheral.getAccessType());
         register.setResetValue(peripheral.getResetValue());
         register.setResetMask(peripheral.getResetMask());
      }
      for (Node node = registerElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
          register.setName(element.getTextContent());
         }
         
         else if (element.getTagName() == ADDRESSOFFSET_TAG) {
            register.setAddressOffset(getIntElement(element));
         }
         else if (element.getTagName() == DISPLAYNAME_TAG) {
            if ((register.getName() == null) || (register.getName().length() == 0))
            register.setName(element.getTextContent());
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            register.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == ALTERNATEGROUP_TAG) {
            register.setAlternateGroup(element.getTextContent());
         }
         else if (element.getTagName() == RESETVALUE_TAG) {
            register.setResetValue(getIntElement(element));
         }
         else if (element.getTagName() == RESETMASK_TAG) {
            register.setResetMask(getIntElement(element));
         }
         else if (element.getTagName() == DIM_TAG) {
         }
         else if (element.getTagName() == DIMINCREMENT_TAG) {
            register.setDimensionIncrement((int) getIntElement(element));
         }
         else if (element.getTagName() == DIMINDEX_TAG) {
            register.setDimensionIndexes(element.getTextContent());
         }
         else if (derived)  {
            throw new Exception(String.format("Unexpected field in derived <register>, p=%s, r=%s, v=%s", 
                  peripheral.getName(), register.getName(), element.getTagName()));
         }
         else if (element.getTagName() == SIZE_TAG) {
            register.setWidth(getIntElement(element));
         }
         else if (element.getTagName() == ACCESS_TAG) {
            register.setAccessType(getAccessElement(element));
         }
         else if (element.getTagName() == MODIFIEDWRITEVALUES_TAG) {
            //TODO: Implement modifiedWriteValues
         }
         else if (element.getTagName() == WRITECONSTRAINT_TAG) {
            //TODO: Implement modifiedWriteValues
         }
         else if (element.getTagName() == READACTION_TAG) {
            //TODO: Implement readAction
         }
         else if (element.getTagName() == FIELDS_TAG) {
            parseFields(register, element);
         }
         else if (element.getTagName() == ALTERNATEREGISTER_TAG) {
            //TODO: Implement
         }
         else {
            throw new Exception(String.format("Unexpected field in <register>, p=%s, r=%s, v=%s", 
                  peripheral.getName(), register.getName(), element.getTagName()));
         }
      }
      register.checkAccess();
      return register;
   }

   /**
    * Parse a <cluster> element
    * @param peripheral 
    * 
    * @param  peripheralElement <register> element
    * 
    * @return Register described
    *  
    * @throws Exception 
    */
   private Cluster parseCluster(Peripheral peripheral, Element clusterElement) throws Exception {

      Cluster cluster = new Cluster(peripheral);
      
      // Inherit default from device
      cluster.setAccessType(peripheral.getAccessType());
      cluster.setResetValue(peripheral.getResetValue());
      cluster.setResetMask(peripheral.getResetMask());

      for (Node node = clusterElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == DIM_TAG) {
         }
         else if (element.getTagName() == DIMINCREMENT_TAG) {
            cluster.setDimensionIncrement((int) getIntElement(element));
         }
         else if (element.getTagName() == DIMINDEX_TAG) {
            cluster.setDimensionIndexes(element.getTextContent());
         }
         else if (element.getTagName() == NAME_TAG) {
            cluster.setName(element.getTextContent());
         }
         else if (element.getTagName() == ADDRESSOFFSET_TAG) {
            cluster.setAddressOffset(getIntElement(element));
         }
         else if (element.getTagName() == REGISTER_TAG) {
            Register register = parseRegister(peripheral, cluster, element);
            cluster.addRegister(register);
            if (register.getWidth() == 0) {
               register.setWidth(peripheral.getWidth());
            }
            if (register.getAccessType() == null) {
               register.setAccessType(peripheral.getAccessType());
            }
         }
         else {
            throw new Exception("Unexpected field in CLUSTER', value = \'"+element.getTagName()+"\'");
         }
      }
      return cluster;
   }

   /**
    * Parse a <registers> element
    * 
    * @param  peripheral Peripheral to add registers to
    * @param  peripheralElement <registers> element
    * 
    * @throws Exception 
    */
   private void parseRegisters(Peripheral peripheral, Element peripheralElement) throws Exception {

      for (Node node = peripheralElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == REGISTER_TAG) {
            Register register = parseRegister(peripheral, null, element);
            peripheral.addRegister(register);
            if (register.getWidth() == 0) {
               register.setWidth(peripheral.getWidth());
            }
            if (register.getAccessType() == null) {
               register.setAccessType(peripheral.getAccessType());
            }
         }
         else if (element.getTagName() == CLUSTER_TAG) {
            Cluster cluster = parseCluster(peripheral, element);
            peripheral.addRegister(cluster);
//            if (register.getSize() == 0) {
//               register.setSize(peripheral.getSize());
//            }
//            if (register.getAccessType() == null) {
//               register.setAccessType(peripheral.getAccessType());
//            }
         }
         else {
            throw new Exception("Unexpected field in REGISTERS', value = \'"+element.getTagName()+"\'");
         }
      }
   }
   
   /**
    * Parse a <addressBlock> element
    * 
    * @param  addressBlockElement <addressBlock> element
    * 
    * @return AddressBlock described
    *  
    * @throws Exception 
    */
   private AddressBlock parseAddressBlock(Element addressBlockElement) throws Exception {

//      System.out.println("parsePeripheral");
      AddressBlock addressBlock = new AddressBlock();

      for (Node node = addressBlockElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
//            System.out.println("parseAddressBlock - " + node.getTextContent());

         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == OFFSET_TAG) {
            addressBlock.setOffset(getIntElement(element));
         }
         else if (element.getTagName() == SIZE_TAG) {
            addressBlock.setSize(getIntElement(element));
         }
         else if (element.getTagName() == WIDTH_TAG) {
            addressBlock.setWidth(getIntElement(element));
         }
         else if (element.getTagName() == USAGE_TAG) {
            addressBlock.setUsage(element.getTextContent());
         }
         else {
            throw new Exception("Unexpected field in ADDRESSBLOCK', value = \'"+element.getTagName()+"\'");
         }
      }
      return addressBlock;
   }

   /**
    * Parse a <interrupt> element
    * 
    * @param  interruptElement <interrupt> element
    * 
    * @return InterruptEntry described
    *  
    * @throws Exception 
    */
   private InterruptEntry parseInterrupt(Element interruptElement) throws Exception {

      InterruptEntry interruptEntry = new InterruptEntry();

      for (Node node = interruptElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
            interruptEntry.setName(element.getTextContent());
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            interruptEntry.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == VALUE_TAG) {
            interruptEntry.setNumber((int)getIntElement(element));
         }
         else {
            throw new Exception("Unexpected field in INTERRUPT', value = \'"+element.getTagName()+"\'");
         }
      }
      return interruptEntry;
   }

   /**
    * Parse a <peripheral> element
    * 
    * @param  peripheralElement <peripheral> element
    * 
    * @return Peripheral described 
    * 
    * @throws Exception 
    */
   private Peripheral parsePeripheral(DevicePeripherals device, Element peripheralElement) throws Exception {

      boolean derived       = false;
      boolean interruptsSet = false;
      Peripheral peripheral = null;
      
      if (peripheralElement.hasAttribute(DERIVEDFROM_ATTRIB)) {
         Peripheral referencedPeripheral = null;
         referencedPeripheral = device.findPeripheral(Peripheral.getMappedPeripheralName(peripheralElement.getAttribute(DERIVEDFROM_ATTRIB)));
         if (referencedPeripheral == null) {
            // Try unmapped name
            referencedPeripheral = device.findPeripheral(peripheralElement.getAttribute(DERIVEDFROM_ATTRIB));
            if (referencedPeripheral == null) {
               throw new Exception("Referenced peripheral cannot be found: \""+peripheralElement.getAttribute(DERIVEDFROM_ATTRIB)+"\"");
            }
         }
         peripheral = (Peripheral) referencedPeripheral.clone();
         derived    = true;
      }
      else {
         peripheral = new Peripheral(device);
         // Inherit default from device
         peripheral.setWidth(device.getWidth());
         peripheral.setAccessType(device.getAccessType());
         peripheral.setResetValue(device.getResetValue());
         peripheral.setResetMask(device.getResetMask());
      }
      if (peripheralElement.hasAttribute(SOURCEFILE_ATTRIB)) {
         peripheral.setSourceFilename(peripheralElement.getAttribute(SOURCEFILE_ATTRIB));
      }
      if (peripheralElement.hasAttribute(PREFERREDACCESSWIDTH_ATTRIB)) {
         peripheral.setBlockAccessWidth((int)getIntAttribute(peripheralElement, PREFERREDACCESSWIDTH_ATTRIB));
      }
      if (peripheralElement.hasAttribute(BLOCKALIGNMENT_ATTRIB)) {
         peripheral.setForcedAccessWidth((int)getIntAttribute(peripheralElement, BLOCKALIGNMENT_ATTRIB));
      }
      for (Node node = peripheralElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
//            System.out.println("parsePeripheral() name = "+element.getTextContent());
            peripheral.setName(element.getTextContent());
         }
         else if (element.getTagName() == VERSION_TAG) {
            //TODO: Implement version
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            peripheral.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == GROUPNAME_TAG) {
            peripheral.setGroupName(element.getTextContent());
         }
         else if (element.getTagName() == PREPENDTONAME_TAG) {
            peripheral.setPrependToName(element.getTextContent());
         }
         else if (element.getTagName() == APPENDTONAME_TAG) {
            peripheral.setAppendToName(element.getTextContent());
         }
         else if (element.getTagName() == DISABLECONDITION_TAG) {
            //TODO: Implement disableCondition
         }
         else if (element.getTagName() == BASEADDRESS_TAG) {
            peripheral.setBaseAddress(getIntElement(element));
         }
         else if (element.getTagName() == INTERRUPT_TAG) {
            if (derived && !interruptsSet) {
               // First interrupt - remove existing derived ones
               peripheral.clearInterruptEntries();
               interruptsSet = true;
            }
            peripheral.addInterruptEntry(parseInterrupt(element));
         }
         else if (derived) {
            throw new Exception("Unexpected field in derived PERIPHERAL', value = \'"+element.getTagName()+"\'");
         }
         else if (element.getTagName() == ACCESS_TAG) {
            peripheral.setAccessType(getAccessElement(element));
         }
         else if (element.getTagName() == RESETVALUE_TAG) {
            peripheral.setResetValue(getIntElement(element));
         }
         else if (element.getTagName() == RESETMASK_TAG) {
            peripheral.setResetMask(getIntElement(element));
         }
         else if (element.getTagName() == SIZE_TAG) {
            peripheral.setWidth(getIntElement(element));
         }
         else if (element.getTagName() == ADDRESSBLOCK_TAG ){
            peripheral.addAddressBlock(parseAddressBlock(element));
         }
         else if (element.getTagName() == REGISTERS_TAG) {
            parseRegisters(peripheral, element);
         }
         else {
            throw new Exception("Unexpected field in PERIPHERAL', value = \'"+element.getTagName()+"\'");
         }
      }
      return peripheral;
   }

   /**
    * Parse a <peripherals> element
    * 
    * @param  device Device to add peripherals to
    * 
    * @param  peripheralsElement <peripherals> element
    * 
    * @throws Exception
    */
   private void parsePeripherals(DevicePeripherals device, Element peripheralsElement) throws Exception {

//      System.out.println("parsePeripherals");

      for (Node node = peripheralsElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == PERIPHERAL_TAG) {
            Peripheral peripheral = parsePeripheral(device, element);
            device.addPeripheral(peripheral);
            if (peripheral.getWidth() == 0) {
               peripheral.setWidth(device.getWidth());
            }
            if (peripheral.getAccessType() == null) {
               peripheral.setAccessType(device.getAccessType());
            }
         }
         else {
            throw new Exception("Unexpected field in PERIPHERALS', value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * Parse a <cpu> element
    * 
    * @param  devicePeripherals Device to add peripherals to
    * 
    * @param  cpuElement <cpu> element
    * 
    * @throws Exception
    */
   private void parseCpu(DevicePeripherals devicePeripherals, Element cpuElement) throws Exception {

      Cpu cpu = new Cpu();
      
      for (Node node = cpuElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
            cpu.setName(element.getTextContent());
         }
         else if (element.getTagName() == REVISION_TAG) {
            cpu.setRevision(element.getTextContent());
         }
         else if (element.getTagName() == ENDIAN_TAG) {
            cpu.setEndian(element.getTextContent());
         }
         else if (element.getTagName() == MPUPRESENT_TAG) {
            cpu.setMpuPresent(element.getTextContent().equalsIgnoreCase(TRUE_TAG));
         }
         else if (element.getTagName() == FPUPRESENT_TAG) {
            cpu.setFpuPresent(element.getTextContent().equalsIgnoreCase(TRUE_TAG));
         }
         else if (element.getTagName() == NVICPRIOBITS_TAG) {
            cpu.setNvicPrioBits((int)getIntElement(element));
         }
         else if (element.getTagName() == VENDORSYSTICKCONFIG_TAG) {
            // Ignored
         }
         else {
            throw new Exception("Unexpected field in CPU', value = \'"+element.getTagName()+"\'");
         }
      }
      devicePeripherals.setCpu(cpu);
   }

   /**
    * Parse a <interrupts> element
    * 
    * @param  devicePeripherals Device to add peripherals to
    * 
    * @param  interruptElement <interrupts> element
    * 
    * @throws Exception
    */
   private void parseInterrupts(DevicePeripherals devicePeripherals, Element interruptElement) throws Exception {
      int lastEntryNumber = -1000;
      VectorTable vectorTable = new VectorTable();

      for (Node node = interruptElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
            vectorTable.setName(element.getTextContent());
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            vectorTable.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == INTERRUPT_TAG) {
            InterruptEntry entry = parseInterrupt(element);
            if (entry.getNumber() <= -100) {
               entry.setNumber(++lastEntryNumber);
            }
            else if (entry.getNumber() <= lastEntryNumber) {
               throw new Exception("Interrupt vectors must be monotonic");
            }
            lastEntryNumber = entry.getNumber();
            if (vectorTable.getEntry(lastEntryNumber) != null) {
               throw new Exception("Repeated Interrupt number");
            }
            if (entry.getNumber()<0) {
               System.err.println("Warning: Discarding predefined vector \""+entry.getName()+"\"");
               continue;
            }
            vectorTable.addEntry(entry);
         }
         else {
            System.err.println("Unexpected field in <interrupts>', value = \'"+element.getTagName()+"\'");
         }
      }
      devicePeripherals.setVectorTable(vectorTable);
   }

   /**
    * Parse a <vendorExtensions> element
    * 
    * @param  devicePeripherals Device to add peripherals to
    * 
    * @param  vendorExtensionsElement <vendorExtensions> element
    * 
    * @throws Exception
    */
   private void parseVendorExtensions(DevicePeripherals devicePeripherals, Element vendorExtensionsElement) throws Exception {
      for (Node node = vendorExtensionsElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == INTERRUPTS_TAG) {
            parseInterrupts(devicePeripherals, element);
         }
         else {
            System.err.println("Unexpected field in <vendorExtensions>', value = \'"+element.getTagName()+"\'");
         }
      }
   }

   /**
    * @return Description of the device peripherals
    * 
    * @throws Exception
    */
   private DevicePeripherals parseDocument(Element documentElement) throws Exception {
      
      if (documentElement == null) {
         System.out.println("DeviceDatabase.parseDocument() - failed to get documentElement");
         return null;
      }

      DevicePeripherals devicePeripherals = new DevicePeripherals();

      for (Node node = documentElement.getFirstChild();
            node != null;
            node = node.getNextSibling()) {
         if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
         }
         Element element = (Element) node;
         if (element.getTagName() == NAME_TAG) {
            devicePeripherals.setName(element.getTextContent());
         }
         else if (element.getTagName() == VERSION_TAG) {
            devicePeripherals.setVersion(element.getTextContent());
         }
         else if (element.getTagName() == DESCRIPTION_TAG) {
            devicePeripherals.setDescription(element.getTextContent().trim());
         }
         else if (element.getTagName() == ADDRESSUNITSBITS_TAG) {
            devicePeripherals.setAddressUnitBits(getIntElement(element));
         }
         else if (element.getTagName() == WIDTH_TAG) {
            devicePeripherals.setWidth(getIntElement(element));
         }
         else if (element.getTagName() == SIZE_TAG) {
            devicePeripherals.setWidth(getIntElement(element));
         }
         else if (element.getTagName() == ACCESS_TAG) {
            devicePeripherals.setAccessType(getAccessElement(element));
         }
         else if (element.getTagName() == RESETVALUE_TAG) {
            devicePeripherals.setResetValue(getIntElement(element));
         }
         else if (element.getTagName() == RESETMASK_TAG) {
            devicePeripherals.setResetMask(getIntElement(element));
         }
         else if (element.getTagName() == PERIPHERALS_TAG) {
            parsePeripherals(devicePeripherals, element);
         }
         else if (element.getTagName() == CPU_TAG) {
            parseCpu(devicePeripherals, element);
         }
         else if (element.getTagName() == VENDOREXTENSIONS_TAG) {
            parseVendorExtensions(devicePeripherals, element);
         }
         else {
            throw new Exception("Unexpected field in DEVICE', value = \'"+element.getTagName()+"\'");
         }
      }
      return devicePeripherals;
   }

   /**
    *  Constructor
    */
   private SVD_XML_Parser() {
   }
   
   /**
    *  Creates peripheral database for device
    * 
    *  @param devicenameOrFilename Name of SVD file or device name e.g. "MKL25Z128M5" or family name e.g. "MK20D5"
    *  
    *  @return device peripheral description or null on error
    */
   public static DevicePeripherals createDatabase(String devicenameOrFilename) {
      SVD_XML_Parser database = new SVD_XML_Parser();

      DevicePeripherals devicePeripherals = null;
      
      // Parse the XML file into the XML internal DOM representation
      Document dom;
      try {
         // Try name as given (may be full path)
         dom = parseXmlFile(devicenameOrFilename);
         if (dom == null) {
            // Try name with default extension
            dom = parseXmlFile(devicenameOrFilename+".svd");
         }
         if (dom == null) {
            // Retry with mapped name
            String mappedFilename = DeviceFileList.createDeviceFileList(DEVICELIST_FILENAME).getSvdFilename(devicenameOrFilename);
            dom = parseXmlFile(mappedFilename);
         }
         if (dom == null) {
            System.err.println("SVD_XML_Parser.createDatabase() - Unable to locate SVD data for \""+devicenameOrFilename+"\"");
         }
         else {
            // Get the root element
            Element documentElement = dom.getDocumentElement();

            //  Process XML contents and generate Device description
            devicePeripherals = database.parseDocument(documentElement);
         }
      } catch (Exception e) {
         System.err.println("SVD_XML_Parser.createDatabase() - Exception in SVD_XML_Parser.createDatabase(), reason: " + e.getMessage());
      }
      return devicePeripherals;
   }
   
   /**
    *  Creates peripheral database for device
    * 
    *  @param path Path to SVD file describing device peripherals
    *  
    *  @return device peripheral description or null on error
    */
   public static DevicePeripherals createDatabase(IPath path) {
      SVD_XML_Parser database = new SVD_XML_Parser();

      DevicePeripherals devicePeripherals = null;
      // Parse the XML file into the XML internal DOM representation
      Document dom;
      try {
         dom = parseXmlFile(path);
         // Get the root element
         Element documentElement = dom.getDocumentElement();

         //  Process XML contents and generate Device description
         devicePeripherals = database.parseDocument(documentElement);
      } catch (Exception e) {
         System.err.println("Error while processing "+path.toOSString());
         e.printStackTrace();
      }

      return devicePeripherals;
   }
   
}
