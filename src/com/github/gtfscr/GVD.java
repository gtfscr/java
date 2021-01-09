package com.github.gtfscr;

import java.io.*;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import com.csvreader.CsvReader;

public class GVD {

	GTFS gtfs;
	Karws karws;
	public GVDSort gvds = new GVDSort();
	public GVDParser gvdp;

	Set<File> ttySet = new TreeSet<>();
	File gvdd;

	int sd;
	int ed;
	BigInteger m;

	Map<String, String> mtt = new HashMap<>();
	Set<String> agency = new TreeSet<>();

	public int[] size = null;
	public int[] count = null;
	public int cind = 0;

	public Set<String> agencyReject = null;

	static SimpleDateFormat dfGvd = new SimpleDateFormat("yyyy-MM-dd");

	public GVD(GTFS gtfs, String GVD_PATH) throws Exception {
		this.gtfs = gtfs;
		ed = gtfs.eday;
		sd = gtfs.sday;
		m = BigInteger.ONE.shiftLeft(ed - sd + 1).subtract(BigInteger.ONE);
		gvdd = new File(GVD_PATH);
		if (gvdd.exists() && gvdd.isDirectory()) {
			for (File f : gvdd.listFiles()) {
				if (f.isDirectory()) {
					ttySet.add(f);
				}
			}
		}
		count = new int[ttySet.size()];
		size = new int[ttySet.size()];
		try {
			karws = new Karws(gtfs.PATH+"karws/");
		} catch (Exception e) {
			e.printStackTrace();
		}
		gvdp = new GVDParser(karws.seznamStatu);

		mtt.put("11", "Os");
		mtt.put("C1", "Ex");
		mtt.put("C2", "R");
		mtt.put("C3", "Sp");

		agencyReject = new HashSet<>();
		agencyReject.add("5400");
	}

	public void make() throws Exception {
		for (File f : ttySet) {
			makeYear(f);
			if (ttySet.size()<cind) {
				cind++;
			}
		}
	}

	public void finish() throws Exception {
		addAgency();
		addStops();
	}

	public void makeYear(File dir) throws Exception {
		count[cind] = 0;
		size[cind] = 0;
		gvds.sort(dir);
		size[cind] = gvds.sort.size();
		String year = dir.getName();
		for (Set<String> set : gvds) {
			makeTR(set, year);
			count[cind]++;
		}
	}

	public void makeTR(Set<String> set, String year) throws Exception {

		String trainName = "";
		for (String tr : set) {
			gvds.parseLine(tr);
			trainName = gvds.get("trainName");
			if (trainName.length() > 0) {
				trainName = " " + trainName;
				break;
			}
		}
		BigInteger big = new BigInteger("0");
		for (String tr : set) {
			gvds.parseLine(tr);
			BigInteger bc = new BigInteger(gvds.get("bitmapDays"), 16);
			int s = GVD.gvdToInt(gvds.get("startDate"));
			int e = s + bc.bitLength() - 1;

			if (e >= sd && s <= ed) {
				bc = bc.shiftRight(e - ed);
				bc = bc.and(m);
				bc = bc.andNot(big);
				e = ed;
				if (bc.bitCount() != 0) {
					big = big.or(bc);
					if (agencyReject != null
							&& agencyReject.contains(gvds.get("company"))) {
						continue;
					}
					if (bc.getLowestSetBit() != 0) {
						e = ed - bc.getLowestSetBit();
						bc = bc.shiftRight(bc.getLowestSetBit());
					}
					s = e - (bc.bitLength() - 1);

					String name = gvds.get("name");
					InputStream is = (name.indexOf(".xml.zip") != -1) ? new GZIPInputStream(
							new FileInputStream(gvdd.getAbsolutePath() + "/"
									+ year + "/" + name)) : gvds.gvdz
							.getInputStream(gvds.gvdz.getEntry(name));

					String nameId = getTripId(name);
					gvdp.parse(is, nameId);
					gtfs.zip.write(gvdp.get().getBytes());
					if (gvdp.list.size() >= 2) {
						String ser = GTFS.intToDate(s) + ","
								+ GTFS.intToDate(e) + "," + calCal(e, bc) + ","
								+ bc.toString(2);
						String serId = gtfs.getCal(ser);
						// System.out.println(serId);
						agency.add(gvds.get("company"));

						Set<String> rln = new LinkedHashSet<>();
						Set<Integer> rsn = new TreeSet<>();
						for (Entry<String, String[]> en : gvdp.otns.entrySet()) {
							String[] arr = en.getValue();
							String kdv = "";
							if (karws.seznamKomDrVl.containsKey(arr[1])) {
								kdv = karws.seznamKomDrVl.get(arr[1]);
							} else if (mtt.containsKey(arr[0])) {
								kdv = mtt.get(arr[0]);
							}
							rln.add(kdv + " " + en.getKey());
							rsn.add(Integer.parseInt(en.getKey()));
						}
						String lName = GTFS.join(" /", rln) + trainName;
						int sName = rsn.iterator().next();
						String ro = gvds.get("company") + ",\"" + sName
								+ "\",\"" + lName + "\",2";
						String rr = GTFS.join("-", gvdp.otns.keySet());
						String routeId = gtfs.getRoutes(rr, ro);
						String tid = rr + "_" + nameId;
						gtfs.fosTrips.write((routeId + "," + serId + "," + tid
								+ ",\"" + gvdp.stopName + "\"\r\n").getBytes());
						System.out.println(lName);

					}
				}
			}
		}
	}

