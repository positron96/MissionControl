/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Locale;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventListener;
import missioncontrol.pipeline.EventPipeline;
import missioncontrol.pipeline.Terminatable;

/**
 *
 * @author positron
 */
public class SpeechGenerator implements EventListener, Terminatable {

	private MissionControl engine;
	private boolean muted = false;
	private boolean verbose = false;

	public static final String SETTING_VERBOSE = "speech.verbose";

	public static final String EVENT_SPEAK = "speech.*";

	public SpeechGenerator(MissionControl engine, EventPipeline pipeline) {
		this.engine = engine;
		pipeline.registerListener(this, EVENT_SPEAK);
		verbose = Boolean.parseBoolean( engine.currentState.getProperty(SETTING_VERBOSE, "false") );
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
		if(verbose) {
			Util.log(this, "speak: "+s);
		}
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
		if(EVENT_SPEAK.equals(e.type)) {
			String subtype = e.subType;
			if(subtype == null) subtype="speak";
			switch(subtype.toUpperCase()) {
				case "VERBOSE":
					if(((String)e.data).length()==0) verbose = true;
					else verbose = e.data.equals("1");
					break;
				case "MUTE":
					mute();
					break;
				case "UNMUTE":
					unmute();
					break;
				case "DO":
				case "SPEAK":
					speak((String)e.data);
					break;
				default:
					speak(e.subType+" "+(String)e.data);
					break;
			}


		}
	}

	@Override
	public void terminate() {
		engine.currentState.setProperty(SETTING_VERBOSE, Boolean.toString(verbose));
	}

}
