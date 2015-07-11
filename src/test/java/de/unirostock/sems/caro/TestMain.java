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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



/**
 * Unit tests for CaRo.
 * 
 * @author Martin Scharm
 * 
 */
public class TestMain
{

	
	/** The out content. */
	private ByteArrayOutputStream	outContent;
	
	/** The err content. */
	private ByteArrayOutputStream	errContent;
	
	private static PrintStream out, err;
	
	/**
	 * Test caro.
	 */
	@BeforeClass
	public static void testInit ()
	{
		assertTrue ("combine archive showcase does not exist",
			CaRoTests.CA_EXAMPLE1.exists ());
		assertTrue ("document object does not exist", CaRoTests.RO_EXAMPLE1.exists ());
		out = System.out;
		err = System.err;
	}

	
	
	/**
	 * Sets the up streams.
	 */
	@Before
	public void setUpStreams ()
	{
		outContent	= new ByteArrayOutputStream ();
		errContent	= new ByteArrayOutputStream ();
		System.setOut (new PrintStream (outContent));
		System.setErr (new PrintStream (errContent));
	}
	
	
	/**
	 * Clean up streams.
	 */
	@After
	public void cleanUpStreams ()
	{
		System.setOut (out);
		System.setErr (err);
	}
	
	
	/**
	 * Initial tests.
	 */
	@Test
	public void testCaRoHelp ()
	{
		new CaRo ();
		
		CaRo.DIE = false;
		
		CaRo.main (null);
		
		// check there was an error
		assertTrue ("expected an error", errContent.toString ().length () > 5);
		
		// check help
		String [] lines = outContent.toString ().split ("\n");
		assertTrue ("expected more options on help page", 6 < lines.length);
		
		errContent.reset ();
		outContent.reset ();
		
		CaRo.main (new String[] { "--help" });
		assertTrue ("expected an error", errContent.toString ().length () > 5);
		lines = outContent.toString ().split ("\n");
		assertTrue ("expected more options on help page", 6 < lines.length);
		
		errContent.reset ();
		outContent.reset ();
		
		CaRo.main (new String[] { "--caro", "-i", CaRoTests.CA_EXAMPLE1.getAbsolutePath (), "--roca", "-o", "who-cares.ro" });
		assertTrue ("expected an error", errContent.toString ().length () > 5);
		lines = outContent.toString ().split ("\n");
		assertTrue ("expected more options on help page", 6 < lines.length);
		
		errContent.reset ();
		outContent.reset ();
		
		CaRo.main (new String[] { "--caro", "-i", CaRoTests.CA_EXAMPLE1.getAbsolutePath (), "--help", "-o", "who-cares.ro" });
		assertTrue ("expected no error", errContent.toString ().length () == 0);
		lines = outContent.toString ().split ("\n");
		assertTrue ("expected more options on help page", 6 < lines.length);
	}
	
	/**
	 * Test converting.
	 */
	@Test
	public void testConverting ()
	{
		CaRo.DIE = false;
		File tmpBundle = null;
		File tmpOmex = null;
		try
		{
			tmpBundle = File.createTempFile ("testCaRoMain", ".bundle");
			tmpOmex = File.createTempFile ("testCaRoMain", ".omex");
			tmpBundle.delete ();
			tmpOmex.delete ();
		}
		catch (IOException e)
		{
			fail ("failed to create temp files: " + e.getMessage ());
		}
		
		CaRo.main (new String[] { "--caro", "-i", CaRoTests.CA_EXAMPLE1.getAbsolutePath (), "-o", tmpBundle.getAbsolutePath () });
		// check there was an error
		assertTrue ("did not expect an error converting the bundle: " + errContent.toString (), errContent.toString ().length () == 0);

		// do not overwrite file!
		CaRo.main (new String[] { "--caro", "-i", CaRoTests.CA_EXAMPLE1.getAbsolutePath (), "-o", tmpBundle.getAbsolutePath () });
		// check there was an error
		assertTrue ("expected an error", errContent.toString ().length () > 5);

		errContent.reset ();
		outContent.reset ();
		
		// test the other way around
		CaRo.main (new String[] { "--roca", "-i", CaRoTests.RO_EXAMPLE1.getAbsolutePath (), "-o", tmpOmex.getAbsolutePath () });
		// check there was an error
		assertTrue ("did not expect an error converting the ro: " + errContent.toString (), errContent.toString ().length () == 0);

		// do not overwrite file!
		CaRo.main (new String[] { "--roca", "-i", CaRoTests.RO_EXAMPLE1.getAbsolutePath (), "-o", tmpOmex.getAbsolutePath () });
		// check there was an error
		assertTrue ("expected an error", errContent.toString ().length () > 5);

		errContent.reset ();
		outContent.reset ();
		
		// test invalid input
		CaRo.main (new String[] { "--roca", "-i", CaRoTests.RO_EXAMPLE1.getAbsolutePath () + "-doesnotexists", "-o", tmpOmex.getAbsolutePath () });
		// check there was an error
		assertFalse ("did expect an error converting the non exising ro: " + errContent.toString (), errContent.toString ().length () == 0);

		tmpBundle.delete ();
		tmpOmex.delete ();
	}
	
	
	/**
	 * Test stream collection.
	 */
	@Test
	public void testStreams ()
	{
		assertFalse ("sys.err collection did not work properly", errContent.toString ().length () > 0);
		assertFalse ("sys.out collection did not work properly", outContent.toString ().length () > 0);
		System.out.print ("testOut");
		System.err.print ("testErr");
		assertTrue ("sys.err collection did not work properly", errContent.toString ().equals ("testErr"));
		assertTrue ("sys.out collection did not work properly", outContent.toString ().equals ("testOut"));
		errContent.reset ();
		outContent.reset ();
		assertFalse ("sys.err collection did not work properly", errContent.toString ().length () > 0);
		assertFalse ("sys.out collection did not work properly", outContent.toString ().length () > 0);
	}
}
