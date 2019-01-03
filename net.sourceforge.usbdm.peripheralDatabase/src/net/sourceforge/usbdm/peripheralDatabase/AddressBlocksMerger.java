package net.sourceforge.usbdm.peripheralDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

/**
 * Class used to merge address blocks
 * 
 * If the peripheral has a non-zero getForcedBlockMultiple() then
 * blocks are rounded up in size to be a multiple.
 * If the peripheral has a non-zero getPreferredAccessWidth() then
 * the blocks width is made equal to this if compatible with the block 
 * size and address.
 */
class AddressBlocksMerger {
   
   /**
    * Used to hold memory block information while processing
    */
   private static class BlockInfo {
      /** Start address of block within peripheral */
      public final long        fStartAddress;
      /** Width of register in bits */
      public final long        fWidth;
      /** Access type for block */
      public final AccessType  fAccessType;
      /** Address block is isolated from adjoining blocks */
      public final int         fIsolatedIndex;
      
      /**
       * 
       * @param startAddress  Start address of block within peripheral
       * @param width         Width of register in bits
       * @param accessType    Access type for block
       * @param isolatedIndex Isolated block index used to isolated from adjoining blocks 
       * 
       * @throws Exception 
       */
      public BlockInfo(long startAddress, long width, AccessType accessType, int isolatedIndex) throws Exception {
         if (accessType == null) {
            throw new Exception("accessType == null");
         }
         fStartAddress  = startAddress;
         fWidth         = width;
         fAccessType    = accessType;
         fIsolatedIndex = isolatedIndex;
      }
   }
   
   /** Address blocks will use this width to access memory if non-zero */
   final int  preferredAccessWidth;
   
   /** Address blocks will be aligned to this size */
   final int  forcedBlockMultiple;
   
   /** Used to force alignment */
   final long addressMask;
   
   /** Where to save merged blacks */
   final ArrayList<AddressBlock> peripheralBlocks;

   /** Accumulates address blocks for merging */
   final ArrayList<BlockInfo> originalBlocks;
   
   /** Start of current block */
   long bStartAddress             = 0;
   
   /** Size of current block in bytes */
   long bSizeInBytes              = 0;
   
   /** Widths required for current block */
   long bWidthBitmask             = 0;
   
   /** Isolating index for current block */
   int bIsolatedIndex             = 0;
   
   /**
    * Create address block merger for peripheral
    * 
    * @param peripheral Peripheral associated with blocks to be merged.
    */
   public AddressBlocksMerger(Peripheral peripheral) throws Exception {
      preferredAccessWidth = peripheral.getPreferredAccessWidth()/8;
      forcedBlockMultiple  = peripheral.getForcedBlockMultiple()/8;
      switch(forcedBlockMultiple) {
         case 0  : 
         case 1  : addressMask = 0xFFFFFFFFL; break;
         case 2  : addressMask = 0xFFFFFFFEL; break; 
         case 4  : addressMask = 0xFFFFFFFCL; break;
         default : throw new Exception("Unexpected forced address width");
      }
      peripheralBlocks = peripheral.getAddressBlocks();
      originalBlocks   = new ArrayList<BlockInfo>();
   }

   /** Access type for current block when merging */
   AccessType bAccessType = null;

   /**
    * Check if access types are compatible
    * 
    * @param type1
    * @param type2
    * 
    * @return  true/false indication
    */
   private boolean compatibleAccess(AccessType type1, AccessType type2) {
      if (type1 == type2) {
         return true;
      }
      if (type1.and(AccessType.ReadOnly) == type2.and(AccessType.ReadOnly)) {
         // Both include reading or both exclude
         return true;
      }
      return false;
   }

   private void completeBlock() {
      if (preferredAccessWidth != 0) {
         bWidthBitmask = (1<<preferredAccessWidth);
      }
      if (((bStartAddress|bSizeInBytes) & 0x03) != 0) {
         bWidthBitmask &= ~(1<<4);
      }
      if (((bStartAddress|bSizeInBytes) & 0x01) != 0) {
         bWidthBitmask &= ~(1<<2);
      }
      long width = 8;
      if ((bWidthBitmask & (1<<2)) != 0) {
         width = 16;
      }
      if ((bWidthBitmask & (1<<4)) != 0) {
         width = 32;
      }
      System.err.println(String.format("Adding Block     [0x%04X..0x%04X]", bStartAddress, bStartAddress+bSizeInBytes-1));
      peripheralBlocks.add(new AddressBlock(bStartAddress, bSizeInBytes, width, "registers"));
   }

