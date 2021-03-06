package org.auscope.portal.server.web.service.csw;

import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.responses.csw.CSWRecordTransformer;
import org.auscope.portal.core.services.responses.csw.CSWRecordTransformerFactory;
import org.w3c.dom.Node;

public class GriddedCSWRecordTransformerFactory extends
        CSWRecordTransformerFactory {
    /**
     * Creates a new instance of GriddedCSWRecordTransformer which will draw from the specified
     * gmd:MD_Metadata Node representation as a template
     * @param mdMetadataNode
     * @return
     */
    @Override
    public CSWRecordTransformer newCSWRecordTransformer(Node mdMetadataNode) {
        return new GriddedCSWRecordTransformer(mdMetadataNode);
    }
    
    /**
     * Creates a new instance of GriddedCSWRecordTransformer and generates an empty document that will be
     * used for constructing DOM.
     * @return
     * @throws PortalServiceException 
     */
    @Override
    public CSWRecordTransformer newCSWRecordTransformer() throws PortalServiceException  {
        return new GriddedCSWRecordTransformer();
    }
}
