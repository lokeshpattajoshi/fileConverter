package com.example.file.converter.service;

public class UserDefineNode {

	private String header;
	
	
	public String getHeader() {
		return header;
	}
	public void setHeader(String header) {
		this.header = header;
	}
	private String value;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "NodeValue [header=" + header + ", value=" + value + "]";
	}
	
	
	
}
