package net.sourceforge.usbdm.deviceEditor.information;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.parser.DocumentUtilities;
import net.sourceforge.usbdm.deviceEditor.parser.WriterBase;
import net.sourceforge.usbdm.deviceEditor.xmlParser.XmlDocumentUtilities;

/**
 * Represents a peripheral.<br>
 * Includes
 * <li>Name e.g. FTM0
 * <li>Class-name e.g. Ftm0
 * <li>Base-name e.g. FTM0 => FTM
 * <li>Instance e.g. FTM0 => 0
 * <li>Clock register mask e.g. ADC0_CLOCK_REG
 * <li>Clock register e.g. SIM->SCGC6
 */
public class Peripheral {
   
   /** Name of peripheral e.g. FTM2 */
   private final String fName;

   /** Name of C peripheral class e.g. Ftm2 */
   private final String fClassName;
   
   /** Base name of the peripheral e.g. FTM0 = FTM, PTA = PT */
   private final String fBaseName;
   
   /** Instance name/number of the peripheral instance e.g. FTM0 = 0, PTA = A */
   private final String fInstance;
   
   /** The template associated with this peripheral */
   private final PeripheralTemplateInformation fTemplate;

   /** Description of peripheral */
   private final WriterBase fWriter;

   /** Clock register e.g. SIM->SCGC6 */
   private String fClockReg = null;

   /** Clock register mask e.g. ADC0_CLOCK_REG */
   private String fClockMask = null;
   
   /** Hardware interrupt numbers */
   private final ArrayList<String> fIrqNums = new ArrayList<String>();
   
   /** IRQ handler name */
   private String fIrqHandler;
   
   /** Class used to hold different classes of peripheral functions */
   public class InfoTable {
      /** Functions that use this writer */
      public  Vector<PeripheralFunction> table = new Vector<PeripheralFunction>();
      private String fName;

      public InfoTable(String name) {
         fName = name;
      }
      public String getName() {
         return fName;
      }
   }

   /** Functions that use this writer */
   protected InfoTable fPeripheralFunctions = new InfoTable("info");

   /** Map of all functions on this peripheral */
   private TreeMap<String, PeripheralFunction> fFunctions = new TreeMap<String, PeripheralFunction>(PeripheralFunction.comparator);
   
   /**
    * Create peripheral
    * 
    * @param basename      Base name e.g. FTM3 => FTM
    * @param instance      Instance e.g. FTM3 => 3
    * @param writerBase    Description of peripheral
    * @param template      The template associated with this peripheral 
    */
   Peripheral(String basename, String instance, PeripheralTemplateInformation template) {
      fBaseName      = basename;
      fInstance      = instance;
      fTemplate      = template;

      fName          = basename+instance;
      fClassName     = basename.substring(0, 1).toUpperCase()+basename.substring(1).toLowerCase()+instance;
      fWriter        = fTemplate.getInstanceWriter(this);
   }
   
   /**
    * Get name of peripheral e.g. FTM2 
    * 
    * @return
    */
   public String getName() {
      return fName;
   }

   /**
    * Get base name of the peripheral e.g. FTM0 = FTM, PTA = PT 
    * 
    * @return
    */
   public String getBaseName() {
      return fBaseName;
   }

   /**
    * Get instance name/number of the peripheral instance e.g. FTM0 = 0, PTA = A 
    * 
    * @return
    */
   public String getInstance() {
      return fInstance;
   }

   /**
    * Get name of C peripheral class e.g. Ftm2 
    * 
    * @return
    */
   public String getClassName() {
      return fClassName;
   }

   /**
    * Get description of peripheral 
    * 
    * @return
    */
   public String getDescription() {
      return fWriter.getGroupTitle();
   }
   
   /**
    * Get the template associated with this peripheral 
    * 
    * @return
    */
   public PeripheralTemplateInformation getPeripheralTemplate() {
      return fTemplate;
   }
   
