package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.Mode;
import net.sourceforge.usbdm.deviceEditor.information.DevicePackage;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;

public class ParseFamilyCSV {

	/** The parsed information */
	private final DeviceInfo           fDeviceInfo;

	/**
	 * Parser for CSV file
	 * 
	 * @param deviceInfomation  Where to place information
	 * 
	 * @throws IOException     
	 * @throws UsbdmException
	 */
	public ParseFamilyCSV(DeviceInfo deviceInfomation) throws IOException, UsbdmException {
		fDeviceInfo        = deviceInfomation;
	}

	private HashMap<String, net.sourceforge.usbdm.peripheralDatabase.Peripheral> createPeripheralsMap(DevicePeripherals devicePeripherals) {

		HashMap<String, net.sourceforge.usbdm.peripheralDatabase.Peripheral> map = 
				new HashMap<String, net.sourceforge.usbdm.peripheralDatabase.Peripheral>();
		for (net.sourceforge.usbdm.peripheralDatabase.Peripheral peripheral:devicePeripherals.getPeripherals()) {
			map.put(peripheral.getName(), peripheral);
		}
		return map;
	}

	/** Index of Pin name column in CSV file */
	private int fPinIndex       = 1;

	/** List of all indices of package columns in CSV file */
	private ArrayList<PackageColumnInfo> fPackageIndexes = new ArrayList<PackageColumnInfo>();

	/** Index of reset signal column in CSV file */
	private int fResetIndex     = 3;

	/** Index of default signal column in CSV file */
	@SuppressWarnings("unused")
	private int fDefaultIndex   = 4;

	/** Start index of multiplexor signal columns in CSV file */
	private int fAltStartIndex  = 5;

	/** Last index of multiplexor signal columns in CSV file */
	private int fAltEndIndex    = fAltStartIndex+7;

	/**
	 * Compares two lines based upon the Port name in line[0]
	 */
	private static Comparator<String[]> LineComparitor = new Comparator<String[]>() {
		@Override
		public int compare(String[] arg0, String[] arg1) {
			if (arg0.length < 2) {
				return (arg1.length<2)?0:-1;
			}
			if (arg1.length < 2) {
				return 1;
			}
			return Signal.comparator.compare(arg0[1], arg1[1]);
		}
	};

	/**
	 * Convert some common names<br>
	 * e.g.<br>
	 * PTA => GPIOA_
	 * 
	 * @param pinText
	 * @return
	 */
	private static String convertName(String pinText) {
		pinText = pinText.replaceAll("PTA", "GPIOA_");
		pinText = pinText.replaceAll("PTB", "GPIOB_");
		pinText = pinText.replaceAll("PTC", "GPIOC_");
		pinText = pinText.replaceAll("PTD", "GPIOD_");
		pinText = pinText.replaceAll("PTE", "GPIOE_");
		return pinText;
	}

	/**
	 * Create a list of signals described by a string
	 * 
	 * @param pinText Text of signal names e.g. <b><i>PTA4/LLWU_P3</b></i>
	 * 
	 * @return List of signals created
	 * 
	 * @throws Exception
	 */
	private ArrayList<Signal> createSignalsFromString(String pinText, Boolean convert) {
		ArrayList<Signal> signalList = new ArrayList<Signal>();
		pinText = pinText.trim();
		if (pinText.isEmpty()) {
			return signalList;
		}
		String[] signalNames = pinText.split("\\s*/\\s*");
		for (String signalName:signalNames) {
			signalName = signalName.trim();
			signalName = fixSignalName(signalName);
			if (signalName.isEmpty()) {
				continue;
			}
			if (convert) {
				signalName = convertName(signalName);
			}
			Signal signal = fDeviceInfo.findOrCreateSignal(signalName);
			if (signal != null) {
				// TODO Log creating signals
				//            System.err.println("Added "+signalName+" to "+signal.getPeripheral());
				signalList.add(signal);
				if (signal != Signal.DISABLED_SIGNAL) {
					Peripheral x = signal.getPeripheral();
					if ((x==null) || x.getClass().equals(WriterForNull.class)) {
						System.err.println("Signal \'" + signalName + "\' added to WriterForNull");
					}
				}
			}
			else {
				System.err.println("Signal not found for " + signalName);
			}
		}
		return signalList;
	}

