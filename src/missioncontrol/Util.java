/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author positron
 */
public class Util {
	public static SpeechGenerator speech;

	public synchronized static void log(Object src, String mes) {
		System.out.println(timeToString(new Date())+" "+src+": "+mes);
		//speech.speak(mes);
	}

	private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

	public static String timeToString(Date dd) {
		return sdf.format(dd);
	}
}
