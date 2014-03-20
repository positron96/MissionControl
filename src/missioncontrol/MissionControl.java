/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author positron
 */
public class MissionControl {

	List<EventSource> eventSources = new LinkedList<> ();

	LightController lightController;

	SpeechGenerator speech;

	SerialInput spp;

	Properties currentState = new Properties();
	private static final String STATE_FILE = "missionstate.properties";
	private static final String SETTINGS_FILE = "missioncontrol.properties";

	public MissionControl() {
		Runtime.getRuntime().addShutdownHook ( shutdownHook );

		speech = new SpeechGenerator(this);
		Util.speech = speech;
		speech.speak("Hello");
		spp = new SerialInput(this);
		eventSources.add( spp );
		eventSources.add( new PipeInput(this) );
		eventSources.add( new TimeEventSource(this) );
		lightController = new LightController(this);
		try {
			//System.getProperties().
			Properties pp = new Properties();
			pp.load( new FileReader( SETTINGS_FILE ) );
			System.getProperties().putAll(pp);
		} catch(IOException e) {
			Util.log(this, "Could not load settings: "+e);
		}

		try {
			currentState.load( new FileReader(STATE_FILE) );
		} catch(IOException e) {
			Util.log(this, "Could not load state: "+e);
		}
	}

	public void work() {
		for(EventSource es : eventSources) es.start();

		BufferedReader rd = new BufferedReader(new InputStreamReader(System.in) );
		while(true) {
			try {
				String s = rd.readLine().trim();
				if(s.startsWith("send ")) {
					String cmd = s.substring(5);
					spp.sendCommand( (byte)'a', cmd.getBytes() );
				} else
				if(s.equals("q")) {
					Util.log(this, "quit command received");
					System.exit(0);
					break;
				} else
				if(s.equals("light")) {
					Util.log(this, "Light state = "+lightController.getState() );
				} else
				if(s.startsWith("auto ")) {
					//Util.log(this, "Light state = "+lightController.getState() );
					switch(s.substring(6) ) {
						case "off": lightController.setAutoControlEnabled(false);
							break;
						case "on":lightController.setAutoControlEnabled( true);
							break;
					}
				} else
				if(s.startsWith("speak ")) {
					speech.speak(s.substring(6));
				} else
				if(s.startsWith("people ")) {
					String n = s.substring(7);
					lightController.setPeople( Integer.parseInt(n));
				} else {
					Util.log(this, "unknown command: "+s);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				break;
			}
		}
	}



	private final Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			Util.log(MissionControl.this, "Shutdown hook in action");
			for(EventSource es : eventSources) {
				es.terminate();
				if(es instanceof Thread)
					try {
						synchronized (es) {
							es.wait(500);
						}
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
			}
			lightController.terminate();
			try {
				currentState.store(new FileWriter(STATE_FILE), "Saved at "+new Date() );
			} catch (IOException ex) {
				Util.log(this, "Could not save state: "+ex);
			}
		}
	};

	public static void main(String[] args) {
		new MissionControl().work();

	}

}
