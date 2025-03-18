package es.um.sisdist.backend.grpc.impl;

import org.bson.json.JsonObject;
import org.bson.Document;

import com.mysql.cj.xdevapi.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
//import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;


import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PingResponse;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.ResponseRequest;
import es.um.sisdist.backend.grpc.ResponseResponse;
import io.grpc.stub.StreamObserver;

class GrpcServiceImpl extends GrpcServiceGrpc.GrpcServiceImplBase 
{
	private Logger logger;
	
    public GrpcServiceImpl(Logger logger) 
    {
		super();
		this.logger = logger;
	}

	@Override
	public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) 
	{
		logger.info("Recived PING request, value = " + request.getV());
		responseObserver.onNext(PingResponse.newBuilder().setV(request.getV()).build());
		responseObserver.onCompleted();
	}

	@Override
    public void sendPrompt(PromptRequest request, StreamObserver<PromptResponse> responseObserver) {
        String prompt = request.getPrompt();
        logger.info("Enviando prompt al servicio REST: " + prompt);

        try {
            // Hacer una petición POST al servicio REST en /prompt
            URL url = new URL("http://localhost:5020/prompt");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Crear el JSON con MongoDB JsonObject
        	// Se crea un String con el JSON, ya que MongoDB JsonObject es un envoltorio sobre el string JSON
        	String jsonString = "{\"prompt\":\"" + prompt + "\"}";
        	JsonObject json = new JsonObject(jsonString); // Crear JsonObject a partir del String

        	// Escribir el JSON en el cuerpo de la petición
        	try (OutputStream os = conn.getOutputStream()) {
            	os.write(json.toString().getBytes());
            	os.flush();
        	}

        	int responseCode = conn.getResponseCode();
        	if (responseCode == 202) {
            	// Obtener la URL de consulta desde la cabecera "Location"
            	String location = conn.getHeaderField("Location");
            	responseObserver.onNext(PromptResponse.newBuilder().setPrompt(location).build());
        	} else {
            	logger.warning("Error al enviar el prompt, código: " + responseCode);
            	responseObserver.onNext(PromptResponse.newBuilder().setPrompt("").build());
        	}
    	} catch (IOException e) {
        	logger.severe("Error en la comunicación con el servicio REST: " + e.getMessage());
        	responseObserver.onError(e);
    	} finally {
        	responseObserver.onCompleted();
    	}
	}

	@Override
	public void getResponse(ResponseRequest request, StreamObserver<ResponseResponse> responseObserver) {
		String url = request.getUrl();
		logger.info("Consultando respuesta en: " + url);
	
		try {
			URL responseUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) responseUrl.openConnection();
			conn.setRequestMethod("GET");
	
			int responseCode = conn.getResponseCode();
			if (responseCode == 204) {
				// Aún no hay respuesta
				responseObserver.onNext(ResponseResponse.newBuilder()
						.setCompleted(false)
						.build());
			} else if (responseCode == 200) {
				// Leer la respuesta JSON
				try (Scanner scanner = new Scanner(conn.getInputStream())) {
					String responseBody = scanner.useDelimiter("\\A").next();
	
					// Usar MongoDB Document para parsear la respuesta JSON
					Document json = Document.parse(responseBody);
	
					// Extraer los valores de los campos 'prompt' y 'answer'
					String prompt = json.getString("prompt");
					String answer = json.getString("answer");
	
					// Enviar la respuesta construida
					responseObserver.onNext(ResponseResponse.newBuilder()
							.setPrompt(prompt)  // Usamos el prompt obtenido del JSON
							.setAnswer(answer)  // Usamos la respuesta obtenida del JSON
							.setCompleted(true)  // Marcar como completado
							.build());
				}
			}
		} catch (IOException e) {
			logger.severe("Error al consultar la respuesta: " + e.getMessage());
			responseObserver.onError(e);
		} finally {
			responseObserver.onCompleted();
		}
    }


/*
	@Override
	public void storeImage(ImageData request, StreamObserver<Empty> responseObserver)
    {
		logger.info("Add image " + request.getId());
    	imageMap.put(request.getId(),request);
    	responseObserver.onNext(Empty.newBuilder().build());
    	responseObserver.onCompleted();
	}

	@Override
	public StreamObserver<ImageData> storeImages(StreamObserver<Empty> responseObserver) 
	{
		// La respuesta, sólo un objeto Empty
		responseObserver.onNext(Empty.newBuilder().build());

		// Se retorna un objeto que, al ser llamado en onNext() con cada
		// elemento enviado por el cliente, reacciona correctamente
		return new StreamObserver<ImageData>() {
			@Override
			public void onCompleted() {
				// Terminar la respuesta.
				responseObserver.onCompleted();
			}
			@Override
			public void onError(Throwable arg0) {
			}
			@Override
			public void onNext(ImageData imagedata) 
			{
				logger.info("Add image (multiple) " + imagedata.getId());
		    	imageMap.put(imagedata.getId(), imagedata);	
			}
		};
	}

	@Override
	public void obtainImage(ImageSpec request, StreamObserver<ImageData> responseObserver) {
		// TODO Auto-generated method stub
		super.obtainImage(request, responseObserver);
	}

	@Override
	public StreamObserver<ImageSpec> obtainCollage(StreamObserver<ImageData> responseObserver) {
		// TODO Auto-generated method stub
		return super.obtainCollage(responseObserver);
	}
	*/
}