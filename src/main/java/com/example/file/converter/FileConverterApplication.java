package com.example.file.converter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.file.converter.service.ConvertXMLToCsvService;

@SpringBootApplication
public class FileConverterApplication implements CommandLineRunner{

	public static void main(String[] args) {
		SpringApplication.run(FileConverterApplication.class, args);
	}

	
	@Override
	public void run(String... args) throws Exception {
		ConvertXMLToCsvService convertXMLToCsvService = new ConvertXMLToCsvService();
		convertXMLToCsvService.process(args);
		
	}

}
