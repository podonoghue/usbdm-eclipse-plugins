package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cpu extends ModeControl {
   
   String   name;
   String   revision;
   String   endian;
   boolean  mpuPresent;
   boolean  fpuPresent;
   int      nvicPrioBits;
   boolean  vendorSystickConfig;
   
   public Cpu() {
      name                = "CM3";       // "CM0", "CM0PLUS", "CM3", "CM4"
      revision            = "r1p0";      // maybe
      endian              = "little";
      mpuPresent          = false;
      fpuPresent          = false;
      nvicPrioBits        = 0;
      vendorSystickConfig = false;
   }
   
   /**
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * @param name the name to set
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the revision
    */
   public String getRevision() {
      return revision;
   }

   /**
    * @return the revision
    */
   public String getRevisionNumber() {
      final Pattern p = Pattern.compile("^r(\\d)p(\\d)$");
      Matcher m = p.matcher(revision);
      if (m.matches()) {
         return m.replaceAll("0x0$10$2");
      }
      return "0x0000";
   }

   /**
    * @param revision the revision to set
    */
   public void setRevision(String revision) {
      this.revision = revision;
   }

   /**
    * @return the endian
    */
   public String getEndian() {
      return endian;
   }

   /**
    * @param endian the endian to set
    */
   public void setEndian(String endian) {
      this.endian = endian;
   }

   /**
    * @return the mpuPresent
    */
   public boolean isMpuPresent() {
      return mpuPresent;
   }

   /**
    * @param mpuPresent the mpuPresent to set
    */
   public void setMpuPresent(boolean mpuPresent) {
      this.mpuPresent = mpuPresent;
   }

   /**
    * @return the fpuPresent
    */
   public boolean isFpuPresent() {
      return fpuPresent;
   }

   /**
    * @param fpuPresent the fpuPresent to set
    */
   public void setFpuPresent(boolean fpuPresent) {
      this.fpuPresent = fpuPresent;
   }

   /**
    * @return the nvicPrioBits
    */
   public int getNvicPrioBits() {
      return nvicPrioBits;
   }

   /**
    * @param nvicPrioBits the nvicPrioBits to set
    */
   public void setNvicPrioBits(int nvicPrioBits) {
      this.nvicPrioBits = nvicPrioBits;
   }

   /**
    * @return the vendorSystickConfig
    */
   public boolean isVendorSystickConfig() {
      return vendorSystickConfig;
   }

   /**
    * @param vendorSystickConfig the vendorSystickConfig to set
    */
   public void setVendorSystickConfig(boolean vendorSystickConfig) {
      this.vendorSystickConfig = vendorSystickConfig;
   }

   public boolean equals(Cpu other) {
      return (this.getName().equalsIgnoreCase(other.getName())) &&
             (this.getRevision().equalsIgnoreCase(other.getRevision())) && 
             (this.getEndian().equalsIgnoreCase(other.getEndian())) && 
             (this.isMpuPresent()          == other.isMpuPresent()) &&
             (this.isFpuPresent()          == other.isFpuPresent()) &&
             (this.getNvicPrioBits()       == other.getNvicPrioBits()) &&
             (this.isVendorSystickConfig() == other.isVendorSystickConfig());
   }
   
   
   private final String banner = 
         "/* ----------------Configuration of the cm4 Processor and Core Peripherals---------------- */\n";
   
   /**
    *   Writes the CPU description to file in C Header file format
    *   
    *  @param writer          The destination for the data
    */
   public void writeCHeaderFile(PrintWriter writer) {
      writer.print(banner);
      writer.print(String.format(   "#define %s                %s\n",             getVersionIdString(), getRevisionNumber()));
      writer.print(String.format(   "#define __MPU_PRESENT            %s\n",             isMpuPresent()?"1":"0"));
      writer.print(String.format(   "#define __NVIC_PRIO_BITS         %d\n",             getNvicPrioBits()));
      writer.print(String.format(   "#define __Vendor_SysTickConfig   %s\n",             isVendorSystickConfig()?"1":"0"));
      writer.print(String.format(   "#define __FPU_PRESENT            %s\n\n",           isFpuPresent()?"1":"0"));
   }

   String getHeaderFileName() {
      if (getName().startsWith("CFV1")) {
         return "core_cfv1.h";
      }
      if (getName().startsWith("CFV2")) {
         return "core_cfv2.h";
      }
      if (getName().startsWith("CM4")) {
         return "core_cm4.h";
      }
      if (getName().startsWith("CM3")) {
         return "core_cm3.h";
      }
      if (getName().startsWith("CM0")) {
         return "core_cm0plus.h";
      }
      return "core_cm3.h";
   }
   
   String getVersionIdString() {
      if (getName().startsWith("CM4")) {
         return "__CM4_REV";
      }
      if (getName().startsWith("CM0")) {
         return "__CM0PLUS_REV";
      }
      if (getName().startsWith("CM3")) {
         return "__CM3_REV";
      }
      return "__CM3_REV";
   }
   
   /**
    *   Writes the CPU description to file in a SVF format
    *   
    *  @param writer          The destination for the XML
    *  @param standardFormat  Suppresses some non-standard size optimizations 
    *  @param owner           The owner - This is used to reduce the size by inheriting default values
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, DevicePeripherals owner) {
      writer.println(                 "   <cpu>");
      writer.println(String.format(   "      <name>%s</name>",                               SVD_XML_BaseParser.escapeString(getName())));
      writer.println(String.format(   "      <revision>%s</revision>",                       SVD_XML_BaseParser.escapeString(getRevision())));
      writer.println(String.format(   "      <endian>%s</endian>",                           getEndian()));
      writer.println(String.format(   "      <mpuPresent>%b</mpuPresent>",                   isMpuPresent()));
      writer.println(String.format(   "      <fpuPresent>%b</fpuPresent>",                   isFpuPresent()));
      writer.println(String.format(   "      <nvicPrioBits>%d</nvicPrioBits>",               getNvicPrioBits()));
      writer.println(String.format(   "      <vendorSystickConfig>%b</vendorSystickConfig>", isVendorSystickConfig()));
      writer.println(                 "   </cpu>");
   }
}
