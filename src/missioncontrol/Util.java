/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author positron
 */
public class Util {
	public static SpeechGenerator speech;

	public synchronized static void log(Object src, String mes) {
		System.out.println(timeToString(new Date())+" "+src+": "+mes);
		//speech.speak(mes);
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	public static String timeToString(Date dd) {
		return sdf.format(dd);
	}

	public static String popenAsString(String ... args) {
		ProcessBuilder pb = new ProcessBuilder(args);
		try {
			Process p = pb.start();
			BufferedReader rd = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
			String line;
			StringBuilder bb = new StringBuilder();
			while( (line=rd.readLine())!=null) {
				bb.append(line);
				bb.append("\n");
			}
			int er = p.waitFor();
			if(er!= 0) throw new RuntimeException("child process retuned "+er);
			return bb.toString();
		}catch(IOException e) {
			Util.log("Util", "Could not exec: "+e);
		} catch (InterruptedException ex) {
			Util.log("Util", "Could not exec: "+ex);
		}
		return null;
	}

	public static void popen(String ... args) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(args);
		try {
			Process p = pb.start();
			int er = p.waitFor();
			if(er!= 0) throw new RuntimeException("child process retuned "+er);
		} catch (InterruptedException ex) {
			Util.log("Util", "Not a clean exit: "+ex);
		}
	}

	public static void popenStdin(String[] args, InputStream feed) throws IOException {
		ProcessBuilder pb = new ProcessBuilder(args);
		try {
			Process p = pb.start();
			try (OutputStream stdin = p.getOutputStream()) {
				byte bb[] = new byte[1024];
				int rd;
				while( (rd = feed.read(bb)) != -1) {
					stdin.write(bb, 0, rd);
				}
				feed.close();
			}
			int er = p.waitFor();
			if(er!= 0) throw new RuntimeException("child process retuned "+er);
		} catch(InterruptedException ex) {
			Util.log("Util", "Not a clean exit: "+ex);
		}
	}

	/*
	public static InputStream popenAsStream(String ... args) {
		ProcessBuilder pb = new ProcessBuilder(args);
		try {
			Process p = pb.start();
			int er = p.waitFor();
			if(er!= 0) throw new RuntimeException("child process retuned "+er);
		}catch(IOException e) {
			Util.log("Util", "Could not exec: "+e);
		} catch (InterruptedException ex) {
			Util.log("Util", "Could not exec: "+ex);
		}
	}*/
}
