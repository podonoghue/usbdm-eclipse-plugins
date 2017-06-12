package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;

   public class InterruptEntry {

      private int       fIndex;
      private String    fName;
      private String    fHandlerName; // This is a transient property and not written to SVD
      private String    fDescription;
      private boolean   fClassMemberUsedAsHandler = false;
      
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
       * @return the index number
       */
      public int getIndexNumber() {
         return fIndex;
      }

      /**
       * @param fIndex number the number to set
       */
      public void setIndexNumber(int number) {
         this.fIndex = number;
      }

      /**
       * @return the name
       */
      public String getName() {
         return fName;
      }

      /**
       * @param name the name to set
       */
      public void setName(String name) {
         this.fName = name;
      }

      /**
       * @return the name of the handler
       */
      public String getHandlerName() {
         return fHandlerName;
      }

      /**
       * @return the name of the handler<br>
       * This overrides the default name created from the name of the vector
       */
      public void setHandlerName(String handlerName) {
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
         this.fDescription = description;
      }

      /**
       * Set whether the handler for this interrupt is a static member of a class
       * 
       * @param handlerIsClassMember
       */
      public void setClassMemberUsedAsHandler(boolean handlerIsClassMember) {
         fClassMemberUsedAsHandler = handlerIsClassMember;
      }
      
      /**
       * Set whether the handler for this interrupt is a static member of a class
       * 
       * @return true if handler is a member function
       */
      public boolean isClassMemberUsedAsHandler() {
         return fClassMemberUsedAsHandler;
      }
      
      /**
       * Write SVD for interrupt entry
       * 
       * @param writer Where to write
       * @param indent Indent to use.  If negative then entry will be written as a single line.
       */
      public void writeSVD(PrintWriter writer, int indent, boolean writeOwner) {
         if (indent<0) {
            writer.print(              "<interrupt>");
            writer.print(String.format("<name>%s</name>", fName));
            writer.print(String.format("<description>%s</description>", SVD_XML_BaseParser.escapeString(fDescription)));
            writer.print(String.format("<value>%d</value>", fIndex));
            if (writeOwner && (fAssociatedPeripheral != null)) {
               writer.print(String.format("<peripheral>%s</peripheral>", fAssociatedPeripheral));
            }
            writer.println(            "</interrupt>");
         }
         else {
            final String indenter = RegisterUnion.getIndent(indent);
            writer.println(              indenter+"<interrupt>");
            writer.println(String.format(indenter+"   <name>%s</name>", fName));
            writer.println(String.format(indenter+"   <description>%s</description>", SVD_XML_BaseParser.escapeString(fDescription)));
            writer.println(String.format(indenter+"   <value>%d</value>", fIndex));
            if (writeOwner && (fAssociatedPeripheral != null)) {
               writer.println(String.format(indenter+"   <peripheral>%s</peripheral>", fAssociatedPeripheral));
            }
            writer.println(              indenter+"</interrupt>");
         }
      }

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
      
      String fAssociatedPeripheral;
      
      public void setPeripheral(String peripheralName) {
         fAssociatedPeripheral = peripheralName;
      }
      
   };