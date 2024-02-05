package com.ays.theatre.crawler.core.resources;

import java.time.OffsetDateTime;
import java.util.List;

import com.ays.theatre.crawler.core.dao.TheatrePlayDao;
import com.ays.theatre.crawler.core.utils.Constants;
import com.ays.theatre.crawler.theatreartbg.job.TheatreArtBgRunner;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/theatre/play")
public class TheatrePlayResource {

    private final TheatrePlayDao theatrePlayDao;
    private final TheatreArtBgRunner theatreArtBgJob;

    public TheatrePlayResource(TheatrePlayDao theatrePlayDao, TheatreArtBgRunner theatreArtBgJob) {
        this.theatrePlayDao = theatrePlayDao;
        this.theatreArtBgJob = theatreArtBgJob;
    }

    @GET
    @Path("/all")
    public List<String> getTheatrePlays() {
        return theatrePlayDao.getTheatrePlaysByOrigin(Constants.THEATRE_ART_BG_ORIGIN, OffsetDateTime.now());
    }

    @POST
    @Path("/discover")
    public Response runDiscovery() {
        theatreArtBgJob.run();
        return Response.ok().build();
    }

}
