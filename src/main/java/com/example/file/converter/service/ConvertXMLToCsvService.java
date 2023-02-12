package com.example.file.converter.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opencsv.CSVWriter;

@Service
public class ConvertXMLToCsvService {

	private static final Logger logger = LoggerFactory.getLogger(ConvertXMLToCsvService.class);

	public static Map<String,String> vrVsOffenderMappingMap = new HashMap<String, String>();

	private static String sourceFilePath;
    private static String destinationFilePath;
    private static String csvFilePath;
    private static String zipFileName;
    
    @Value("${source.file.path}")
    public void setSourceFilePath(String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    @Value("${destination.file.path}")
    public void setDestinationFilePath(String destinationFilePath) {
        this.destinationFilePath = destinationFilePath;
    }

    @Value("${csv.file.path}")
    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }
	
	/**
	 *  Read from XML and write to CSV file
	 */
	public void process(String... args)throws Exception {
		logger.info("Processing start....................");
		//setFilePaths(args);
		
		unzipFiles();
		
		String manifiestFilePath ="Manifest.xml";
		manifiestFilePath = destinationFilePath+"\\" + manifiestFilePath;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Map<String, List<UserDefineNode>> outputMap = new HashMap<String, List<UserDefineNode>>();
		try {
	          DocumentBuilder db = dbf.newDocumentBuilder();
	          Document doc = db.parse(new File(manifiestFilePath));
	          doc.getDocumentElement().normalize();

	          List<String> fileList = new ArrayList<String>();
	          String vrFileName = null;
	          // get <staff>
	          NodeList nodeList = doc.getElementsByTagName("File");
	          for (int index = 0; index < nodeList.getLength(); index++) {
	              Node node = nodeList.item(index);
	              if (node.getNodeType() == Node.ELEMENT_NODE) {

	                  Element element = (Element) node;
	                  //Prefix
	                  String fileName =  element.getElementsByTagName("Name").item(0).getTextContent();
	                  fileName = destinationFilePath+"\\" + fileName;
	                  System.out.println("Reading file from..."+fileName);
	                  if(fileName.contains(".VR.")) {
	                	  vrFileName = fileName;
	                  }
	                  fileList.add(fileName);
	                  //
	              }
	          }
	          //First populate mapping
	          populateVrMappingMap(vrFileName);
	          //Load XML file to Map
	          for(String fileName : fileList) {
	        	  readXMLFile(fileName, outputMap); 
	          }
		}catch(Exception e) {
			logger.error("Error while reading file names from Manifiest file..Existing."+e.getMessage());
		}
				
        //write to CSV File
        writeToCsvFile(outputMap);
        
		logger.info("Processing end...............................");

	}

	public void setFilePaths(String... args) throws Exception {
		if(args.length < 3) {
			throw new Exception("Please enter correct ..");
		}
	}
	
	private void populateVrMappingMap(String vrFileName) {
        if(null != vrFileName) {
	          //add VR Mapping
	  		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();		      
			try {
		          DocumentBuilder db = dbf.newDocumentBuilder();
		          Document doc = db.parse(new File(vrFileName));
		          doc.getDocumentElement().normalize();
		          NodeList nodeList = doc.getElementsByTagName("INMATE");
		          for (int temp = 0; temp < nodeList.getLength(); temp++) {
		              Node node = nodeList.item(temp);
		              if (node.getNodeType() == Node.ELEMENT_NODE) {
		                  Element element = (Element) node;
		                  //Prefix
		                  String victimId =  element.getElementsByTagName("VICTIM_ID").item(0).getTextContent();
		                  String offenderId =  element.getElementsByTagName("OFFENDER_ID").item(0).getTextContent();
		                  vrVsOffenderMappingMap.put(victimId, offenderId);
		              }
		          }
			}catch (Exception e) {
				e.printStackTrace();
			}	        	  
        }
	}
	
