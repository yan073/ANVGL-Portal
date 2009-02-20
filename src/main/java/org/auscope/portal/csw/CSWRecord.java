package org.auscope.portal.csw;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;

/**
 * User: Mathew Wyatt
 * Date: 11/02/2009
 * Time: 11:58:21 AM
 */
public class CSWRecord {
    private Node recordNode;

    public CSWRecord(Node node) {
        this.recordNode = node;
    }

    public String getServiceName() throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new CSWNamespaceContext());
        String serviceTitleExpression = "/csw:GetRecordsResponse/csw:SearchResults/gmd:MD_Metadata/gmd:identificationInfo/srv:SV_ServiceIdentification/gmd:citation/gmd:CI_Citation/gmd:title";
        Node node = (Node) xPath.evaluate(serviceTitleExpression, recordNode, XPathConstants.NODE);
        return node.getTextContent();
    }

    public String getServiceUrl() throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new CSWNamespaceContext());
        String serviceUrleExpression = "/csw:GetRecordsResponse/csw:SearchResults/gmd:MD_Metadata/gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions/gmd:onLine/gmd:CI_OnlineResource/gmd:linkage";
        Node node = (Node) xPath.evaluate(serviceUrleExpression, recordNode, XPathConstants.NODE);
        return node.getTextContent();        
    }
    
}