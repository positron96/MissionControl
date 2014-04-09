/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package missioncontrol;

import java.io.IOException;
import java.net.InetAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import missioncontrol.pipeline.Event;
import missioncontrol.pipeline.EventPipeline;
import missioncontrol.pipeline.EventSource;
import missioncontrol.pipeline.Terminatable;

/**
 *
 * @author positron
 */
public class InetListener implements Terminatable, EventSource {
	private final MissionControl engine;
	private EventPipeline pipeline;
	private HttpServer serv;

	private static final String PREF_PORT = "inet-listen.port";
	private static final String PREF_ADDR = "inet-listen.loopback";

	public InetListener(MissionControl engine) {
		this.engine = engine;
	}


	@Override
	public void start() {
		try {
			InetAddress addr;
			if( Boolean.parseBoolean(System.getProperty(PREF_ADDR, "true")))
				addr = InetAddress.getLoopbackAddress();
			else
				addr = new InetSocketAddress(0).getAddress();

			int port = Integer.parseInt(System.getProperty(PREF_PORT, "8888"));
			Util.log(this, "http server started on address "+addr+":"+port);
			serv = HttpServer.create(new InetSocketAddress(addr,port), 10);
			serv.createContext("/", handler);
			serv.setExecutor(null);
			serv.start();
		} catch(IOException e) {
			Util.log(this, "Could not start socket: "+e);
		}

	}

	private HttpHandler handler = new HttpHandler() {
		@Override
		public void handle(HttpExchange exc) throws IOException {
			try {
				URI uri = exc.getRequestURI();
				List<String> path = new LinkedList<>(Arrays.asList( uri.getPath().split("/") ) );
				if(path.get(0).length()==0) path.remove(0);
				if(path.get(0).equals("mcc")) path.remove(0);
				String sender = exc.getRemoteAddress().toString();
				if(exc.getRequestHeaders().containsKey("X-Forwarded-For"))
					sender = exc.getRequestHeaders().get("X-Forwarded-For").get(0);

				switch(path.get(0) ) {
					case "event":
					case "pipeline":
						Map<String, String> par = buildParams(uri.getQuery());
						if(par.get("type")==null) throw new IllegalArgumentException("pipe query without event type");
						Event e = new Event(par.get("type"), par.get("subtype"), par.get("data"), InetListener.this);
						Util.log(InetListener.this, "Event from "+sender+": "+e);
						pipeline.pumpEvent( e);

						sendResponse(exc, 200, "Event sent to pipeline");
						break;
					case "set":
						if(path.get(1).equals("light")) {
							pipeline.pumpEvent(
									new Event.LightEvent(Event.LightEvent.State.SWITCH, "Inet command from "+sender, InetListener.this));
						}
						sendResponse(exc, 200, "set succeeded");
						break;
					default:
						sendResponse(exc, 404, "Default handler doing nothing for uri "+uri);
						Util.log(InetListener.this, "Bad request from "+sender+": "+uri);
						break;
				}
				drainInputStream(exc.getRequestBody());
			} catch(RuntimeException e) {
				Util.log(InetListener.this, "Could not process request: "+e);
				sendResponse(exc, 400, "Error while processing request: ", e);

			}
			exc.close();
		}
	};

	private void sendResponse(HttpExchange exc, int code, String mes)  {
		try {
			exc.sendResponseHeaders(code, 0 );
			exc.setAttribute("Content-type", "text/plain");
			exc.setAttribute("Cache-Control", "no-cache");
			PrintWriter ps = new PrintWriter(exc.getResponseBody());
			ps.println(mes);
			ps.close();
		} catch (IOException ex) {
			Util.log(this, "could not send response: "+ex);
		}
	}

	private void sendResponse(HttpExchange exc, int code, String mes, Exception e)  {
		try {
			exc.sendResponseHeaders(code, 0);
			exc.setAttribute("Content-type", "text/plain");
			exc.setAttribute("Cache-Control", "no-cache");
			PrintStream ps = new PrintStream(exc.getResponseBody());
			ps.println(mes);
			if(e!=null) e.printStackTrace(ps);
			exc.close();
		} catch (IOException ex) {
			Util.log(this, "could not send response: "+ex);
		}
	}

	private void drainInputStream(InputStream is) throws IOException {
        byte[] buffer = new byte[512];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {

        }
		is.close();
    }

	private Map<String,String> buildParams(String query) {
		Map<String, String> res = new HashMap<>();
		for (String qq: query.split("&") ) {
			String[] rr = qq.split("=");
			if(rr.length==1)
				res.put(rr[0].toLowerCase(), rr[0]);
			else
				res.put(rr[0].toLowerCase(), rr[1]);
		}
		return res;
	}

	@Override
	public void terminate() {

		if(serv!=null) serv.stop(0);

	}

	@Override
	public void setEventPipeline(EventPipeline ss) {
		pipeline = ss;
	}

}