	private Map<String, List<UserDefineNode>> readXMLFile(String fileName,
			Map<String, List<UserDefineNode>> outputMap) {
	    
		logger.info("Processing file starts."+fileName);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();	      
		try {
	          DocumentBuilder db = dbf.newDocumentBuilder();
	          Document doc = db.parse(new File(fileName));
	          doc.getDocumentElement().normalize();

	          // get <staff>
	          NodeList nodeList = doc.getElementsByTagName("INMATE");

	          for (int temp = 0; temp < nodeList.getLength(); temp++) {
	        	  List<UserDefineNode> listOfNode = new ArrayList<>();
	        	  
	              Node node = nodeList.item(temp);
	              if (node.getNodeType() == Node.ELEMENT_NODE) {

	                  Element element = (Element) node;
	                  //Prefix
	                  String prefix =  element.getElementsByTagName("HEADER").item(0).getTextContent();	                  
	                  String offenderId = null;
	                  
	                  if("VA".equalsIgnoreCase(prefix)) {
	                	  String victimId = element.getElementsByTagName("VICTIM_ID").item(0).getTextContent();
	                	  logger.info("Victim_ID will become offender ID for this VA file.");
	                	  offenderId = vrVsOffenderMappingMap.get(victimId);
	                  }else if("ME".equalsIgnoreCase(prefix)) {
	                	  String oldOffenderId = element.getElementsByTagName("OLD_OFFENDER_ID").item(0).getTextContent();
	                	  String newOffenderId = element.getElementsByTagName("NEW_OFFENDER_ID").item(0).getTextContent();
	                	  //If there is existing offenderId with newOffenderId mapp it or else check with oldOffenderId. 
	                	  //If no matches found then select new one.
	                	  if(outputMap.containsKey(newOffenderId)) {
		                	  logger.info("NEW_OFFENDER_ID will become offender ID for this ME file.");
	                		  offenderId =   newOffenderId;
	                	  }else if(outputMap.containsKey(oldOffenderId)) {
		                	  logger.info("OLD_OFFENDER_ID will become offender ID for this ME file.");
	                		  offenderId =   oldOffenderId;
	                	  }else {
	                		  offenderId = newOffenderId;
	                	  }
	                  }else {
	                	  offenderId = element.getElementsByTagName("OFFENDER_ID").item(0).getTextContent();
	                	  if(null == offenderId) {
	                		  logger.warn("Offender_ID is missing so considering Book_Number..");
		                	  offenderId = element.getElementsByTagName("BOOK_NUMBER").item(0).getTextContent();	                		  
	                	  }
	                  }
                	  if(offenderId == null) {
                		  logger.error("No Offender ID found for this INMATE. Please check the input. Will proceed with other records");
                		  continue;
                	  }
	                  addNode(prefix, element, listOfNode);
	                  
	                  //Check if node already exists addAll
	                  if(outputMap.get(offenderId) == null) {
	                	  outputMap.put(offenderId, listOfNode);
	                  }else {
	                	  List<UserDefineNode> existingList = outputMap.get(offenderId);
	                	  existingList.addAll(listOfNode);
	                	  outputMap.put(offenderId, existingList);
	                  }
	              }
	          }
	  		logger.info("Processing file successful for "+fileName);

		}catch(Exception e) {
			logger.error("Error while procesing file "+fileName +" "+e.getMessage());
		}
		return outputMap;
	}
	
