import com.github.gtfscr.GTFS;
import com.github.gtfscr.GVD;

public class Example {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		String o = "out.zip";
		String p = ".";
		String g = "ftp.cisjr.cz/draha/celostatni/szdc";
		String s = "";
		String e = "";

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "-o":
				o = args[++i];
				break;
			case "-p":
				p = args[++i];
				break;
			case "-g":
				g = args[++i];
				break;
			case "-s":
				s = args[++i];
				break;
			case "-e":
				e = args[++i];
				break;
			}
		}
		System.out.println("-o " + o + "-p " + p + " -g " + g + " -s " + s
				+ " -e " + e);

		try {
			GTFS gtfs = new GTFS(o, p);

			if (s.length() == 8) {
				gtfs.setStartDate(s);
			}
			if (e.length() == 8) {
				gtfs.setEndDate(e);
			}
			System.out.println(gtfs.startDate + " - " + gtfs.endDate);

			GVD gvd = new GVD(gtfs, g);
			gvd.make();
			gvd.finish();
			gtfs.finish();

			System.out.println("OK");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("cas: " + (System.currentTimeMillis() - start));
	}

}
