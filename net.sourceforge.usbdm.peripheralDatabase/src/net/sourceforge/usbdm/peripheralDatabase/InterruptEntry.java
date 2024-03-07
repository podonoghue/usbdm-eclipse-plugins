package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

   public class InterruptEntry {

      /**
       * Indicates mode for this vector
       */
      public static enum Mode {
         /** Use system name for handler e.g. PIT0_IRQHandler */
         NotInstalled,
         /** Use class name for handler e.g. USBDM::Rtc::irqAlarmHandler */
         ClassMethod,
      };
      
      private int                    fIndex;
      private String                 fName;
      private String                 fDescription;
      private ArrayList<Peripheral>  fAssociatedPeripheral = new ArrayList<Peripheral>();
      
      // This is a transient property and not written to SVD
      private String                 fHandlerName;
      
      // This is a transient property and not written to SVD
      private Mode                   fMode = Mode.NotInstalled;
      
      public InterruptEntry() {
         this("", 100, null, "");
      }

      /**
       * Create Interrupt table entry
       * 
       * @param name          Simple name of the entry
       * @param number        0-based index in vector table
       * @param handlerName   Name to use for hander function (may be null)
       * @param description   Description of entry
       * 
       * Note: handlerName is a transient property and not written to SVD
       */
      public InterruptEntry(String name, int number, String handlerName, String description) {
         this.fIndex      = number;
         this.fName        = name;
         this.fHandlerName = handlerName;
         this.fDescription = description;
      }

      @Override
      public boolean equals(Object other) {
         if (!(other instanceof InterruptEntry)) {
            return false;
         }
         InterruptEntry o = (InterruptEntry)other;
         return (this.fIndex == o.fIndex) &&
                 this.fName.equalsIgnoreCase(o.fName) &&
                (this.fDescription.equalsIgnoreCase(o.fDescription));
      }
      
      /**
       * @return the Vector index number to set
       */
      public int getIndexNumber() {
         return fIndex;
      }

      /**
       * @param fIndex number Vector index number to set
       */
      public void setIndexNumber(int number) {
         this.fIndex = number;
      }

      /**
       * @return Name of vector
       */
      public String getName() {
         return fName;
      }

      /**
       * @param name The name to set
       */
      public void setName(String name) {
         this.fName = name;
      }

      /**
       * @return Name of the handler
       */
      public String getHandlerName() {
         return fHandlerName;
      }

      /**
       * @return the name of the handler<br>
       * This overrides the default name created from the name of the vector
       */
      public void setHandlerName(String handlerName) {
//         if (handlerName != null) {
//            System.err.println("New handler = '"+handlerName+"'");
//         }
         fHandlerName = handlerName;
      }

      /**
       * @return the description
       */
      public String getDescription() {
         return fDescription;
      }

      /**
       * @param description the description to set
       */
      public void setDescription(String description) {
         if ((description == null) || description.isEmpty()) {
            return;
         }
         if ((fDescription != null) && !fDescription.isEmpty()) {
            System.err.println("Replacing description '" + fDescription + "' with '"+description+"'");
         }
         this.fDescription = description;
      }

      /**
       * Set type of handler for this interrupt
       * 
       * @param type of handler
       */
      public void setHandlerMode(Mode mode) {
         fMode = mode;
      }
      
      /**
       * Check type of handler for this interrupt
       * 
       * @return type of handler
       */
      public Mode getHandlerMode() {
         return fMode;
      }
      
      /**
       * Write SVD for interrupt entry
       * 
       * @param writer Where to write
       * @param indent Indent to use.  If negative then entry will be written as a single line.
       */
      private void writeSVD(PrintWriter writer, int indent, boolean writeOwner) {
         if (indent<0) {
            // Write on single line
            writer.print(              "<interrupt>");
            writer.print(String.format("<name>%s</name>", fName));
            writer.print(String.format("<description>%s</description>", SVD_XML_BaseParser.escapeString(fDescription)));
            writer.print(String.format("<value>%d</value>", fIndex));
            if (writeOwner) {
               for (Peripheral peripheral:fAssociatedPeripheral) {
                  writer.print(String.format("<peripheral>%s</peripheral>", peripheral.getName()));
               }
            }
            writer.println(            "</interrupt>");
         }
         else {
            // Write on multiple lines
            final String indenter = RegisterUnion.getIndent(indent);
            writer.println(              indenter+"<interrupt>");
            writer.println(String.format(indenter+"   <name>%s</name>", fName));
            writer.println(String.format(indenter+"   <description>%s</description>", SVD_XML_BaseParser.escapeString(fDescription)));
            writer.println(String.format(indenter+"   <value>%d</value>", fIndex));
            if (writeOwner) {
               for (Peripheral peripheral:fAssociatedPeripheral) {
                  writer.println(String.format(indenter+"   <peripheral>%s</peripheral>", peripheral.getName()));
               }
            }
            writer.println(              indenter+"</interrupt>");
         }
      }

      /**
       * Write SVD for interrupt entry
       * 
       * @param writer Where to write
       * @param indent Indent to use.  If negative then entry will be written as a single line.
       */
      public void writeSVD(PrintWriter writer, int indent) {
         writeSVD(writer, indent, false);
      }
      
      /**
       * Write SVD entry for consolidated SVD vector table
       * 
       * @param writer Where to write
       * @param indent Indent to use.  If negative then entry will be written as a single line.
       */
      public void writeSVDTableEntry(PrintWriter writer, int indent) {
         writeSVD(writer, indent, true);
      }

      /**
       * Get description in format suitable for C source file
       */
      public String getCDescription() {
         return SVD_XML_BaseParser.unEscapeString(fDescription);
      }

      /* (non-Javadoc)
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         StringBuffer sb = new StringBuffer();
         sb.append("InterruptEntry(");
         sb.append(fIndex+", ");
         sb.append(fName+", ");
         sb.append(fDescription+", ");
         sb.append(fHandlerName+", ");
         sb.append(")");
         return sb.toString();
      }

      /**
       * Add associated peripheral
       * 
       * @param peripheral
       */
      public void addPeripheral(Peripheral peripheral) {
         fAssociatedPeripheral.add(peripheral);
      }
      
      /**
       * Get associated peripherals
       * 
       * @return Array of associated peripherals
       */
      public ArrayList<Peripheral> getPeripherals() {
         return fAssociatedPeripheral;
      }

      /**
       * Get information about vector after pattern matching<br>
       * Information <b>"name|index|C-description|handler-name|peripherals[,peripheral]"</b><br>
       * Example:    <b>"PORTCD|3|Port interrupt||PORTC,PORTD"</b><br>
       * Example pattern: <b>"^(.*?)\|(.*?)\|(.*?)\|(.*?)\|(.*?)$"</b><br>
       * Example replacement: <b>"$1 used by $5"</b><br>
       * 
       * @param pattern       Pattern used to match to vector information (may be null)
       * @param replacement   Replacement (may be null)
       * 
       * @return  Result   Result after matching/replacement or null if failed match
       */
      public String getInformation(Pattern pattern, String replacement) {
         StringBuilder sb = new StringBuilder();
         
         sb.append(getName());
         sb.append('|');
         sb.append(getIndexNumber());
         sb.append('|');
         sb.append(getCDescription());
         sb.append('|');
         sb.append(getHandlerName());
         sb.append('|');
         boolean firstPeripheral = true;
         for (Peripheral p:getPeripherals()) {
            if (!firstPeripheral) {
               sb.append(',');
            }
            sb.append(p.getName());
            firstPeripheral = false;
         }
         String description = sb.toString();
         if (pattern != null) {
            Matcher matcher = pattern.matcher(description);
            if (!matcher.matches()) {
               return null;
            }
            if (replacement != null) {
               description = matcher.replaceAll(replacement);
            }
         }
         return description;
      }
      
   };