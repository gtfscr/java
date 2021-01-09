package com.github.gtfscr;

import java.io.InputStream;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;

public class GVDParser {
	public boolean pickup1 = false;

	String id;
	public Map<String, String> stops = new HashMap<>();;
	Map<String, String> isoCode; // seznam statu
	List<String[]> list = new ArrayList<>();
	public Map<String, String[]> otns = new LinkedHashMap<>();
	protected Map<String, String[]> _otns = new LinkedHashMap<>();
	DocumentBuilder builder;
	Document doc;

	String[] time = new String[2]; // ALA, ALD,
	int timing = 0;
	int seq = 0;
	boolean saved;

	String stopId;
	String stopName;
	String iso;
	String tt;
	String ctt;
	String otn;
	Set<String> tat = new HashSet<>();

	public GVDParser(Map<String, String> isoCode) throws Exception {
		this.isoCode = isoCode;
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public String get() {
		if (list.size() <= 1) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		String ot = GTFS.join("-", otns.keySet());
		for (String[] s : list) {
			sb.append(ot + '_' + s[1] + ',' + s[2] + "\r\n");
		}
		return sb.toString();
	}

	public void parse(InputStream is, String id) throws Exception {
		clear();
		this.id = id;
		doc = builder.parse(is);
		NodeList nl = doc.getElementsByTagName("CZPTTLocation");
		for (int i = 0; i < nl.getLength(); i++) {
			Element el = (Element) nl.item(i);
			if (el.getElementsByTagName("LocationPrimaryCode").getLength() == 0) {
				continue;
			}
			iso = el.getElementsByTagName("CountryCodeISO").item(0)
					.getTextContent();
			NodeList nlTiming = el.getElementsByTagName("Timing");
			if (nlTiming.getLength() == 0) {
				continue;
			}
			time = new String[2];
			for (int a = 0; a < nlTiming.getLength(); a++) {
				Element elTim = (Element) nlTiming.item(a);
				switch (elTim.getAttribute("TimingQualifierCode")) {
				case "ALA":
					timing = 0;
					break;
				case "ALD":
					timing = 1;
					break;
				}
				time[timing] = elTim.getElementsByTagName("Time").item(0)
						.getTextContent().split("\\.")[0];
				char offset = elTim.getElementsByTagName("Offset").item(0)
						.getTextContent().charAt(0);
				if (offset != 48) {
					time[timing] = plus24(time[timing], offset);
				}
			}
			if (time[0] == null && time[1] == null) {
				continue;
			}
			tat.clear();
			NodeList nlTAT = el.getElementsByTagName("TrainActivityType");
			for (int a = 0; a < nlTAT.getLength(); a++) {
				tat.add(nlTAT.item(a).getTextContent());
			}
			NodeList nlOTN = el.getElementsByTagName("OperationalTrainNumber");
			if (nlOTN.getLength() > 0) {
				otn = nlOTN.item(0).getTextContent();
			}
			stopId = el.getElementsByTagName("LocationPrimaryCode").item(0)
					.getTextContent();
			stopName = el.getElementsByTagName("PrimaryLocationName").item(0)
					.getTextContent();
			if (el.getElementsByTagName("TrafficType").getLength() != 0) {
				tt = el.getElementsByTagName("TrafficType").item(0)
						.getTextContent();
			}
			if (el.getElementsByTagName("CommercialTrafficType").getLength() != 0) {
				ctt = el.getElementsByTagName("CommercialTrafficType").item(0)
						.getTextContent();
			}
			addLine(false);
		}

		if (!saved) {
			addLine(true);
		}
		if (list.size() > 1) {
			if (!list.get(0)[0].equals(list.get(1)[0])) {
				list.get(0)[0] = list.get(1)[0];
			}
			int ll = list.size() - 1;
			if (!list.get(ll - 1)[0].equals(list.get(ll)[0])) {
				list.get(ll)[0] = list.get(ll - 1)[0];
			}
			list.get(ll)[2] = "0"; // pro pripad pickup1
		}
		otns.clear();
		for (String[] s : list) {
			String[] arr = _otns.get(s[0]);
			otns.put(s[0], arr);
		}
	}

	public void addLine(boolean end) {
		seq++;
		saved = false;

		if (time[0] == null) {
			time[0] = time[1];
		} else if (time[1] == null) {
			time[1] = time[0];
		}

		boolean stopYes = false;
		if (end || seq == 1 || tat.contains("0001") || tat.contains("0030")
				|| tat.contains("0032")) {

			stopYes = true;
		}
		if (pickup1 || stopYes) {
			if (isoCode.containsKey(iso)) {
				iso = isoCode.get(iso);
			}
			if (tat.contains("0031") || tat.contains("0032")) {
				time[1] = time[0];
			}
			char pickup = (tat.contains("0030")) ? '3' : '0';
			if (!stopYes) {
				pickup = '1';
			}
			stops.put(iso + stopId, stopName);
			String line = id + ',' + time[0] + ',' + time[1] + ',' + iso
					+ stopId + ',' + seq;
			list.add(new String[] { otn, line, String.valueOf(pickup) });
			saved = true;
			if (otn != null) {
				_otns.put(otn, new String[] { tt, ctt });
			}
			// System.out.println(Arrays.toString(list.get(list.size() - 1)));
		}
	}

	public void clear() {
		list.clear();
		_otns.clear();
		seq = 0;
		time = new String[2];
		otn = null;
		tt = "";
		ctt = "";
	}

	public String plus24(String time, char offset) {
		int plus = (offset - 48) * 24;
		return (Integer.parseInt(time.substring(0, 2)) + plus)
				+ time.substring(2);
	}

}