	private class PackageColumnInfo {
		final public String  name;
		final public int     index;

		public PackageColumnInfo(String name, int index) {
			this.name         = name;
			this.index        = index;
		}

		@Override
		public String toString() {
			return "("+name+", "+index+")";
		}
	}
	/**
	 * Parse line containing Key information
	 *  
	 * @param line
	 * 
	 * @return true - line is valid
	 * 
	 * @throws Exception
	 */
	private boolean parseKeyLine(String[] line) {

		// Set default values for column indices
		fPinIndex       = 1;
		fDefaultIndex   = 4;
		fAltStartIndex  = 5;
		fAltEndIndex    = fAltStartIndex+7;
		fPackageIndexes = new ArrayList<PackageColumnInfo>();

		// Add base device without aliases

		final Pattern packagePattern  = Pattern.compile("[P|p]kg\\s*(.*)\\s*");

		for (int col=0; col<line.length; col++) {
			if (line[col].equalsIgnoreCase("Pin")) {
				fPinIndex = col;
			}
			Matcher packageMatcher = packagePattern.matcher(line[col]);
			if (packageMatcher.matches()) {
				fPackageIndexes.add(new PackageColumnInfo(packageMatcher.group(1), col));
			}
			if (line[col].equalsIgnoreCase("Reset")) {
				fResetIndex = col;
			}
			if (line[col].equalsIgnoreCase("Default")) {
				fDefaultIndex = col;
			}
			if (line[col].equalsIgnoreCase("ALT0")) {
				fAltStartIndex = col;
				fAltEndIndex = col;
			}
			if (line[col].toUpperCase().startsWith("ALT")) {
				if (fAltEndIndex<col) {
					fAltEndIndex = col;
				}
			}
		}
		return true;
	}

	/**
	 * Do some simple conversion on pin names.<br>
	 * Also discards some names.<br>
	 * 
	 * e.g.<br>
	 *  XXX_B => XXX_b<br>
	 *  IRQ_x => discarded<br>
	 * 
	 * 
	 * @param pinName
	 * @return
	 */
	private String fixSignalName(String pinName) {
		if (pinName.matches("IRQ_\\d+$")) {
			return "";
		}
		if (pinName.matches("(.*)(_B)$")) {
			pinName = pinName.replaceAll("(.*)(_B)$", "$1_b");
		}
		if (pinName.matches("(.*)(_A)$")) {
			pinName = pinName.replaceAll("(.*)(_A)$", "$1_a");
		}
		return pinName;
	}

