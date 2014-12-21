package net.sourceforge.usbdm.peripheralDatabase;

import java.util.ArrayList;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

/**
 * Class used to merge address blocks
 * 
 * If the peripheral has a non-zero getForcedBlockMultiple() then
 * blocks are rounded up in size a multiple.
 * If the peripheral has a non-zero getPreferredAccessWidth() then
 * the blocks width is made equal to this is compatible with the block 
 * size and address.
 *
 */
class AddressBlocksMerger {
   long bStartAddress             = 0;
   long bSizeInBytes              = 0;
   long bWidths                   = 0;
   int  preferredAccessWidth      = 0;  // address blocks will use this width to access memory
   int  forcedBlockMultiple       = 0;  // address blocks will be aligned to this size
   long addressMask               = 0;
   AccessType bAccessType         = null;
   ArrayList<AddressBlock> blocks = null;

   public AddressBlocksMerger(Peripheral peripheral) throws Exception {
      blocks               = peripheral.getAddressBlocks();
      preferredAccessWidth = peripheral.getPreferredAccessWidth()/8;
      forcedBlockMultiple  = peripheral.getForcedBlockMultiple()/8;
      switch(forcedBlockMultiple) {
      case 0  : 
      case 1  : addressMask = 0xFFFFFFFFL; break;
      case 2  : addressMask = 0xFFFFFFFEL; break; 
      case 4  : addressMask = 0xFFFFFFFCL; break;
      default : throw new Exception("Unexpected forced address width");
      }
   }

   private void completeBlock() {
      long width = bWidths;
      if (preferredAccessWidth != 0) {
         bWidths = (1<<preferredAccessWidth);
      }
//         System.err.println(String.format("addBlock() complete: [0x%04X..0x%04X]", bStartAddress, bStartAddress+bSizeInBytes-1));
      if (((bStartAddress|bSizeInBytes) & 0x03) != 0) {
         bWidths &= ~(1<<4);
      }
      if (((bStartAddress|bSizeInBytes) & 0x01) != 0) {
         bWidths &= ~(1<<2);
      }
      width = 8;
      if ((bWidths & (1<<2)) != 0) {
         width = 16;
      }
      if ((bWidths & (1<<4)) != 0) {
         width = 32;
      }
      blocks.add(new AddressBlock(bStartAddress, bSizeInBytes, width, "registers"));
   }

   boolean compatibleAccess(AccessType type) {
      if (type == bAccessType) {
         return true;
      }
      if (bAccessType.and(AccessType.ReadOnly) == type.and(AccessType.ReadOnly)) {
         // Both include reading or both exclude
         return true;
      }
      return false;
   }
   

   long        pendingStartAddress;
   long        pendingSizeInBytes;
   long        pendingWidths;
   AccessType  pendingAccessType;
   
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
   public void addBlock(long startAddress, long width, AccessType accessType) throws Exception {
      /*
       * This routine stages registers that overlap in a simple way
       */
      long sizeInBytes = (width+7)/8;
      do {
         if (pendingSizeInBytes == 0) {
            pendingStartAddress  = startAddress;
            pendingSizeInBytes   = sizeInBytes;
            pendingWidths        = (1<<sizeInBytes);
            pendingAccessType    = accessType;
            return;
         }
         if ((startAddress >= pendingStartAddress) && (startAddress < (pendingStartAddress+pendingSizeInBytes))) {
            // Add to pending block
            pendingWidths |= (1<<sizeInBytes);
            if ((startAddress+sizeInBytes) > (pendingStartAddress+pendingSizeInBytes)) {
               pendingSizeInBytes = (startAddress+sizeInBytes) - pendingStartAddress;
            }
            return;
         }
         processBlock(pendingStartAddress, pendingSizeInBytes, pendingWidths, pendingAccessType);
         pendingSizeInBytes = 0;
      } while (true);
   }

   /**
    * 
    * @param startAddress  Address of range to add
    * @param sizeInBytes   Size of range to add
    * @param width
    * @param accessType
    * @throws Exception 
    */
   private void processBlock(long startAddress, long sizeInBytes, long widths, AccessType accessType) throws Exception {
      //         System.err.println(String.format("addBlock()        :   [0x%04X..0x%04X]", startAddress, startAddress+sizInBytes-1));
      if (startAddress < bStartAddress) {
         throw new Exception("Address is going backwards");
      }
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
         bWidths       = widths;
         bAccessType   = accessType;
         return;
      }
      // Only merge blocks if compatible
      if (compatibleAccess(accessType)) {
         // Merge blocks that overlap irrespective of width
         if ((startAddress < (bStartAddress+bSizeInBytes)) || 
             (((startAddress == (bStartAddress+bSizeInBytes)) && ((preferredAccessWidth != 0) || ((widths&bWidths) != 0))))) {
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
      bStartAddress = startAddress;
      bSizeInBytes  = sizeInBytes;
      bWidths       = widths;
      bAccessType   = accessType;
   }

   public void finalise() throws Exception {
      if (pendingSizeInBytes != 0) {
         processBlock(pendingStartAddress, pendingSizeInBytes, pendingWidths, pendingAccessType);
         pendingSizeInBytes = 0;
      }
      if (bSizeInBytes != 0) {
         // Save current address range
         completeBlock();
      }
   }
}
