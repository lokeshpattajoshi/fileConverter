package com.example.file.converter.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		Map<String, List<List<UserDefineNode>>> outputMap = new HashMap<String, List<List<UserDefineNode>>>();
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

		//Custom Sorting based on key of TreeMap        
		final Pattern p = Pattern.compile("^\\d+");
        Comparator<String> customSort = new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                Matcher m = p.matcher(object1);
                Integer number1 = null;
                if (!m.find()) {
                    Matcher m1 = p.matcher(object2);
                    if (m1.find()) {
                        return object2.compareTo(object1);
                    } else {
                        return object1.compareTo(object2);
                    }
                } else {
                    Integer number2 = null;
                    number1 = Integer.parseInt(m.group());
                    m = p.matcher(object2);
                    if (!m.find()) {
                        // return object1.compareTo(object2);
                        Matcher m1 = p.matcher(object1);
                        if (m1.find()) {
                            return object2.compareTo(object1);
                        } else {
                            return object1.compareTo(object2);
                        }
                    } else {
                        number2 = Integer.parseInt(m.group());
                        int comparison = number1.compareTo(number2);
                        if (comparison != 0) {
                            return comparison;
                        } else {
                            return object1.compareTo(object2);
                        }
                    }
                }
            }
        };

		
        //write to CSV File
		TreeMap<String, List<List<UserDefineNode>>> sortedMap = new TreeMap<String, List<List<UserDefineNode>>>(customSort);

		sortedMap.putAll(outputMap);
        writeToCsvFile(sortedMap);
        
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
	
	private Map<String, List<List<UserDefineNode>>> readXMLFile(String fileName,
			Map<String, List<List<UserDefineNode>>> outputMap) {
	    
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

		                 addNode(prefix, element, listOfNode, null, null);
		                  if(outputMap.get(offenderId) == null) {
		                	  List<List<UserDefineNode>> listOfList = new ArrayList<>();
		                	  listOfList.add(listOfNode);
		                	  outputMap.put(offenderId, listOfList);
		                  }else {
	                		  List<List<UserDefineNode>> listOfList = outputMap.get(offenderId);
	                		  listOfList.get(0).addAll(listOfNode);
		                	  outputMap.put(offenderId, listOfList);	                		  
		                  }

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
	                	  if(offenderId == null) {
	                		  logger.error("No Offender ID found for this INMATE. Please check the input. Will proceed with other records");
	                		  continue;
	                	  }
		                  addNode(prefix, element, listOfNode, null, null);
		                  if(outputMap.get(offenderId) == null) {
		                	  List<List<UserDefineNode>> listOfList = new ArrayList<>();
		                	  listOfList.add(listOfNode);
		                	  outputMap.put(offenderId, listOfList);
		                  }else {
	                		  List<List<UserDefineNode>> listOfList = outputMap.get(offenderId);
	                		  listOfList.get(0).addAll(listOfNode);
		                	  outputMap.put(offenderId, listOfList);	                		  
		                  }
	                  }else {
	                	  offenderId = element.getElementsByTagName("OFFENDER_ID").item(0).getTextContent();
	                	  if(offenderId == null) {
	                		  logger.error("No Offender ID found for this INMATE. Please check the input. Will proceed with other records");
	                		  continue;
	                	  }
	                	  String bookNumber = element.getElementsByTagName("BOOK_NUMBER").item(0).getTextContent();	                		  
	                	  addNode(prefix, element, listOfNode, offenderId, bookNumber);
		                  
		                  if(outputMap.get(offenderId) == null) {
		                	  List<List<UserDefineNode>> listOfList = new ArrayList<>();
		                	  listOfList.add(listOfNode);
		                	  outputMap.put(offenderId, listOfList);
		                  }else {
	                		  List<List<UserDefineNode>> listOfList = outputMap.get(offenderId);
	                		  List<UserDefineNode> listTobeAdded = checkIfBookNumberExistsForOffenderId(bookNumber, 
																	                				  offenderId, 
																	                				  prefix, 
																	                				  listOfList,
																	                				  listOfNode);
		                	  if(null != listTobeAdded) {
		                		  listTobeAdded.addAll(listOfNode);
			                	  outputMap.put(offenderId, listOfList);	                		  
		                	  }else {
		                		  listOfList.add(listOfNode);
			                	  outputMap.put(offenderId, listOfList);	                		  
		                	  }
		                  }
	                  }	     

	              }
	          }
	  		logger.info("Processing file successful for "+fileName);

		}catch(Exception e) {
			logger.error("Error while procesing file "+fileName +" "+e.getMessage());
		}
		return outputMap;
	}		
	
	private List<UserDefineNode> checkIfBookNumberExistsForOffenderId(String bookNumber, 
																		String offenderId, 
																		String prefix, 
																		List<List<UserDefineNode>> listOfList,
																		List<UserDefineNode> newNode) {
		for(List<UserDefineNode> listOfNode: listOfList) {
			for(UserDefineNode node: listOfNode) {
				if(null != node.getBookNumber() 
						&& null != node.getOffenderId()
						&& node.getBookNumber().equalsIgnoreCase(bookNumber)
						&& node.getOffenderId().equalsIgnoreCase(offenderId) ) {
					if(null != node.getValue() 
							&& node.getValue().equalsIgnoreCase(prefix)) {
						//If duplicate exists in same file then create new records.
						return null;
					}else {
						//Suppose OA has 2 records with 200-4000 and A already has that then second record should be created in new row
						//Check same record has already filled up so create new record
						if(checkIfAlreadyRecordExists(prefix, listOfNode)) {
							return null;
						}
						//If duplicate exists in different file then merge
						return listOfNode;
					}
					
				}
			}
		}
		return null;
	}

	private boolean checkIfAlreadyRecordExists(String prefix, 
			List<UserDefineNode> listOfNode) {
		for(UserDefineNode node: listOfNode) {
			if(node.getHeader().startsWith(prefix+"_") 
					&& null != node.getValue()) {
				return true;
			}
		}
		return false;
	}
	private void addNode(String prefix, 
			Node node, 
			List<UserDefineNode> listOfNode,
			String offenderId,
			String bookNumber) {
		
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
          	      	  //We have logic below to read all the duplicate elements if there is any so below logic is to skip we find it again
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
                  	      	  userDefineNode.setBookNumber(bookNumber);
                  	      	  userDefineNode.setOffenderId(offenderId);
                  	      	  userDefineNode.setHeader(header);
                  	      	  userDefineNode.setValue(val);
                  	      	  listOfNode.add(userDefineNode);  
                  	      	  ++counter;
          	        	  }
          	          }else {
              	      	  String header = prefix+"_"+nodeName;
              	      	  UserDefineNode userDefineNode = new UserDefineNode();
              	      	  userDefineNode.setBookNumber(bookNumber);
              	      	  userDefineNode.setOffenderId(offenderId);
              	      	  userDefineNode.setHeader(header);
              	      	  userDefineNode.setValue(value);
              	      	  listOfNode.add(userDefineNode);          	        	  
          	          }
          	          
          		  }
          	  //}
      	  }
        }  		
	}
	
	private void writeToCsvFile(Map<String,List<List<UserDefineNode>>> outputMap) {
		List<String[]> csvData = createCsvDataSimple(outputMap);
		try {
			String filePath = csvFilePath + zipFileName +".csv";
			logger.info("File Path where CSV file will be created is "+filePath);
	        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
	            writer.writeAll(csvData);
	        }
			
		}catch(Exception e) {
			logger.error("Exception while writing to CSV .. "+e.getMessage());
		}
	}
	
	private List<String[]> createCsvDataSimple(Map<String, List<List<UserDefineNode>>> outputMap) {
		List<String[]> outputList = new ArrayList<>();
		int count = 0;
    	List<String> header  = new ArrayList<>();
    	header.add("Id");
    	//Create Header first
    	for (Map.Entry<String, List<List<UserDefineNode>>> entry : outputMap.entrySet()) {
        	List<List<UserDefineNode>> nodeList = entry.getValue();
        	for(List<UserDefineNode> nodeListInner: nodeList) {
        		for(UserDefineNode node: nodeListInner)
        		if(!header.contains(node.getHeader())) {
            		header.add(node.getHeader());
        		}
        	}
    	}
    	
    	//Create values to row
    	
        for (Map.Entry<String, List<List<UserDefineNode>>> entryList : outputMap.entrySet()) {
        	for(List<UserDefineNode> nodeList: entryList.getValue()) {
            	List<String> value = new ArrayList<>();
            	String offenderId = entryList.getKey();
            	//First column value should be offenderId
            	value.add(offenderId);
            	//Initialize list to empty String
            	addEmptyValueToList(value, header.size());
            	for(UserDefineNode node: nodeList) {
        			int index = header.indexOf(node.getHeader());
        			value.set(index, node.getValue());
            	}
                String[] valueArr = value.toArray(new String[value.size()]);        	
            	outputList.add(valueArr);        		
        	}
        }
        
        String[] array = header.toArray(new String[header.size()]);
        outputList.add(0, array);
		return outputList;
	}
	
	private void addEmptyValueToList(List<String> value, int count) {
		for(int i=1;i<count; i++) {
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
