/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.io.File;
import java.io.IOException;
import missioncontrol.pipeline.EventPipeline;
import missioncontrol.pipeline.EventSource;

/**
 *
 * @author positron
 */
public class IRControlLauncher implements EventSource {

	private static final String SETTING_FILE = "ircontrol.file";
	private final File irrecvFile;
	private Process process;
	private Thread waiter;
	private MissionControl engine;

	public IRControlLauncher(MissionControl engine) {
		irrecvFile = new File(System.getProperty(SETTING_FILE, "irrecv"));
		this.engine = engine;
	}

	@Override
	public void start() {
		String pids = Util.popenAsString( new String[]{"pgrep", irrecvFile.getName()}, true );
		if(pids!=null && pids.length() != 0) {
			Util.log(this, "irrecv seems to be running already, not starting");
			process=null;
			return;
		}
		try {
			ProcessBuilder pb=new ProcessBuilder(
					irrecvFile.getCanonicalPath(),
					engine.pin.pipeFile.getCanonicalPath());
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			process = pb.start();
			waiter = new Thread("irrecv waiter thread") {
				@Override
				public void run() {
					try {
						int res = process.waitFor();
						Util.log(IRControlLauncher.this, "irrecv exitted with status "+res);
					} catch(InterruptedException e) {
						Util.log(IRControlLauncher.this, "irrecv waiter interrupted");
					}
				}
			};
			waiter.setDaemon(true);
			waiter.start();
			Util.log(this, "irrecv started successfully");
		} catch (IOException ex) {
			//ex.printStackTrace();
			Util.log(this, "start: problem starting irrecv: "+ex);
		}
	}

	@Override
	public void terminate() {
		if(process!=null) {
			Util.log(this, "Terminating irrecv");
			process.destroy();
		}
		if(waiter!=null)
			waiter.interrupt();
	}

	@Override
	public void setEventPipeline(EventPipeline ss) {

	}

}
