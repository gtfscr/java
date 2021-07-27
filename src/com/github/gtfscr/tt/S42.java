/**
 * Převod souřadnic mezi S42 (Gauss-Krüger) a WGS84
 * Vůbec tomu nerozumín, je to obšlehnuté z nějakeho makra pro excel :D 
 * http://sas2.elte.hu/tg/majster.htm
 * pro extrémní hodnoty (daleko od ČR) to může při idos=true vracet blbosti
 */

package com.github.gtfscr.tt;

public class S42 {
	
	public static String wgs84s(int x, int y, int len, boolean idos) {
		double[] d = wgs84(x, y, idos);
		return String.format("%."+len+"f-%."+len+"f", d[0], d[1]).replace(',', '.').replace('-', ',');
	}
	
	public static double[] wgs84(int x, int y, boolean idos) {
		double[] d = fromS42(x, y, idos);
		return toWGS84(d[0], d[1]);
	}
	
	public static String s42s(double lat, double lon, boolean idos) {
		int[] s = s42(lat, lon, idos);
		return s[0]+","+s[1];
	}
	
	public static int[] s42(double lat, double lon, boolean idos) {
		double[] d = fromWGS84(lat, lon);
		double[] s = toS42(d[0], d[1], idos);
		return new int[] {(int)Math.ceil(s[0]), (int)Math.ceil(-s[1])};
	}
	
	private static double[] fromS42(int x, int y, boolean idos) {
		double A2 = x;
		double B2 = Math.abs(y);
		double C2 = 6378245;
		double D2 = 0.0818133340169312;
		double E2 = 500000+((int)A2/1000000)*1000000;
		double F2 = 0;
		double G2 = 21+6*(((int)A2/1000000)-4);
		if (idos) {
			E2 = 3500000;
			G2 = 15;
		}
		double H2 = G2*Math.PI/180;
		double I2 = 0;
		double J2 = I2*Math.PI/180;
		double K2 = 1;
		double L2 = A2-E2;
		double M2 = B2-F2;
		double N2 = (1-Math.sqrt(1-D2*D2))/(1+Math.sqrt(1-D2*D2));
		double O2 = C2*(J2*(1-D2*D2/4-3*Math.pow(D2,4)/64-5*Math.pow(D2,6)/256)-Math.sin(2*J2)*(3*D2*D2/8+3*Math.pow(D2,4)/32+45*Math.pow(D2,6)/1024)+Math.sin(4*J2)*(15*Math.pow(D2,4)/256+45*Math.pow(D2,6)/1024)-Math.sin(6*J2)*35*Math.pow(D2,6)/3072);
		double P2 = O2+M2/K2;
		double Q2 = P2/(C2*(1-D2*D2/4-3*Math.pow(D2,4)/64-5*Math.pow(D2,6)/256));
		double R2 = Q2+Math.sin(2*Q2)*(3*N2/2-27*Math.pow(N2,3)/32)+Math.sin(4*Q2)*(21*N2*N2/16-55*Math.pow(N2,4)/32)+Math.sin(6*Q2)*151*Math.pow(N2,3)/96+Math.sin(8*Q2)*1097*Math.pow(N2,4)/512;
		double S2 = D2*D2/(1-D2*D2);
		double T2 = S2*Math.cos(R2)*Math.cos(R2);
		double U2 = Math.tan(R2)*Math.tan(R2);
		double V2 = C2/Math.sqrt(1-D2*D2*Math.sin(R2)*Math.sin(R2));
		double W2 = C2*(1-D2*D2)/Math.pow(1-D2*D2*Math.sin(R2)*Math.sin(R2),1.5);
		double X2 = L2/(V2*K2);
		double Y2 = R2-(V2*Math.tan(R2)/W2)*(X2*X2/2-(5+3*U2+10*T2-4*T2*T2-9*S2)*Math.pow(X2,4)/24+(61+90*U2+298*T2+45*U2*U2-252*S2-3*T2*T2)*Math.pow(X2,6)/720);
		double Z2 = H2+(X2-(1+2*U2+T2)*Math.pow(X2,3)/6+(5-2*T2+28*U2-3*T2*T2+8*S2+24*U2*U2)*Math.pow(X2,5)/120)/Math.cos(R2);
		double AA2 = Y2*180/Math.PI;
		double AB2 = Z2*180/Math.PI;
		return new double[]{AA2, AB2};
	}
	
