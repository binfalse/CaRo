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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.binfalse.bflog.LOGGER;
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
public class TestRoToCa
{
	
	/** A temporary folder. */
	@Rule
	public TemporaryFolder		folder			= new TemporaryFolder ();
	
	/**
	 * Test ro-to-ca.
	 */
	@Test
	public void testRoCa ()
	{
		try
		{
			File tmp = File.createTempFile ("testRoToCa", ".omex");
			tmp.delete ();
			CaRoConverter conv = new RoToCa (CaRoTests.RO_EXAMPLE1);
			assertTrue ("converting failed", conv.convertTo (tmp));
			
			// compare archives
			CombineArchive convertedCa = new CombineArchive (tmp);
			Bundle sourceBundle = Bundles.openBundleReadOnly (CaRoTests.RO_EXAMPLE1.toPath ());
			CaRoTests.ComparisonResult comparison = CaRoTests.compareContainers (convertedCa, sourceBundle);
			assertEquals ("conversion resulted in diff in entries (ca only): " + comparison, 0, comparison.numCaOnly);
			assertEquals ("conversion resulted in diff in entries (ro only): " + comparison, 0, comparison.numRoOnly);
			assertEquals ("conversion resulted in diff in entries (ro remote): " + comparison, 0, comparison.numRoRemote);
			convertedCa.close ();
			sourceBundle.close ();
			
			// try vice versa
			conv = new CaToRo (tmp);
			File tmp2 = File.createTempFile ("testRoToCa", ".bundle");
			assertTrue ("converting failed", conv.convertTo (tmp2));
			convertedCa = new CombineArchive (tmp);
			Bundle convertedBundle = Bundles.openBundleReadOnly (tmp2.toPath ());
			comparison = CaRoTests.compareContainers (convertedCa, convertedBundle);
			assertEquals ("conversion resulted in diff in entries (ca only): " + comparison, 0, comparison.numCaOnly);
			assertEquals ("conversion resulted in diff in entries (ro only): " + comparison, 0, comparison.numRoOnly);
			assertEquals ("conversion resulted in diff in entries (ro remote): " + comparison, 0, comparison.numRoRemote);
			convertedCa.close ();
			convertedBundle.close ();
			//System.out.println (tmp + " -- " + tmp2);
			
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
	 * Test ro-to-ca.
	 */
	@Test
	public void testRoWithRemotes ()
	{
		try
		{
			File tmp = File.createTempFile ("testRoToCa", ".omex");
			tmp.delete ();
			CaRoConverter conv = new RoToCa (CaRoTests.RO_EXAMPLE_CONTAINING_REMOTES);
			assertTrue ("converting failed", conv.convertTo (tmp));

			assertTrue ("expected some warnings", conv.hasWarnings ());
			
		}
		catch (IOException e)
		{
			e.printStackTrace ();
			fail ("converting failed");
		}
	}

	/**
	 * Test failes.
	 */
	@Test
	public void testFail ()
	{
		try
		{
			File tmp = File.createTempFile ("testRoToCa", ".omex");
			CaRoConverter conv = new RoToCa (CaRoTests.RO_EXAMPLE1);
			assertFalse ("converting did not fail", conv.convertTo (new File (tmp.getAbsolutePath () + "/does/not/exist")));
			assertTrue ("expected some errors", conv.hasErrors ());
			
			conv = new RoToCa (new File (CaRoTests.RO_EXAMPLE1.getAbsolutePath () + "/does/not/exist"));
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
	 * Initial tests.
	 */
	// TODO need to speak with stian @Test
	public void testRoCaWithCa ()
	{
		try
		{
			File tmp = File.createTempFile ("testRoToCa", ".omex");
			tmp.delete ();
			CaRoConverter conv = new RoToCa (CaRoTests.CA_EXAMPLE1);
			System.out.println ("converting was successful: " + conv.convertTo (tmp));
			System.out.println (tmp);
			tmp.delete ();
		}
		catch (IOException e)
		{
			e.printStackTrace ();
			fail ("converting failed");
		}
	}
}
