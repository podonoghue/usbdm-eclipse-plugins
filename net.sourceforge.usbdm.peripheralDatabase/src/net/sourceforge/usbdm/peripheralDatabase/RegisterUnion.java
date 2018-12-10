package net.sourceforge.usbdm.peripheralDatabase;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 
 * Used to collect registers when creating peripheral <b>struct</b> in a header file.<br>
 * This is necessary to detect unions i.e. registers that overlap
 *
 */
public class RegisterUnion {
   /** Collection of registers/clusters that may overlap */
   private ArrayList<Cluster>  union               = new ArrayList<Cluster>(10);
   
   /** Offset from start??? of union */
   private        long         fOffset;
   
   /** Last address written */
   private        long         fLastWrittenOffset;
   
   /** Size of overlapping registers */
   private        long         fSize;
   
   /** Suffix to apply to next Reserved fill */
   private static int          fSuffix = 0;
   
   /** Writer to use */
   private final  Writer       fWriter;
   
   /** Peripheral owning registers */
   private final  Peripheral   fPeripheral;
   
   /** Indent level */
   private final  int          fIndent;
   
   /** Used to create indent */
   private final  String       fIndenter;
   
   /** Base address ??? */
   private final  long         fBaseAddress;        
   
   private final  String       nestedStructOpening  = "struct {";
   private final  String       nestedStructClosing  = "};\n";
                               
   private final  String       unionOpening         = "union {";
   private final  String       unionClosing         = "};\n";
   
   private        boolean      sorted               = false;
  
   /**
    * Creates a structure to hold a collection of registers being written to a header file.<br>
    * It handles assembling them and writing the appropriate STRUCTS using the writer.
    *  
    * @param writer      Writer to use when outputting the STRUCTS.
    * @param indent      Indent level to indent the structure by.
    * @param peripheral  The peripheral containing the registers.
    * @param baseAddress Base address of 1st register being added.
    */
   public RegisterUnion(Writer writer, int indent, Peripheral peripheral, long baseAddress) {
      reset(0);
      fWriter      = writer;
      fPeripheral  = peripheral;
      fIndent      = indent;
      fIndenter    = getIndent(indent);
      fBaseAddress = baseAddress;
      fSize        = 0;
   }

   /**
    * Clear suffix for Reserved filler bytes in STRUCTS
    */
   static void clearSuffix() {
      fSuffix = 0;
   }
   
   /**
    * Sort the register in the required order for creating STRUCTS
    */
   private void sort() {
      if (sorted ) {
         return;
      }
      Collections.sort(union, new Comparator<Cluster>() {

         @Override
         public int compare(Cluster o1, Cluster o2) {
            long num1 = o1.getAddressOffset();
            long num2 = o2.getAddressOffset();
            if (num1 == num2) {
               num2 = o1.getTotalSizeInBytes();
               num1 = o2.getTotalSizeInBytes();
            }
            if (num1<num2) {
               return -1;
            }
            if (num1>num2) {
               return 1;
            }
            return 0;
         }
      });
      sorted = true;
   }
   
   /** 
    * Add a register to union.
    * If the register does not overlap any of the currently present register then they a written beforehand.
    * 
    * @param  cluster   Add register to STRUCT/UNION being assembled
    * 
    * @throws Exception
    */
   public void add(Cluster cluster) throws Exception {
//      if ("CNTRx%s".equalsIgnoreCase(cluster.getName())) {
//         System.err.println(cluster.getName());
//      }
//      if ("CNTRy%s".equalsIgnoreCase(cluster.getName())) {
//         System.err.println(cluster.getName());
//      }
//      if (cluster.getName().startsWith("FILT")) {
//         System.err.print("RegisterUnion.add - " + this.toString() + "\n");
//      }
      if (cluster.getAddressOffset()<fLastWrittenOffset) {
         throw new Exception(String.format("Register addresses not monotonic, p=%s, c=%s",
               fPeripheral.getName(), cluster.getName()));
      }
      if (!overlaps(cluster)) {
         // Flush current union as we have moved on (current cluster can't be added to union)
         writeHeaderFileUnion();
      }
      if (union.isEmpty()) {
         // First register for this union
         fOffset = cluster.getAddressOffset();
      }
      // Add register to union being assembled & update union size
      if ((cluster.getTotalSizeInBytes()+cluster.getAddressOffset()) > (fSize+fOffset)) {
         fSize = (cluster.getTotalSizeInBytes()+cluster.getAddressOffset()) - fOffset;
      }
      union.add(cluster);
   }
   