	private void addNode(String prefix, 
			Node node, 
			List<UserDefineNode> listOfNode ) {
		
        if (node.hasChildNodes()){
      	  NodeList childNodeList = node.getChildNodes();
      	  List<String> duplicateList = new ArrayList<String>();
      	  for (int i = 0; i < childNodeList.getLength(); i++) {
          	  Node elemNode = childNodeList.item(i);
          	  if(Node.ELEMENT_NODE == elemNode.getNodeType()) {
          		  /*if(null != elemNode.getTextContent() && 
          				  !"".equals(elemNode.getTextContent().trim())) { */
          			  
          			  String nodeName = elemNode.getNodeName();
          	      	  String value = elemNode.getTextContent();

          	      	  if(duplicateList.contains(nodeName)) {
          	      		  continue;
          	      	  }
          			  //Handle Duplicate
          			  Element element = (Element)node;
          			  NodeList nodeListInner =  element.getElementsByTagName(nodeName);	                  
          	          int length = nodeListInner.getLength();
          	          //If it has duplicate
          	          if(length>1) {
          	        	  duplicateList.add(nodeName);
    	        		  List<String> valueList = new ArrayList<>();
    	        		  int counter = 0;
          	        	  for(int k=0; k<length; k++) {
        	        		  String valueInner = nodeListInner.item(k).getTextContent();
        	        		  if(!valueList.contains(valueInner)) {
            	        		  valueList.add(valueInner);        	        			  
        	        		  }
          	        	  }
          	        	  for(String val : valueList) {
                  	      	  String header = prefix+"_"+nodeName;
          	        		  if(counter != 0) {
          	        			header = header+"_"+counter;
          	        		  }
                  	      	  UserDefineNode userDefineNode = new UserDefineNode();
                  	      	  
                  	      	  userDefineNode.setHeader(header);
                  	      	  userDefineNode.setValue(val);
                  	      	  listOfNode.add(userDefineNode);  
                  	      	  ++counter;
          	        	  }
          	          }else {
              	      	  String header = prefix+"_"+nodeName;
              	      	  UserDefineNode userDefineNode = new UserDefineNode();
              	      	  
              	      	  userDefineNode.setHeader(header);
              	      	  userDefineNode.setValue(value);
              	      	  listOfNode.add(userDefineNode);          	        	  
          	          }
          	          
          		  }
          	  //}
      	  }
        }  		
	}
	
	private void writeToCsvFile(Map<String, List<UserDefineNode>> outputMap) {
		List<String[]> csvData = createCsvDataSimple(outputMap);
		try {
			String filePath = csvFilePath + zipFileName +".csv";
	        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
	            writer.writeAll(csvData);
	        }
			
		}catch(Exception e) {
			logger.error("Exception while writing to CSV .. "+e.getMessage());
		}
	}
	
	private List<String[]> createCsvDataSimple(Map<String, List<UserDefineNode>> outputMap) {
		List<String[]> outputList = new ArrayList<>();
		int count = 0;
    	List<String> header  = new ArrayList<>();
    	header.add("Id");
        for (Map.Entry<String, List<UserDefineNode>> entry : outputMap.entrySet()) {
        	List<String> value = new ArrayList<>();
        	String offenderId = entry.getKey();
        	//First column value should be offenderId
        	value.add(offenderId);
        	List<UserDefineNode> valueList = entry.getValue();
        	if(count != 0) {
        		addEmptyValueToList(value, count);
        	}
        	count = count+valueList.size();
        	for(UserDefineNode node: valueList) {
        		if(header.contains(node.getHeader())) {
        			value.add(header.indexOf(node.getHeader()), node.getValue());
        		}else {
            		header.add(node.getHeader());
            		value.add(node.getValue());        			
        		}
        	}
            String[] valueArr = value.toArray(new String[value.size()]);        	
        	outputList.add(valueArr);
        }
        String[] array = header.toArray(new String[header.size()]);
        outputList.add(0, array);
		return outputList;
	}
	
	private void addEmptyValueToList(List<String> value, int count) {
		for(int i=0;i<count; i++) {
			value.add("");
		}
	}
	
	
    private static void unzipFiles() throws Exception {
    	
    	logger.info("Extracting .zip file "+sourceFilePath +" to "+destinationFilePath);
    	
        File destDir = new File(destinationFilePath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        FileInputStream fileInputStream;
        byte[] buffer = new byte[1024];
        try {
        	File sourceDir = new File(sourceFilePath);
            fileInputStream = new FileInputStream(sourceDir);
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            zipFileName = sourceDir.getName();
            zipFileName = zipFileName.substring(0,zipFileName.indexOf(".zip")); 

            while (zipEntry != null) {
            	String fileName = zipEntry.getName();
                File newFile = new File(destDir + File.separator + fileName);
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.close();
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
        	logger.info("File Extracted successfully");
            zipInputStream.closeEntry();
            zipInputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
        	logger.info("Error while unzipping file. Programme will terminate "+e.getMessage());
        	throw e;
        }
    }

}
