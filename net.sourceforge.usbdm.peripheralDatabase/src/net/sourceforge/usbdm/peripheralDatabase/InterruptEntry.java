package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;

   class InterruptEntry {

      private int       index;
      private String    name;
      private String    handlerName; // This is a transient property and not written to SVD
      private String    description;
      
      public InterruptEntry() {
         this.index      = -100;     
         this.name        = "";       
         this.description = "";
         this.handlerName = null;
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
         this.index      = number;     
         this.name        = name;    
         this.handlerName = handlerName;
         this.description = description;
      }

      @Override
      public boolean equals(Object other) {
         if (!(other instanceof InterruptEntry)) {
            return false;
         }
         InterruptEntry o = (InterruptEntry)other;
         return (this.index == o.index) &&
                 this.name.equalsIgnoreCase(o.name) &&
                (this.description.equalsIgnoreCase(o.description));
      }
      
      /**
       * @return the index number
       */
      public int getIndexNumber() {
         return index;
      }

      /**
       * @param index number the number to set
       */
      public void setIndexNumber(int number) {
         this.index = number;
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
       * @return the name of the handler
       */
      public String getHandlerName() {
         return handlerName;
      }

      /**
       * @return the description
       */
      public String getDescription() {
         return description;
      }

      /**
       * @param description the description to set
       */
      public void setDescription(String description) {
         this.description = description;
      }

      public void writeSVD(PrintWriter writer, int indent) {
         final String indenter = RegisterUnion.getIndent(indent);
         writer.println(              indenter+"<interrupt>");
         writer.println(String.format(indenter+"   <name>%s</name>", name));
         writer.println(String.format(indenter+"   <description>%s</description>", SVD_XML_BaseParser.escapeString(description)));
         writer.println(String.format(indenter+"   <value>%d</value>", index));
         writer.println(              indenter+"</interrupt>");
      }

      public String getCDescription() {
         return SVD_XML_BaseParser.unEscapeString(description);
      }
   };