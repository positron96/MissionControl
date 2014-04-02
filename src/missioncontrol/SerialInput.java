/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import missioncontrol.pipeline.EventSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class SerialInput extends Thread implements EventSource {

	private EventPipeline sink;
	private Socket sock;
	private OutputStream out;
	private InputStream in;

	private MissionControl engine;

	private long lastChange;

	private long lastSockOpen=0;

	private static final int GIVEUP_INTL = 2000;

	public SerialInput(MissionControl engine) {
		super("SerialInput Thread");
		this.engine = engine;
		lastSockOpen = System.currentTimeMillis() - GIVEUP_INTL*5;
	}

	private static final int PACK_DAT = 'D';
	private static final int PACK_ACK = 'A';
	private static final int PACK_DISCOVER = 'H';

	private byte calcCheckSum(int cmd, int sender, int len, byte dat[]) {
		int res = cmd ^ sender ^ len;
		for(int i=0; i<dat.length; i++) res = res ^ dat[i];
		return (byte)res;
	}

	@Override
	public void run() {
		open();
		try {
			sendRaw("H");
		}catch(IOException e) {
			Util.log(this, "DISCOVER sending failed: "+e);
		}
		while( !isInterrupted() ) {
			try {
				int cmd = in.read();
				if(cmd==-1) {
					Util.log(this, "EOF, Trying to reconnect");
					in.close();
					open();
				}
				//System.out.println("cmd = "+cmd);
				//Util.log(this, "got cmd = "+cmd);
				switch(cmd) {
					case PACK_DAT: // data received
						int sender = in.read();
						//System.out.println("sender = "+sender);
						int len = in.read();
						//System.out.println("len = "+len);
						byte[] dat = new byte[len - '0'];
						in.read(dat);
						byte checksum = (byte)in.read();
						byte achecksum = calcCheckSum(cmd, sender, len, dat);
						if(checksum == achecksum) {
							//System.out.println("Checksum ok");
							out.write(PACK_ACK);
							out.write(sender);
							//System.out.println("Got data from "+(char)sender+": "+Arrays.toString(dat));
							try {
								processCommand(sender, dat);
							} catch(Exception e) {
								Util.log(this, "processCommand failed with "+e);
							}
						} else {
							Util.log(this, "run(): Checksum mismatch: got "+checksum+"; calc "+achecksum);
						}

						break;
				}
			} catch(IOException e) {
				if(!interrupted()) e.printStackTrace();
				break;
			} catch(Exception e) {
				Util.log(this, "Got exception: "+e+" at "+e.getStackTrace()[0] );
			}
		}

		Util.log(this, "run(): closing & quitting");
		if(sock!=null && sock.isConnected()) try {
			sock.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
		synchronized (this) {
			this.notifyAll();
		}
	}

	private void processCommand(int sender, byte[] data) {
		switch(data[0]) {
			case 'H':
				Util.log(this, "Discover from "+(char)sender);
				break;
			case 'p':
				Util.log(this, "Recieved "+data[0]+" people, not supported anymore");
				//engine.lightController.setPeople(data[1]);
				//sink.pumpEvent( Event.PeopleCounterEvent.createPeopleCount(data[1], "by wire", this));
				break;
			case 'P':
				Util.log(this, "Recieved people counter "+(char)data[1]);
				if(System.currentTimeMillis() - lastChange < 1000) {
					Util.log(this,"Not processing counter due to high freq event");
					lastChange = System.currentTimeMillis();
					return;
				}
				if(data[1]=='+') //engine.lightController.incPeople(+1);
					sink.pumpEvent( Event.PeopleCounterEvent.createPeopleIncrement(+1, "sensor", this));
				else if(data[1]=='-')
					sink.pumpEvent( Event.PeopleCounterEvent.createPeopleIncrement(-1, "sensor", this));
					//engine.lightController.incPeople(-1);
				lastChange = System.currentTimeMillis();
				break;
			default:
				Util.log(this, "got data from "+(char)sender+":" +new String(data) );
		}
	}


	private void open() {
		if(System.currentTimeMillis() < lastSockOpen+GIVEUP_INTL) {
			Util.log(this, "Too frequent socket closing, ser2net probably not working");
			sock = null;
		} else
			try {
				lastSockOpen = System.currentTimeMillis();
				sock = new Socket();
				sock.connect(new InetSocketAddress("127.0.0.1", 2323));
				in = sock.getInputStream();
				out = sock.getOutputStream();
				Util.log(this, "open(): spp opened");
			} catch (IOException e) {
				//e.printStackTrace();
				Util.log(this, "open ser2net socket failed: "+e);
				sock = null;
			}
		if(sock==null) {
			Util.log(this, "run(): spp did not open correctly, quitting thread");
			terminate();
		}
	}

	@Override
	public void terminate()   {
		//if(sock!=null) {
		if(isAlive() && !isInterrupted()) {
			interrupt();
			if(sock!=null) try {
				sock.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}

		}
	}

	synchronized void sendRaw(String data) throws IOException {
		Util.log(this, "sendRaw: sending "+data);
		out.write(data.getBytes());
	}
	synchronized void sendRaw(byte[] data) throws IOException {
		Util.log(this, "sendRaw: sending "+Arrays.toString(data));
		out.write(data);
	}
	synchronized void sendCommand(byte target, byte[] data) throws IOException {
		Util.log(this, "sendCommand: sending command "+Arrays.toString(data)+" to "+target);
		out.write(PACK_DAT);
		out.write(target);
		out.write(data.length + '0');
		out.write(data);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+hashCode();

	}

	@Override
	public void setEventPipeline(EventPipeline ss) {
		sink = ss;
	}


}
