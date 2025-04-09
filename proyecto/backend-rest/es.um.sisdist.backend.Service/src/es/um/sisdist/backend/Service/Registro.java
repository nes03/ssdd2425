package es.um.sisdist.backend.Service;


import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.models.RegisterUser;
import es.um.sisdist.models.UserDTO;
import es.um.sisdist.models.UserDTOUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

// POJO, no interface no extends

@Path("/Registro")
public class Registro
{
    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterUser request) {
        // Crear un nuevo usuario a partir de los datos de la solicitud
        UserDTO user = new UserDTO(request.getEmail(), request.getEmail(), request.getPassword(), request.getName(), "prueba", 0);

        // Retornar la respuesta
        return Response.ok(UserDTOUtils.fromDTO(user)).build();
    }
}

