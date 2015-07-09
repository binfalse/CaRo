/**
 * 
 */
package de.unirostock.sems.caro.converters;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathMetadata;
import org.jdom2.JDOMException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.caro.CaRoConverter;
import de.unirostock.sems.caro.CaRoNotification;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
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
			combineArchive = new CombineArchive (temporaryLocation);
			
			// read bundle stuff
			Manifest mf = researchObject.getManifest ();
			List<PathMetadata> aggregations = mf.getAggregates ();
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
				tmp.deleteOnExit ();
				Files.copy (pmd.getFile (), tmp.toPath ());
				// import
				URI format = pmd.getConformsTo ();
				if (format == null)
				{
					if (pmd.getMediatype () != null)
						format = new URI ("http://purl.org/NET/mediatypes/" + pmd.getMediatype ());
					else
						format = Formatizer.guessFormat (tmp);
				}
				combineArchive.addEntry (tmp, pmd.getFile ().toString (), format);
				
				// respect annotations and special files
			}
			
			return true;
		}
		catch (IOException | JDOMException | ParseException | CombineArchiveException | URISyntaxException e)
		{
			LOGGER.warn (e, "wasn't able to convert research object at ", sourceFile, " into a combine archive");
			notifications.add (new CaRoNotification (CaRoNotification.SERVERITY_ERROR, "wasn't able to convert research object at " + sourceFile + " into a combine archive : " + e.getMessage ()));
		}
		return false;
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
