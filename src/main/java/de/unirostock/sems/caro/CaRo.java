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

import java.util.Comparator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;



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
	
	/**
	 * The main method to be called by the command line.
	 * 
	 * @param args
	 *          the arguments
	 */
	public static void main (String[] args)
	{
		args = new String[] { "-h" };
		
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
		try
		{
			CommandLine line = parser.parse (options, args);
			if (line.hasOption ("help"))
			{
				// initialise the member variable
				help (options);
				return;
			}
		}
		catch (ParseException e)
		{
			System.err.println ("Parsing of command line options failed.  Reason: "
				+ e.getMessage ());
			help (options);
			return;
		}
		
		System.out.println ("test");
	}
	
	
	public static void help (Options options)
	{
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
	}
}
