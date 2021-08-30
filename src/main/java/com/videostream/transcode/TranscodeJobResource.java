package com.videostream.transcode;

import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/transcode/job")
public class TranscodeJobResource {

    final static Logger logger = LoggerFactory.getLogger(TranscodeJobResource.class);


    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public TranscodeJobDTO submitTranscodeJob(
                    // @Suspended final AsyncResponse asyncResponse, // no need to suspend the response as we won't be needing reply from event bus back
                    @Context Vertx vertx,
                    TranscodeJobDTO transcodeJobRequest)
    {
        logger.info("Transcode Job request submitted");
        vertx.eventBus().request("transcodejob", transcodeJobRequest);
        // no need to have a callback lambda as we won't be needing reply from event bus back
        // vertx.eventBus().<TranscodeJobDTO>request("transcodejob", transcodeJobRequest,
        //     msg -> {
        //         if (msg.succeeded()) {
        //             asyncResponse.resume(Response.ok().entity(msg.result().body()).toString());
        //         } else {
        //             asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        //         }
        //     }
        // );
        return transcodeJobRequest;
    }


}
