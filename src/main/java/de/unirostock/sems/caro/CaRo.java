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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.caro.converters.CaToRo;
import de.unirostock.sems.caro.converters.RoToCa;



/**
 * CaRo -- converter for CombineArchives and ResearchObjects
 * 
 * This is the main class.
 * 
 * @author Martin Scharm
 * 
 */
public class CaRo
{
	public static boolean DIE = true;
	
	public static String CARO_VERSION = "unknown";
	
	static {
		// read the current version of CaRo
		InputStream in = CaRo.class.getResourceAsStream ("/caro-version.properties");
		if (in != null)
		{
			Properties prop = new Properties();
			try
			{
				prop.load (in);
				String v = (String) prop.get ("version");
				if (v != null && v.length () > 0)
					CARO_VERSION = v;
				in.close ();
			}
			catch (IOException e)
			{
				LOGGER.warn (e, "wasn't able to open CaRo version file");
			}
		}
	}
	
	/**
	 * The main method to be called by the command line.
	 * 
	 * @param args
	 *          the arguments
	 */
	public static void main (String[] args)
	{
		//System.out.println (CARO_VERSION);
		//new File ("/tmp/testro.zip").delete ();
		//args = new String[] { "--caro", "-i", "test/CombineArchiveShowCase.omex", "-o", "/tmp/testro.zip" };
		//args = new String[] { "--caro", "-i", "test/test-ca-contains-valid-evolution.omex", "-o", "/tmp/testro.zip" };
		
		Options options = new Options ();
		
		options
			.addOption (new Option ("h", "help", false, "print the help message"));
		options.addOption (Option.builder ().longOpt ("roca")
			.desc ("convert a research object into a combine archive").build ());
		options.addOption (Option.builder ().longOpt ("caro")
			.desc ("convert a combine archive into a research object").build ());
		options.addOption (Option.builder ("i").longOpt ("in").required ()
			.argName ("FILE").hasArg ().desc ("source container to be converted")
			.build ());
		options.addOption (Option.builder ("o").longOpt ("out").required ()
			.argName ("FILE").hasArg ().desc ("target container to be created")
			.build ());
		
		CommandLineParser parser = new DefaultParser ();
		CommandLine line = null;
		try
		{
			line = parser.parse (options, args);
			if (line.hasOption ("help"))
			{
				// initialise the member variable
				help (options, null);
				return;
			}
		}
		catch (ParseException e)
		{
			help (options, "Parsing of command line options failed.  Reason: "
				+ e.getMessage ());
			return;
		}
		
		File in = new File (line.getOptionValue ("in"));
		File out = new File (line.getOptionValue ("out"));
		
		if (!in.exists ())
		{
			help (options, "file " + in + " does not exist");
			return;
		}
		
		if (out.exists ())
		{
			help (options, "file " + out + " already exist");
			return;
		}
		
		if (line.hasOption ("caro") && line.hasOption ("roca"))
		{
			help (options, "only one of --roca and --caro is allowed");
			return;
		}
		
		CaRoConverter conv = null;
		
		if (line.hasOption ("caro"))
			conv = new CaToRo (in);
		else if (line.hasOption ("roca"))
			conv = new RoToCa (in);
		else
		{
			help (options, "you need to either supply --roca or --caro");
			return;
		}
		
		/*if (!conv.openSourceContainer ())
		{
			help (options, "cannot read the conainer at " + in);
			return;
		}
		if (!conv.convert ())
		{
			help (options, "cannot convert the container at " + in);
			return;
		}
		if (!conv.write (out))
		{
			help (options, "cannot writ the container to " + out);
			return;
		}*/
		conv.convertTo (out);

		if (conv.hasErrors ())
			System.err.println ("There were errors!");

		if (conv.hasWarnings ())
			System.err.println ("There were warnings!");
		
		List<CaRoNotification> notifications = conv.getNotifications ();
		for (CaRoNotification note : notifications)
			System.out.println (note);
		
	}
	
	
	public static void help (Options options, String err)
	{
		if (err != null && err.length () > 0)
			System.err.println (err);
		System.out.println ("this is CaRo version " + CARO_VERSION);
		HelpFormatter formatter = new HelpFormatter ();
		formatter.setOptionComparator (new Comparator<Option> ()
		{
			
			private static final String	OPTS_ORDER	= "hcrio";
			
			
			public int compare (Option o1, Option o2)
			{
				return OPTS_ORDER.indexOf (o1.getLongOpt ())
					- OPTS_ORDER.indexOf (o2.getLongOpt ());
			}
		});
		formatter.printHelp ("java -jar CaRo.jar", options, true);
		if (DIE)
			System.exit (1);
	}
}
