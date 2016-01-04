package org.royaldev.royalauth;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class CommandFilter implements Filter {
	boolean started = false;

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return (started) ? State.STARTED : State.STOPPED;
	}

	@Override
	public void initialize() {
		started = true;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isStopped() {
		return started;
	}

	@Override
	public void start() {
		started = true;
	}

	@Override
	public void stop() {
		started = false;
	}

	@Override
	public Result filter(LogEvent event) {
		if(!started) return Result.NEUTRAL;
		
		String message = event.getMessage().toString().toLowerCase();
		if (message.contains("/login") || message.contains("/register") ||message.contains ("/royalauth") || message.contains("/passwd") || message.contains("/logout")) {
			return Result.DENY;
		}
		return Result.NEUTRAL;
	}

	@Override
	public Result filter(Logger arg0, Level arg1, Marker arg2, String arg3, Object... arg4) {
		// TODO Auto-generated method stub
		return Result.NEUTRAL;
	}

	@Override
	public Result filter(Logger arg0, Level arg1, Marker arg2, Object arg3, Throwable arg4) {
		// TODO Auto-generated method stub
		return Result.NEUTRAL;
	}

	@Override
	public Result filter(Logger arg0, Level arg1, Marker arg2, Message arg3, Throwable arg4) {
		// TODO Auto-generated method stub
		return Result.NEUTRAL;
	}

	@Override
	public Result getOnMatch() {
		// TODO Auto-generated method stub
		return Result.NEUTRAL;
	}

	@Override
	public Result getOnMismatch() {
		// TODO Auto-generated method stub
		return Result.NEUTRAL;
	}
}
