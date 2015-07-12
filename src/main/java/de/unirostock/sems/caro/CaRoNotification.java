/**
 * Copyright © 2015 Martin Scharm <martin@binfalse.de>
 * 
 * This file is part of CaRo.
 * 
 * CaRo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * CaRo is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with CaRo. If not, see <http://www.gnu.org/licenses/>.
 */
package de.unirostock.sems.caro;

import de.binfalse.bflog.LOGGER;



/**
 * The Class CaRoNotification to store notifications.
 * 
 * @author Martin Scharm
 */
public class CaRoNotification
{
	
	/** The Constant SERVERITY_NOTE. */
	public static final int	SERVERITY_NOTE	= LOGGER.INFO;
	
	/** The Constant SERVERITY_WARN. */
	public static final int	SERVERITY_WARN	= LOGGER.WARN;
	
	/** The Constant SERVERITY_ERROR. */
	public static final int	SERVERITY_ERROR	= LOGGER.ERROR;
	
	/** The message. */
	private String					message;
	
	/** The severity. */
	private int							severity;
	
	
	/**
	 * The Constructor.
	 * 
	 * @param severity
	 *          the severity
	 * @param message
	 *          the message
	 */
	public CaRoNotification (int severity, String message)
	{
		this.severity = severity;
		this.message = message;
	}
	
	
	/**
	 * Gets the message.
	 * 
	 * @return the message
	 */
	public String getMessage ()
	{
		return message;
	}
	
	
	/**
	 * Gets the severity.
	 * 
	 * @return the severity
	 */
	public int getSeverity ()
	{
		return severity;
	}
	
	
	/**
	 * Turns the severity into a string.
	 * 
	 * @param severity
	 *          the severity
	 * @return the string
	 */
	public static final String severityToString (int severity)
	{
		if (severity == SERVERITY_NOTE)
			return "NOTE";
		if (severity == SERVERITY_WARN)
			return "WARN";
		if (severity == SERVERITY_ERROR)
			return "ERROR";
		return "UNKNOWN";
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString ()
	{
		return severityToString (severity) + ": " + message;
	}
}
