/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol.pipeline;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import missioncontrol.Util;

/**
 *
 * @author positron
 */
public class EventPipeline extends Thread {
	//private final Set<String> types = new HashSet<>();
	private final Map<String, Set<EventListener> > listeners = new HashMap<>();

	private final Set<EventSource> sources = new HashSet<>();

	private final BlockingQueue<Event> queue;

	public EventPipeline() {
		queue = new LinkedBlockingQueue<>();
	}

	public void registerListener(EventListener ll, String eventType) {
		//types.add(eventType);
		Set<EventListener> ee = listeners.get(eventType);
		if(ee==null) {ee = new HashSet<>();  listeners.put(eventType, ee); }
		ee.add(ll);
	}

	public void pumpEvent(Event e) {
		queue.offer(e);
	}

	@Override
	public void run() {
		while(!isInterrupted()) {
			try {
				Event e = queue.take();
				String type = e.getType();

				Set<EventListener> set = listeners.get(type);
				if(set!=null) for(EventListener l: set)
					l.processEvent(e);
				if(e.getType() == Event.SHUTDOWN_EVENT_TYPE) {
					terminateImpl();
					//System.exit(0);
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				break;
			}
		}
	}

	public void registerSource(EventSource ss) {
		sources.add(ss);
		ss.setEventPipeline(this);
	}

	@Override
	public void start() {
		for(EventSource e: sources) e.start();
		super.start();
	}

	public void terminate() {
		pumpEvent(Event.SHUTDOWN_EVENT);
	}

	private void terminateImpl() {
		for(EventSource e: sources) {
			e.terminate();
			if(e instanceof Thread)
				try {
					synchronized (e) {
						e.wait(500);
					}
				} catch (InterruptedException ex) {
					//ex.printStackTrace();
					Util.log(this, "Normal interrupt sequence for "+ e+" interrupted by "+ex);
				}
		}
		interrupt();
	}

}
