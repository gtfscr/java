package com.github.gtfscr;

import java.io.*;
import java.math.BigInteger;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.*;

public class GTFS {

	public Date startDate;
	public Date endDate;
	int sday;
	int eday;

	String PATH = ".";
	ZipOutputStream zip;

	File tmpStops;
	File tmpTrips;
	FileOutputStream fosStops;
	FileOutputStream fosTrips;
	StringBuffer sbAgency = new StringBuffer(
			"agency_id,agency_name,agency_url,agency_timezone,agency_lang\r\n");

	public Map<String, String> calendar = new LinkedHashMap<>(); // s date, e
	Set<String> agency = new TreeSet<>();
	Map<String, String> routes = new LinkedHashMap<>();
	protected Set<String> routeKey = new HashSet<>();
	protected Set<String> calendarKey = new HashSet<>();

	public static DateFormat sdfGTFS = new SimpleDateFormat("yyyyMMdd");
	public final static long DAY_MILLIS = 86400000;
	final static int PO = 3;

	public GTFS(String gtfsname) throws Exception {
		zip = new ZipOutputStream(new FileOutputStream(gtfsname));
		ini();
	}

	public GTFS(String gtfsname, String PATH) throws Exception {
		File fpath = new File(PATH);
		if (!fpath.exists()) {
			fpath.mkdirs();
		}
		this.PATH = fpath.getAbsolutePath() + "/";
		System.out.println("TPA: " + this.PATH);
		zip = new ZipOutputStream(new FileOutputStream(this.PATH + gtfsname));
		ini();
	}

	public void ini() throws Exception {
		startDate = sdfGTFS.parse(sdfGTFS.format(new Date()));
		// startDate = sdfGTFS.parse("20201103");
		sday = day(startDate);
		eday = sday + 360;
		endDate = sdfGTFS.parse(intToDate(eday));
		System.out.println(sdfGTFS.format(startDate) + " -- "
				+ sdfGTFS.format(endDate));
		
		tmpStops = new File(PATH + "stops.txt");
		tmpTrips = new File(PATH + "trips.txt");
		fosStops = new FileOutputStream(tmpStops);
		fosTrips = new FileOutputStream(tmpTrips);
		fosTrips.write("route_id,service_id,trip_id,trip_headsign\r\n"
				.getBytes());
		fosStops.write("stop_id,stop_name,stop_lat,stop_lon\r\n".getBytes());
		zip.putNextEntry(new ZipEntry("stop_times.txt"));
		zip.write("trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type\r\n"
				.getBytes());
	}

	public void setStartDate(String s) throws Exception {
		startDate = sdfGTFS.parse(s);
		sday = day(startDate);
	}

	public void setEndDate(String s) throws Exception {
		endDate = sdfGTFS.parse(s);
		eday = day(endDate);
	}

	public void finish() throws Exception {
		saveRoutes();
		saveCal();

		fosTrips.close();
		save(tmpTrips);

		fosStops.close();
		save(tmpStops);

		zip.closeEntry();
		zip.putNextEntry(new ZipEntry("agency.txt"));
		zip.write(sbAgency.toString().getBytes());

		zip.closeEntry();
		zip.finish();
	}

	public String getCal(String ser) {
		if (!calendar.containsKey(ser)) {
			String days = ser.split(",")[2];
			int iid = 1;
			String sid = days + "-" + iid;

			while (calendarKey.contains(sid)) {
				iid++;
				sid = days + "-" + iid;
			}
			calendar.put(ser, sid);
			calendarKey.add(sid);
		}
		return calendar.get(ser);
	}

	public String getRoutes(String id, String routs) {
		if (!routes.containsKey(routs)) {
			String sid = id;
			int iid = 0;
			while (routeKey.contains(sid)) {
				iid++;
				sid = id + "_" + iid;
				System.out.println("SID: " + sid);
			}
			routeKey.add(sid);
			routes.put(routs, sid);
		}
		return routes.get(routs);
	}

	public void saveRoutes() throws Exception {
		zip.closeEntry();
		zip.putNextEntry(new ZipEntry("routes.txt"));
		zip.write("route_id,agency_id,route_short_name,route_long_name,route_type\r\n"
				.getBytes());
		for (Entry<String, String> e : routes.entrySet()) {
			zip.write((e.getValue() + "," + e.getKey() + "\r\n").getBytes());
		}
	}

	public void saveCal() throws Exception {
		StringBuffer sbc = new StringBuffer(
				"service_id,date,exception_type\r\n");
		zip.closeEntry();
		zip.putNextEntry(new ZipEntry("calendar.txt"));
		zip.write("service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date\r\n"
				.getBytes());
		for (Entry<String, String> en : calendar.entrySet()) {
			String key = en.getValue();
			String[] arr = en.getKey().split(",");
			// System.out.println(key+" "+Arrays.toString(arr));
			BigInteger b = new BigInteger(arr[3], 2);
			Date ed = sdfGTFS.parse(arr[1]);

			int e = day(ed);
			int s = e - b.bitLength() + 1;
			int d = ((s + PO) % 7);
			char[] ch = arr[2].toCharArray();
			int x = b.bitLength();

			StringBuffer sd = new StringBuffer();
			for (char c : ch) {
				sd.append(c + ",");
			}
			zip.write((key + "," + sd.toString() + intToDate(s) + "," + arr[1] + "\r\n")
					.getBytes());
			for (int i = s; i <= e; i++) {
				x--;
				if (d > 6) {
					d = 0;
				}
				if (b.testBit(x) && ch[d] == '0') {
					sbc.append(key + "," + intToDate(i) + ",1\r\n");
				} else if (!b.testBit(x) && ch[d] == '1') {
					sbc.append(key + "," + intToDate(i) + ",2\r\n");
				}
				d++;
			}
		}
		zip.closeEntry();
		zip.putNextEntry(new ZipEntry("calendar_dates.txt"));
		zip.write(sbc.toString().getBytes());
	}

	public void save(File file) throws Exception {
		zip.closeEntry();
		zip.putNextEntry(new ZipEntry(file.getName()));
		byte[] b = new byte[8192];
		int len;
		FileInputStream is = new FileInputStream(file);
		while ((len = is.read(b)) != -1) {
			zip.write(b, 0, len);
		}
		file.delete();
	}

	public InputStream getIs(String name) throws Exception {
		File f = new File(PATH + "res/raw/" + name);
		if (f.exists()) {
			return new FileInputStream(f);
		}
		return GTFS.class.getResourceAsStream("/raw/" + name);
	}

	public static final int day(Date date) {
		int p = (date.getTime() % DAY_MILLIS != 0) ? 1 : 0;
		return (int) (date.getTime() / DAY_MILLIS) + p;
	}

	public static final String intToDate(int day) {
		return sdfGTFS.format(new Date(day * DAY_MILLIS));
	}

	public static String join(String delimeter, Iterable<String> elements) {
		StringBuffer sb = new StringBuffer();
		boolean k = false;
		for (String s : elements) {
			if (k) {
				sb.append(delimeter);
			}
			sb.append(s);
			k = true;
		}
		return sb.toString();
	}
}