	/**
	 * Parse line containing Pin information
	 *  
	 * @param line
	 * @throws Exception
	 */
	private void parsePinLine(String[] line) throws Exception {

		StringBuffer sb = new StringBuffer();

		if (!line[0].equals("Pin")) {
			return;
		}
		String pinName  = line[fPinIndex];
		if ((pinName == null) || (pinName.isEmpty())) {
			throw new Exception("No pin name");
		}
		pinName = fixSignalName(pinName);
		// Use first name on pin as Pin name e.g. PTC4/LLWU_P8 => PTC4
		Pattern p = Pattern.compile("(.+?)/.*");
		Matcher m = p.matcher(pinName);
		if (m.matches()) {
			pinName = m.group(1);
		}
		final Pin pin = fDeviceInfo.createPin(pinName);

		sb.append(String.format("%-10s => ", pin.getName()));

		boolean pinIsMapped = false;
		for (int col=fAltStartIndex; col<=fAltEndIndex; col++) {
			if (col>=line.length) {
				break;
			}
			ArrayList<Signal> signals = createSignalsFromString(line[col], true);
			for (Signal signal:signals) {
				sb.append(signal.getName()+", ");
				if ((signal != null)) {
					MuxSelection muxValue = MuxSelection.valueOf(col-fAltStartIndex);
					fDeviceInfo.createMapping(signal, pin, muxValue);
					pinIsMapped = true;
				}
			}
		}

		if ((line.length>fResetIndex) && (line[fResetIndex] != null) && (!line[fResetIndex].isEmpty())) {
			String resetName  = line[fResetIndex];
			ArrayList<Signal> resetSignals = createSignalsFromString(resetName, true);
			if (!pinIsMapped) {
				for (Signal resetSignal:resetSignals) {
					sb.append("R:" + resetSignal.getName() + ", ");
					// Pin is not mapped to this signal in the ALT columns - must be a non-mappable pin
					MappingInfo mapping = fDeviceInfo.createMapping(resetSignal, pin, MuxSelection.fixed);
					for (Signal signal:mapping.getSignals()) {
						signal.setResetPin(mapping);
					}
				}
			}
			pin.setResetSignals(fDeviceInfo, resetName);
		}
		else {
			sb.append("R:" + Signal.DISABLED_SIGNAL.getName() + ", ");
			fDeviceInfo.createMapping(Signal.DISABLED_SIGNAL, pin, MuxSelection.unassigned);
			pin.setResetSignals(fDeviceInfo, Signal.DISABLED_SIGNAL.getName());
		}
		for (PackageColumnInfo pkgIndex:fPackageIndexes){
			String pinNum = line[pkgIndex.index];
			if (pinNum.equals("*")) {
				continue;
			}
			DevicePackage devicePackage = fDeviceInfo.findDevicePackage(pkgIndex.name);

			if (devicePackage == null) {
				throw new RuntimeException("Failed to find package " + pkgIndex.name + ", for "+pinName);
			}
			devicePackage.addPin(pin, pinNum);
			sb.append("(" + pkgIndex.name + ":" + pinNum + ") ");
		}
	}

	static final int PERIPHERAL_NAME_COL = 1;
	static final int CLOCK_REG_COL       = 2;
	static final int CLOCK_MASK_COL      = 3;
	static final int CLOCK_SOURCE_COL    = 4;
	static final int IRQ_NUM_COL         = 5;
	static final int CLOCK_ENABLE_COL    = 2;
	static final int CLOCK_DISABLE_COL   = 3;

	/**
	 * Parse line containing Peripheral information
	 * 
	 * @param line
	 * @throws Exception 
	 */
	private void parsePeripheralInfoLine(String[] line) throws Exception {
		if (!line[0].equals("Peripheral")) {
			return;
		}
		if (line.length < 2) {
			throw new RuntimeException("Illegal Peripheral Mapping line");
		}
		String peripheralName = line[PERIPHERAL_NAME_COL];
		Peripheral peripheral = fDeviceInfo.findOrCreatePeripheral(peripheralName);
		if (peripheral == null) {
			throw new RuntimeException("Unable to find peripheral "+peripheralName);
		}
		if (line.length < 3) {
			// No parameters
			return;
		}

		if ((line[CLOCK_ENABLE_COL] != null) && !line[CLOCK_ENABLE_COL].isEmpty()) {
			String clockEnable  = line[CLOCK_ENABLE_COL];
			String clockDisable = null;
			if (clockEnable.matches("SIM->(SCGC\\d?)")) {
				// Old format - convert
				String peripheralClockReg  = line[CLOCK_REG_COL];
				String peripheralClockMask = null;
				if (line.length > CLOCK_MASK_COL) {
					peripheralClockMask = line[CLOCK_MASK_COL];
				}
				if ((peripheralClockMask==null) || peripheralClockMask.isEmpty()) {
					peripheralClockMask = peripheralClockReg.replace("->", "_")+"_"+peripheralName+"_MASK";
				}
				if ((peripheralClockReg != null) && !peripheralClockReg.isEmpty()) {
					Pattern pattern = Pattern.compile("SIM->(SCGC\\d?)");
					Matcher matcher = pattern.matcher(peripheralClockReg);
					if (!matcher.matches()) {
						throw new RuntimeException("Unexpected Peripheral Clock Register " + peripheralClockReg + " for " + peripheralName);
					}
					//               peripheralClockReg = matcher.group(1);
					if (!peripheralClockMask.contains(matcher.group(1))) {
						throw new RuntimeException("Clock Mask "+peripheralClockMask+" doesn't match Clock Register " + peripheralClockReg);
					}
					clockEnable  = peripheralClockReg + " |= " + peripheralClockMask + ";";
					clockDisable = peripheralClockReg + " &= ~" + peripheralClockMask + ";";
					peripheral.setClockControlInfo(clockEnable, clockDisable);
				}
			}
			else {
				clockEnable = clockEnable.replaceAll("\\%", peripheral.getName());
				if (line.length > CLOCK_DISABLE_COL) {
					clockDisable = line[CLOCK_DISABLE_COL];
				}
				if ((clockDisable == null) || clockDisable.isEmpty()) {
					clockDisable = clockEnable.replaceAll("\\|=\\s", "&= ~");
				}
				peripheral.setClockControlInfo(clockEnable, clockDisable);
			}
		}
	}

