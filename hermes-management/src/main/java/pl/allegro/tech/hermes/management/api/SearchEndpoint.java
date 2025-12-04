package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.api.SearchResults;
import pl.allegro.tech.hermes.management.domain.search.SearchQuery;
import pl.allegro.tech.hermes.management.domain.search.SearchService;

@Path("search")
public class SearchEndpoint {

  private final SearchService searchService;

  @Autowired
  public SearchEndpoint(SearchService searchService) {
      this.searchService = searchService;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/query")
  @ApiOperation(
          value = "Search",
          response = List.class,
          httpMethod = HttpMethod.GET)
  public SearchResults query(
      @QueryParam("q") String query
  ) {
    SearchQuery searchQuery = new SearchQuery(query);
    return searchService.search(searchQuery);
  }
}
