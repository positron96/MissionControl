/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package missioncontrol;

import missioncontrol.pipeline.EventSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.StringTokenizer;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class PipeInput extends Thread implements EventSource {

	private final MissionControl engine;
	private EventPipeline sink;
	//private SerialPort spp;
	private File file;
	private FileChannel fin = null;

	public PipeInput(MissionControl engine) {
		super("PipeInput Thread");
		this.engine = engine;
	}

	@Override
	public void setEventPipeline(EventPipeline ss) {
		sink = ss;
	}


	private void open() {
		try {
			file = new File(System.getProperty("pipe.file", "missioninput"));
			Util.log(this,"open(): pipe file is " + file.getAbsolutePath());
			if (!file.exists()) {
				try {
					// mkpipe
					Process p = Runtime.getRuntime().exec(new String[]{"mkfifo", file.getAbsolutePath()});
					p.waitFor();
					Util.log(this, "open(): created pipe");
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			} else {
				Util.log(this, "open(): pipe already created");
			}
		} catch (Exception e) {
			e.printStackTrace();
			//spp=null;
		}
	}

	@Override
	public void run() {
		open();
		try {
			while (!isInterrupted()) {
				//fin = new FileInputStream(file).getChannel();
				BufferedReader in = new BufferedReader( new FileReader(file) );//new InputStreamReader( Channels.newInputStream(fin) )  );
				try {
					while (in.ready() && !isInterrupted()) {
						processMessage(in.readLine());
					}
					in.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			Util.log(this,"run(): graceful exit");
		} catch (FileNotFoundException e) {
			Util.log(this, "pipe not found, quitting");
		}
		synchronized (this) {
			this.notifyAll();
		}
	}


	@Override
	public void terminate() {
		this.interrupt();
		try {
			FileWriter f = new FileWriter(file);
			f.write("PIPE. term");
			f.close();
			file.delete();
		} catch(IOException e) {

		}
		/*if(fin!=null) try {
			fin.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}*/
		Util.log(this, "terminate()");
	}

	private void processMessage(String msg) {
		//System.out.println("msg is "+msg);
		msg = msg.trim();
		Util.log(this, "got message: "+msg);
		if(msg.toLowerCase().equals("q")) {
			sink.pumpEvent(Event.SHUTDOWN_EVENT);
			return;
		}
		StringTokenizer st= new StringTokenizer(msg, ".");
		String src = st.nextToken();
		String arg = st.hasMoreElements() ? st.nextToken() : "";
		switch (src) {
			case "IR" :
				java.util.Scanner sc = new Scanner(arg);
				int device = sc.nextInt();
				int cmd = sc.nextInt();
				String comment = sc.nextLine();
				if(device==0 && (cmd==9 || cmd==21)) {
					//engine.lightController.incPeople(+1);
					sink.pumpEvent( Event.PeopleCounterEvent.createPeopleIncrement(+1, "IR command", this));
				} else
				if(device==0 && (cmd==13 || cmd==7)) {
					sink.pumpEvent( Event.PeopleCounterEvent.createPeopleIncrement(-1, "IR Command", this));
					//engine.lightController.incPeople(-1);
				} else {
					sink.pumpEvent( new Event.LightEvent(
							Event.LightEvent.State.SWITCH,
							String.format("IR from %d cmd %d %s", device, cmd, comment),
							this) );
					//engine.lightController.flip(true, );
				}
				break;
			case "LIGHT":
				sink.pumpEvent( new Event(LightController.EVENT_PRINT_STATUS, this) );
				break;
			case "PEOPLE":

				break;
			case "PIPE":
				if(arg.toUpperCase().equals("TERM")) {}
				break;
			default:
				throw new RuntimeException("Unknown source received: "+src);
		}

	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+hashCode();

	}

}
