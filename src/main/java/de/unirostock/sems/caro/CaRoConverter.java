/**
 * 
 */
package de.unirostock.sems.caro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.taverna.robundle.Bundle;

import de.unirostock.sems.cbarchive.CombineArchive;


/**
 * The abstract class CaRoConverter defines the infrastructure for converting containers.
 *
 * @author Martin Scharm
 */
public abstract class CaRoConverter
{
	
	/** The source file. */
	protected File sourceFile;
	
	/** The combine archive. */
	protected CombineArchive combineArchive;
	
	/** The research object. */
	protected Bundle researchObject;
	
	/** The notifications. */
	protected List<CaRoNotification> messages;
	
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
		messages = new ArrayList <CaRoNotification> ();
	}
	
	/**
	 * Checks for errors.
	 *
	 * @return true, if the converter reported errors
	 */
	public boolean hasErrors ()
	{
		for (CaRoNotification crn : messages)
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
		for (CaRoNotification crn : messages)
			if (crn.getSeverity () == CaRoNotification.SERVERITY_WARN)
				return true;
		return false;
	}

	/**
	 * Read the container.
	 *
	 * @return true, if reading was successful
	 */
	public abstract boolean read ();
	
	/**
	 * Convert the container.
	 *
	 * @return true, if converting was successful
	 */
	public abstract boolean convert ();
	
	/**
	 * Write the container.
	 *
	 * @param target the target file to write to
	 * @return true, if exporting was successful
	 */
	public abstract boolean write (File target);
}
