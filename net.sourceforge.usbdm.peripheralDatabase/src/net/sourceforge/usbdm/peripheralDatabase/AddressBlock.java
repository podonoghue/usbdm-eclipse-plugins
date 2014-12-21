package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;

public class AddressBlock extends ModeControl implements Cloneable {

   private long   offset;        // Offset from start of peripheral
   private long   sizeInBytes;   // Size of address block in bytes
   private String usage;         // ??
   private long   widthInBits;   // Width of address block elements in bits (memory access size)

   public AddressBlock() {
      offset = 0L;
      sizeInBytes   = 0L;
      widthInBits  = 32L;
      usage  = "registers";
   }

   public AddressBlock(long offset, long sizeInBytes, long widthInBits, String usage) {
      this.offset       = offset;
      this.sizeInBytes  = sizeInBytes;
      this.widthInBits  = widthInBits;
      this.usage        = usage;
   }

   public AddressBlock(AddressBlock addressBlock) {
      this.offset       = addressBlock.offset;
      this.sizeInBytes  = addressBlock.sizeInBytes;
      this.widthInBits  = addressBlock.widthInBits;
      this.usage        = new String(addressBlock.getUsage());
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
   public long getSizeInBytes() {
      return sizeInBytes;
   }

   /**
    * Sets size of address block
    * 
    * @param sizeInBytes -  Size in bytes
    */
   public void setSize(long sizeInBytes) {
      this.sizeInBytes = sizeInBytes;
   }

   /**
    * Return memory access width of address block
    * 
    * @return Width in bits
    */
   public long getWidthInBits() {
      return widthInBits;
   }

   /**
    * Sets width of address block
    * 
    * @param width -  Width in bits
    */
   public void setWidthInBits(long width) {
      this.widthInBits = width;
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
      System.out.println(String.format("       AddressBlock[0x%08X, 0x%08X].%d", offset, offset+sizeInBytes-1, widthInBits));
   }

   /**
    * 
    *  @param writer         The destination for the XML
    *  @param standardFormat Suppresses some non-standard size optimisations 
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat) {
      writer.println(              "         <addressBlock>");
      if (getWidthInBits() != 32) {
         writer.println(String.format("            <?width \"%d\" ?>",        getWidthInBits()));
      }
      writer.println(String.format("            <offset>0x%X</offset>",    getOffset()));
      writer.println(String.format("            <size>0x%X</size>",        getSizeInBytes()));
      writer.println(String.format("            <usage>%s</usage>",        SVD_XML_BaseParser.escapeString(getUsage())));
      writer.println(              "         </addressBlock>");
   }

}
