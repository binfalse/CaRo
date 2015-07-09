/**
 * 
 */
package de.unirostock.sems.caro.converters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
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
	 * @param combineArchive the combine archive
	 */
	public CaToRo (File combineArchive)
	{
		super (combineArchive);
	}

	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#read()
	 */
	@Override
	public boolean read ()
	{
		try
		{
			combineArchive = new CombineArchive (sourceFile);
			List<String> errs = combineArchive.getErrors ();
			for (String s : errs)
				notifications.add (new  CaRoNotification (CaRoNotification.SERVERITY_WARN, "reading archive: " + s));
			return true;
		}
		catch (IOException | JDOMException | ParseException
			| CombineArchiveException e)
		{
			LOGGER.warn (e, "wasn't able to read the combine archive at ", sourceFile);
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to read the combine archive at " + sourceFile + " : " + e.getMessage ()));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#convert()
	 */
	@Override
	public boolean convert ()
	{
		try
		{
			researchObject = Bundles.createBundle ();
			Manifest roManifest = researchObject.getManifest ();
			Path annotationsDir = researchObject.getRoot().resolve("/.ro/annotaions");
			List<PathAnnotation> annotations = roManifest.getAnnotations ();
			List<ArchiveEntry> mainEntries = combineArchive.getMainEntries ();
			tagConvertedContainer (annotations);
			int annotationNumber = 0;
			for (ArchiveEntry entry : combineArchive.getEntries ())
			{
				File tmp = File.createTempFile ("CaRoFromCa", "tmp");
				entry.extractFile (tmp);
				Path target = researchObject.getRoot ().resolve (entry.getEntityPath ());
				
				// check some special files
				if (!includeFile (target, entry))
					continue;
				
				Files.createDirectories (target.getParent ());
				Files.copy (tmp.toPath (), target);
				
				// special case for evolution in turtle format
				if (target.startsWith ("/.ro/evolution.ttl"))
				{
					List<Path> hist = new ArrayList<Path> ();
					hist.add (target);
					roManifest.setHistory (hist);
				}
				else
				{
					PathMetadata pmd = roManifest.getAggregation (target);
					pmd.setConformsTo (entry.getFormat ());
					annotationNumber = handleMetaDate (target, entry.getDescriptions (), annotationsDir, annotationNumber, annotations);
					if (mainEntries.contains (entry))
						setMainEntry (target, annotations);
				}
			}
			return true;
		}
		catch (IOException e)
		{
			LOGGER.warn (e, "wasn't able to convert combine archive at ", sourceFile, " into a research object");
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to convert combine archive at " + sourceFile + " into a research object : " + e.getMessage ()));
		}
		return false;
	}
	
	private void setMainEntry (Path target, List<PathAnnotation> annotations)
	{
		PathAnnotation tag = new PathAnnotation ();
		tag.setAbout (target);
		tag.setContent (URI_MAIN_ENTRY);
		annotations.add (tag);
	}

	private int handleMetaDate (Path target, List<MetaDataObject> meta, Path annotationsDir, int annotationNumber, List<PathAnnotation> annotations)
	{
		for (MetaDataObject m : meta)
		{
			// add that to the evolution?
			Element rdf = new Element ("RDF", RDF_NAMESPACE);
			rdf.addContent (m.getXmlDescription ().clone ());
			try
			{
				if (!Files.exists (annotationsDir))
					Files.createDirectories (annotationsDir);
				Path file = annotationsDir.resolve("omex-conversion-" + ++annotationNumber);
				Bundles.setStringValue(file, XmlTools.prettyPrintDocument (new Document (rdf)));
				PathAnnotation pa = new PathAnnotation ();
				pa.setAbout (target);
				pa.setContent (file);
				pa.generateAnnotationId ();
				annotations.add (pa);
				// tag this annotation as a conversion from a combine archive
				tagAnnoation (pa, annotations);
			}
			catch (IOException e)
			{
				LOGGER.error (e, "was not able to convert annotation for entry ", m.getAbout ());
				notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN, 
					"skipping conversion of annotation for " + m.getAbout () + " -- reason: " + e.getMessage ()));
			}
		}
		return annotationNumber;
	}
	
	private void tagAnnoation (PathAnnotation pa, List<PathAnnotation> annotations)
	{
		PathAnnotation tag = new PathAnnotation ();
		tag.setAbout (pa.getUri ());
		tag.setContent (URI_OMEX_META);
		annotations.add (tag);
	}
	
	private void tagConvertedContainer (List<PathAnnotation> annotations)
	{
		PathAnnotation tag = new PathAnnotation ();
		tag.setAbout (researchObject.getRoot ());
		tag.setContent (URI_CA_RO_CONV);
		annotations.add (tag);
	}
	
	
	/**
	 * Should we include a certain file?
	 *
	 * @param target the file in question
	 * @param entry the corresponding archive entry
	 * @return true, if file can be included
	 */
	private boolean includeFile (Path target, ArchiveEntry entry)
	{
		for (String path : RO_RESTRICTIONS)
			if (target.startsWith (path))
			{
				notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN, 
					"dropping " + path + " as this is a special file in research objects!"));
				return false;
			}
		// special case for the evolution: if it's stored in /.ro/evolution.ttl and of type URI_TURTLE_MIME then we assume it's a valid research object evolution.ttl
		if (target.startsWith ("/.ro/evolution.ttl") && !entry.getFormat ().equals (URI_TURTLE_MIME))
		{
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN, 
				"dropping /.ro/evolution.ttl as this is a special file in research objects!"));
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#write(java.io.File)
	 */
	@Override
	public boolean write (File target)
	{
		try
		{
			Bundles.closeAndSaveBundle(researchObject, target.toPath ());
			return true;
		}
		catch (IOException e)
		{
			LOGGER.warn (e, "wasn't able to save research object to ", target);
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to save research object to " + target + " : " + e.getMessage ()));
		}
		return false;
	}
	
	/**
	 * Returns the research object.
	 * 
	 * Will be <code>null</code> unless you called {@link de.unirostock.sems.caro.CaRoConverter#convert()}.
	 *
	 * @return the converted research object
	 */
	public Bundle getResearchObject ()
	{
		return researchObject;
	}
}
