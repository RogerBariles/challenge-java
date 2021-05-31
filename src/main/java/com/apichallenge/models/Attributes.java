package com.apichallenge.models;

import java.util.List;

public class Attributes extends Categories {

	private List<Categories> values; 
	
	public Attributes(String id, String name) {
		super(id, name);
	}

	public List<Categories> getValues() {
		return values;
	}

	public void setValues(List<Categories> values) {
		this.values = values;
	}

	
}
