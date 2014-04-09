/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template pipeFile, choose Tools | Templates
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
	public final File pipeFile;
	private FileChannel fin = null;

	public PipeInput(MissionControl engine) {
		super("PipeInput Thread");
		this.engine = engine;
		pipeFile = new File(System.getProperty("pipe.file", "missioninput"));
	}

	@Override
	public void setEventPipeline(EventPipeline ss) {
		sink = ss;
	}


	private void open() {
		try {
			Util.log(this,"open(): pipe file is " + pipeFile.getCanonicalPath() );
			if (!pipeFile.exists()) {
				try {
					// mkpipe
					Process p = Runtime.getRuntime().exec(new String[]{"mkfifo", pipeFile.getAbsolutePath()});
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
				//fin = new FileInputStream(pipeFile).getChannel();
				//Util.log(this, System.currentTimeMillis()+ "ms: pipe opening");
				BufferedReader in = new BufferedReader( new FileReader(pipeFile) );
				try {
					while (!isInterrupted()) {
						String s = in.readLine();
						if(s==null) {/*Util.log(this, System.currentTimeMillis()+ "ms: read null");*/break;}
						//Util.log(this, System.currentTimeMillis()+ "ms: got "+s.length()+" bytes");
						processMessage(s);
					}
					in.close();
					//Util.log(this, System.currentTimeMillis()+ "ms: pipe closed");
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			Util.log(this, "pipe not found, quitting");
		}
		Util.log(this,"run(): quitting");
		if(pipeFile.exists() && pipeFile.delete() ) {}
		else Util.log(this, "could not delete pipe");

		synchronized (this) {
			this.notifyAll();
		}
	}


	@Override
	public void terminate() {
		this.interrupt();
		if(pipeFile.exists())
			try {
				FileWriter f = new FileWriter(pipeFile);
				f.write("PIPE. term");
				f.close();
			} catch(IOException e) {
				//Util.log(this, "could not write close command to pipe: "+e);
			}
		else {Util.log(this, "WTF?? Pipe is deleted");}
		/*if(fin!=null) try {
			fin.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}*/
		Util.log(this, "terminate()");
	}

	public void insertMessage(String msg) {
		processMessage(msg);
	}

	private synchronized void processMessage(String msg) {
		//msg = msg.trim();
		//Util.log(this, "got message: "+msg);
		int p, p1;
		p = msg.indexOf(".");
		p1 = msg.indexOf(" ");
		if(p1==-1 && p==-1) p=-1; else
		if(p==-1) p=p1; else
		if(p1==-1) p=p; else
			p = Math.min(p,p1);

		String src = p!=-1 ? msg.substring(0,p) : msg;
		src = src.toUpperCase().trim();
		String arg = p!=-1 ? msg.substring(p+1).trim() : "";
		p = arg.indexOf(' ');
		String arg1 = p!=-1 ? arg.substring(0, p).trim() : arg;
		String arg2 = p!=-1 ? arg.substring(p).trim() : "";
		Util.log(this, "got message: "+src+" "+arg);
		switch (src) {
			case "Q":
			case "QUIT":
			case "EXIT":
				sink.pumpEvent(Event.SHUTDOWN_EVENT);
				break;
			case "IR" :
				java.util.Scanner sc = new Scanner(arg);
				int device = sc.nextInt();
				int cmd = sc.nextInt();
				String comment = sc.nextLine();
				if(device==0 && (cmd==9 || cmd==21)) {
					sink.pumpEvent( Event.PeopleCounterEvent.createPeopleIncrement(+1, "IR command", this));
				} else
				if(device==0 && (cmd==13 || cmd==7)) {
					sink.pumpEvent( Event.PeopleCounterEvent.createPeopleIncrement(-1, "IR Command", this));
				} else {
					sink.pumpEvent( new Event.LightEvent(
							Event.LightEvent.State.SWITCH,
							String.format("IR from %d cmd %d %s", device, cmd, comment),
							this) );
					//engine.lightController.flip(true, );
				}
				break;
			case "SPEAK":
				sink.pumpEvent( new Event(SpeechGenerator.EVENT_SPEAK, "SPEAK", arg, this) );
				break;
			case "SPEECH":
				sink.pumpEvent( new Event(SpeechGenerator.EVENT_SPEAK, arg1, arg2, this) );
				break;
			case "LIGHT":
				sink.pumpEvent( new Event(LightController.EVENT_PRINT_STATUS, this) );
				break;
			case "PEOPLE":
				sink.pumpEvent( Event.PeopleCounterEvent.createPeopleCount(Integer.parseInt(arg), "", this));
				break;
			case "SEND":
				try {
					engine.spp.sendCommand((byte)arg1.charAt(0), arg2.getBytes() );
				}catch(IOException e) {
					e.printStackTrace();
				}
				break;
			case "SENDRAW":
				try {
					engine.spp.sendRaw(arg);
				}catch(IOException e) {
					e.printStackTrace();
				}
				break;
			case "PIPE":
				if(arg.toUpperCase().equals("TERM")) {}
				break;
			default:
				Util.log(this, "processCommand: Unknown source received: "+src);
				//throw new RuntimeException("Unknown source received: "+src);
		}

	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+hashCode();

	}

}
