package org.acme;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.ThreadLocalRandom;

@Path("/joke")
public class JokeResource {

    @Inject
    @RestClient
    ChuckNorrisService chuckNorrisService;

    @Inject
    @RestClient
    JokesService jokesService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String joke() {

        return ThreadLocalRandom.current().nextBoolean() ?
          chuckNorrisService.randomQuote() :
          jokesService.randomJoke();
    }
}
