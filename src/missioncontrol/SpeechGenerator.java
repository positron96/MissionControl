/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

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


	public SpeechGenerator(MissionControl engine) {
		this.engine = engine;
	}

	public synchronized void speak(String s) {
		speak(s, Locale.getDefault().getLanguage());
	}

	public synchronized void speak(String s, String lang) {
		playStream( callGoogle(s) );
	}

	private void playStream(InputStream is) {
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
		}catch(IOException e) {
			//e.printStackTrace();
			Util.log(this, "Could not speak: "+e);
		} catch (InterruptedException ex) {
			//ex.printStackTrace();
		}

	}

	private InputStream callGoogle(String speech) {
		try {
			URL url = new URL("http://translate.google.com/translate_tts?tl=en&q="+
					URLEncoder.encode(speech));
			URLConnection conn = url.openConnection();
			conn.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.107 Safari/537.36");
			return conn.getInputStream();

		}catch(IOException e) {
			Util.log(this, "Could not reach google: "+e);
		}
		return null;
	}

}
