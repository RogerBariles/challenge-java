package com.apichallenge.controllers;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apichallenge.services.impl.MercadoLibreServiceImpl;

@RestController
@RequestMapping(value="list")
public class MercadoLibreController {
	
	@Autowired
	private MercadoLibreServiceImpl mercadoLibreServiceImpl;
	
	private static String urlMELI = "https://api.mercadolibre.com/";
	
	private static String siteAR = "MLA";
	
	@GetMapping(path="/{search}")
	public ResponseEntity<Map<String, Double>> categories(@PathVariable("search") String search) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Type", "application/json; charset=utf-8");
		
		mercadoLibreServiceImpl.offset = new Long(0);
		
		// BUSCAMOS LAS CATEGORIAS
		String url = urlMELI + "sites/" + siteAR + "/categories";
		String resultDataCategorias = MercadoLibreServiceImpl.peticionHttpMercadoLibre(url);
		if(resultDataCategorias == null) {
			return new ResponseEntity<Map<String, Double>>(null, headers ,HttpStatus.BAD_REQUEST);
		}
		// OBTENEMOS ID  CORRESPONDIENTE A LAS MOTOS ----------------------------------------------
		String idCategoria = mercadoLibreServiceImpl.searchDataImportantString(search, resultDataCategorias, false, false);
		
		// COMO EL idCategoria CORRESPONDE TANTO A MOTOS Y AUTOS, DEBEMOS BUSCAR NUEVAMENTE EL ID CORRECTO PARA SOLAMENTE MOTOS
		url = urlMELI + "/categories/" + idCategoria;
		String resultDataByCategoriaId = MercadoLibreServiceImpl.peticionHttpMercadoLibre(url);
		if(resultDataByCategoriaId == null) {
			return new ResponseEntity<Map<String, Double>>(null, headers ,HttpStatus.BAD_REQUEST);
		}
		String idMotoCategoria = mercadoLibreServiceImpl.searchDataImportantString(search, resultDataByCategoriaId, true, false);
		idMotoCategoria = idMotoCategoria.substring(1, idMotoCategoria.length()-1);
		
		// HACEMOS SOLO UNA PETICION PARA PODER OBTENER EL ID CORRESPONDIENTE A AQUELLOS ARTICULOS NUEVOS
		url = urlMELI + "sites/" + siteAR + "/search?category=" + idMotoCategoria + "&limit=1";;
		String resultDataMotos = MercadoLibreServiceImpl.peticionHttpMercadoLibre(url);
		if(resultDataMotos == null) {
			return new ResponseEntity<Map<String, Double>>(null, headers ,HttpStatus.BAD_REQUEST);
		}
		
		String idArticulosNuevos = mercadoLibreServiceImpl.searchDataImportantString("Nuevo", resultDataMotos, false, false);		
		
		// AHORA OBTENEMOS LAS MOTOS NUEVAS --------------------------------------------------------
		do {
				String resultDataMotosNuevas = MercadoLibreServiceImpl.searchMotosNuevas(idMotoCategoria, idArticulosNuevos);
				if(resultDataMotosNuevas == null) {
					return new ResponseEntity<Map<String, Double>>(null, headers ,HttpStatus.BAD_REQUEST);
				}
				mercadoLibreServiceImpl.searchDataImportantString(null, resultDataMotosNuevas, true, true);
				
		}while(mercadoLibreServiceImpl.offset <= 1000);
		
		Map<String, Double> mapBrandAndPrice = mercadoLibreServiceImpl.mapBrandAndPrice;
		Map<String, Long> mapBrandAndAccount = mercadoLibreServiceImpl.mapBrandAndAccount;
	
		// 	CALCULAMOS LOS PROMEDIOS, MAPEAMOS POR LAS LLAVES
		Collection<String> setKeys = mapBrandAndPrice.keySet();
		
		for(String key: setKeys) {
			// limitamos a dos decimales
			DecimalFormat decimalFormat = new DecimalFormat("#.00");
			mapBrandAndPrice.replace(key, new Double(decimalFormat.format( mapBrandAndPrice.get(key) / mapBrandAndAccount.get(key))) );
			
		}
		
		System.out.println(mapBrandAndPrice);
		
		return new ResponseEntity<Map<String, Double>>(mapBrandAndPrice, headers, HttpStatus.OK);
	};
	

	
}
