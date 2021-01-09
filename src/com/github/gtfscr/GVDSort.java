package com.github.gtfscr;

import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import com.csvreader.CsvReader;

public class GVDSort implements Iterable<TreeSet<String>> {

	public ZipFile gvdz;
	public int size = 0;
	public int count = 0;
	Map<String, TreeSet<String>> sort;
	Map<String, Integer> index;
	String[] array;
	public final String header = "trCore,creation,name,startDate,endDate,variant,bitmapDays,otns,trainName,company";
	DocumentBuilder builder;

	public GVDSort() throws Exception {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	@Override
	public Iterator<TreeSet<String>> iterator() {
		return sort.values().iterator();
	};

	public void sort(File dir) throws Exception {
		index = new HashMap<>();
		for (String s : header.split(",")) {
			index.put(s, index.size());
		}
		sort = new TreeMap<>();
		Set<String> isMap = new HashSet<>();
		boolean isSave = false;
		File fsort = new File(dir.getAbsolutePath() + "s.txt");
		System.out.println(dir.getAbsolutePath());
		if (fsort.exists()) {
			CsvReader csv = new CsvReader(new BufferedReader(
					new InputStreamReader(new FileInputStream(fsort))));
			csv.readHeaders();
			if (csv.getRawRecord().equals(header)) {
				while (csv.readRecord()) {
					isMap.add(csv.get("name"));
					addTRSort(csv.get(0), csv.getRawRecord());
				}
			}
		}

		File fgvd = new File(dir.getAbsolutePath() + "/GVD" + dir.getName()
				+ ".zip");
		if (!fgvd.exists()) {
			fgvd = new File(dir.getAbsolutePath() + "/GVD" + dir.getName()
					+ ".ZIP");
		}
		size = dir.list().length;
		count = 0;
		if (fgvd.exists()) {
			gvdz = new ZipFile(fgvd);
			size += gvdz.size();
			System.out.println("Files: " + size);
			System.out.println("tridim...");
			Enumeration<? extends ZipEntry> en = gvdz.entries();
			while (en.hasMoreElements()) {
				ZipEntry ze = en.nextElement();
				if (!isMap.contains(ze.getName()) && !ze.isDirectory()) {
					String[] arr = parse(gvdz.getInputStream(ze), ze.getName());
					// System.out.println(Arrays.toString(arr) + " " + count);
					addTRSort(arr[0], arr[1]);
					isSave = true;
				}
				count++;
			}
		}

		for (File f : dir.listFiles()) {
			if (!isMap.contains(f.getName())
					&& f.getName().indexOf(".xml.zip") != -1) {
				String[] arr = parse(
						new GZIPInputStream(new FileInputStream(f)),
						f.getName());
				addTRSort(arr[0], arr[1]);
				// System.out.println(Arrays.toString(arr));
				isSave = true;
			}
			count++;
		}
		if (isSave) {
			saveSort(fsort.getAbsolutePath());
		}
	}

	public void addTRSort(String key, String val) {
		if (sort.containsKey(key)) {
			sort.get(key).add(val);
		} else {
			TreeSet<String> set = new TreeSet<>(Collections.reverseOrder());
			set.add(val);
			sort.put(key, set);
		}
	}

	public void saveSort(String path) throws Exception {
		System.out.println(sort.size() + " " + path);
		FileOutputStream fos = new FileOutputStream(path);
		FileOutputStream fosb = new FileOutputStream(path.replace("s.txt",
				"b.txt"));
		fos.write((header + "\r\n").getBytes());
		fosb.write((header + "\r\n").getBytes());
		for (Entry<String, TreeSet<String>> e : sort.entrySet()) {
			Iterator<String> it = e.getValue().iterator();
			while (it.hasNext()) {
				String s = it.next();
				fos.write((s + "\r\n").getBytes());
				String[] arr = s.split(",");
				BigInteger b = new BigInteger(arr[index.get("bitmapDays")], 16);
				arr[index.get("bitmapDays")] = b.toString(2);
				fosb.write((GTFS.join(",", Arrays.asList(arr)) + "\r\n")
						.getBytes());
			}
		}
	}

	public String[] parse(InputStream is, String name) throws Exception {
		Document doc = builder.parse(is);
		String trCore = "";
		String trVariant = "";
		String company = "";
		Set<String> otns = new LinkedHashSet<>();
		String creation = doc.getElementsByTagName("CZPTTCreation").item(0)
				.getTextContent();

		NodeList nlPTID = doc
				.getElementsByTagName("PlannedTransportIdentifiers");
		for (int i = 0; i < nlPTID.getLength(); i++) {
			Element el = (Element) nlPTID.item(i);
			if (el.getElementsByTagName("ObjectType").item(0).getTextContent()
					.equals("TR")) {
				trCore = el.getElementsByTagName("Core").item(0)
						.getTextContent();
				trVariant = el.getElementsByTagName("Variant").item(0)
						.getTextContent();
				company = el.getElementsByTagName("Company").item(0)
						.getTextContent();
			}
		}
		NodeList nlOTN = doc.getElementsByTagName("OperationalTrainNumber");
		for (int i = 0; i < nlOTN.getLength(); i++) {
			otns.add(nlOTN.item(i).getTextContent());
		}
		Element elCal = (Element) doc.getElementsByTagName("PlannedCalendar")
				.item(0);
		String bitMap = elCal.getElementsByTagName("BitmapDays").item(0)
				.getTextContent();
		String startDate = elCal.getElementsByTagName("StartDateTime").item(0)
				.getTextContent().split("T")[0];
		NodeList nlTrainN = doc.getElementsByTagName("Value");
		String trainName = (nlTrainN.getLength() == 1) ? nlTrainN.item(0)
				.getTextContent() : "";
		String val = trCore + "," + (creation) + "," + name + "," + startDate
				+ ",," + trVariant + ","
				+ new BigInteger(bitMap, 2).toString(16) + ","
				+ String.join("/", otns) + "," + trainName.replace(',', ';')
				+ "," + company;
		return new String[] { trCore, val };
	}

	public void parseLine(String line) {
		array = line.split(",");
	}

	public String get(String name) {
		int i = index.get(name);
		if (i > array.length) {
			return null;
		} else {
			return array[i];
		}
	}

}
