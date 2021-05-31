package com.apichallenge.services;

import javax.json.JsonArray;

import org.springframework.stereotype.Service;

@Service
public interface MercadoLibreService {
	
	public String searchDataImportantString(String keyImportant, String jsonConvert, boolean obtenerMotos, boolean obtenerData);
	public void finalScore(JsonArray listArticulosMotosNuevas);
	
	public static String searchMotosNuevas(String idArticulosNuevos) throws Exception {
		return null;
	}
	
	public static String peticionHttpMercadoLibre(String url) throws Exception {
		return null;
	}
	
}
