package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 
 * Used to collect registers when creating a header file.
 * This is necessary to detect unions i.e. registers that overlap
 *
 */
public class RegisterUnion {
   private ArrayList<Cluster> union = new ArrayList<Cluster>(10);
   private long  offset;
   private long  lastWrittenOffset;
   private long  size;
   private int   suffix = 0;
   
   private final PrintWriter  writer;
   private final Peripheral   peripheral;
   private final int          indent;
   private final String       indenter;
   private final long         baseAddress;        
   
   private final String nestedStructOpening  = "struct {";
   private final String nestedStructClosing  = "};\n";
   private final String unionOpening        = "union {";
   private final String unionClosing        = "};\n";
  
   /**
    * Creates a structure to hold a collection of registers being written to a header file.<br>
    * It handles assembling them and writing the appropriate structs using the writer.
    *  
    * @param writer      Writer to use when outputting the struct
    * @param indent      Indent level to indent the structure by
    * @param peripheral  The peripheral containing the registers
    * @param owner       The Peripheral or Cluster that owns the register
    */
   public RegisterUnion(PrintWriter writer, int indent, Peripheral peripheral, long baseAddress) {
      reset(0);
      this.writer      = writer;
      this.peripheral  = peripheral;
      this.indent      = indent;
      this.indenter    = getIndent(indent);
      this.baseAddress = baseAddress;
   }

   /**
    * Sort the register in the required order for creating struct
    */
   private void sort() {
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
   }
   
   /** 
    * Add a register to union
    * 
    * @param  cluster   Add register to struct/union being assembled
    * 
    * @throws Exception
    */
   public void add(Cluster cluster) throws Exception {
      if (cluster.getName().matches("CNTH")) {
         System.err.println("");
      }
      if (cluster.getAddressOffset()<lastWrittenOffset) {
         throw new Exception(String.format("Register addresses not monotonic, p=%s, c=%s",
               peripheral.getName(), cluster.getName()));
      }
      if (union.isEmpty()) {
         // First register for this union
         offset = cluster.getAddressOffset();
      }
      else if (!overlaps(cluster)) {
         // Flush current union as we have moved on (current cluster can't be added to union)
         writeHeaderFileUnion();
         offset = cluster.getAddressOffset();
      }
      // Add register to union being assembled & update union size
      if ((cluster.getTotalSizeInBytes()+cluster.getAddressOffset()) > (size+offset)) {
         size = (cluster.getTotalSizeInBytes()+cluster.getAddressOffset()) - offset;
      }
      union.add(cluster);
   }
   
   /**
    * Writes C code for the contents of the RegisterUnion e.g. a list of clusters/registers at the same address<br>
    * If elements don't overlap then simple declarations are written.<br>
    * If elements overlap then they are wrapped in an anonymous UNION declaration e.g. <b>union { ... };</b>
    * 
    * @param writer
    * @param devicePeripherals
    */
   public void writeHeaderFileUnion() throws Exception {
      if (union.size() == 0) {
         return;
      }
      if (offset == -1) {
         throw new Exception("RegisterUnion in invalid state");
      }
//    writer.println("Writing union");
      boolean wrapInUnion = union.size()>1;
      
      // Write Fill if necessary
      if (offset>lastWrittenOffset) {
         // Fill to current location
         fillTo(offset);
         lastWrittenOffset = offset;
      }
      if (wrapInUnion) {
         writer.print(String.format(Register.lineFormat, indenter+unionOpening, baseAddress, String.format("(size=%04X)", size)));
      }
      sort();
      
      RegisterUnion subStruct     = null;
      int           regsDone      = 0;
      boolean       wrapInStruct  = false;
      
      // Process regs
      for (Cluster register : union) {
         long regSize = register.getTotalSizeInBytes();
         if (regSize == size) {
            // Initial regs that occupy the entire union are simply written
            register.writeHeaderFileDeclaration(writer, indent+(wrapInUnion?3:0), this, peripheral, baseAddress+offset);
            regsDone++;
         }
         else {
            if (subStruct == null) {
               // Need to wrap in struct if more than a single element or needs padding before the element
               wrapInStruct = ((union.size()-regsDone)>1)||(register.getAddressOffset() != lastWrittenOffset);
               if (wrapInStruct) {
                  writer.print(String.format(Register.lineFormat, indenter+"   "+nestedStructOpening, baseAddress, String.format("(size=%04X)", size)));
               }
               subStruct = new RegisterUnion(writer, indent+3+(wrapInStruct?3:0), peripheral, baseAddress);
               subStruct.reset(lastWrittenOffset);
            }
            subStruct.add(register);
         }
      }
      if (subStruct != null) {
         // Flush any remaining elements
         subStruct.writeHeaderFileUnion();
         if (wrapInStruct) {
            // Fill to boundary
            subStruct.fillTo(offset+size);
            writer.print(indenter+"   "+nestedStructClosing);
         }
      }
      if (wrapInUnion) {
         writer.print(indenter+unionClosing);         
      }
      reset(offset+size);
   }
   
   /**
    * Clears register list and resets lastWrittenOffset to value given
    * 
    * @param offset offset to use
    */
   public void reset(long offset) {
      this.lastWrittenOffset = offset;
      this.offset            = -1;
      this.size              = 0;
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
      return cluster.getAddressOffset()<(offset+size);
   }

   /** 
    * Fills the structure up to the given address
    * 
    * @param address
    */
   public void fillTo(long address) {
      writeFill(writer, indent, suffix++, address, address-lastWrittenOffset);
      lastWrittenOffset = address;
   }
   
   /** 
    * Fills the structure from given address for size
    * 
    * @param address
    * @param size
    */
   public void fill(long address, long size) {
      writeFill(writer, indent, suffix++, address, size);
      lastWrittenOffset = address+size;
   }
   
   /**
    * Write fill within a structure e.g. "uint32_t  RESERVED[x]"
    * 
    * @param writer        Location for write
    * @param indent        Indentation for line
    * @param suffix        Suffix to generate unique symbol
    * @param address       Current address (for alignment)
    * @param size          Number of bytes to pad - may be zero
    */
   public void writeFill(PrintWriter writer, int indent, int suffix, long address, long size) {
      
      if (size == 0) {
         return;
      }
      long numElements = size;
      StringBuffer line = new StringBuffer(getIndent(indent));
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
      if (numElements > 1) {
         line.append(String.format("[%d];", numElements));
      }
      else {
         line.append(';');
      }
      writer.print(String.format(Register.lineFormat, line.toString(), baseAddress+lastWrittenOffset, ""));
   }

   /**
    * Return a string of spaces of given size for indenting
    * 
    * @param indent
    * 
    * @return
    */
   public static String getIndent(int indent) {
      final String indentString = "                                                              ";
      if (indent>indentString.length()) {
         throw new StringIndexOutOfBoundsException("");
      }
      return indentString.substring(0, indent);
   }

}

