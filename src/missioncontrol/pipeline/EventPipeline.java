/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol.pipeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author positron
 */
public class EventPipeline {
	//private final Set<String> types = new HashSet<>();
	private final Map<String, Set<EventListener> > listeners = new HashMap<>();

	private final Set<EventSource> sources = new HashSet<>();

	public EventPipeline() {

	}

	public void registerListener(EventListener ll, String eventType) {
		//types.add(eventType);
		Set<EventListener> ee = listeners.get(eventType);
		if(ee==null) {ee = new HashSet<>();  listeners.put(eventType, ee); }
		ee.add(ll);
	}

	public void pumpEvent(Event e) {
		String type = e.getType();
		Set<EventListener> set = listeners.get(type);
		if(set!=null) for(EventListener l: set)
			l.processEvent(e);
	}

	public void registerSource(EventSource ss) {
		sources.add(ss);
		ss.setEventPipeline(this);
	}

	public void start() {
		for(EventSource e: sources) e.start();
	}

	public void terminate() {
		for(EventSource e: sources) {
			e.terminate();
			if(e instanceof Thread)
				try {
					synchronized (e) {
						e.wait(500);
					}
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
		}
	}

}
