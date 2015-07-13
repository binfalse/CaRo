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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.manifest.Agent;
import org.jdom2.Namespace;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.meta.omex.VCard;



/**
 * The abstract class CaRoConverter defines the infrastructure for converting
 * containers.
 * 
 * @author Martin Scharm
 */
public abstract class CaRoConverter
{
	
	/** file name restrictions for research objects. */
	public static final String[]			RO_RESTRICTIONS	= new String[] {
		"/.ro/manifest.json", "/META-INF/container.xml", "/META-INF/manifest.xml",
		"/mimetype"																		};
	
	/** file name restrictions for combine archives. */
	public static final String[]			CA_RESTRICTIONS	= new String[] {
		"/metadata.rdf", "/manifest.xml"								};
	
	/** The RDF namespace. */
	public static Namespace						RDF_NAMESPACE		= Namespace
																											.getNamespace ("rdf",
																												"http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	
	/** The uri turtle file format. */
	public static URI									URI_TURTLE_MIME;
	
	/** The annotation to indicate omex meta. */
	public static URI									URI_OMEX_META;
	
	/** The annotation to indicate main entries. */
	public static URI									URI_MAIN_ENTRY;
	
	/** The annotation to indicate a main entry. */
	public static URI									URI_BF_MAIN_ENTRY;
	
	/** The annotation to indicate it was an RO annotation. */
	public static URI									URI_RO_CONV_ANNOTATION;
	
	/** The annotation to indicate it was an annotation referenced in the RO. */
	public static URI									URI_RO_COPY_ANNOTATION;
	
	/** The annotation to indicate that this was converted. */
	public static URI									URI_CA_RO_CONV;
	
	/** The annotation to indicate that this was converted. */
	public static URI									URI_RO_CA_CONV;
	
	static
	{
		try
		{
			URI_TURTLE_MIME = new URI ("http://purl.org/NET/mediatypes/text/turtle");
			URI_OMEX_META = new URI (
				"http://sems.uni-rostock.de/CaRo/annotations#omexMeta");
			URI_MAIN_ENTRY = new URI (
				"http://sems.uni-rostock.de/CaRo/annotations#mainEntry");
			URI_BF_MAIN_ENTRY = new URI ("http://binfalse.de#rootdocument");
			URI_CA_RO_CONV = new URI (
				"http://sems.uni-rostock.de/CaRo/annotations#ca2ro");
			URI_RO_CA_CONV = new URI (
				"http://sems.uni-rostock.de/CaRo/annotations#ro2ca");
			URI_RO_CONV_ANNOTATION = new URI (
				"http://sems.uni-rostock.de/CaRo/annotations#roConvertedAnnotation");
			URI_RO_COPY_ANNOTATION = new URI (
				"http://sems.uni-rostock.de/CaRo/annotations#roCopiedAnnotation");
		}
		catch (URISyntaxException e)
		{
			LOGGER.error (e, "cannot create turtle URI");
		}
	}
	
	/** The source file. */
	protected File										sourceFile;
	
	/** The combine archive. */
	protected CombineArchive					combineArchive;
	
	/** The research object. */
	protected Bundle									researchObject;
	
	/** The notifications. */
	protected List<CaRoNotification>	notifications;
	
	
	/**
	 * The Constructor.
	 * 
	 * @param sourceFile
	 *          the source file
	 */
	public CaRoConverter (File sourceFile)
	{
		this.sourceFile = sourceFile;
		combineArchive = null;
		researchObject = null;
		notifications = new ArrayList<CaRoNotification> ();
	}
	
	
	/**
	 * Checks for errors.
	 * 
	 * @return true, if the converter reported errors
	 */
	public boolean hasErrors ()
	{
		for (CaRoNotification crn : notifications)
			if (crn.getSeverity () == CaRoNotification.SERVERITY_ERROR)
				return true;
		return false;
	}
	
	
	/**
	 * Checks for warnings. Might return <code>false</code> even if
	 * {@link #hasErrors()} returns <code>true</code>.
	 * 
	 * @return true, if the converter reported warnings
	 */
	public boolean hasWarnings ()
	{
		for (CaRoNotification crn : notifications)
			if (crn.getSeverity () == CaRoNotification.SERVERITY_WARN)
				return true;
		return false;
	}
	
	
	/**
	 * Gets the notifications occured during the conversion.
	 * 
	 * @return the notifications
	 */
	public List<CaRoNotification> getNotifications ()
	{
		return notifications;
	}
	
	
	/**
	 * Open the source container.
	 * 
	 * @return true, if opening was successful
	 */
	protected abstract boolean openSourceContainer ();
	
	
	/**
	 * Open the source container.
	 * 
	 * @return true, if opening was successful
	 */
	protected abstract boolean closeSourceContainer ();
	
	
	/**
	 * Convert the container.
	 * 
	 * @return true, if converting was successful
	 */
	protected abstract boolean convert ();
	
	
	/**
	 * Write the container.
	 * 
	 * @param target
	 *          the target file to write to
	 * @return true, if exporting was successful
	 */
	protected abstract boolean write (File target);
	
	
	/**
	 * Convert this container to <code>target</code>.
	 * 
	 * @param target
	 *          the target file
	 * @return true, if converting was successful
	 */
	public boolean convertTo (File target)
	{
		// open
		if (!openSourceContainer ())
		{
			LOGGER.error ("wasn't able to open ", sourceFile);
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR, "wasn't able to open " + sourceFile));
			closeSourceContainer ();
			return false;
		}
		// convert
		if (!convert ())
		{
			LOGGER.error ("wasn't able to convert ", sourceFile);
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR, "wasn't able to convert "
					+ sourceFile));
			closeSourceContainer ();
			return false;
		}
		// close source container
		if (!closeSourceContainer ())
		{
			LOGGER.error ("wasn't able to close ", sourceFile);
			notifications
				.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR,
					"wasn't able to close " + sourceFile));
			closeSourceContainer ();
			return false;
		}
		// write target file
		if (!write (target))
		{
			LOGGER.error ("wasn't able to write ", target);
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR, "wasn't able to write " + target));
			closeSourceContainer ();
			return false;
		}
		return true;
	}
	
	
	/**
	 * Convert an RO agent to a CA vcard.
	 * 
	 * @param agent
	 *          the agent
	 * @return the vcard
	 */
	protected static VCard agentToVCard (Agent agent)
	{
		if (agent == null)
			return null;
		
		VCard vcard = new VCard ();
		
		String name = agent.getName ();
		URI mail = agent.getUri ();
		
		boolean emptyVcard = true;
		
		if (name != null && name.length () > 0)
		{
			String[] names = name.split (" ");
			if (names.length > 1)
			{
				vcard.setGivenName (names[0]);
				vcard.setFamilyName ("");
				for (int i = 1; i < names.length; i++)
					vcard.setFamilyName (vcard.getFamilyName ()
						+ (i < names.length - 1 ? " " : "") + names[i]);
			}
			else if (names.length == 1)
				vcard.setGivenName (names[0]);
			emptyVcard = false;
		}
		
		if (mail != null && mail.getScheme ().equals ("mailto"))
		{
			vcard.setEmail (mail.getRawSchemeSpecificPart ());
			emptyVcard = false;
		}
		
		return emptyVcard ? null : vcard;
	}
	
	
	/**
	 * Convert a CA vcard to an RO agent.
	 * 
	 * @param vcard
	 *          the vcard
	 * @param notifications
	 *          the notifications
	 * @return the agent
	 */
	protected static Agent vcardToAgent (VCard vcard,
		List<CaRoNotification> notifications)
	{
		if (vcard == null)
			return null;
		
		Agent agent = new Agent ();
		
		boolean emptyAgent = true;
		String name = "";
		if (vcard.getGivenName () != null && vcard.getFamilyName () != null)
			name = vcard.getGivenName () + " " + vcard.getFamilyName ();
		else if (vcard.getGivenName () != null)
			name = vcard.getGivenName ();
		else if (vcard.getFamilyName () != null)
			name = vcard.getFamilyName ();
		
		if (name.length () > 0)
		{
			agent.setName (name);
			emptyAgent = false;
		}
		
		try
		{
			if (vcard.getEmail () != null && vcard.getEmail ().length () > 0)
			{
				agent.setUri (new URI ("mailto", vcard.getEmail (), null));
				emptyAgent = false;
			}
		}
		catch (URISyntaxException e)
		{
			LOGGER.warn (e, "wasn't able to create mailto uri for ",
				vcard.getEmail ());
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN,
				"wasn't able to create mailto uri for " + vcard.getEmail ()));
		}
		
		return emptyAgent ? null : agent;
	}
	
	
	/**
	 * Compare two VCards.
	 * 
	 * @param a
	 *          the first VCard
	 * @param b
	 *          the second VCards
	 * @return true, if equal
	 */
	protected boolean sameVcard (VCard a, VCard b)
	{
		if (a.getFamilyName () == null && b.getFamilyName () != null)
			return false;
		if (a.getGivenName () == null && b.getGivenName () != null)
			return false;
		if (a.getFamilyName () != null && b.getFamilyName () == null)
			return false;
		if (a.getGivenName () != null && b.getGivenName () == null)
			return false;
		
		if ( ( (a.getGivenName () == null || a.getGivenName ().equals (
			b.getGivenName ())) && (a.getFamilyName () == null || a.getFamilyName ()
			.equals (b.getFamilyName ())))
			|| (a.getEmail () != null && a.getEmail ().equals (b.getEmail ())))
			return true;
		return false;
	}
}
