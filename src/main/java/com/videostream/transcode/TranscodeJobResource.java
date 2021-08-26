package com.videostream.transcode;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/transcode/job")
public class TranscodeJobResource {
    @POST
    @Produces({MediaType.TEXT_PLAIN})
    @Consumes({MediaType.APPLICATION_JSON})
    public void submitTranscodeJob(
                    @Suspended final AsyncResponse asyncResponse,
                    @Context Vertx vertx,
                    String request)
    {
        vertx.eventBus().<String>request("transcodejob", request, msg -> {
            if (msg.succeeded()) {
                asyncResponse.resume(Response.ok().entity(msg.result().body().toString()).toString());
//                asyncResponse.resume(Response.status(Response.Status.OK).build(msg.result().body().toString()));
            } else {
                asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            }
        });

    }
}
