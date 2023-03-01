package com.example.file.converter.service;

public class UserDefineNode {

	private String header;
	private String value;
	private String offenderId;
	private String bookNumber;
	
	
	public String getHeader() {
		return header;
	}
	public void setHeader(String header) {
		this.header = header;
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getOffenderId() {
		return offenderId;
	}
	public void setOffenderId(String offenderId) {
		this.offenderId = offenderId;
	}
	public String getBookNumber() {
		return bookNumber;
	}
	public void setBookNumber(String bookNumber) {
		this.bookNumber = bookNumber;
	}
	@Override
	public String toString() {
		return "UserDefineNode [header=" + header + ", value=" + value + ", offenderId=" + offenderId + ", bookNumber="
				+ bookNumber + "]";
	}
	
	
}
