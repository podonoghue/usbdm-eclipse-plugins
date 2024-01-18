package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Pin.PinIrqDmaValue;
import net.sourceforge.usbdm.deviceEditor.information.Pin.PinPullValue;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Describes the set of peripheral signals that are mapped to a pin for a particular mux value<br>
 */
public class MappingInfo extends ObservableModel {

   public final static long PORT_PCR_PULL_SHIFT  =  0;
   public final static long PORT_PCR_PULL_MASK   =  (0x03L << PORT_PCR_PULL_SHIFT);
   public final static long PORT_PCR_SRE_SHIFT   =  2;
   public final static long PORT_PCR_SRE_MASK    =  (0x01L << PORT_PCR_SRE_SHIFT);
   public final static long PORT_PCR_PFE_SHIFT   =  4;
   public final static long PORT_PCR_PFE_MASK    =  (0x01L << PORT_PCR_PFE_SHIFT);
   public final static long PORT_PCR_ODE_SHIFT   =  5;
   public final static long PORT_PCR_ODE_MASK    =  (0x01L << PORT_PCR_ODE_SHIFT);
   public final static long PORT_PCR_DSE_SHIFT   =  6;
   public final static long PORT_PCR_DSE_MASK    =  (0x01L << PORT_PCR_DSE_SHIFT);
   public final static long PORT_PCR_MUX_SHIFT   =  8;
   public final static long PORT_PCR_MUX_MASK    =  (0x07L << PORT_PCR_MUX_SHIFT);
   public final static long PORT_PCR_LK_SHIFT    =  15;
   public final static long PORT_PCR_LK_MASK     =  (0x01L << PORT_PCR_LK_SHIFT);
   public final static long PORT_PCR_IRQC_SHIFT  =  16;
   public final static long PORT_PCR_IRQC_MASK   =  (0x0FL << PORT_PCR_IRQC_SHIFT);
   public final static long PORT_PCR_ISF_SHIFT   =  24;
   public final static long PORT_PCR_ISF_MASK    =  (0x01L << PORT_PCR_ISF_SHIFT);

   // This is a dummy mask used internally
//   public final static long PORT_POLARITY_SHIFT  =  30;
//   public final static long PORT_POLARITY_MASK   =  (0x01L << PORT_POLARITY_SHIFT);
   public final static long PCR_MASK        =
         PORT_PCR_PULL_MASK|PORT_PCR_SRE_MASK|PORT_PCR_PFE_MASK|PORT_PCR_ODE_MASK|
         PORT_PCR_DSE_MASK|PORT_PCR_LK_MASK|PORT_PCR_IRQC_MASK;

   public final static long PROPERTIES_MASK        = PCR_MASK;

   /** List of peripheral signals that are mapped  by this selection */
   private final ArrayList<Signal> fSignals = new ArrayList<Signal>();
   
   /** Pin that signals are mapped to */
   private final Pin fPin;
   
   /** Pin multiplexor setting to map these signals on the pin */
   private final MuxSelection fMuxValue;

   /** Indicates if the current mapping is selected */
   private boolean fSelected;

//   private boolean fBusy = false;
   
   public boolean marked = false;

   /** PCR value (excluding MUX) */
   private long fProperties;
   
   /** Indicate mapping does not correspond to a pin i.e. unassigned */
   public static MappingInfo UNASSIGNED_MAPPING = new MappingInfo(Pin.UNASSIGNED_PIN, MuxSelection.unassigned);
   
   /**
    * Associates a peripheral signal and a pin<br>
    * 
    * @param signal     Peripheral signal
    * @param pin        Pin
    * @param muxValue   Pin multiplexor setting to select associated signal on the pin
    */
   public MappingInfo(Pin pin, MuxSelection muxValue)  {
      fPin       = pin;
      fMuxValue  = muxValue;
   }
   
   /**
    * Get brief description e.g. "PTA1 =>  GPIOC_6/LLWU_P10 @ mux5
    * 
    * @return Description
    */
   String getDescription() {
      return String.format("%s => %s @ %s", fPin.getName(), getSignalNames(), fMuxValue);
   }
   
   /**
    * Get list of peripheral signals that are mapped by this selection
    * 
    * @return List of mapped signals
    */
   public ArrayList<Signal> getSignals() {
      return fSignals;
   }

