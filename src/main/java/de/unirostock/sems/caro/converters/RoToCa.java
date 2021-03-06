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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.transform.TransformerException;

import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.fs.BundleFileSystemProvider;
import org.apache.taverna.robundle.manifest.Agent;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.binfalse.bfutils.AlphabetIterator;
import de.unirostock.sems.caro.CaRo;
import de.unirostock.sems.caro.CaRoConverter;
import de.unirostock.sems.caro.CaRoNotification;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.MetaDataFile;
import de.unirostock.sems.cbarchive.meta.MetaDataObject;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbext.Formatizer;



/**
 * The Class RoToCa converts research objects to combine archives.
 * 
 * @author Martin Scharm
 */
public class RoToCa
	extends CaRoConverter
{
	
	private List<PathAnnotation>	handledAnnotations;
	
	/** The temporary location. */
	private File									temporaryLocation;
	
	
	/**
	 * Instantiates a new converter.
	 * 
	 * @param researchObject
	 *          the research object
	 */
	public RoToCa (File researchObject)
	{
		super (researchObject);
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
			String mime = Files.probeContentType (sourceFile.toPath ());
			if (!mime.equals (BundleFileSystemProvider.APPLICATION_VND_WF4EVER_ROBUNDLE_ZIP))
			{
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_ERROR, "file at " + sourceFile
						+ " does not seem to be a research object: wrong mime type : " + mime));
				return false;
			}
		}
		catch (IOException e)
		{
			LOGGER.error (e, "wasn't able to get mime type of ", sourceFile);
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR, "wasn't able to get mime type of "
					+ sourceFile + " : " + e.getMessage ()));
		}
		try
		{
			researchObject = Bundles.openBundleReadOnly (sourceFile.toPath ());
			return true;
		}
		catch (IOException e)
		{
			LOGGER.error (e, "wasn't able to read the research object at ",
				sourceFile);
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
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN,
				"wasn't able to close the research object at " + sourceFile + " : "
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
		handledAnnotations = new ArrayList<PathAnnotation> ();
		try
		{
			// setup empty combine archive
			temporaryLocation = File.createTempFile ("CaRoFromRo", "container");
			temporaryLocation.delete ();
			combineArchive = new CombineArchive (temporaryLocation);
			HashMap<String, ArchiveEntry> archiveEntries = new HashMap<String, ArchiveEntry> ();
			
			// read bundle stuff
			Manifest roManifest = researchObject.getManifest ();
			List<PathAnnotation> annotations = roManifest.getAnnotations ();
			List<PathMetadata> aggregations = roManifest.getAggregates ();
			for (PathMetadata pmd : aggregations)
			{
				if (pmd.getFile () != null && Files.isDirectory (pmd.getFile ()))
				{
					// TODO: handle root annotations
					continue;
				}
				
				// if it's a file add to combine archive
				if (pmd.getFile () == null)
				{
					if (!handleRemoteFile (pmd))
						continue;
				}
				
				// extract
				File tmp = File.createTempFile ("CaRoFromRo", pmd.getFile ()
					.getFileName ().toString ());
				tmp.delete ();
				Files.copy (pmd.getFile (), tmp.toPath ());
				tmp.deleteOnExit ();
				
				// check some special files
				if (!includeFile (pmd.getFile ()))
					continue;
				
				// import
				URI format = pmd.getConformsTo ();
				if (format == null)
				{
					format = Formatizer.guessFormat (tmp);
				}
				ArchiveEntry entry = combineArchive.addEntry (tmp, pmd.getFile ()
					.toString (), format);
				archiveEntries.put (entry.getFilePath (), entry);
				
				// respect annotations
				handleAnnotations (pmd.getFile (), pmd, annotations, entry,
					archiveEntries);
				
				// handle manifest annotations
				handleCreators (entry, pmd);
			}
			
			// evolution(s)?
			if (roManifest.getHistory () != null)
			{
				for (Path hist : roManifest.getHistory ())
				{
					// extract
					File tmp = File.createTempFile ("CaRoFromRo", hist.getFileName ()
						.toString ());
					tmp.delete ();
					Files.copy (hist, tmp.toPath ());
					tmp.deleteOnExit ();
					
					// check some special files
					if (!includeFile (hist))
						continue;
					
					// import
					URI format = hist.toString ().equals ("evolution.ttl") ? URI_TURTLE_MIME
						: null;
					if (format == null)
					{
						format = Formatizer.guessFormat (tmp);
					}
					// add it to the .ro directory to indicate, that it is coming from a research object
					ArchiveEntry entry = combineArchive.addEntry (tmp, "/.ro/" + hist.toString (),
						format);
					archiveEntries.put (entry.getFilePath (), entry);
				}
			}
			
			// other ro meta data
			String annotationsDir = "/.ro/annotations/";
			AlphabetIterator alhpa = AlphabetIterator.getLowerCaseIterator ();
			for (PathAnnotation annot : annotations)
			{
				if (!handledAnnotations.contains (annot))
				{
					if (annot.getContent ().equals (URI_CA_RO_CONV))
						continue;
					
					try
					{
						File newAnnotation = Files.createTempFile (
							"CaRoFromRoConvertedAnnotation", ".fromRo").toFile ();
						Properties properties = new Properties ();
						properties.put ("about", annot.getAbout ().toString ());
						properties.put ("body", annot.getContent ().toString ());
						if (annot.getUri () != null)
							properties.put ("uri", annot.getUri ().toString ());
						FileOutputStream out = new FileOutputStream (newAnnotation);
						properties.store (out, "conversion from research object");
						out.close ();
						
						// import file
						String targetName = newAnnotation.getName ().toString ();
						while (combineArchive.getEntry (annotationsDir + targetName) != null)
							targetName += alhpa.next ();
						combineArchive.addEntry (newAnnotation,
							annotationsDir + targetName, URI_RO_CONV_ANNOTATION);
						
						// if the body is a file in the annotations directory we need to
						// also incluse the file.
						if (annot.getContent ().toString ()
							.startsWith ("/.ro/annotations/"))
						{
							String annotation = annot.getContent ().toString ();
							File tmp = File.createTempFile ("CaRoFromRo", "annotation");
							tmp.delete ();
							Files.copy (researchObject.getRoot ().resolve (annotation),
								tmp.toPath ());
							combineArchive.addEntry (tmp, annotation, URI_RO_COPY_ANNOTATION);
						}
					}
					catch (IOException e)
					{
						LOGGER.warn (e, "wasn't able to convert annotation about ",
							annot.getAbout ());
						notifications.add (new CaRoNotification (
							CaRoNotification.SERVERITY_WARN,
							"wasn't able to convert annotation about " + annot.getAbout ()));
					}
				}
			}
			
			OmexDescription omex = new OmexDescription (new VCard (null,
				"CaRo version " + CaRo.CARO_VERSION, null, "sems.uni-rostock.de"),
				new Date ());
			omex.setDescription (URI_RO_CA_CONV.toString ());
			combineArchive.addDescription (new OmexMetaDataObject (omex));
			
			return true;
		}
		catch (IOException | JDOMException | ParseException
			| CombineArchiveException e)
		{
			LOGGER.warn (e, "wasn't able to convert research object at ", sourceFile,
				" into a combine archive");
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR,
				"wasn't able to convert research object at " + sourceFile
					+ " into a combine archive : " + e.getMessage ()));
		}
		return false;
	}
	
	
	/**
	 * Handle a creator -- should that be included in the combine archive meta
	 * data file?
	 * 
	 * @param author
	 *          the author as stated in the RO manifest
	 * @param descriptions
	 *          meta date already available for the entry
	 * @param creatorsToAdd
	 *          the creators to add
	 */
	private void handleCreator (Agent author, List<MetaDataObject> descriptions,
		List<VCard> creatorsToAdd)
	{
		if (author != null)
		{
			VCard toAdd = agentToVCard (author);
			boolean addVcard = true;
			for (MetaDataObject meta : descriptions)
			{
				if (!addVcard)
					break;
				if (meta instanceof OmexMetaDataObject)
				{
					List<VCard> vcards = ((OmexMetaDataObject) meta)
						.getOmexDescription ().getCreators ();
					if (vcards != null && vcards.size () > 0)
					{
						for (VCard creator : vcards)
						{
							if (sameVcard (creator, toAdd))
							{
								addVcard = false;
								break;
							}
						}
					}
				}
			}
			if (addVcard)
			{
				for (VCard exists : creatorsToAdd)
					if (sameVcard (exists, toAdd))
						return;
				creatorsToAdd.add (toAdd);
			}
		}
	}
	
	
	/**
	 * Handle the creators/authors of an aggregated file.
	 * 
	 * @param entry
	 *          the created archive entry
	 * @param pmd
	 *          the meta data of the entry in the RO manifest
	 */
	private void handleCreators (ArchiveEntry entry, PathMetadata pmd)
	{
		List<MetaDataObject> descriptions = entry.getDescriptions ();
		List<VCard> creatorsToAdd = new ArrayList<VCard> ();
		
		handleCreator (pmd.getCreatedBy (), descriptions, creatorsToAdd);
		if (pmd.getAuthoredBy () != null)
			for (Agent author : pmd.getAuthoredBy ())
				handleCreator (author, descriptions, creatorsToAdd);
		
		if (creatorsToAdd.size () > 0)
		{
			OmexDescription descr = null;
			
			FileTime createdOn = pmd.getCreatedOn ();
			if (createdOn != null)
				descr = new OmexDescription (creatorsToAdd, new Date (
					createdOn.toMillis ()), "converted from Research Object manifest");
			else
				descr = new OmexDescription (creatorsToAdd, new Date (),
					"converted from Research Object manifest");
			
			entry.addDescription (new OmexMetaDataObject (descr));
		}
	}
	
	
	/**
	 * Handle annotations.
	 * 
	 * @param file
	 *          the file
	 * @param pmd
	 *          the pmd
	 * @param annotations
	 *          the annotations
	 * @param entry
	 *          the entry
	 * @param archiveEntries
	 *          the archive entries
	 */
	private void handleAnnotations (Path file, PathMetadata pmd,
		List<PathAnnotation> annotations, ArchiveEntry entry,
		HashMap<String, ArchiveEntry> archiveEntries)
	{
		List<PathAnnotation> curAnnotations = getAnnotations (pmd, annotations);
		for (PathAnnotation annot : curAnnotations)
		{
			if (annot.getContent ().equals (URI_MAIN_ENTRY)
				|| annot.getContent ().equals (URI_BF_MAIN_ENTRY))
			{
				// this is a main entry
				combineArchive.addMainEntry (entry);
				handledAnnotations.add (annot);
				continue;
			}
			
			if (annotationHasOmexTag (annot, annotations))
			{
				handledAnnotations.add (annot);
				// copy it to the list of overall annotations
				try
				{
					// System.out.println (annot.getContent ());
					Path annoPath = researchObject.getRoot ().resolve (
						annot.getContent ().toString ());
					List<String> errors = new ArrayList<String> ();
					MetaDataFile.readFile (annoPath, archiveEntries, combineArchive,
						null, null, true, errors);
					if (errors.size () > 0)
						for (String err : errors)
						{
							LOGGER.warn ("wasn't able to read omex meta file ",
								annot.getContent (), " in research object at ", sourceFile,
								" because ", err);
							notifications
								.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN,
									"wasn't able to read omex meta file " + annot.getContent ()
										+ " in research object at " + sourceFile + " because "
										+ err));
						}
				}
				catch (IOException | ParseException | JDOMException
					| CombineArchiveException e)
				{
					LOGGER.warn (e, "reading meta data file ", annot.getContent (),
						" in research object at ", sourceFile, " failed");
					notifications.add (new CaRoNotification (
						CaRoNotification.SERVERITY_ERROR, "reading meta data file "
							+ annot.getContent () + " in research object at " + sourceFile
							+ " failed because: " + e.getMessage ()));
				}
			}
		}
	}
	
	
	/**
	 * Annotation has omex tag.
	 * 
	 * @param annotation
	 *          the annotation
	 * @param annotations
	 *          the annotations
	 * @return true, if successful
	 */
	private boolean annotationHasOmexTag (PathAnnotation annotation,
		List<PathAnnotation> annotations)
	{
		for (PathAnnotation annot : annotations)
			// does `annot` describe `annotation`?
			if (annot.getAbout ().equals (annotation.getUri ()))
				// is there the flag?
				if (annot.getContent ().equals (URI_OMEX_META))
				{
					handledAnnotations.add (annot);
					return true;
				}
		return false;
	}
	
	
	/**
	 * Gets the annotations.
	 * 
	 * @param pmd
	 *          the pmd
	 * @param annotations
	 *          the annotations
	 * @return the annotations
	 */
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
	 * @param pmd
	 *          the meta date of the file
	 * @return true, if it was included
	 */
	private boolean handleRemoteFile (PathMetadata pmd)
	{
		// TODO: try to download the file?
		LOGGER.warn ("skipping manifest entry ", pmd.getUri (),
			" as it seems to be no local file");
		notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_WARN,
			"skipping manifest entry " + pmd.getUri ()
				+ " as it seems to be no local file"));
		
		return false;
	}
	
	
	/**
	 * Should we include a certain file?.
	 * 
	 * @param target
	 *          the file in question
	 * @return true, if file can be included
	 */
	private boolean includeFile (Path target)
	{
		for (String path : CA_RESTRICTIONS)
			if (target.startsWith (path))
			{
				notifications.add (new CaRoNotification (
					CaRoNotification.SERVERITY_WARN, "dropping " + path
						+ " as this is a special file in combine archives!"));
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
			notifications.add (new CaRoNotification (
				CaRoNotification.SERVERITY_ERROR,
				"wasn't able to save combine archive to " + target + " : "
					+ e.getMessage ()));
		}
		return false;
	}
	
	
	/**
	 * Returns the combine archive.
	 * 
	 * Will be <code>null</code> unless you called
	 * {@link de.unirostock.sems.caro.CaRoConverter#convert()}.
	 * 
	 * @return the converted combine archive
	 */
	public CombineArchive getCombineArchive ()
	{
		return combineArchive;
	}
	
}
