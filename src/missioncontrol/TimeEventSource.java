/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import missioncontrol.pipeline.EventSource;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class TimeEventSource implements EventSource {
	private MissionControl engine;
	private EventPipeline sink;
	private static final int INTERVAL_MIN = 5;

	private Timer tm = null;
	private Calendar cl = Calendar.getInstance();

	public TimeEventSource(MissionControl engine) {
		this.engine = engine;
	}

	@Override
	public void start() {
		if(tm==null) {
			TimeEvent ev = new TimeEvent();
			ev.run();
			cl.set(Calendar.MINUTE, (cl.get(Calendar.MINUTE)/5+1)*5 );
			cl.set(Calendar.SECOND, 0);
			cl.set(Calendar.MILLISECOND, 0);
			tm = new Timer("TimeEventSource timer");
			tm.scheduleAtFixedRate(ev, cl.getTime(), 1000*60*INTERVAL_MIN);
		}
	}

	@Override
	public void terminate() {
		tm.cancel();
		tm = null;
	}

	@Override
	public void setEventPipeline(EventPipeline ss) {
		sink = ss;
	}

	private class TimeEvent extends TimerTask {
		private boolean first = true;

		@Override
		public void run() {
			cl.setTime(new Date());
			//Util.log("TimeEventSource.run", "fired at "+cl.getTime() );
			int mins = cl.get(Calendar.MINUTE);
			if(first || (mins >= 0 && mins<INTERVAL_MIN)) {
				sink.pumpEvent( new Event(Event.EVENT_TIME, "time update", cl, TimeEventSource.this));
				//engine.lightController.updateDaytime(cl);
			}
			first = false;
		}

	}


}
