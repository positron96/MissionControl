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
import java.util.Properties;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class MissionControl {

	EventPipeline pipeline;

	private LightController lightController;

	private SpeechGenerator speech;

	final SerialInput spp;
	final PipeInput pin;

	Properties currentState = new Properties();
	private static final String STATE_FILE = "missionstate.properties";
	private static final String SETTINGS_FILE = "missioncontrol.properties";

	public MissionControl() {
		Runtime.getRuntime().addShutdownHook ( shutdownHook );

		try {
			Properties pp = new Properties();
			pp.load( new FileReader( SETTINGS_FILE ) );
			pp.putAll(System.getProperties());
			System.getProperties().putAll(pp);
		} catch(IOException e) {
			Util.log(this, "Could not load settings: "+e);
		}

		try {
			currentState.load( new FileReader(STATE_FILE) );
		} catch(IOException e) {
			Util.log(this, "Could not load state: "+e);
		}

		pipeline = new EventPipeline();

		spp = new SerialInput(this);
		pin = new PipeInput(this);
		pipeline.registerSource( pin );
		pipeline.registerSource( spp );
		pipeline.registerSource( new TimeEventSource(this) );
		pipeline.registerSource( new IRControlLauncher(this) );

		speech = new SpeechGenerator(this, pipeline);
		speech.speak("Hello");


		lightController = new LightController(this, pipeline);
	}

	public void speek(String s) {
		pipeline.pumpEvent( Event.createWithData(SpeechGenerator.EVENT_SPEAK, s, null));
	}

	public void work() {
		pipeline.start();

		Thread console  = new Thread("") {
			@Override
			public void run() {

				BufferedReader rd = new BufferedReader(new InputStreamReader(System.in) );
				while(true) {
					try {
						System.out.print(">");
						String s = rd.readLine();
						if(s==null) {
							Util.log(this, "stdin closed, not listening");
							break;
						}
						if(s.length()==0) {continue;}
						try {
							pin.insertMessage(s);
						} catch(RuntimeException e) {
							System.err.println(""+e);
						}
						/*s = s.trim();

						if(s.startsWith("send ")) {
							String cmd = s.substring(5);
							spp.sendCommand( (byte)'a', cmd.getBytes() );
						} else
						if(s.equals("q")) {
							Util.log(this, "quit command received");
							pipeline.pumpEvent(Event.SHUTDOWN_EVENT);
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
						}*/
					} catch (IOException ex) {
						ex.printStackTrace();
						break;
					}
				}
			}
		};
		console.setDaemon(true);
		console.start();
	}

	private final Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			Util.log(MissionControl.this, "Shutdown hook in action");
			if( pipeline.isAlive() ) pipeline.terminate();
			lightController.terminate();
			try {
				Util.log(MissionControl.this, "Saving state");
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
