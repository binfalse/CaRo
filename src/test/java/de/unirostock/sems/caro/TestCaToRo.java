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
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with CombineExt. If not, see <http://www.gnu.org/licenses/>.
 */
package de.unirostock.sems.caro;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.jdom2.JDOMException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.unirostock.sems.caro.CaRoTests.CaComparisonResult;
import de.unirostock.sems.caro.converters.CaToRo;
import de.unirostock.sems.caro.converters.RoToCa;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;



/**
 * Unit tests for CaRo.
 * 
 * @author Martin Scharm
 * 
 */
public class TestCaToRo
{
	
	/** A temporary folder. */
	@Rule
	public TemporaryFolder		folder			= new TemporaryFolder ();
	
	
	/**
	 * Initial tests.
	 */
	@Test
	public void testCaRo ()
	{
		try
		{
			File tmp = File.createTempFile ("testCaToRo", ".bundle");
			tmp.delete ();
			CaRoConverter conv = new CaToRo (CaRoTests.CA_EXAMPLE1);
			assertTrue ("converting failed", conv.convertTo (tmp));

			// compare archives
			CombineArchive sourceCa = new CombineArchive (CaRoTests.CA_EXAMPLE1);
			Bundle convertedBundle = Bundles.openBundleReadOnly (tmp.toPath ());
			CaRoTests.ComparisonResult comparison = CaRoTests.compareContainers (sourceCa, convertedBundle);
			assertEquals ("conversion resulted in diff in entries (ca only): " + comparison, 0, comparison.numCaOnly);
			assertEquals ("conversion resulted in diff in entries (ro only): " + comparison, 0, comparison.numRoOnly);
			assertEquals ("conversion resulted in diff in entries (ro remote): " + comparison, 0, comparison.numRoRemote);
			sourceCa.close ();
			convertedBundle.close ();

			
			// try vice versa
			conv = new RoToCa (tmp);
			File tmp2 = File.createTempFile ("testRoToCa", ".omex");
			tmp2.delete ();
			assertTrue ("converting failed", conv.convertTo (tmp2));
			convertedBundle = Bundles.openBundleReadOnly (tmp.toPath ());
			CombineArchive convertedCa = new CombineArchive (tmp2);
			comparison = CaRoTests.compareContainers (convertedCa, convertedBundle);
			assertEquals ("conversion resulted in diff in entries (ca only): " + comparison, 0, comparison.numCaOnly);
			assertEquals ("conversion resulted in diff in entries (ro only): " + comparison, 0, comparison.numRoOnly);
			assertEquals ("conversion resulted in diff in entries (ro remote): " + comparison, 0, comparison.numRoRemote);
			convertedCa.close ();
			convertedBundle.close ();
			
			CaComparisonResult caComparison = CaRoTests.compareContainers (sourceCa, convertedCa);
			assertEquals ("double conversion resulted in diff in entries (ca1 only): " + caComparison, 0, caComparison.numCa1Only);
			assertEquals ("double conversion resulted in diff in entries (ca2 only): " + caComparison, 0, caComparison.numCa2Only);
			assertEquals ("double conversion resulted in diff in meta: " + caComparison, 0, caComparison.numMetaDiff);
			assertEquals ("double conversion resulted in diff in main files: " + caComparison, 0, caComparison.numMainDiff);
			
			
			// System.out.println (tmp + " -- " + tmp2);
			tmp.delete ();
			tmp2.delete ();
		}
		catch (IOException | JDOMException | ParseException | CombineArchiveException e)
		{
			e.printStackTrace ();
			fail ("converting failed");
		}
	}
	
	
	/**
	 * Initial tests.
	 * @throws CombineArchiveException 
	 * @throws ParseException 
	 * @throws JDOMException 
	 * @throws IOException 
	 */
	@Test
	public void testRoToRo ()
	{
		try
		{
			File tmp = File.createTempFile ("testCaToRo", ".bundle");
			tmp.delete ();
			CaRoConverter conv = new CaToRo (CaRoTests.RO_EXAMPLE1);
			assertFalse ("converting did not fail", conv.convertTo (tmp));
			conv = new CaToRo (CaRoTests.CA_EXAMPLE1);
			assertFalse ("converting did not fail", conv.convertTo (new File (tmp.getAbsolutePath () + "/does/not/exist")));
			assertTrue ("expected some errors", conv.hasErrors ());
		}
		catch (IOException e)
		{
			e.printStackTrace ();
			fail ("converting failed");
		}
	}
	
	/**
	 * Test conflicts.
	 */
	@Test
	public void testConflicts ()
	{
		try
		{
			File tmp = File.createTempFile ("testCaToRo", ".bundle");
			tmp.delete ();
			CaRoConverter conv = new CaToRo (CaRoTests.CA_EXAMPLE_CONTAINS_EVOLUTION);
			assertTrue ("converting did fail", conv.convertTo (tmp));
			assertTrue ("expected some notifications", conv.getNotifications ().size () > 0);
			
			
			
			conv = new CaToRo (CaRoTests.CA_EXAMPLE_CONTAINS_MANIFEST);
			assertTrue ("converting did fail", conv.convertTo (tmp));
			assertTrue ("expected some warnings", conv.hasWarnings ());
		}
		catch (IOException e)
		{
			e.printStackTrace ();
			fail ("converting failed");
		}
	}
	
	
}
