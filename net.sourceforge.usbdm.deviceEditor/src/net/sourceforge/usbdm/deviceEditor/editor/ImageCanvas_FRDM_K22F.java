package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.deviceEditor.model.PackageImageModel;

public class ImageCanvas_FRDM_K22F extends ImageCanvas {

   public ImageCanvas_FRDM_K22F(Composite parent, PackageImageModel model) {
      super(parent, model);
      addRegion(new Region(201,  641,  455, 674, "A0"));
      addRegion(new Region(201,  674,  455, 707, "A1"));
      addRegion(new Region(201,  707,  455, 740, "A2"));
      addRegion(new Region(201,  740,  455, 773, "A3"));
      addRegion(new Region(201,  773,  455, 806, "A4"));
      addRegion(new Region(201,  806,  455, 839, "A5"));
      addRegion(new Region(459,  643,  607, 674, "A11"));
      addRegion(new Region(459,  674,  607, 705, "A10"));
      addRegion(new Region(459,  705,  607, 737, "A9"));
      addRegion(new Region(459,  737,  607, 768, "A8"));
      addRegion(new Region(459,  768,  607, 799, "A7"));
      addRegion(new Region(459,  799,  607, 830, "A6"));
      addRegion(new Region(806,  431,  998, 462, "D27"));
      addRegion(new Region(806,  462,  998, 493, "D26"));
      addRegion(new Region(810,  500,  998, 533, "D25"));
      addRegion(new Region(810,  533,  998, 565, "D24"));
      addRegion(new Region(711,  581,  998, 613, "D23"));
      addRegion(new Region(711,  613,  998, 645, "D22"));
      addRegion(new Region(711,  645,  998, 676, "D21"));
      addRegion(new Region(711,  676,  998, 708, "D20"));
      addRegion(new Region(711,  708,  998, 740, "D19"));
      addRegion(new Region(711,  740,  998, 772, "D18"));
      addRegion(new Region(711,  772,  998, 803, "D17"));
      addRegion(new Region(711,  803,  998, 835, "D16"));
      addRegion(new Region(1001, 247, 1385, 278, "D15"));
      addRegion(new Region(1001, 278, 1385, 308, "D14"));
      addRegion(new Region(1001, 371, 1278, 403, "D13"));
      addRegion(new Region(1001, 403, 1278, 436, "D12"));
      addRegion(new Region(1001, 436, 1278, 468, "D11"));
      addRegion(new Region(1001, 468, 1278, 500, "D10"));
      addRegion(new Region(1001, 500, 1278, 533, "D9"));
      addRegion(new Region(1001, 533, 1278, 565, "D8"));
      addRegion(new Region(998,  579, 1395, 611, "D7"));
      addRegion(new Region(998,  611, 1395, 643, "D6"));
      addRegion(new Region(998,  643, 1395, 675, "D5"));
      addRegion(new Region(998,  675, 1395, 707, "D4"));
      addRegion(new Region(998,  707, 1395, 738, "D3"));
      addRegion(new Region(998,  738, 1395, 770, "D2"));
      addRegion(new Region(998,  770, 1395, 802, "D1"));
      addRegion(new Region(998,  802, 1395, 834, "D0"));

   }

}
