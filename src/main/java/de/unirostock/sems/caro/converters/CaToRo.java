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
package de.unirostock.sems.caro.converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.caro.CaRoConverter;
import de.unirostock.sems.caro.CaRoNotification;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.xmlutils.tools.XmlTools;



/**
 * The Class CaToRo converts combine archives into research objects.
 * 
 * @author Martin Scharm
 */
public class CaToRo
	extends CaRoConverter
{
	
	/**
	 * The Constructor.
	 * 
	 * @param combineArchive
	 *          the combine archive
	 */
	public CaToRo (File combineArchive)
	{
		super (combineArchive);
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.unirostock.sems.caro.CaRoConverter#openSourceContainer()
	 */
	@Override
	protected boolean openSourceContainer ()
	{
		try
		{
			combineArchive = new CombineArchive (sourceFile);
			List<String> errs = combineArchive.getErrors ();
			for (String s : errs)
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_WARN, "reading archive: " + s));
			return true;
		}
		catch (IOException | JDOMException | ParseException
			| CombineArchiveException e)
		{
			LOGGER
				.warn (e, "wasn't able to read the combine archive at ", sourceFile);
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR,
				"wasn't able to read the combine archive at " + sourceFile + " : "
					+ e.getMessage ()));
		}
		return false;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.unirostock.sems.caro.CaRoConverter#closeSourceContainer()
	 */
	@Override
	protected boolean closeSourceContainer ()
	{
		if (combineArchive == null)
			return true;
		try
		{
			combineArchive.close ();
			return true;
		}
		catch (IOException e)
		{
			LOGGER.error (e, "wasn't able to close combine archive ", sourceFile);
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN,
				"wasn't able to close the combine archive at " + sourceFile + " : "
					+ e.getMessage ()));
		}
		return false;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.unirostock.sems.caro.CaRoConverter#convert()
	 */
	@Override
	protected boolean convert ()
	{
		try
		{
			// create ro infrastructure
			researchObject = Bundles.createBundle ();
			Manifest roManifest = researchObject.getManifest ();
			Path annotationsDir = researchObject.getRoot ().resolve (
				"/.ro/annotations");
			List<PathAnnotation> annotations = roManifest.getAnnotations ();
			tagConvertedContainer (annotations);
			// read the ca
			List<ArchiveEntry> mainEntries = combineArchive.getMainEntries ();
			int annotationNumber = 0;
			for (ArchiveEntry entry : combineArchive.getEntries ())
			{
				// check special annotations
				if (entry.getFormat ().equals (URI_RO_CONV_ANNOTATION)
					|| entry.getFormat ().equals (URI_RO_COPY_ANNOTATION))
				{
					if (handleConvertedAnnotation (entry, annotations))
						continue;
				}
				
				// consider copying the entries
				File tmp = File.createTempFile ("CaRoFromCa", "tmp");
				tmp.deleteOnExit ();
				entry.extractFile (tmp);
				Path target = researchObject.getRoot ()
					.resolve (entry.getEntityPath ());
				
				// check some special files
				if (!includeFile (target, entry))
					continue;
				
				// copy entry
				Files.createDirectories (target.getParent ());
				Files.copy (tmp.toPath (), target);
				
				// special case for evolution in turtle format
				if (target.startsWith ("/.ro/evolution.ttl"))
				{
					notifications.add (new CaRoNotification (
						CaRoNotification.SERVERITY_NOTE,
						"adding history /.ro/evolution.ttl"));
					List<Path> hist = new ArrayList<Path> ();
					hist.add (target);
					roManifest.setHistory (hist);
				}
				else
				{
					// handle meta date
					PathMetadata pmd = roManifest.getAggregation (target);
					pmd.setConformsTo (entry.getFormat ());
					annotationNumber = handleMetaData (pmd, target,
						entry.getDescriptions (), annotationsDir, annotationNumber,
						annotations);
					// is this a main entry?
					if (mainEntries.contains (entry))
						setMainEntry (target, annotations);
				}
			}
			return true;
		}
		catch (IOException e)
		{
			LOGGER.warn (e, "wasn't able to convert combine archive at ", sourceFile,
				" into a research object");
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR,
				"wasn't able to convert combine archive at " + sourceFile
					+ " into a research object : " + e.getMessage ()));
		}
		return false;
	}
	
	
	private boolean handleConvertedAnnotation (ArchiveEntry entry,
		List<PathAnnotation> annotations)
	{
		if (entry.getFormat ().equals (URI_RO_CONV_ANNOTATION))
		{
			// reintegrate this annotation
			try
			{
				File tmp = File.createTempFile ("CaRoFromCa", "ConvAnnotation");
				entry.extractFile (tmp);
				Properties properties = new Properties ();
				FileInputStream in = new FileInputStream (tmp);
				properties.load (in);
				in.close ();
				
				PathAnnotation pa = new PathAnnotation ();
				pa.setAbout (new URI ((String) properties.get ("about")));
				pa.setContent (new URI ((String) properties.get ("body")));
				
				annotations.add (pa);
				return true;
			}
			catch (IOException | URISyntaxException e)
			{
				LOGGER.warn (e, "wasn't able to reintegrate converted annotation ",
					entry.getEntityPath ());
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_WARN,
					"wasn't able to reintegrate converted annotation "
						+ entry.getEntityPath () + " : " + e.getMessage ()));
			}
		}
		else if (entry.getFormat ().equals (URI_RO_COPY_ANNOTATION))
		{
			// reintegrate the file
			try
			{
				File tmp = File.createTempFile ("CaRoFromCa", "tmp");
				tmp.deleteOnExit ();
				entry.extractFile (tmp);
				Path target = researchObject.getRoot ()
					.resolve (entry.getEntityPath ());
				// copy entry
				Files.createDirectories (target.getParent ());
				Files.copy (tmp.toPath (), target);
				return true;
			}
			catch (IOException e)
			{
				LOGGER.warn (e, "wasn't able to reintegrate converted annotation ",
					entry.getEntityPath ());
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_WARN,
					"wasn't able to reintegrate converted annotation "
						+ entry.getEntityPath () + " : " + e.getMessage ()));
			}
		}
		return false;
	}
	
	
	/**
	 * Add an annotation signalling that this is the main entry.
	 * 
	 * @param target
	 *          the path to the file
	 * @param annotations
	 *          the list of existing annotations
	 */
	private void setMainEntry (Path target, List<PathAnnotation> annotations)
	{
		PathAnnotation tag = new PathAnnotation ();
		tag.setAbout (target);
		tag.setContent (URI_MAIN_ENTRY);
		annotations.add (tag);
	}
	
	
	/**
	 * Handle meta data of a ca entry.
	 * 
	 * @param target
	 *          the path to the file in the ro
	 * @param meta
	 *          the meta data of the ca entry
	 * @param annotationsDir
	 *          the annotations dir in the ro
	 * @param annotationNumber
	 *          the annotation number
	 * @param annotations
	 *          the list of existing annotations
	 * @return the annotationNumber
	 */
	private int handleMetaData (PathMetadata pmd, Path target,
		List<MetaDataObject> meta, Path annotationsDir, int annotationNumber,
		List<PathAnnotation> annotations)
	{
		List<Agent> authors = pmd.getAuthoredBy ();
		
		boolean addAll = true;
		if (meta.size () == 1)
		{
			if (meta.get (0) instanceof OmexMetaDataObject)
			{
				OmexDescription omex = ((OmexMetaDataObject) meta.get (0))
					.getOmexDescription ();
				if (omex.getCreators ().size () == 1)
				{
					VCard creator = omex.getCreators ().get (0);
					if (creator != null && creator.getOrganization () == null
						|| creator.getOrganization ().length () == 0)
					{
						Agent agent = vcardToAgent (creator, notifications);
						if (agent != null)
						{
							if (authors == null)
							{
								authors = new ArrayList<Agent> ();
								pmd.setAuthoredBy (authors);
							}
							authors.add (agent);
							addAll = false;
						}
					}
				}
			}
		}
		
		if (addAll)
			for (MetaDataObject m : meta)
			{
				if (m instanceof OmexMetaDataObject)
				{
					// add COMBINE creators as RO authors
					OmexDescription omex = ((OmexMetaDataObject) m).getOmexDescription ();
					for (VCard creator : omex.getCreators ())
					{
						Agent agent = vcardToAgent (creator, notifications);
						if (agent != null)
						{
							if (authors == null)
							{
								authors = new ArrayList<Agent> ();
								pmd.setAuthoredBy (authors);
							}
							authors.add (agent);
						}
					}
				}
				
				// add that to the evolution?
				Element rdf = new Element ("RDF", RDF_NAMESPACE);
				rdf.addContent (m.getXmlDescription ().clone ());
				try
				{
					if (!Files.exists (annotationsDir))
						Files.createDirectories (annotationsDir);
					Path file = annotationsDir.resolve ("omex-conversion-"
						+ ++annotationNumber + ".rdf");
					Bundles.setStringValue (file,
						XmlTools.prettyPrintDocument (new Document (rdf)));
					PathAnnotation pa = new PathAnnotation ();
					pa.setAbout (target);
					pa.setContent (file);
					pa.generateAnnotationId ();
					annotations.add (pa);
					// tag this annotation as a conversion from a combine archive
					tagAnnotation (pa, annotations);
				}
				catch (IOException e)
				{
					LOGGER.error (e, "was not able to convert annotation for entry ",
						m.getAbout ());
					notifications.add (new CaRoNotification (
						CaRoNotification.SERVERITY_WARN,
						"skipping conversion of annotation for " + m.getAbout ()
							+ " -- reason: " + e.getMessage ()));
				}
			}
		
		if (authors != null && authors.size () == 1)
			pmd.setCreatedBy (authors.get (0));
		
		return annotationNumber;
	}
	
	
	/**
	 * Tag an annotation to indicate that it was created by conversion.
	 * 
	 * @param pa
	 *          the pa
	 * @param annotations
	 *          the list of existing annotations
	 */
	private void tagAnnotation (PathAnnotation pa,
		List<PathAnnotation> annotations)
	{
		PathAnnotation tag = new PathAnnotation ();
		tag.setAbout (pa.getUri ());
		tag.setContent (URI_OMEX_META);
		annotations.add (tag);
	}
	
	
	/**
	 * Tag the container to indicate that it was created by conversion..
	 * 
	 * @param annotations
	 *          the list of existing annotations
	 */
	private void tagConvertedContainer (List<PathAnnotation> annotations)
	{
		PathAnnotation tag = new PathAnnotation ();
		tag.setAbout (researchObject.getRoot ());
		tag.setContent (URI_CA_RO_CONV);
		annotations.add (tag);
	}
	
	
	/**
	 * Should we include a certain file?.
	 * 
	 * @param target
	 *          the file in question
	 * @param entry
	 *          the corresponding archive entry
	 * @return true, if file can be included
	 */
	private boolean includeFile (Path target, ArchiveEntry entry)
	{
		for (String path : RO_RESTRICTIONS)
			if (target.startsWith (path))
			{
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_WARN, "dropping " + path
						+ " as this is a special file in research objects!"));
				return false;
			}
		// special case for the evolution: if it's stored in /.ro/evolution.ttl and
		// of type URI_TURTLE_MIME then we assume it's a valid research object
		// evolution.ttl
		if (target.startsWith ("/.ro/evolution.ttl")
			&& !entry.getFormat ().equals (URI_TURTLE_MIME))
		{
			notifications
				.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN,
					"dropping /.ro/evolution.ttl as this is a special file in research objects!"));
			return false;
		}
		return true;
	}
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.unirostock.sems.caro.CaRoConverter#write(java.io.File)
	 */
	@Override
	protected boolean write (File target)
	{
		if (researchObject == null)
			return false;
		try
		{
			Bundles.closeAndSaveBundle (researchObject, target.toPath ());
			return true;
		}
		catch (IOException e)
		{
			LOGGER.warn (e, "wasn't able to save research object to ", target);
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR,
				"wasn't able to save research object to " + target + " : "
					+ e.getMessage ()));
		}
		return false;
	}
	
	
	/**
	 * Returns the research object.
	 * 
	 * Will be <code>null</code> unless you called
	 * {@link de.unirostock.sems.caro.CaRoConverter#convert()}.
	 * 
	 * @return the converted research object
	 */
	public Bundle getResearchObject ()
	{
		return researchObject;
	}
}
