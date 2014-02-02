package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;

   class InterruptEntry {

      private int       number;
      private String    name;
      private String    handlerName;
      private String    description;
      
      public InterruptEntry() {
         this.number      = -100;     
         this.name        = "";       
         this.description = "";
         this.handlerName = null;
      }

      public InterruptEntry(String name, int number, String handlerName, String description) {
         this.number      = number;     
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
         return (this.number == o.number) &&
                 this.name.equalsIgnoreCase(o.name) &&
                (this.description.equalsIgnoreCase(o.description));
      }
      
      /**
       * @return the number
       */
      public int getNumber() {
         return number;
      }

      /**
       * @param number the number to set
       */
      public void setNumber(int number) {
         this.number = number;
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
       * @return the name
       */
      public String getHandlerName() {
         if (handlerName == null) {
            return name;
         }
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
         writer.println(String.format(indenter+"   <value>%d</value>", number));
         writer.println(              indenter+"</interrupt>");
      }

      public String getCDescription() {
         return SVD_XML_BaseParser.unEscapeString(description);
      }
   };