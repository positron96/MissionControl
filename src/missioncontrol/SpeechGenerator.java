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
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventListener;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class SpeechGenerator implements EventListener {

	private MissionControl engine;
	private boolean muted = false;

	public static final String EVENT_SPEAK = "speech.*";

	public SpeechGenerator(MissionControl engine, EventPipeline pipeline) {
		this.engine = engine;
		pipeline.registerListener(this, EVENT_SPEAK);
		Util.speech = this;
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
				InputStream in = null;
				try {
					in = callGoogle(s);
				}catch(IOException e) {
					Util.log(this, "speak by google: "+e);
					playFile( callPico(s) );
				}
				playStream( in );
			} catch(IOException ex) {
				Util.log(this, "speak: "+ex);
			}
		}
	}

	private void playStream(InputStream is) throws IOException {
		Util.popenStdin(new String[]{"mpg123", "-q", "-"}, is);
	}

	private void playFile(File ff) {
		//if(ff==null) Util.log();, EVENT_SPEAK);
		try {
			Util.popen("mpg123", "-q", ff.getAbsolutePath());
		} catch(IOException e) {
			Util.log(this, "Could not speak: "+e);
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

	private File callPico(String speech) throws IOException {
		//try {
			File ff = File.createTempFile("speech", ".wav");
			Util.popen("pico2wave", "-w", ff.getAbsolutePath(), speech);
			return ff;
		/*catch(IOException e) {
			Util.log(this, "callPico: "+e);
		}
		return null;*/
	}

	@Override
	public void processEvent(Event e) {
		if(e.type == EVENT_SPEAK) {
			switch(e.subType) {
				case "MUTE":
					mute();
					break;
				case "UNMUTE":
					unmute();
					break;
				case "DO":
				case "SPEEK":
					speak((String)e.data);
					break;
				default:
					speak(e.subType);
					break;
			}


		}
	}

}
