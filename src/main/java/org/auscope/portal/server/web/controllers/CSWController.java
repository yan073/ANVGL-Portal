package org.auscope.portal.server.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.auscope.portal.server.web.service.CSWService;
import org.auscope.portal.csw.CSWRecord;
import org.auscope.portal.server.web.view.JSONModelAndView;
import org.auscope.portal.server.util.PortalPropertyPlaceholderConfigurer;
import org.apache.log4j.Logger;
import net.sf.json.JSONArray;

/**
 * User: Mathew Wyatt
 * Date: 23/06/2009
 * Time: 4:57:12 PM
 */
@Controller
public class CSWController {

    private Logger logger = Logger.getLogger(getClass());
    @Autowired private CSWService cswService;
    
    private KnownType[] knownTypes = {  new KnownType("er:Mine", "Earth Resource Mine", "", "/doMineFilter.do", "http://maps.google.com/mapfiles/kml/paddle/pink-blank.png"),
                                        new KnownType("er:MineralOccurrence", "Earth Resource Mineral Occurrence", "", "/doMineralOccurrenceFilter.do", "http://maps.google.com/mapfiles/kml/paddle/purple-blank.png"),
                                        new KnownType("er:MiningActivity", "Earth Resource Mining Activity", "", "/doMiningActivityFilter.do", "http://maps.google.com/mapfiles/kml/paddle/orange-blank.png"),
                                        new KnownType("gsml:Borehole", "National Virtual Core Library", "", "/getAllFeatures.do", "http://maps.google.com/mapfiles/kml/paddle/blu-blank.png"),
                                        new KnownType("geodesy:stations", "Geodesy", "", "/getAllFeatures.do", "http://maps.google.com/mapfiles/kml/paddle/wht-blank.png")};

    /**
     * Construct
     * @param prtalPropertyPlaceholderConfigurer
     */
    @Autowired
    public CSWController(PortalPropertyPlaceholderConfigurer prtalPropertyPlaceholderConfigurer) {
        cswService.setServiceUrl(prtalPropertyPlaceholderConfigurer.resolvePlaceholder("HOST.cswservice.url"));

        try {
            cswService.updateRecordsInBackground();
        } catch (Exception e) {
            logger.error(e);
        }

    }

    /**
     * This controller queries geonetwork for all of its data records, then created a JSON response as a list
     * which can then be put into a table.
     *
     * Returns a JSON response with a data structure like so
     *
     * [
     * [title: "", description: "", proxyURL: "", serviceType: "", id: "", typeName: "", serviceURLs],
     * [title: "", description: "", proxyURL: "", serviceType: "", id: "", typeName: "", serviceURLs]
     * ]
     *
     * @return
     */
    @RequestMapping("/getComplexFeatures.do")
    public ModelAndView getComplexFeatures() throws Exception {
        //the main holder for the items
        JSONArray dataItems = new JSONArray();

        for(KnownType knownType : knownTypes) {
            //Add the mineral occurrence
            JSONArray tableRow = new JSONArray();

            //add the name of the layer/feature type
            tableRow.add(knownType.displayName);

            CSWRecord[] records = cswService.getWFSRecordsForTypename(knownType.featureTypeName);
            String servicesDescription = "\nServices from: \n";
            JSONArray serviceURLs = new JSONArray();

            if(records.length == 0)
                break;

            for(CSWRecord record : records) {
                serviceURLs.add(record.getServiceUrl());
                servicesDescription += record.getContactOrganisation() + "\n";
                //serviceURLs.add("http://www.gsv-tb.dpi.vic.gov.au/AuScope-MineralOccurrence/services");
                //serviceURLs.add("http://auscope-services.arrc.csiro.au/deegree-wfs/services");
            }

            //add the abstract text to be shown as updateCSWRecords description
            tableRow.add(knownType.description + servicesDescription);

            //add the service URL - this is the spring controller for handling minocc
            tableRow.add(knownType.proxyUrl);

            //add the type: wfs or wms
            tableRow.add("wfs");

            //TODO: add updateCSWRecords proper unique id
            tableRow.add(knownType.hashCode());

            //add the featureType name (in case of updateCSWRecords WMS feature)
            tableRow.add(knownType.featureTypeName);

            tableRow.add(serviceURLs);

            tableRow.add("true");
            tableRow.add("<img src=\"js/external/ext-2.2/resources/images/default/grid/done.gif\">");

            tableRow.add("<img width=\"16\" heigh=\"16\" src=\"" + knownType.iconUrl + "\">");
            tableRow.add(knownType.iconUrl);

            dataItems.add(tableRow);
        }

        return new JSONModelAndView(dataItems);
    }

    @RequestMapping("/getWMSLayers.do")
    public ModelAndView getWMSLayers() throws Exception {

        //the main holder for the items
        JSONArray dataItems = new JSONArray();

        CSWRecord[] records = cswService.getWMSRecords();

        for(CSWRecord record : records) {
            //Add the mineral occurrence
            JSONArray tableRow = new JSONArray();

            //add the name of the layer/feature type
            tableRow.add(record.getOnlineResourceName());

            //add the abstract text to be shown as updateCSWRecords description
            tableRow.add(record.getOnlineResourceDescription());

            //wms dont need updateCSWRecords proxy url
            tableRow.add("");

            //add the type: wfs or wms
            tableRow.add("wms");

            //TODO: add updateCSWRecords proper unique id
            tableRow.add(record.hashCode());

            //add the featureType name (in case of updateCSWRecords WMS feature)
            tableRow.add(record.getOnlineResourceName());

            JSONArray serviceURLs = new JSONArray();

            serviceURLs.add(record.getServiceUrl());

            tableRow.add(serviceURLs);

            tableRow.add("true");
            tableRow.add("<img src=\"js/external/ext-2.2/resources/images/default/grid/done.gif\">");

            dataItems.add(tableRow);
        }

        return new JSONModelAndView(dataItems);
    }

    class KnownType {

        KnownType(String featureTypeName, String displayName, String description, String proxyUrl, String iconUrl) {
            this.featureTypeName = featureTypeName;
            this.displayName = displayName;
            this.description = description;
            this.proxyUrl = proxyUrl;
            this.iconUrl = iconUrl;
        }

        public String featureTypeName;
        public String displayName;
        public String description;
        public String proxyUrl;
        public String iconUrl;
    }
}