   /**
    * 
    * @param startAddress  Address of range to add
    * @param sizeInBytes   Size of range to add
    * @param widths
    * @param accessType
    * @param isolatedIndex
    * 
    * @throws Exception
    */
   private void processBlock(long startAddress, long sizeInBytes, long widths, AccessType accessType, int isolatedIndex) throws Exception {
      //         System.err.println(String.format("addBlock()        :   [0x%04X..0x%04X]", startAddress, startAddress+sizInBytes-1));
      if (forcedBlockMultiple != 0) {
         // Align block boundaries according to forced access width
         startAddress &= addressMask;
         sizeInBytes   = (sizeInBytes+forcedBlockMultiple-1) & addressMask;
         //            System.err.println(String.format("AddressBlocksMerger.addBlock(), forced=%d, mask=0x%X", forcedAccessWidth, addressMask));
      }
      if (bSizeInBytes == 0) {
         // New address range
         bStartAddress = startAddress;
         bSizeInBytes  = sizeInBytes;
         bWidthBitmask = widths;
         bAccessType   = accessType;
         return;
      }
      if (isolatedIndex != 0) {
         // XXX Delete
         System.err.println(String.format(
               "Isolated Block   (s=0x%04X, w=0x%X bytes, a=%s, i=%d)",
               startAddress, widths/8, accessType, isolatedIndex));
      }
      // Only merge blocks if not isolated and compatible
      if ((isolatedIndex == bIsolatedIndex) && compatibleAccess(accessType, bAccessType)) {
//         if (startAddress < bStartAddress) {
//            throw new Exception(String.format("Address is going backwards 0x%08X<0x%08X", startAddress, bStartAddress));
//         }
         // Merge blocks that overlap irrespective of width
         if ( (startAddress >= bStartAddress) && (startAddress < (bStartAddress+bSizeInBytes)) || 
             (((startAddress == (bStartAddress+bSizeInBytes)) && ((preferredAccessWidth != 0) || ((widths&bWidthBitmask) != 0))))) {
            // Can add to current range
            if ((sizeInBytes+startAddress) > (bSizeInBytes+bStartAddress)) {
               bSizeInBytes = (sizeInBytes+startAddress)-bStartAddress;
            }
//            System.err.println(String.format("addBlock() merged:   [0x%04X..0x%04X]", bStartAddress, bStartAddress+bSizeInBytes-1));
            return;
         }
      }
      // Save current address range
      completeBlock();

      // Start new address range
      bStartAddress  = startAddress;
      bSizeInBytes   = sizeInBytes;
      bWidthBitmask  = widths;
      bAccessType    = accessType;
      bIsolatedIndex = isolatedIndex;
   }

   long        pendingStartAddress;
   long        pendingSizeInBytes;
   long        pendingWidths;
   AccessType  pendingAccessType;
   int         pendingIsolatedIndex;
   
   /**
    * Add range of memory to address the set of AddressBlocks<b>
    * Attempts to merge adjacent registers.
    * 
    * @param startAddress  Address of range to add
    * @param width         Access width (and memory range size)
    * @param accessType    Access type
    * 
    * @throws Exception 
    */
   private void processOriginalBlock(BlockInfo block) throws Exception {
      /*
       * This routine stages registers that overlap in a simple way
       */
      if (block.fIsolatedIndex != 0) {
         // XXX Delete
      System.err.println(String.format(
            "processOriginalBlock(s=0x%04X, w=0x%X bytes, a=%s, i=%d)",
            block.fStartAddress, block.fWidth/8, block.fAccessType, block.fIsolatedIndex));
      }
      long sizeInBytes = (block.fWidth+7)/8;
      do {
         if (pendingSizeInBytes == 0) {
            // Create new pending block
            pendingStartAddress  = block.fStartAddress;
            pendingSizeInBytes   = sizeInBytes;
            pendingWidths        = (1<<sizeInBytes);
            pendingAccessType    = block.fAccessType;
            pendingIsolatedIndex = block.fIsolatedIndex;
            return;
         }
         if ((block.fStartAddress >= pendingStartAddress) && 
               (block.fStartAddress < (pendingStartAddress+pendingSizeInBytes)) &&
               (block.fIsolatedIndex == pendingIsolatedIndex)) {
            // Add to pending block
            pendingWidths |= (1<<sizeInBytes);
            if ((block.fStartAddress+sizeInBytes) > (pendingStartAddress+pendingSizeInBytes)) {
               pendingSizeInBytes = (block.fStartAddress+sizeInBytes) - pendingStartAddress;
            }
            return;
         }
         processBlock(pendingStartAddress, pendingSizeInBytes, pendingWidths, pendingAccessType, pendingIsolatedIndex);
         pendingSizeInBytes = 0;
      } while (true);
   }