   /**
    * Writes C code for the contents of the RegisterUnion e.g. a list of clusters/registers at the same address<br>
    * If elements don't overlap then simple declarations are written.<br>
    * If elements overlap then they are wrapped in an anonymous UNION declaration e.g. <b>union { ... };</b>
    */
   public void writeHeaderFileUnion() throws Exception {
      if (union.size() == 0) {
         return;
      }
      if (fOffset == -1) {
         throw new Exception("RegisterUnion in invalid state");
      }
      boolean wrapInUnion = union.size()>1;
//      if (union.get(0).getName().startsWith("FILT")) {
//         System.err.print("Writing union - " + this.toString() + "\n");
//         System.err.print(union.toString()+"\n");
//      }
      // Write Fill if necessary
      if (fOffset>fLastWrittenOffset) {
         // Fill to current location
         fillTo(fBaseAddress+fOffset);
         fLastWrittenOffset = fOffset;
      }
      if (wrapInUnion) {
         fWriter.write(String.format(Register.LINE_FORMAT, fIndenter+unionOpening, fBaseAddress+fOffset, String.format("(size=%04X)", fSize)));
      }
      sort();
      
      RegisterUnion subStruct     = null;
      int           regsDone      = 0;
      boolean       wrapInStruct  = false;
      
      // Process registers
      for (Cluster register : union) {
         long regSize = register.getTotalSizeInBytes();
         if (regSize == fSize) {
            // Initial registers that occupy the entire union are simply written
            if ((union.size()>1)&&register.isComplexArray()) {
               // If the register is an non-simple array and there is more than one element in the union then
               // it is necessary to wrap in a anonymous STRUCT
               fWriter.write(String.format(Register.LINE_FORMAT, fIndenter+"   "+nestedStructOpening, fBaseAddress+fOffset, String.format("(size=%04X)", fSize)));
               register.writeHeaderFileDeclaration(fWriter, fIndent+(wrapInUnion?3:0), this, fPeripheral, fBaseAddress+fOffset);
               fWriter.write(fIndenter+"   "+unionClosing);         
            }
            else {
               register.writeHeaderFileDeclaration(fWriter, fIndent+(wrapInUnion?3:0), this, fPeripheral, fBaseAddress+fOffset);
            }
            regsDone++;
         }
         else {
            if (subStruct == null) {
               // Need to wrap in STRUCT if more than a single element or needs padding before the element
               wrapInStruct = ((union.size()-regsDone)>1)||(register.getAddressOffset() != fLastWrittenOffset);
               if (wrapInStruct) {
                  fWriter.write(String.format(Register.LINE_FORMAT, fIndenter+"   "+nestedStructOpening, fBaseAddress+fOffset, String.format("(size=%04X)", fSize)));
               }
               subStruct = new RegisterUnion(fWriter, fIndent+3+(wrapInStruct?3:0), fPeripheral, fBaseAddress);
               subStruct.reset(fLastWrittenOffset);
            }
            subStruct.add(register);
         }
      }
      if (subStruct != null) {
         // Flush any remaining elements
         subStruct.writeHeaderFileUnion();
         if (wrapInStruct) {
            // Fill to boundary
            subStruct.fillTo(fBaseAddress+fOffset+fSize);
            fWriter.write(fIndenter+"   "+nestedStructClosing);
         }
      }
      if (wrapInUnion) {
         fWriter.write(fIndenter+unionClosing);         
      }
      reset(fOffset+fSize);
   }
   