   /**
    * Set clock information
    * 
    * @param clockReg   Clock register name e.g. SCGC5
    * @param clockMask  Clock register maks e.g. SIM_SCGC5_PORTB_MASK
    */
   public void setClockInfo(String clockReg, String clockMask) {
      this.fClockReg  = clockReg;
      this.fClockMask = clockMask;
   }
   
   @Override
   public String toString() {
      return fName;
   }

   /**
    * Get clock register e.g. SIM->SCGC6 
    * 
    * @return
    */
   public String getClockReg() {
      return fClockReg;
   }

   /**
    * Get clock register mask e.g. ADC0_CLOCK_REG 
    * 
    * @return
    */
   public String getClockMask() {
      return fClockMask;
   }

   /**
    * Add to map of functions on this peripheral 
    * 
    * @param peripheralFunction
    */
   public void addFunction(PeripheralFunction peripheralFunction) {
      fFunctions.put(peripheralFunction.getName(), peripheralFunction);
      fWriter.addFunction(peripheralFunction);
   }
   
   public TreeMap<String, PeripheralFunction> getFunctions() {
      return fFunctions;
   }

   public void addIrqNum(String irqNum) {
      this.fIrqNums.add(irqNum);
   }

   public ArrayList<String> getIrqNums() {
      return fIrqNums;
   }

   public int getIrqCount() {
      return fIrqNums.size();
   }

   public void setIrqHandler(String irqHandler) {
      this.fIrqHandler  = irqHandler;
   }
   
   public String getIrqHandler() {
      return fIrqHandler;
   }

   public String getIrqNumsAsInitialiser() {
      if (fIrqNums.isEmpty()) {
         return null;
      }
      StringBuffer buff = new StringBuffer();
      boolean firstElement = true;
      for (String num:fIrqNums) {
         if (!firstElement) {
            buff.append(", ");
         }
         buff.append(num);
         firstElement = false;
      }
      return buff.toString();
   }

   /**
    * Indicates that it is necessary to create a Peripheral Information class
    *  
    * @return true if Information class is needed
    * @throws Exception 
    */
   public boolean classIsUsed() {
      boolean needed = 
            (getClockMask() != null) || 
            (getClockReg() != null);
      return needed;
   }

   /**
    * Write Peripheral to XML file<br>
    * 
    * <pre>
    * </pre>
    * @param documentUtilities Where to write
    * 
    * @throws IOException 
    */
   public void writeXmlInformation(XmlDocumentUtilities documentUtilities) throws IOException {
      documentUtilities.openTag("peripheral");
      documentUtilities.writeAttribute("name", fName);

      // Additional, peripheral specific, information
      if ((fClockReg != null) || (fClockMask != null)) {
         documentUtilities.openTag("clock");
         if (fClockReg != null) {
            documentUtilities.writeAttribute("clockReg",  fClockReg);
         }
         if (fClockMask != null) {
            documentUtilities.writeAttribute("clockMask", fClockMask);
         }
         documentUtilities.closeTag();
      }
      if (fIrqHandler != null) {
         documentUtilities.writeAttribute("irqHandler",  fIrqHandler);
      }
      for (String irq:fIrqNums) {
         documentUtilities.openTag("irq");
         documentUtilities.writeAttribute("num", irq);
         documentUtilities.closeTag();
      }
      documentUtilities.closeTag();
   }

   public void writeInfoClass(DeviceInformation deviceInformation, DocumentUtilities writer) throws IOException {
      fWriter.writeInfoClass(deviceInformation, writer);
   }

   public void writeWizard(DocumentUtilities writer) throws IOException {
      fWriter.writeWizard(writer);
   }

   public ArrayList<InfoTable> getFunctionTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fPeripheralFunctions);
      return rv;
   }

   public String getDeclarations() {
      return fWriter.getTemplate();
   }

   public WriterBase getWriter() {
      return fWriter;
   }
   

}