   /**
    * Add signal to the list of signals in this mapping
    * 
    * @param signal
    */
   public void addSignal(Signal signal) {
      fSignals.add(signal);
   }
   
   
   /**
    * Get pin that signals are mapped to
    * 
    * @return Associated pin
    */
   public Pin getPin() {
      return fPin;
   }

   /**
    * Get pin multiplexor setting to map these signals on the pin
    * 
    * @return Mux value
    */
   public MuxSelection getMux() {
      return fMuxValue;
   }

   /**
    * Sets selection state i.e. whether mux setting is current for a pin<br>
    * If changed then listeners are notified
    * 
    * @param origin     Originating object (Pin or Signal)
    * @param selected   Selected state to set
    * 
    * @return True if the selection changed
    */
   public boolean select(Object origin, boolean selected) {
      if (this == MappingInfo.UNASSIGNED_MAPPING) {
         return false;
      }
      if (!fPin.isAvailableInPackage()) {
         return false;
      }
      if (fSelected == selected) {
         // No change
         return false;
      }
      fSelected = selected;
      if (fPin != origin) {
         fPin.modelElementChanged(null, IModelChangeListener.PROPERTY_MAPPING);
      }
      for (Signal signal:getSignals()) {
         if (signal != origin) {
            signal.modelElementChanged(null, IModelChangeListener.PROPERTY_MAPPING);
         }
      }
      return true;
   }

   /**
    * Indicates if the current mapping is selected (or fixed)
    * 
    * @return
    */
   public boolean isSelected() {
      return fSelected || (fMuxValue == MuxSelection.fixed);
   }
   
   @Override
   public String toString() {
      return "Mapping("+getDescription() + ", " + fSelected+")";
   }

   @Override
   public int hashCode() {
      return fMuxValue.hashCode()^fPin.hashCode()^fSignals.hashCode();
   }

   /**
    * Notifies associated signals that the pin properties have changed
    * 
    * @param pin  Pin with changes
    */
   public void pinPropertiesChanged(Pin pin) {
      for (Signal signal:fSignals) {
         signal.notifyListeners();
      }
   }
   
   /**
    * Determine user description from associated signals.
    * 
    * @return
    */
   public String getMappedSignalsUserDescriptions( ) {
      StringBuilder sb = new StringBuilder();
      boolean doSeparator = false;
      for (Signal signal:getSignals()) {
         if (signal.getUserDescription().isBlank()) {
            continue;
         }
         if (doSeparator) {
            sb.append('/');
         }
         sb.append(signal.getUserDescription());
         doSeparator = true;
      }
      return sb.toString();
   }

   /**
    * Get PCR value (including MUX for convenience)<br>
    * Unavailable properties are returned as 0 (No PCR or Analogue)
    * 
    * @return PCR value
    */
   public long getPcr() {
      if (!hasPcr()) {
         return 0;
      }
      
      long value = (fProperties & PROPERTIES_MASK);

      // Force bits as necessary
      value = (value&~fSignals.get(0).getPcrForcedBitsMask())|fSignals.get(0).getPcrForcedBitsValueMask();
      
      if (!hasDigitalFeatures()) {
         // Clear bits not available
         value &= PORT_PCR_LK_MASK;
      }
      
      if (fMuxValue.isMappedValue()) {
         value |= fMuxValue.value<<PORT_PCR_MUX_SHIFT;
      }
      return value;
   }

   /**
    * Get Pin Interrupt/DMA property field
    * 
    * @return PinIrq property or null if unavailable (No PCR or Analogue)
    */
   public PinIrqDmaValue getInterruptDmaSetting() {
      Long value = getProperty(PORT_PCR_IRQC_MASK, PORT_PCR_IRQC_SHIFT);
      if (value == null) {
         return null;
      }
      return PinIrqDmaValue.valueOf(value.intValue());
   }

   /**
    * Set Pin Interrupt/DMA functions<br>
    * Changes to unavailable properties are ignored (No PCR or Analogue).
    * 
    * @param value Function to set
    */
   public void setInterruptDmaSetting(PinIrqDmaValue value) {
      setProperty(PORT_PCR_IRQC_MASK, PORT_PCR_IRQC_SHIFT, value.getValue());
   }