   /**
    * Generate the merged address blocks.<br>
    * The generated blocks will be added to the register
    * 
    * @throws Exception
    */
   public void generate() throws Exception {
      sort();
      pendingSizeInBytes = 0;
      for (BlockInfo block : originalBlocks) {
         processOriginalBlock(block);
      }
      if (pendingSizeInBytes != 0) {
         processBlock(pendingStartAddress, pendingSizeInBytes, pendingWidths, pendingAccessType, pendingIsolatedIndex);
         pendingSizeInBytes = 0;
      }
      if (bSizeInBytes != 0) {
         // Save current address range
         completeBlock();
      }
      finalSort();
      removeRepeats();
   }

   private void removeRepeats() {
      ArrayList<AddressBlock> duplicates = new ArrayList<AddressBlock>();
      AddressBlock anchorBlock = null;
      for (AddressBlock b:peripheralBlocks) {
         if ((anchorBlock == null) || (anchorBlock.getOffset() != b.getOffset())) {
            anchorBlock = b;
            continue;
         }
         if (b.equals(anchorBlock)) {
            duplicates.add(b);
         }
      }
      for (AddressBlock b:duplicates) {
         peripheralBlocks.remove(b);
      }
   }

   /**
    * Sort originalBlocks in the order required for merging<br>
    *   <li>Isolated index
    *   <li>Address
    *   <li>Size
    */
   private void sort() {
      Collections.sort(originalBlocks, new Comparator<BlockInfo>() {

         @Override
         public int compare(BlockInfo o1, BlockInfo o2) {
            long num1;
            long num2;
            num1 = o1.fIsolatedIndex;
            num2 = o2.fIsolatedIndex;
            if (num1 == num2) {
               num1 = o1.fStartAddress;
               num2 = o2.fStartAddress;
            }
            if (num1 == num2) {
               num2 = o1.fWidth;
               num1 = o2.fWidth;
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
    * Sort originalBlocks in the order required for merging<br>
    *   <li>Isolated index
    *   <li>Address
    *   <li>Size
    */
   private void finalSort() {
      Collections.sort(peripheralBlocks, new Comparator<AddressBlock>() {

         @Override
         public int compare(AddressBlock o1, AddressBlock o2) {
            long num1;
            long num2;
            num1 = o1.getOffset();
            num2 = o2.getOffset();
            if (num1 == num2) {
               num1 = o1.getSizeInBytes();
               num2 = o2.getSizeInBytes();
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
    * Add memory range corresponding to a register.<br>
    * These ranges will be merged later on.
    * 
    * @param startAddress  Start address of block within peripheral
    * @param isolatedIndex 
    * @param register      Register to obtain block characteristics from
    * 
    * @throws Exception 
    */
   public void addBlock(long startAddress, int isolatedIndex, Register register) throws Exception {
      if (isolatedIndex != 0) {
         // XXX Delete
         System.err.println("addBlock(Isolated block #"+isolatedIndex+")");
      }
      originalBlocks.add(
            new BlockInfo(
                  startAddress, 
                  register.getWidth(), 
                  register.getAccessType(), 
                  isolatedIndex));
   }

   private int isolationIndex = 0;
   public int createNewIsolation() {
      return  ++isolationIndex;
   }
}
