package net.sourceforge.usbdm.deviceDatabase;

import java.util.ArrayList;
import java.util.Iterator;

public class FileList extends ArrayList<FileAction> {

      private static final long serialVersionUID = 3622396071313657392L;
      
      void put(FileAction info) {
         this.add(info);
//         System.err.println("FileList.put("+info.toString()+")");
      }
      FileAction find(String key) {
         Iterator<FileAction> it = this.iterator();
         while(it.hasNext()) {
            FileAction fileInfo = it.next();
            if (fileInfo.getId().equals(key)) {
               return fileInfo;
            }
         }     
         return null;
      }
      @Override
      public String toString() {
         StringBuffer buffer = new StringBuffer();
         Iterator<FileAction> it = this.iterator();
         while(it.hasNext()) {
            FileAction fileInfo = it.next();
            buffer.append(fileInfo.toString());
         }
         return buffer.toString();     
      }
      public void add(FileList fileList) {
         for(FileAction i : fileList) {
            this.add(i);
         }

      }
   }