	/**
	 * Parse DMA info line
	 * 
	 * @param line
	 */
	private void parseDmaMuxInfoLine(String[] line)  {
		if (!line[0].equals("DmaMux")) {
			return;
		}
		if (line.length < 4) {
			throw new RuntimeException("Illegal DmaMux Mapping line");
		}
		fDeviceInfo.createDmaInfo(line[1], Integer.parseInt(line[2]), line[3]);
	}

	/**
	 * Parse file
	 * 
	 * @param reader
	 * @throws Exception
	 */
	private void parseFile(BufferedReader reader) throws Exception {

		ArrayList<String[]> grid = new ArrayList<String[]>();
		do {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			grid.add(line.split(","));
			//         System.err.println(line);
		} while (true);

		Collections.sort(grid, LineComparitor);
		for(String[] line:grid) {
			for (int index=0; index<line.length; index++) {
				line[index] = line[index].trim();
			}
		}
		for(String[] line:grid) {
			if (line.length < 2) {
				continue;
			}
			try {
				parsePinLine(line);
			} catch (Exception e) {
				System.err.println("Exception @line " + line[1].toString());
				throw new RuntimeException(e);
			}
		}
		for(String[] line:grid) {
			if (line.length < 2) {
				continue;
			}
			parsePeripheralInfoLine(line);
		}
		for(String[] line:grid) {
			if (line.length < 2) {
				continue;
			}
			parseDmaMuxInfoLine(line);
		}
		for(String[] line:grid) {
			if (line.length < 2) {
				continue;
			}
			parseParamInfoLine(line);
		}
		for(String[] line:grid) {
			if (line.length < 2) {
				continue;
			}
			parseConstantInfoLine(line);
		}
	}

	/**
	 * Parse peripheral line 
	 * 
	 * @param line
	 */
	private void parseParamInfoLine(String[] line) {
		if (!line[0].equalsIgnoreCase("Param")) {
			return;
		}
		if (line.length != 4) {
			throw new RuntimeException("Illegal Param line");
		}

		PeripheralWithState peripheral = null;
		try {
			peripheral = (PeripheralWithState) fDeviceInfo.findPeripheral(line[1], Mode.fail);
		} catch (ClassCastException e) {
			e.printStackTrace();
		}
		peripheral.addParam(line[2], line[3]);
	}

	/**
	 * Parse constant line
	 * 
	 * @param line
	 */
	private void parseConstantInfoLine(String[] line) {
		if (!line[0].equalsIgnoreCase("Constant")) {
			return;
		}
		if (line.length != 4) {
			throw new RuntimeException("Illegal Constant line");
		}

		PeripheralWithState peripheral = (PeripheralWithState) fDeviceInfo.findPeripheral(line[1], Mode.fail);
		peripheral.addConstant(line[2], line[3]);
	}

