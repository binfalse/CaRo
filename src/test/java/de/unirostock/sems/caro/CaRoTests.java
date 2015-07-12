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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;



/**
 * Unit tests for CaRo.
 * 
 * @author Martin Scharm
 * 
 */
@RunWith(Suite.class)
@SuiteClasses({ TestMain.class, TestCaToRo.class, TestRoToCa.class, TestCaRoNotifications.class })
public class CaRoTests
{
	
	public static final File	CA_EXAMPLE1	= new File ("test/CombineArchiveShowCase.omex");
	public static final File	CA_EXAMPLE_CONTAINS_MANIFEST	= new File ("test/test-ca-contains-ro-manifest.omex");
	public static final File	CA_EXAMPLE_CONTAINS_EVOLUTION	= new File ("test/test-ca-contains-valid-evolution.omex");
	public static final File	RO_EXAMPLE1	= new File ("test/DocumentObject.ro");
	public static final File	RO_EXAMPLE_CONTAINING_REMOTES	= new File ("test/test-ro-remotefile.ro");
	public static final File	RO_EXAMPLE_CONTAINS_MANIFEST	= new File ("test/test-ro-camanifest.ro");
	public static final File	RO_EXAMPLE_CONTAINS_METAFILE	= new File ("test/test-ro-cametadata.ro");
	public static final File	RO_EXAMPLE_CONTAINS_METATESTS	= new File ("test/test-ro-metatests.ro");
	
	/** A temporary folder. */
	@Rule
	public TemporaryFolder		folder			= new TemporaryFolder ();
	
	
	public static ComparisonResult compareContainers (CombineArchive ca, Bundle ro) throws IOException
	{
		Collection<ArchiveEntry> caEntries = ca.getEntries ();
		List<PathMetadata> aggregated = ro.getManifest ().getAggregates ();
		
		ComparisonResult result = (new CaRoTests ()).new ComparisonResult ();
		
		// find all ca entries in ro
		for (ArchiveEntry entry : caEntries)
		{
			boolean found = false;
			for (PathMetadata pmd : aggregated)
			{
				if (pmd.getFile () == null || Files.isDirectory (pmd.getFile ()))
					continue;
				if (entry.getFilePath ().equals (pmd.getFile ().toString ()))
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				System.out.println ("did not find in ro: " + entry.getFilePath ());
				result.numCaOnly++;
			}
		}
		
		// find all ro entries in ca
		for (PathMetadata pmd : aggregated)
		{
			if (pmd.getFile () == null)
			{
				result.numRoRemote++;
				continue;
			}
			if (Files.isDirectory (pmd.getFile ()))
				continue;
			if (ca.getEntry (pmd.getFile ().toString ()) == null)
			{
				System.out.println ("did not find in ca: " + pmd.getFile ().toString ());
				result.numRoOnly++;
			}
		}
		
		// TODO: check meta
		return result;
	}
	
	
	public static CaComparisonResult compareContainers (CombineArchive ca1, CombineArchive ca2) throws IOException
	{
		CaComparisonResult result = (new CaRoTests ()).new CaComparisonResult ();
		// find all ca entries in ro
		for (ArchiveEntry entry : ca1.getEntries ())
		{
			ArchiveEntry entry2 = ca2.getEntry (entry.getFilePath ());
			if (entry2 == null)
				result.numCa1Only++;
			else
				result.numMetaDiff += Math.abs (entry.getDescriptions ().size () - entry2.getDescriptions ().size ());
		}
		// find all ca entries in ro
		for (ArchiveEntry entry : ca2.getEntries ())
		{
			ArchiveEntry entry2 = ca1.getEntry (entry.getFilePath ());
			if (entry2 == null)
				result.numCa2Only++;
		}
		result.numMainDiff = Math.abs (ca1.getMainEntries ().size () - ca2.getMainEntries ().size ());
		return result;
	}
	
	public class CaComparisonResult
	{
		int numCa1Only = 0;
		int numCa2Only = 0;
		int numMetaDiff = 0;
		int numMainDiff = 0;
		public String toString ()
		{
			return "COMPARISON: numCa1Only: " + numCa1Only + " -- numCa2Only: " + numCa2Only + " -- numMainDiff: " + numMainDiff + " -- numMetaDiff: " + numMetaDiff;
		}
	}
	
	public class ComparisonResult
	{
		int numCaOnly = 0;
		int numRoOnly = 0;
		int numRoRemote = 0;
		public String toString ()
		{
			return "COMPARISON: numCaOnly: " + numCaOnly + " -- numRoOnly: " + numRoOnly + " -- numRoRemote: " + numRoRemote;
		}
	}
}