   /**
    * Clears register list and resets lastWrittenOffset to value given
    * 
    * @param offset offset to use
    */
   public void reset(long offset) {
      fLastWrittenOffset = offset;
      fOffset            = -1;
      fSize              = 0;
      union.clear();
   }

   /**
    * Indicates that current union is empty
    * 
    * @return
    */
   public boolean isEmpty() {
      return union.isEmpty();
   }
   
   /** Checks if cluster overlaps with the union being assembled
    * 
    * @param cluster
    * 
    * @return true if the cluster is overlapping
    */
   public boolean overlaps(Cluster cluster) {
      return cluster.getAddressOffset()<(fOffset+fSize);
   }

   /** 
    * Fills the structure up to the given address
    * 
    * @param address
    * @throws Exception 
    */
   public void fillTo(long address) throws Exception {
      writeFill(fWriter, fIndent, fSuffix++, fBaseAddress+fLastWrittenOffset, address-(fBaseAddress+fLastWrittenOffset));
      fLastWrittenOffset = address-fBaseAddress;
   }
   
   /** 
    * Fills the structure from given address for size
    * 
    * @param address
    * @param size
    * @throws Exception 
    */
   public void fill(long address, long size) throws Exception {
      writeFill(fWriter, fIndent, fSuffix++, address, size);
      fLastWrittenOffset = address+size;
   }
   
   /**
    * Write fill within a structure e.g. "uint32_t  RESERVED[x]"
    * 
    * @param writer2        Location for write
    * @param indent        Indentation for line
    * @param suffix        Suffix to generate unique symbol
    * @param address       Current address (for alignment)
    * @param size          Number of bytes to pad - may be zero
    * @throws Exception 
    */
   public void writeFill(Writer writer2, int indent, int suffix, long address, long size) throws Exception {
      
      if (size == 0) {
         return;
      }
      long numElements = size;
      StringBuffer line = new StringBuffer(getIndent(indent));
      if (ModeControl.isUseBytePadding()) {
         line.append(String.format("     uint8_t   RESERVED_%d", suffix));
      } else {
         if (((numElements&0x3) == 0) && ((address&0x3) == 0)) {
            line.append(String.format("__I  uint32_t  RESERVED%d", suffix));
            numElements /= 4;
         }
         else if (((numElements&0x1) == 0) && ((address&0x1) == 0)) {
            line.append(String.format("__I  uint16_t  RESERVED%d", suffix));
            numElements /= 2;
         }
         else {
            line.append(String.format("__I  uint8_t   RESERVED%d", suffix));
         }
      }
      if (numElements > 1) {
         line.append(String.format("[%d];", numElements));
      }
      else {
         line.append(';');
      }
      String comment = String.format("0x%X bytes", size);
      if (size < 0) {
         throw new Exception("Negative fill requested");
      }
      writer2.write(String.format(Register.LINE_FORMAT, line.toString(), address, comment));//, baseAddress+lastWrittenOffset, ""));
//      writer2.write(String.format(Register.LINE_FORMAT_NO_DOCUMENTATION, line.toString()));//, baseAddress+lastWrittenOffset, ""));
   }

   /**
    * Return a string of spaces of given size for indenting
    * 
    * @param indent
    * 
    * @return
    */
   public static String getIndent(int indent) {
      final String indentString = 
            "                                                                       " +
            "                                                                       ";
      if (indent>indentString.length()) {
         throw new StringIndexOutOfBoundsException("");
      }
      if (indent<0) {
         return "";
      }
      return indentString.substring(0, indent);
   }

   public String toString() {
      StringBuffer sb = new StringBuffer(); 
      sb.append(String.format("s=%d\n", fSize));
      for (Cluster r:union) {
         sb.append(r.toString());
         sb.append("\n");
      }
      return sb.toString();
   }
}