	/**
	 * Parse preliminary information from file
	 * 
	 * @param reader
	 * @throws IOException 
	 * @throws UsbdmException 
	 * 
	 * @throws Exception
	 */
	private void parsePreliminaryInformation(BufferedReader reader) throws IOException, UsbdmException {

		// Set default values for column indices
		fPinIndex          = 1;
		fResetIndex        = 3;
		fDefaultIndex      = 4;
		fAltStartIndex     = 5;
		fAltEndIndex       = fAltStartIndex+7;
		fPackageIndexes    = new ArrayList<PackageColumnInfo>();

		ArrayList<String[]> grid = new ArrayList<String[]>();
		do {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			grid.add(line.split(","));
			//         System.err.println(line);
		} while (true);
		for(String[] line:grid) {
			if (line.length<1) {
				continue;
			}
			for (int index=0; index<line.length; index++) {
				line[index] = line[index].trim();
			}
			if (line[0].equalsIgnoreCase("Key")) {
				// Process title line
				parseKeyLine(line);
			}
			else if (line[0].equalsIgnoreCase("Device")) {
				fDeviceInfo.createDeviceInformation(line[1], line[2], line[3]);
			}
			else if (line[0].equalsIgnoreCase("TargetDeviceSubFamily")) {
				fDeviceInfo.setDeviceSubFamily(line[1]);
				fDeviceInfo.setDeviceFamily(DeviceFamily.valueOf(line[2]));
			}
		}
		if (fPackageIndexes.size() == 0) {
			throw new RuntimeException("No packages provided");
		}
		if (fDeviceInfo.getDeviceVariants().size() == 0) {
			throw new RuntimeException("No Devices found in file");
		}
	}

	/**
	 * Process file
	 * 
	 * @param filePath   File to process
	 * 
	 * @return Class containing information from file
	 * 
	 * @throws IOException 
	 */
	public void parseFile(Path filePath) throws Exception {
		// Open source file
		BufferedReader sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
		parsePreliminaryInformation(sourceFile);
		sourceFile.close();

		fDeviceInfo.initialiseTemplates();

		// Re-open source file
		sourceFile = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
		parseFile(sourceFile);
		sourceFile.close();

		// Information from device database
		final DevicePeripherals fDevicePeripherals = fDeviceInfo.getDevicePeripherals();
		System.out.println("SVD File = "+fDevicePeripherals.getSvdFilename());
		// Create map to allow peripheral lookup
		final Map<String, net.sourceforge.usbdm.peripheralDatabase.Peripheral> 
		fPeripheralMap  = createPeripheralsMap(fDevicePeripherals);

		// Attach information from device database
		for (Entry<String, Peripheral> entry:fDeviceInfo.getPeripherals().entrySet()) {
			Peripheral peripheral = entry.getValue();

			// Get database peripheral entry
			net.sourceforge.usbdm.peripheralDatabase.Peripheral dbPeripheral = fPeripheralMap.get(entry.getKey());
			if ((dbPeripheral == null)) {
				if (!peripheral.isSynthetic()) {
					throw new UsbdmException("Peripheral "+entry.getKey()+" not found");
				}
				continue;
			}
			// Get peripheral version
			peripheral.setVersion(dbPeripheral.getBasePeripheral().getSourceFilename().toLowerCase());

			// Attach DMAMUX information from database
			String[] dmaMuxInputs = dbPeripheral.getDmaMuxInputs();
			if (dmaMuxInputs != null) {
				if ((dmaMuxInputs.length != 64) && (dmaMuxInputs.length != 128)) {
					throw new UsbdmException("Illegal dma table size");
				}
				int slotNum = 0;
				for (String dmaMuxInput:dmaMuxInputs) {
					if (!dmaMuxInput.startsWith("Reserved")) {
						fDeviceInfo.createDmaInfo("0", slotNum, dmaMuxInput);
					}
					slotNum++;
				}
			}
			// Attach interrupt information
			final Pattern p = Pattern.compile("^GPIO([A-Z]).*$");
			Matcher m = p.matcher(entry.getKey());
			if (m.matches()) {
				dbPeripheral = fPeripheralMap.get(m.replaceAll("PORT$1"));
			}
			if (dbPeripheral == null) {
				if (!fDeviceInfo.getDeviceFamily().equals(DeviceFamily.mke)) {
					throw new UsbdmException("Peripheral "+m.replaceAll("PORT$1")+" not found");
				}
			}
			else {
				ArrayList<InterruptEntry> interruptEntries = dbPeripheral.getInterruptEntries();
				if (interruptEntries != null) {
					for (InterruptEntry interruptEntry:interruptEntries) {
						peripheral.addIrqNum(interruptEntry.getName()+"_IRQn");
					}
				}
			}
		}
		fDeviceInfo.consistencyCheck();
	}

}
