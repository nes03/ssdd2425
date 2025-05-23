package es.um.sisdist.backend.Service.impl;

import java.util.Optional;
import java.util.logging.Logger;

import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.IDAOFactory;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.models.utils.UserUtils;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.GrpcServiceClient;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResponse;
import es.um.sisdist.backend.grpc.ResponseRequest;
import es.um.sisdist.backend.grpc.ResponseResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class AppLogicImpl {
    private static final Logger logger = Logger.getLogger(AppLogicImpl.class.getName());
    private final ManagedChannel channel;
    private final GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;

    private final GrpcServiceClient grpcClient; // Instancia de GrpcServiceClient

    private static final AppLogicImpl instance = new AppLogicImpl();

    private final IDAOFactory daoFactory;
    private final IUserDAO dao;

    private AppLogicImpl() {
        daoFactory = new DAOFactoryImpl();
        Optional<String> backend = Optional.ofNullable(System.getenv("DB_BACKEND"));

        if (backend.isPresent() && backend.get().equals("mongo")) {
            dao = daoFactory.createMongoUserDAO();
        } else {
            dao = daoFactory.createSQLUserDAO();
        }

        var grpcServerName = Optional.ofNullable(System.getenv("GRPC_SERVER"));
        var grpcServerPort = Optional.ofNullable(System.getenv("GRPC_SERVER_PORT"));

        channel = ManagedChannelBuilder
                .forAddress(grpcServerName.orElse("backend-grpc"), Integer.parseInt(
                        grpcServerPort.orElse("50051")))
                .usePlaintext()
                .build();

        blockingStub = GrpcServiceGrpc.newBlockingStub(channel);

        // grpcClient = new GrpcServiceClient();
    }

    public static AppLogicImpl getInstance() {
        return instance;
    }

    public Optional<User> getUserByEmail(String userId) {
        return dao.getUserByEmail(userId);
    }

    public Optional<User> getUserById(String userId) {
        return dao.getUserById(userId);
    }

    public boolean ping(int v) {
        logger.info("Enviando ping al servicio gRPC con valor: " + v);
        try {
            PingRequest request = PingRequest.newBuilder().setV(v).build();
            var response = blockingStub.ping(request);
            return response.getV() == v;
        } catch (Exception e) {
            logger.severe("Error durante el ping al servicio gRPC: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envía un prompt al servicio gRPC usando sendPrompt() y devuelve el token como
     * respuesta
     */
    public void fetchPromptResponse(String promptText) {
        logger.info("Enviando prompt al servicio gRPC: " + promptText);
        try {
            PromptRequest request = PromptRequest.newBuilder()
                    .setPrompt(promptText)
                    .build();
            PromptResponse response = blockingStub.sendPrompt(request);
            this.lastPromptResponse = response.getToken(); // Guardamos respuesta
        } catch (Exception e) {
            logger.severe("Error al procesar el prompt en el servicio gRPC: " + e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio gRPC", e);
        }
    }

    public String fetchPromptResponseSync(String promptText) {
        logger.info("Enviando prompt al servicio gRPC (sincrónico): " + promptText);
        try {
            PromptRequest request = PromptRequest.newBuilder()
                    .setPrompt(promptText)
                    .build();

            PromptResponse response = blockingStub.sendPrompt(request);

            return response.getToken();
        } catch (Exception e) {
            logger.severe("Error al procesar el prompt en el servicio gRPC: " + e.getMessage());
            throw new RuntimeException("Error al comunicarse con el servicio gRPC", e);
        }
    }

    public String getLastResponse_grpc() {
        return grpcClient.getLastResponse();
    }

    public Optional<User> checkLogin(String email, String pass) {
        Optional<User> u = dao.getUserByEmail(email);

        if (u.isPresent()) {
            String hashed_pass = UserUtils.md5pass(pass);
            if (hashed_pass.equals(u.get().getPassword_hash())) {
                return u;
            }
        }

        return Optional.empty();
    }
}