	private static double[] toWGS84(double A7, double B7) {
		double C7 = A7*Math.PI/180;
		double D7 = B7*Math.PI/180;
		double E7 = 6378245;
		double F7 = 0.00335232986925913;
		double G7 = 6378137;
		double H7 = 0.00335281066474748;
		double I7 = 26;
		double J7 = -121;
		double K7 = -78;
		double L7 = Math.sqrt(2*F7-F7*F7);
		double M7 = E7*(1-L7*L7)/Math.pow((1-L7*L7*Math.sin(C7)*Math.sin(C7)),1.5);
		double N7 = E7/Math.sqrt(1-L7*L7*Math.sin(C7)*Math.sin(C7));
		double O7 = (-I7*Math.sin(C7)*Math.cos(D7)-J7*Math.sin(C7)*Math.sin(D7)+K7*Math.cos(C7)+(E7*(H7-F7)+F7*(G7-E7))*Math.sin(2*C7))/(M7*Math.sin(Math.PI/180/3600));
		double P7 = (-I7*Math.sin(D7)+J7*Math.cos(D7))/(N7*Math.cos(C7)*Math.sin(Math.PI/180/3600));
		double Q7 = A7+O7/3600;
		double R7 = B7+P7/3600;
		return new double[] {Q7, R7};
	}
	
	private static double[] toS42(double lat, double lon, boolean idos) {
		double A2 = lat;
		double B2 = lon;
		double C2 = 6378245;
		double D2 = 0.0818133340169312;
		double E2 = 1500000+((((int)B2/6)*1000000));
		double F2 = 0;
		double G2 = 6*(0.5+((int)B2/6));
		if (idos) {
			E2 = 3500000;
			G2 = 15;
		}
		double H2 = G2*Math.PI/180;
		double I2 = 0;
		double J2 = I2*Math.PI/180;
		double K2 = 1;
		double L2 = A2*Math.PI/180;
		double M2 = B2*Math.PI/180;
		double N2 = D2*D2/(1-D2*D2);
		double O2 = C2/Math.sqrt(1-D2*D2*Math.sin(L2)*Math.sin(L2));
		double P2 = Math.tan(L2)*Math.tan(L2);
		double Q2 = N2*Math.cos(L2)*Math.cos(L2);
		double R2 = (M2-H2)*Math.cos(L2);
		double S2 = C2*(L2*(1-D2*D2/4-3*Math.pow(D2,4)/64-5*Math.pow(D2,6)/256)-Math.sin(2*L2)*(3*D2*D2/8+3*Math.pow(D2,4)/32+45*Math.pow(D2,6)/1024)+Math.sin(4*L2)*(15*Math.pow(D2,4)/256+45*Math.pow(D2,6)/1024)-Math.sin(6*L2)*35*Math.pow(D2,6)/3072);
		double T2 = C2*J2*((1-D2*D2/4-3*Math.pow(D2,4)/64-5*Math.pow(D2,6)/256)-Math.sin(2*J2)*(3*D2*D2/8+3*Math.pow(D2,4)/32+45*Math.pow(D2,6)/1024)+Math.sin(4*J2)*(15*Math.pow(D2,4)/256+45*Math.pow(D2,6)/1024)-Math.sin(6*J2)*35*Math.pow(D2,6)/3072);
		double U2 = K2*O2*(R2+(1-P2+Q2)*Math.pow(R2,3)/6+(5-18*P2+P2*P2+72*Q2-85*N2)*Math.pow(R2,5)/120);
		double V2 = K2*(S2-T2+O2*Math.tan(L2)*(R2*R2/2+(5-P2+9*Q2+4*Q2*Q2)*Math.pow(R2,4)/24+(61-58*P2+P2*P2+600*Q2-330*N2)*Math.pow(R2,6)/720));
		double W2 = U2+E2;
		double X2 = V2+F2;
		return new double[] {W2, X2};
	}
	
	private static double[] fromWGS84(double lat, double lon) {
		double A10 = lat;
		double B10 = lon;
		double C10 = A10*Math.PI/180;
		double D10 = B10*Math.PI/180;
		double E10 = 6378137;
		double F10 = 0.00335292371299641;
		double G10 = 6378245;
		double H10 = 0.00335232986925913;
		double I10 = -26;
		double J10 = 121;
		double K10 = 78;
		double L10 = Math.sqrt(2*F10-F10*F10);
		double M10 = E10*(1-L10*L10)/Math.pow((1-L10*L10*Math.sin(C10)*Math.sin(C10)),1.5);
		double N10 = E10/Math.sqrt(1-L10*L10*Math.sin(C10)*Math.sin(C10));
		double O10 = (-I10*Math.sin(C10)*Math.cos(D10)-J10*Math.sin(C10)*Math.sin(D10)+K10*Math.cos(C10)+(E10*(H10-F10)+F10*(G10-E10))*Math.sin(2*C10))/(M10*Math.sin(Math.PI/180/3600));
		double P10 = (-I10*Math.sin(D10)+J10*Math.cos(D10))/(N10*Math.cos(C10)*Math.sin(Math.PI/180/3600));
		double Q10 = A10+O10/3600;
		double R10 = B10+P10/3600;
		return new double[] {Q10, R10};
	}
}
