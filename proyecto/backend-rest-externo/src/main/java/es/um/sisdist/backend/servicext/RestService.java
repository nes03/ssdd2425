package es.um.sisdist.backend.servicext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import es.um.sisdist.backend.grpc.GrpcServiceClient;

@SpringBootApplication
@ComponentScan(basePackages = "es.um.sisdist.backend")
@RestController // Retorna JSON o texto plano y se usa en APIs REST
// @RequestMapping("/u/{userId}/dialogue")
public class RestService {
	private final GrpcServiceClient grpcServiceClient;

	// Constructor que recibe un cliente gRPC
	public RestService(GrpcServiceClient grpcServiceClient) {
		this.grpcServiceClient = grpcServiceClient;
	}

	// Metodo para la ruta raíz "/"
	@GetMapping("/")
	public ResponseEntity<String> root() {
		String message = "Bienvenido al servicio externo REST de prompt";
		String message2 = " - Este servidor hace uso del servidor interno gRPC";
		String message3 = "Este servicio permite crear, eliminar y continuar con conversaciones";
		String message4 = "Asegúrese de autenticar la solicitud con el Auth-Token correspondiente";
		String message5 = "Por favor, use /u/{userId}/dialogue para interactuar.\n";

		return ResponseEntity
				.ok(message + "<br>" + message2 + "<br><br>" + message3 + "<br>" + message4 + "<br><br>" + message5);
	}

	// Método para crear un nuevo prompt
	// @PostMapping
	@PostMapping("/u/{userId}/dialogue")
	public ResponseEntity<String> createPrompt(
			@PathVariable String userId,
			@RequestBody String prompt,
			@RequestHeader("Date") String date,
			@RequestHeader("Auth-Token") String authToken) throws NoSuchAlgorithmException {
		// HttpServletRequest request) throws NoSuchAlgorithmException {
		if (!validateAuthToken(date, authToken, userId))
			return ResponseEntity.status(401).body("Unauthorized");

		// Crear el prompt y recibir el token
		grpcServiceClient.sendPromptAndFetchResponse(prompt);

		// Retornar un código 202 Accepted con el token de la conversación
		return ResponseEntity.status(202).header("Location", "/u/" + userId + "/dialogue/response/" + "some_token")
				.body("Prompt accepted");
	}

	// Método para continuar la conversación con un token
	// @GetMapping("/response/{token}")
	@GetMapping("/u/{userId}/dialogue/response/{token}")
	public ResponseEntity<String> continueConversation(
			@PathVariable String token,
			@PathVariable String userId, // Se agrega userId como un parámetro en la URL
			@RequestHeader("Date") String date,
			@RequestHeader("Auth-Token") String authToken) throws NoSuchAlgorithmException {

		// Validar el token de autenticación
		if (!validateAuthToken(date, authToken, userId))
			return ResponseEntity.status(401).body("Unauthorized");

		// Crear un CountDownLatch para sincronización
		CountDownLatch latch = new CountDownLatch(1);

		// Llamar a fetchResponse que hace el trabajo en segundo plano
		grpcServiceClient.fetchResponse(token, latch);

		// Esperar a que el proceso se complete
		try {
			latch.await(); // Esto espera hasta que el latch se decremente
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Manejar la interrupción
			return ResponseEntity.status(500).body("Error during processing");
		}

		// Si la respuesta está lista, devolverla
		String response = grpcServiceClient.getResponseForREST(); // Necesitas implementar esto

		// Verificar si la respuesta es nula o vacía
		if (response != null && !response.isEmpty())
			return ResponseEntity.ok(response); // Responde con 200 OK y la respuesta
		else
			return ResponseEntity.status(204).build(); // No Content en caso de que aún no haya respuesta

	}

	// Validar el AuthToken
	private boolean validateAuthToken(String date, String authToken, String userId)
			throws NoSuchAlgorithmException {
		// Por ejemplo, si la URL está construida de alguna forma, puedes crearla como
		// lo hagas en tu lógica
		String url = "http://localhost:8180/u/" + userId + "/dialogue"; // ejemplo de URL

		// Generar el token de autenticación usando la URL, la fecha y el userId
		String userToken = generateAuthToken(url, date, userId);

		// Comparar el token generado con el que se ha enviado en el encabezado de la
		// solicitud
		return userToken.equals(authToken);
	}

	// Método para generar el token MD5
	private String generateAuthToken(String url, String date, String userId) throws NoSuchAlgorithmException {
		String privateToken = getPrivateTokenForUser(userId); // Suponiendo que tienes un método para obtener el token
																// privado del usuario
		String data = url + date + privateToken;

		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] digest = md.digest(data.getBytes());

		StringBuilder sb = new StringBuilder();
		for (byte b : digest)
			sb.append(String.format("%02x", b));

		return sb.toString();
	}

	// Método para obtener el TOKEN privado para un usuario
	private String getPrivateTokenForUser(String userId) {
		// Lógica para obtener el token privado (en este caso, está hardcodeado)
		return "private_token_for_user_" + userId;
	}

	public static void main(String[] args) {
		SpringApplication.run(RestService.class, args);
	}
}
