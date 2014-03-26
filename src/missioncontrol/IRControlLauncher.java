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
	private final File ff;
	private Process process;

	public IRControlLauncher() {
		ff = new File(System.getProperty(SETTING_FILE, "irrecv"));
	}

	@Override
	public void start() {
		try {
			ProcessBuilder pb=new ProcessBuilder(ff.getCanonicalPath());
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			process = pb.start();
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
	}

	@Override
	public void setEventPipeline(EventPipeline ss) {

	}

}
