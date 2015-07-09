/**
 * 
 */
package de.unirostock.sems.caro.converters;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.caro.CaRoConverter;
import de.unirostock.sems.caro.CaRoNotification;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.MetaDataFile;
import de.unirostock.sems.cbext.Formatizer;


/**
 * @author Martin Scharm
 *
 */
public class RoToCa
	extends CaRoConverter
{
	private File temporaryLocation;
	
	public RoToCa (File researchObject)
	{
		super (researchObject);
	}
	
	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#openSourceContainer()
	 */
	@Override
	protected boolean openSourceContainer ()
	{
		try
		{
			researchObject = Bundles.openBundleReadOnly (sourceFile.toPath ());
			return true;
		}
		catch (IOException e)
		{
			LOGGER.warn (e, "wasn't able to read the research object at ", sourceFile);
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to read the combine archive at " + sourceFile + " : " + e.getMessage ()));
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#closeSourceContainer()
	 */
	@Override
	protected boolean closeSourceContainer ()
	{
		if (researchObject == null)
			return true;
		try
		{
			researchObject.close ();
			return true;
		}
		catch (IOException e)
		{
			LOGGER.error (e, "wasn't able to close research object ", sourceFile);
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN, "wasn't able to close the research object at " + sourceFile + " : " + e.getMessage ()));
		}
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#convert()
	 */
	@Override
	protected boolean convert ()
	{
		try
		{
			// setup empty combine archive
			temporaryLocation = File.createTempFile ("CaRoFromRo", "container");
			temporaryLocation.delete ();
			combineArchive = new CombineArchive (temporaryLocation);
			HashMap<String, ArchiveEntry>	archiveEntries = new HashMap<String, ArchiveEntry> ();
			
			
			// read bundle stuff
			Manifest roManifest = researchObject.getManifest ();
			List<PathAnnotation> annotations = roManifest.getAnnotations ();
			List<PathMetadata> aggregations = roManifest.getAggregates ();
			for (PathMetadata pmd : aggregations)
			{
				// if it's a file add to combine archive
				if (pmd.getFile () == null)
				{
					if (!handleRemoteFile (pmd))
						continue;
				}
				// extract
				File tmp = File.createTempFile ("CaRoFromRo", pmd.getFile ().getFileName ().toString ());
				tmp.delete ();
				Files.copy (pmd.getFile (), tmp.toPath ());
				tmp.deleteOnExit ();
				// import
				URI format = pmd.getConformsTo ();
				if (format == null)
				{
						format = Formatizer.guessFormat (tmp);
				}
				ArchiveEntry entry = combineArchive.addEntry (tmp, pmd.getFile ().toString (), format);
				archiveEntries.put (entry.getFilePath (), entry);
				
				
				// respect annotations
				handleAnnotations (pmd.getFile (), pmd, annotations, entry, archiveEntries);
				
				// special files? -> evolution, 
			}
			
			return true;
		}
		catch (IOException | JDOMException | ParseException | CombineArchiveException e)
		{
			LOGGER.warn (e, "wasn't able to convert research object at ", sourceFile, " into a combine archive");
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to convert research object at " + sourceFile + " into a combine archive : " + e.getMessage ()));
		}
		return false;
	}
	
	
	
	private void handleAnnotations (Path file, PathMetadata pmd, List<PathAnnotation> annotations, ArchiveEntry entry, HashMap<String, ArchiveEntry> archiveEntries)
	{
		List<PathAnnotation> curAnnotations = getAnnotations (pmd, annotations);
		for (PathAnnotation annot : curAnnotations)
		{
			if (annotationHasOmexTag (annot, annotations))
			{
				// copy it to the list of overall annotations
				List<String> errors = new ArrayList<String> ();
				try
				{
					MetaDataFile.readFile (researchObject.getRoot ().resolve (Paths.get (annot.getContent ())), archiveEntries, combineArchive, true, errors);
					if (errors.size () > 0)
						for (String err : errors)
						{
							LOGGER.warn ("wasn't able to read omex meta file ", annot.getContent (), " in research object at ", sourceFile, " because ", err);
							notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to read omex meta file " + annot.getContent () + " in research object at " + sourceFile + " because " + err));
						}
				}
				catch (IOException | ParseException | JDOMException | CombineArchiveException e)
				{
					LOGGER.warn (e, "reading meta data file ", annot.getContent (), " in research object at ", sourceFile, " failed");
					notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "reading meta data file " + annot.getContent () + " in research object at " + sourceFile + " failed because: " + e.getMessage ()));
				}
			}
			else
			{
				// what should we do?
				LOGGER.error ("this is not implemented yet");
			}
		}
	}

	private boolean annotationHasOmexTag (PathAnnotation annotation,
		List<PathAnnotation> annotations)
	{
		for (PathAnnotation annot : annotations)
			// does `annot` describe `annotation`?
			if (annot.getAbout ().equals (annotation.getUri ()))
				// is there the flag?
				if (annot.getContent ().equals (URI_OMEX_META))
					return true;
		return false;
	}

	private List<PathAnnotation> getAnnotations (PathMetadata pmd,
		List<PathAnnotation> annotations)
	{
		List<PathAnnotation> curAnnotations = new ArrayList<PathAnnotation> ();
		for (PathAnnotation annot : annotations)
		{
			if (annot.getAbout ().equals (pmd.getUri ()))
				curAnnotations.add (annot);
		}
		return curAnnotations;
	}

	/**
	 * Handle a remote file.
	 *
	 * @param pmd the meta date of the file
	 * @return true, if it was included
	 */
	private boolean handleRemoteFile (PathMetadata pmd)
	{
		// TODO: try to download the file?
		LOGGER.warn ("skipping manifest entry ", pmd.getUri (), " as it seems to be no local file");
		notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN, "skipping manifest entry " + pmd.getUri () + " as it seems to be no local file"));
		
		return false;
	}
	
	
	/* (non-Javadoc)
	 * @see de.unirostock.sems.caro.CaRoConverter#write(java.io.File)
	 */
	@Override
	protected boolean write (File target)
	{
		if (combineArchive == null)
			return false;
		try
		{
			combineArchive.pack ();
			combineArchive.close ();
			Files.createDirectories (target.getParentFile ().toPath ());
			Files.move (temporaryLocation.toPath (), target.toPath ());
			return true;
		}
		catch (IOException | TransformerException e)
		{
			LOGGER.warn (e, "wasn't able to save combine archive to ", target);
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to save combine archive to " + target + " : " + e.getMessage ()));
		}
		return false;
	}
	
	/**
	 * Returns the combine archive.
	 * 
	 * Will be <code>null</code> unless you called {@link de.unirostock.sems.caro.CaRoConverter#convert()}.
	 *
	 * @return the converted combine archive
	 */
	public CombineArchive getCombineArchive ()
	{
		return combineArchive;
	}
	
}
