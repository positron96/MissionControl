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
public interface EventSource extends Terminatable {

	public void setEventPipeline(EventPipeline ss) ;

	public void start();

	@Override
	public void terminate();

}
