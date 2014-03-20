/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package missioncontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author positron
 */
public class PipeInput extends Thread implements EventSource {

	private MissionControl engine;
	//private SerialPort spp;
	private File file;
	private FileChannel fin = null;

	public PipeInput(MissionControl engine) {
		this.engine = engine;
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
			Util.log(this,"graceful exit");
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
			f.write("SELF. term");
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
		Util.log(this, "got message: "+msg);
		StringTokenizer st= new StringTokenizer(msg, ".");
		String src = st.nextToken();
		String arg = st.nextToken();
		switch (src) {
			case "IR" :
				java.util.Scanner sc = new Scanner(arg);
				int device = sc.nextInt();
				int cmd = sc.nextInt();
				String comment = sc.nextLine();
				if(device==0 && (cmd==9 || cmd==21)) {
					engine.lightController.incPeople(+1);
				} else
				if(device==0 && (cmd==13 || cmd==7)) {
					engine.lightController.incPeople(-1);
				} else {
					engine.lightController.flip(true, String.format("IR from %d cmd %d %s", device, cmd, comment));
				}
				break;
			case "SELF":

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
