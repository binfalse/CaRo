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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;



/**
 * Unit tests for CaRo.
 * 
 * @author Martin Scharm
 * 
 */
public class CaroTests
{
	
	public static final File	CA_EXAMPLE1	= new File ("test/CombineArchiveShowCase.omex");
	public static final File	RO_EXAMPLE1	= new File ("test/DocumentObject.omex");
	
	/** A temporary folder. */
	@Rule
	public TemporaryFolder		folder			= new TemporaryFolder ();
	
	
	/**
	 * Test caro.
	 */
	@BeforeClass
	public void testChecks ()
	{
		assertTrue ("combine archive showcase does not exist",
			CA_EXAMPLE1.exists ());
		assertTrue ("document object does not exist", RO_EXAMPLE1.exists ());
		
	}
	
	
	/**
	 * Initial tests.
	 */
	@Test
	public void testInit ()
	{
		assertTrue (true);
	}
}
