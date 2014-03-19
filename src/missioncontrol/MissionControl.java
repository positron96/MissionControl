/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import missioncontrol.pipeline.EventSource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class MissionControl {

	EventPipeline pipeline;

	private LightController lightController;

	private SpeechGenerator speech;

	SerialInput spp;

	Properties currentState = new Properties();
	private static final String STATE_FILE = "missionstate.properties";

	public MissionControl() {
		Runtime.getRuntime().addShutdownHook ( shutdownHook );

		try {
			currentState.load( new FileReader(STATE_FILE) );
		} catch(IOException e) {
			Util.log(this, "Could not load state: "+e);
		}

		pipeline = new EventPipeline();

		pipeline.registerSource( new PipeInput(this) );
		pipeline.registerSource( new SerialInput(this) );
		pipeline.registerSource(new TimeEventSource(this) );

		speech = new SpeechGenerator(this);
		Util.speech = speech;
		speech.speak("Hello");
		spp = new SerialInput(this);

		lightController = new LightController(this, pipeline);
	}

	public void work() {
		pipeline.start();

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
			pipeline.terminate();
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
