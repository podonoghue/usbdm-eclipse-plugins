package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.swt.graphics.RGB;

public interface ColorConstants {
   RGB COMMENT      = new RGB(  0, 128,   0);       // C comments (apart from annotations)
   RGB NUMBER       = new RGB(  0,   0, 255);       // numbers
   RGB STRING       = new RGB(  0,   0, 255);       // strings and chars in C code 
   RGB STRING2      = new RGB(  0, 255, 255);       // strings and chars in C code 
	RGB OPTION_TAG   = new RGB(255,   0,   0);       // options within comments
   RGB DEFAULT      = new RGB(  0,   0,   0);       // plain C (apart from strings, chars & numbers)
}
