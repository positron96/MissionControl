/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Locale;

/**
 *
 * @author positron
 */
public class SpeechGenerator {

	private MissionControl engine;
	private boolean muted = false;

	public SpeechGenerator(MissionControl engine) {
		this.engine = engine;
	}

	public synchronized void mute() {
		muted = true;
	}

	public synchronized void unmute() {
		muted = false;
	}

	public synchronized void speak(String s) {
		speak(s, Locale.getDefault().getLanguage());
	}

	public synchronized void speak(String s, String lang) {
		if(!muted) {
			try {
				playStream( callGoogle(s) );
			} catch(IOException ex) {
				playFile( callPico(s) );
			}
		}
	}

	private void playStream(InputStream is) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("mpg123", "-q", "-");
		try {
			Process p = pb.start();
			try (OutputStream stdin = p.getOutputStream()) {
				byte bb[] = new byte[1024];
				int rd;
				while( (rd = is.read(bb)) != -1) {
					stdin.write(bb, 0, rd);
				}
				is.close();
			}
			int er = p.waitFor();
			if(er!= 0) throw new RuntimeException("mpg123 returned error "+er);
		} catch (InterruptedException ex) {
			//ex.printStackTrace();
		}
	}

	private void playFile(File ff) {
		ProcessBuilder pb = new ProcessBuilder("mpg123", "-q", ff.getAbsolutePath());
		try {
			Process p = pb.start();
			int er = p.waitFor();
			if(er!= 0) throw new RuntimeException("mpg123 returned error "+er);
		}catch(IOException e) {
			Util.log(this, "Could not speak: "+e);
		} catch (InterruptedException ex) {
		}
	}

	private InputStream callGoogle(String speech) throws IOException {
		try {
			URL url = new URL("http://translate.google.com/translate_tts?tl=en&q="+
					URLEncoder.encode(speech));
			URLConnection conn = url.openConnection();
			conn.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.107 Safari/537.36");
			return conn.getInputStream();
		}catch(IOException e) {
			Util.log(this, "Could not reach google: "+e);
			throw e;
		}
	}

	private File callPico(String speech) {
		try {
			File ff = File.createTempFile("speech", ".wav");
			ProcessBuilder pb = new ProcessBuilder("pico2wave", "-w", ff.getAbsolutePath(), speech);
			pb.start().waitFor();
			return ff;
		}catch(IOException e) {
			Util.log(this, "callPico:"+e);
		} catch (InterruptedException ex) {
			//Util.log(this, "Interrupted: "+ex);
		}
		return null;
	}

}
