/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol.pipeline;

/**
 *
 * @author positron
 */
public class Event {

	public static final String EVENT_PEOPLE_COUNTER = "sensor.people";
	public static final String EVENT_USER_LIGHT = "user.light";
	public static final String EVENT_TIME = "system.time";

	public final String type;
	public final String subType;
	public final Object data;

	private String message;

	private final EventSource source;

	public Event(String type, EventSource src) {
		this.type = type;
		this.subType = null;
		this.data = null;
		this.source = src;
	}
	public Event(String type, String subtype, EventSource src) {
		this.type = type;
		this.subType = subtype;
		this.source = src;
		this.data = null;
	}
	public Event(String type, String subtype, Object data, EventSource src) {
		this.type = type;
		this.subType = subtype;
		this.source = src;
		this.data = data;
	}

	public void setMessage(String msg) {
		message = msg;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public static final String SHUTDOWN_EVENT_TYPE = "system.shutdown";
	public static final Event SHUTDOWN_EVENT = new Event(SHUTDOWN_EVENT_TYPE, null);

	public static class PeopleCounterEvent extends Event {
		public final int increment;
		public final int count;

		private PeopleCounterEvent(int inc, int count, String msg, EventSource es) {
			super(EVENT_PEOPLE_COUNTER, es);
			setMessage(msg);
			this.increment = inc;
			this.count = count;
		}
		public static PeopleCounterEvent createPeopleIncrement(int inc, String msg, EventSource es) {
			return new PeopleCounterEvent(inc, 0, msg, es);
		}
		public static PeopleCounterEvent createPeopleCount(int count, String msg, EventSource es) {
			return new PeopleCounterEvent(0, count, msg, es);
		}
	}

	public static class LightEvent extends Event {
		public enum State {ON, OFF, SWITCH}
		public final State state;
		public final boolean manual;
		public LightEvent(State ss, String msg, EventSource es) {
			super(EVENT_USER_LIGHT, es);
			setMessage(msg);
			state = ss;
			manual = true;
		}
	}

}
