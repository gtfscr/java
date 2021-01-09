package com.github.gtfscr;

import java.io.*;
import java.net.Socket;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class Karws {

	String PATH;
	DocumentBuilder builder;
	String host = "provoz.spravazeleznic.cz";

	public Map<String, String> seznamKomDrVl = new HashMap<>();
	public Map<String, String> seznamStatu = new HashMap<>();
	public Map<String, String> seznamSpol = new HashMap<>();

	String[] Names = { "SeznamKomercniDruhVlaku", "SeznamStatu",
			"SeznamSpolecnosti" };

	public Karws(String PATH) throws Exception {
		this.PATH = PATH;
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		kadr();
	}

	public void kadr() throws Exception {
		File f = new File(PATH);
		if (!f.exists()) {
			f.mkdirs();
		}

		for (String name : Names) {
			File file = new File(getPath(name));
			if (!file.exists()) {
				System.out.println("Download " + name + " ("
						+ file.getAbsolutePath() + ")");
				String str = down(name);
				if (str != null) {
					FileOutputStream fos = new FileOutputStream(file);
					fos.write(str.getBytes());
					fos.close();
				}
			}
		}

		Document doc = builder.parse(new FileInputStream(getPath(Names[0])));
		NodeList nl = doc.getElementsByTagName("KomercniDruhVlaku");
		for (int i = 0; i < nl.getLength(); i++) {
			Element el = (Element) nl.item(i);
			seznamKomDrVl
					.put(el.getAttribute("KodTAF"), el.getAttribute("Kod"));
		}
		System.out.println(seznamKomDrVl);

		doc = builder.parse(new FileInputStream(getPath(Names[1])));
		nl = doc.getElementsByTagName("Stat");
		for (int i = 0; i < nl.getLength(); i++) {
			Element el = (Element) nl.item(i);
			if (!el.getAttribute("KodStatu").equals("")) {
				seznamStatu.put(el.getAttribute("KodISOA2"),
						el.getAttribute("KodStatu"));
			}
		}
		System.out.println(seznamStatu);

		doc = builder.parse(new FileInputStream(getPath(Names[2])));
		nl = doc.getElementsByTagName("Spolecnost");
		for (int i = 0; i < nl.getLength(); i++) {
			Element el = (Element) nl.item(i);
			String evCisloEU = el.getAttribute("EvCisloEU");
			String nazev = el.getAttribute("ObchodNazev");
			String www = el.getAttribute("WWW");
			if (!evCisloEU.equals("")) {
				seznamSpol.put(evCisloEU, "\"" + nazev + "\",http://" + www);
			}
		}
		System.out.println(seznamSpol);
	}

	public String down(String name) throws Exception {
		Socket s = new Socket(host, 80);
		DataInputStream dis = new DataInputStream(s.getInputStream());
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());

		byte[] xml = xml(name);
		byte[] header = header(xml.length);
		dos.write(header, 0, header.length);
		dos.write(xml, 0, xml.length);

		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[4096];
		int read;
		while ((read = dis.read(buf)) != -1) {
			sb.append(new String(buf, 0, read, "UTF-8"));
		}

		dis.close();
		dos.close();
		s.close();

		String[] str = sb.toString().split("\r\n\r\n");
		return (str.length > 1) ? str[1] : null;

	}

	public byte[] xml(String name) {
		return ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n"
				+ "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">"
				+ "<soap12:Body>" + "<" + name
				+ " xmlns=\"http://provoz.szdc.cz/kadr\">"
				+ "<jenAktualnePlatne>true</jenAktualnePlatne>" + "</" + name
				+ ">" + "</soap12:Body></soap12:Envelope>").getBytes();
	}

	public byte[] header(int length) {
		return ("POST /kadrws/ciselniky.asmx HTTP/1.1\r\n" + "Host: " + host
				+ "\r\n"
				+ "Content-Type: application/soap+xml; charset=utf-8\r\n"
				+ "Content-Length: " + length + "\r\n" + "Connection: close\r\n\r\n")
				.getBytes();
	}

	public String getPath(String name) {
		return PATH + name + ".xml";
	}
}
