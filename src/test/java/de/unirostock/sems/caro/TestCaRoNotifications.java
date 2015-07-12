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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;



/**
 * @author Martin Scharm
 * 
 */
public class TestCaRoNotifications
{
	
	/**
	 * Test notifications.
	 */
	@Test
	public void testNotifications ()
	{
		String message = "some note";
		CaRoNotification note = new CaRoNotification (
			CaRoNotification.SERVERITY_NOTE, message);
		assertEquals ("expected different message", message, note.getMessage ());
		assertEquals ("expected different message",
			CaRoNotification.SERVERITY_NOTE, note.getSeverity ());
		assertEquals ("serverity to string is incorrect", "NOTE",
			CaRoNotification.severityToString (CaRoNotification.SERVERITY_NOTE));
		assertEquals ("serverity to string is incorrect", "WARN",
			CaRoNotification.severityToString (CaRoNotification.SERVERITY_WARN));
		assertEquals ("serverity to string is incorrect", "ERROR",
			CaRoNotification.severityToString (CaRoNotification.SERVERITY_ERROR));
		assertEquals ("serverity to string is incorrect", "UNKNOWN",
			CaRoNotification.severityToString (-1));
		assertTrue ("unexpected notification: " + note.toString (), note
			.toString ().contains (message));
		assertTrue (
			"unexpected notification: " + note.toString (),
			note.toString ().contains (
				CaRoNotification.severityToString (CaRoNotification.SERVERITY_NOTE)));
	}
}
