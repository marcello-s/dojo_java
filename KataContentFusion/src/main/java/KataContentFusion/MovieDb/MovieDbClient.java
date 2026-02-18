/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.MovieDb;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@HttpExchange("/3") // the API version
public interface MovieDbClient {
    
    @GetExchange("/search/movie")
    public SearchResponse searchMovies(@RequestParam("query") String query);

    @GetExchange("/movie/{movieId}") 
    public MovieResponse getMovieDetail (@PathVariable("movieId") Integer movieId);
}
