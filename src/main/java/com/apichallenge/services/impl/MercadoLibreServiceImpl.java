package com.apichallenge.services.impl;

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import com.apichallenge.models.Categories;
import com.apichallenge.services.MercadoLibreService;

@Service
public class MercadoLibreServiceImpl implements MercadoLibreService{

	private static Logger LOG = LogManager.getLogger(Categories.class);
	
	private static String urlMELI = "https://api.mercadolibre.com/";
	
	private static String siteAR = "MLA";
	
	public Map<String, Double> mapBrandAndPrice = new HashMap<String, Double>();
	
	public Map<String, Long> mapBrandAndAccount = new HashMap<String, Long>();
	
	public static Long offset = new Long(0);
	public Long totalRegistros = null;
	
	public static String peticionHttpMercadoLibre(String url) throws Exception {
		String resultPeticionHttp = peticionHttp(url);
        
		if(resultPeticionHttp != null) {
			return resultPeticionHttp;
		}
		
        return null;
	}
	
	public static String searchMotosNuevas(String idMotos, String idArticulosNuevos) throws Exception {
		 String url = urlMELI + "sites/" + siteAR + "/search?category=" + idMotos + "&ITEM_CONDITION=" + idArticulosNuevos + "&offset=" + offset.toString();
		 
		 String resultPeticionHttp = peticionHttp(url);
	        
			if(resultPeticionHttp != null) {
				return resultPeticionHttp;
			}
			
	        return null;
	}
	
	public static String peticionHttp(String url) throws Exception {
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
        try {
        	HttpGet request = new HttpGet(url);

            CloseableHttpResponse response = httpclient.execute(request);
            try {
            	HttpEntity entity = response.getEntity();
                if (entity != null) {

                    String result = EntityUtils.toString(entity);
                    return result;
                }
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
        
        LOG.error("Error al obtener datos de api mercado libre");
        return null;
	}
	
	public String searchDataImportantString (String keyImportant, String jsonConvert, boolean obtenerMotos, boolean obtenerData) {
		
		final JsonParser parser = Json.createParser(new StringReader( jsonConvert ));
		
		String key = null;
		String idImportant = null;
        String value = null;
		
		while (parser.hasNext()) {
            final Event event = parser.next();
            switch (event) {
            case KEY_NAME:
            	if(obtenerMotos) break;
            	key = parser.getString();
                break;
                
            case VALUE_STRING:
            	if(obtenerMotos) break;
                value = parser.getString();
                if( value.contains(keyImportant) )  {
                	mapBrandAndPrice.clear();
            		mapBrandAndAccount.clear();
                	return idImportant;
                }
                idImportant = value;
                break;
             
            case START_ARRAY:
            	if(!obtenerMotos) break;
            	JsonArray jsonArrayData = parser.getArray();
            	
            	if(!obtenerData) {
            		for(int i=0; i<jsonArrayData.size(); i++) {
            			JsonObject objectCategoriesById = jsonArrayData.get(i).asJsonObject();
            			String name = objectCategoriesById.get("name").toString();
            			if(name.contains(keyImportant) && !name.contains("Autos")) {
            				return objectCategoriesById.get("id").toString();
            			}
            		}	
            	} else {
            		if(!jsonArrayData.isEmpty()) {
            			finalScore(jsonArrayData);
            			offset += 50;
            			return null;
            		}
            	}
            	
            case START_OBJECT:
            	if(!obtenerMotos) break;
            	if(obtenerData && totalRegistros == null) {
            		JsonObject json = parser.getObject();
            		JsonObject jsonPagginations = json.get("paging").asJsonObject();
            		
            		totalRegistros = Long.parseLong( jsonPagginations.get("total").toString() );          		
            	}
            	break;
            }
		}
           
		
		return idImportant;
	};
	
	public void finalScore(JsonArray listArticulosMotosNuevas) {
		
		for(int i=0; i<listArticulosMotosNuevas.size(); i++) {
			
			JsonObject objectArticulo = listArticulosMotosNuevas.get(i).asJsonObject();
			
			Double priceArticulo = Double.parseDouble(objectArticulo.get("price").toString());
			
			JsonArray jsonAtributesArticulo = objectArticulo.get("attributes").asJsonArray();			
			
			for(int j=0; j<jsonAtributesArticulo.size(); j++) {
				String id = jsonAtributesArticulo.get(j).asJsonObject().get("id").toString();
				
				if(id.contains("BRAND")) {
					String marcaItem = jsonAtributesArticulo.get(j).asJsonObject().get("value_name").toString();
					marcaItem = marcaItem.substring(1, marcaItem.length()-1);
					Collection<String> object = mapBrandAndPrice.keySet();
					
					if(object.size() == 0) {
						mapBrandAndPrice.put( marcaItem, priceArticulo);
						mapBrandAndAccount.put( marcaItem, new Long(1));
					} else {
						boolean bandera = true;
						for(String key: object) {
							if(!key.contains(marcaItem)) {
								bandera = false;
							} else {
								bandera = true;
								break;
							}
						}
						if(!bandera) {
							mapBrandAndPrice.put( marcaItem, priceArticulo);
							mapBrandAndAccount.put( marcaItem, new Long(1));
						} else {
							mapBrandAndPrice.replace(marcaItem, priceArticulo + mapBrandAndPrice.get(marcaItem) );
							mapBrandAndAccount.replace( marcaItem, mapBrandAndAccount.get(marcaItem) + 1);
						}
					}
				}
			}
			
			
		}
	};

}
