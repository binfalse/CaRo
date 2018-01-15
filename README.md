# CaRo -- CombineArchive & ResearchObject

This is going to be a converter between [COMBINE archives](http://co.mbine.org/documents/archive) and [Research objects](http://www.researchobject.org/).


## How the Tanslation is done

### Terms
* the documents in a container are called aggregates


### Research Object to Combine Archive

* For every aggregate:
	* the document is extracted to a temporary file
	* if the document is called /metadata.rdf or /manifest.xml we won't add it to the new Combine Archive, as thoses are special files in COMBINE archives
	* if the format is not provided by the research object: it is guessed using the Combine Ext's formatizer
	* a new combine archive entry is added to the new combine archive
	* annotations of the aggregate are translated into combine archive meta data
		* if the files is annotated with 
	

* remote files?


### Combine Archive to Research Object


## Rules

* History files of a Research Object will stay in their locations. Thus, the CA will probably contain files in /.ro/ directory



