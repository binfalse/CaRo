/**
 * 
 */
package de.unirostock.sems.caro;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.taverna.robundle.Bundle;
import org.jdom2.Namespace;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.CombineArchive;


/**
 * The abstract class CaRoConverter defines the infrastructure for converting containers.
 *
 * @author Martin Scharm
 */
public abstract class CaRoConverter
{
	public static final String [] RO_RESTRICTIONS = new String [] {
		"/.ro/manifest.json",
		"/META-INF/container.xml",
		"/META-INF/manifest.xml",
		"/mimetype"
		};
	
	public static Namespace RDF_NAMESPACE = Namespace.getNamespace ("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	
	public static URI URI_TURTLE_MIME;
	public static URI URI_OMEX_META;
	public static URI URI_MAIN_ENTRY;
	public static URI URI_CA_RO_CONV;
	
	static {
		try
		{
			URI_TURTLE_MIME = new URI ("http://purl.org/NET/mediatypes/text/turtle");
			URI_OMEX_META = new URI ("http://sems.uni-rostock.de/CaRo/annotations#omexMeta");
			URI_MAIN_ENTRY = new URI ("http://sems.uni-rostock.de/CaRo/annotations#mainEntry");
			URI_CA_RO_CONV = new URI ("http://sems.uni-rostock.de/CaRo/annotations#ca2ro");
		}
		catch (URISyntaxException e)
		{
			LOGGER.error (e, "cannot create turtle URI");
		}
	}
	
	
	/** The source file. */
	protected File sourceFile;
	
	/** The combine archive. */
	protected CombineArchive combineArchive;
	
	/** The research object. */
	protected Bundle researchObject;
	
	/** The notifications. */
	protected List<CaRoNotification> notifications;
	
	/**
	 * The Constructor.
	 *
	 * @param sourceFile the source file
	 */
	public CaRoConverter (File sourceFile)
	{
		this.sourceFile = sourceFile;
		combineArchive = null;
		researchObject = null;
		notifications = new ArrayList <CaRoNotification> ();
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
	 * Checks for warnings. Might return <code>false</code> even if {@link #hasErrors()} returns <code>true</code>.
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
	 * @param target the target file to write to
	 * @return true, if exporting was successful
	 */
	protected abstract boolean write (File target);
	
	public boolean convertTo (File target)
	{
		// TODO
		openSourceContainer ();
		convert ();
		closeSourceContainer ();
		write (target);
		return true;
	}
}
