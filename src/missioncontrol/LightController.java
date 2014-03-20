/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import missioncontrol.pipeline.EventListener;
import java.io.IOException;
import java.util.Calendar;
import java.util.Scanner;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventPipeline;

/**
 *
 * @author positron
 */
public class LightController implements EventListener {

	private MissionControl engine;
	private EventPipeline pipeline;
	private static String lightControlExecutable = null;
	private enum State { ON, OFF }
	private State state;
	public enum Daytime { NOT_SET, LIGHT_TIME, DARK_TIME}
	private Daytime daytime = Daytime.NOT_SET;
	private boolean autoEnabled = true;

	/**
	 * if true, do not turn off lights automatically
	 */
	private boolean lastOpManual = false;

	private int cPeople = 0;
	private final String SETTING_PEOPLE = "peoplecounter.count";


	public LightController(MissionControl engine, EventPipeline pp) {
		this.engine = engine;
		this.pipeline = pp;
		pipeline.registerListener(this, Event.EVENT_PEOPLE_COUNTER);
		pipeline.registerListener(this, Event.EVENT_USER_LIGHT);
		pipeline.registerListener(this, Event.EVENT_TIME);

		lightControlExecutable = System.getProperty("lightcontrol.file","lightcontrol");

		cPeople = Integer.parseInt(engine.currentState.getProperty(SETTING_PEOPLE, "0") );
		Util.log(this, cPeople+" people upon creation");

		syncState();
	}

	public void turnOn(boolean manual, String msg) {
		if(!manual && !autoEnabled) {
			Util.log(this, "autocontrol disabled");
			return;
		}
		if(lastOpManual && !manual) {
			Util.log(this, "not turning lights on because they were turned off manually");
			return;
		}
		if(state == State.ON) return;
		if(daytime == Daytime.LIGHT_TIME && !manual) {
			Util.log(this, "not turning lights on due to daytime");
			return;
		}
		lastOpManual = manual;
		lightControl(true, (manual?"manual ":"")+msg);
	}

	public void turnOn(String msg)  {
		turnOn(true, msg);
	}

	public void turnOff(String msg) {
		turnOff(true, msg);
	}

	public void turnOff(boolean manual, String msg) {
		if(!manual && !autoEnabled) {
			Util.log(this, "autocontrol disabled");
			return;
		}
		if(state == State.OFF) return;
		//if(lastOpManual && !manual) return;
		lastOpManual = manual;
		lightControl(false, (manual?"manual ":"")+msg);
	}

	public void flip(boolean manual, String msg) {
		if(state==State.ON) turnOff(manual, msg); else turnOn(manual, msg);
	}


	public void setAutoControlEnabled(boolean enabled) {
		this.autoEnabled = enabled;
		Util.log(this, "Auto control enabled: "+enabled);
	}

	public void updateDaytime(Calendar c) {
		//System.out.println("LightController.updateTime("+c.getTime()+")");
		int hours =  c.get(Calendar.HOUR_OF_DAY) ;
		if(daytime==Daytime.NOT_SET) {
			if(hours < 19 && hours>7) daytime = Daytime.LIGHT_TIME;
			else daytime = Daytime.DARK_TIME;
			Util.log(this, "daytime is now "+daytime);
		} else
		if(daytime!=Daytime.DARK_TIME && hours == 19) {
			daytime = Daytime.DARK_TIME;
			Util.log(this, "daytime is now "+daytime);
			if(cPeople>0) turnOn(false, "night time came, people inside");
		} else
		if(daytime!=Daytime.LIGHT_TIME && hours == 7) {
			daytime = Daytime.LIGHT_TIME;
			Util.log(this, "daytime is now "+daytime);
			if(cPeople==0) turnOff(false, "morning time came, zero people");
		}
	}

	public void incPeople(int inc) {
		setPeople(cPeople + inc);
	}
	public void setPeople(int ppl) {
		if(ppl<0) ppl=0;
		Util.log(this, "setPeople("+ppl+")");
		int lastPeople = cPeople;
		cPeople = ppl;

		if(lastPeople==0 && cPeople!=0)
			turnOn(false, "people counter="+cPeople);
		if(lastPeople>0 && cPeople == 0)
			turnOff(false, "people counter zero");
	}

	public boolean getState() { return state==State.ON; }

	public void syncState() {
		try {
			Process p = new ProcessBuilder(lightControlExecutable, "-r").start();
			state = new Scanner(p.getInputStream()).nextInt() == 1 ? State.ON : State.OFF;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void lightControl(boolean on, String msg) {
		Util.log(this, "lightControl in action: "+(on?"ON":"OFF")+", message: "+msg);
		try {
			Process p = new ProcessBuilder(lightControlExecutable, "-v"+(on?"1":"0"), "-m", msg).start();
			state = on ? State.ON : State.OFF;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void terminate() {
		engine.currentState.setProperty(SETTING_PEOPLE, ""+cPeople);
	}

	@Override
	public void processEvent(Event e) {
		if(e instanceof Event.PeopleCounterEvent) {
			incPeople(cPeople);
		} else
		if(e instanceof Event.LightEvent) {
			Event.LightEvent le = (Event.LightEvent)e;
			switch( le.state ) {
				case ON: turnOn( le.manual, e.getMessage() ); break;
				case OFF: turnOff( le.manual, le.getMessage() ); break;
				case SWITCH: flip(le.manual, le.getMessage() ); break;
			}
		} else
		if(e.type == Event.EVENT_TIME) {
			updateDaytime( (Calendar)e.data );
		}

	}



}
