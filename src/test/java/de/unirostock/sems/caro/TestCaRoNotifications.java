/**
 * 
 */
package de.unirostock.sems.caro;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * @author Martin Scharm
 *
 */
public class TestCaRoNotifications
{
	@Test
	public void testNotifications ()
	{
		String message = "some note";
		CaRoNotification note = new CaRoNotification (CaRoNotification.SERVERITY_NOTE, message);
		assertEquals ("expected different message", message, note.getMessage ());
		assertEquals ("expected different message", CaRoNotification.SERVERITY_NOTE, note.getSeverity ());
		assertEquals ("serverity to string is incorrect", "NOTE", CaRoNotification.severityToString (CaRoNotification.SERVERITY_NOTE));
		assertEquals ("serverity to string is incorrect", "WARN", CaRoNotification.severityToString (CaRoNotification.SERVERITY_WARN));
		assertEquals ("serverity to string is incorrect", "ERROR", CaRoNotification.severityToString (CaRoNotification.SERVERITY_ERROR));
		assertEquals ("serverity to string is incorrect", "UNKNOWN", CaRoNotification.severityToString (-1));
		assertTrue ("unexpected notification: " + note.toString (), note.toString ().contains (message));
		assertTrue ("unexpected notification: " + note.toString (), note.toString ().contains (CaRoNotification.severityToString (CaRoNotification.SERVERITY_NOTE)));
	}
}