   /**
    * Get Pin Interrupt/DMA functions
    * 
    * @return PinPullValue property or null if unavailable (No PCR or Analogue)
    */
   public PinPullValue getPullSetting() {
      Long value = getProperty(PORT_PCR_IRQC_MASK, PORT_PCR_IRQC_SHIFT);
      if (value == null) {
         return null;
      }
      return PinPullValue.valueOf(value.intValue());
   }

   /**
    * Set Pin Interrupt/DMA functions<br>
    * Changes to unavailable properties are ignored (No PCR or Analogue).
    * 
    * @param value Function to set
    */
   public void setPullSetting(PinPullValue value) {
      setProperty(PORT_PCR_PULL_MASK, PORT_PCR_PULL_SHIFT, value.getValue());
   }

   /**
    * Get PCR value (excluding MUX)<br>
    * Unavailable properties are returned as 0 (Analogue)
    * 
    * @return PCR value or null if properties doesn't exist (No PCR)
    */
   public Long getProperties() {
      if (!hasPcr()) {
         return null;
      }
      long value = (fProperties & PROPERTIES_MASK);

      // Force bits as necessary
      value = (value&~fSignals.get(0).getPcrForcedBitsMask())|fSignals.get(0).getPcrForcedBitsValueMask();
      
      if (!hasDigitalFeatures()) {
         // Clear bits not available
         value &= PORT_PCR_LK_MASK;
      }
      return value;
   }

   /**
    * Set PCR value (excluding MUX)<br>
    * Changes to unavailable properties are ignored.
    * 
    * @param properties PCR value (excluding MUX)
    * 
    * @return  True if properties changed
    */
   public boolean setProperties(long properties) {
      if (!hasPcr()) {
         return false;
      }
      properties &= PROPERTIES_MASK;
      
      // Force bits as necessary
      properties = (properties&~fSignals.get(0).getPcrForcedBitsMask())|fSignals.get(0).getPcrForcedBitsValueMask();
      
      if (!hasDigitalFeatures()) {
         // Clear bits not available
         properties &= PORT_PCR_LK_MASK;
      }

      if (fProperties == properties) {
         return false;
      }
      fProperties = properties;
      fPin.modelElementChanged(this, IModelChangeListener.PROPERTY_MAPPING);
      for (Signal signal:getSignals()) {
         signal.modelElementChanged(this, IModelChangeListener.PROPERTY_MAPPING);
      }
      fPin.modelElementChanged(this, IModelChangeListener.PROPERTY_MAPPING);
      fPin.setDirty();
      return true;
   }

   /**
    * Get property (field from getProperties())
    * 
    * @param mask    Mask to extract field
    * @param offset  Offset to shift after extraction
    * 
    * @return Extracted field from property or null if doesn't exist (no PCR or Analogue)
    */
   public Long getProperty(long mask, long offset) {
      Long value = getProperties();
      if (value == null) {
         return null;
      }
      if ((offset != PORT_PCR_LK_SHIFT) && !hasDigitalFeatures()) {
         return null;
      }
      return (value&mask)>>offset;
   }

   /**
    * Set PCR value (excluding MUX)
    * 
    * @param properties PCR value (excluding MUX)
    * 
    * @return  True if value changed
    */
   public boolean setProperty(long mask, long offset, long property) {
      return setProperties((getPcr()&~mask)|((property<<offset)&mask));
   }

   /**
    * Returns a list of peripheral signals and identifiers mapped by this selection as a string
    * e.g. <b><i>GPIOC_6[RedLed]/LLWU_P10</b></i>
    * 
    * @return List of mapped signals as string
    */
   public String getSignalNames() {
      StringBuffer names = new StringBuffer();
      for (Signal signal:fSignals) {
         if (names.length() != 0) {
            names.append("/");
         }
         names.append(signal.getName());
         String id = signal.getCodeIdentifier();
         if ((id != null) && !id.isBlank()) {
            names.append("["+signal.getCodeIdentifier()+"]");
         }
      }
      return names.toString();
   }

   /**
    * Indicates if this mapping uses a PCR i.e. it is not fixed
    * 
    * @return
    */
   public boolean hasPcr() {
      return (fMuxValue.isMappedValue());
   }

   /**
    * Indicates if this mapping has digital features i.e. has a PCR and is not Analogue function
    * 
    * @return
    */
   public boolean hasDigitalFeatures() {
      return hasPcr() && (fMuxValue != MuxSelection.ANALOGUE);
   }
};