	public String calCal(int dayEnd, BigInteger big) {
		int d = ((dayEnd + GTFS.PO) % 7);
		int[] days = new int[7];
		for (int i = 0; i < big.bitLength(); i++) {
			if (d < 0) {
				d = 6;
			}
			if (big.testBit(i)) {
				days[d]++;
			} else {
				days[d]--;
			}
			d--;
		}
		String cal = "";
		for (int i : days) {
			cal += (i > 0) ? "1" : "0";
		}
		return cal;
	}

	public void addAgency() throws Exception {
		Map<String, String> agn = new HashMap<>();
		InputStream is = gtfs.getIs("gvd_agency.txt");
		if (is != null) {
			CsvReader csv = new CsvReader(new BufferedReader(
					new InputStreamReader(is, "UTF-8")));
			csv.readHeaders();
			while (csv.readRecord()) {
				agn.put(csv.get("agency_id"), csv.getRawRecord());
			}
		}

		for (String s : agency) {
			if (agn.containsKey(s)) {
				gtfs.sbAgency.append(agn.get(s) + "\r\n");
			} else {
				String ss = karws.seznamSpol.get(s);
				if (ss == null) {
					ss = "??,http://";
				}
				String ag = s + "," + ss + ",Europe/Prague,cs";
				System.out.println(ag);
				gtfs.sbAgency.append(ag + "\r\n");
			}
		}
	}

	public void addStops() throws Exception {
		Map<String, String> loc = new HashMap<>();
		InputStream is = gtfs.getIs("gvd_loc.txt");
		CsvReader csv = new CsvReader(new BufferedReader(new InputStreamReader(
				is)));
		csv.readHeaders();
		while (csv.readRecord()) {
			loc.put(csv.get("stop_id"),
					csv.get("stop_lat") + "," + csv.get("stop_lon"));
		}

		StringBuffer none = new StringBuffer();
		Map<String, String> abc = new TreeMap<>();
		for (Entry<String, String> e : gvdp.stops.entrySet()) {
			String id = e.getKey();
			String sloc = (loc.containsKey(id)) ? loc.get(id) : ",";
			if (sloc.equals(",")) {
				none.append("(" + e.getKey() + ",\"" + e.getValue()
						+ "\"),\r\n");
			}
			abc.put(e.getValue() + " " + id, id + ",\"" + e.getValue() + "\","
					+ sloc);
		}
		for (Entry<String, String> e : abc.entrySet()) {
			gtfs.fosStops.write((e.getValue() + "\r\n").getBytes());
		}
		System.out.println(none.toString());

	}

	public String getTripId(String name) {
		String[] arr = name.split("_");
		return (arr[2] + '_' + arr[3] + '_' + arr[4]).replace("-", "").split(
				".xml")[0];
	}

	public static int gvdToInt(String date) throws Exception {
		return GTFS.day(dfGvd.parse(date));
	}
}
