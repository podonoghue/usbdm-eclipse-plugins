package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;

public class AddressBlock extends ModeControl implements Cloneable {

   private long   offset; // Offset from start of peripheral
   private long   size;   // Size of address block in bytes
   private String usage;  // ??
   private long   width;  // Width of address block elements in bits (memory access size)

   public AddressBlock() {
      offset = 0L;
      size   = 0L;
      width  = 32L;
      usage  = "registers";
   }

   public AddressBlock(long offset, long size, long width, String usage) {
      this.offset = offset;
      this.size   = size;
      this.width  = width;
      this.usage  = usage;
   }

   public AddressBlock(AddressBlock addressBlock) {
      this.offset = addressBlock.offset;
      this.size   = addressBlock.size;
      this.width  = addressBlock.width;
      this.usage  = new String(addressBlock.getUsage());
   }

   /**
    * Gets offset of address block from start of peripheral
    * 
    * @return offset in bytes
    */
   public long getOffset() {
      return offset;
   }

   /**
    * Sets offset of address block from start of peripheral
    * 
    * @param offset - Offset in bytes
    */
   public void setOffset(long offset) {
      this.offset = offset;
   }

   /**
    * Return size of address block
    * 
    * @return Size in bytes
    */
   public long getSize() {
      return size;
   }

   /**
    * Sets size of address block
    * 
    * @param size -  Size in bytes
    */
   public void setSize(long size) {
      this.size = size;
   }

   /**
    * Return width of address block
    * 
    * @return Width in bits
    */
   public long getWidth() {
      return width;
   }

   /**
    * Sets width of address block
    * 
    * @param width -  Width in bits
    */
   public void setWidth(long width) {
      this.width = width;
   }

   public String getUsage() {
      return usage;
   }

   public void setUsage(String usage) {
      this.usage = usage;
   }
   
   /**
    * Generates a deep copy of this
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

   void report() {
      System.out.println(String.format("       AddressBlock[0x%08X, 0x%08X].%d", offset, offset+size-1, width));
   }

   /**
    * 
    *  @param writer         The destination for the XML
    *  @param standardFormat Suppresses some non-standard size optimisations 
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat) {
      writer.println(              "         <addressBlock>");
      writer.println(String.format("            <offset>0x%X</offset>",    getOffset()));
      writer.println(String.format("            <width>%d</width>",        getWidth()));
      writer.println(String.format("            <size>0x%X</size>",        getSize()));
      writer.println(String.format("            <usage>%s</usage>",        SVD_XML_BaseParser.escapeString(getUsage())));
      writer.println(              "         </addressBlock>");
   }

}
