package com.rinhaQuarkus.controller;

import com.rinhaQuarkus.jdbc.api.DataService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/purge-payments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class Purge {

    @Inject
    DataService service;

    @POST
    public Response purgeDatabase(){
    return Response.ok().build();
    }
}
