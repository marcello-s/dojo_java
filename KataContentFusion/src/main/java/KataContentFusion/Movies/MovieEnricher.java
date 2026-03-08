/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.*;
import KataContentFusion.LocalDb.Movie;
import KataContentFusion.LocalDb.Repos.*;
import KataContentFusion.MovieDb.CreditResponse;
import KataContentFusion.MovieDb.MovieDbClient;
import KataContentFusion.MovieDb.MovieResponse;
import KataContentFusion.MovieDb.PersonResponse;
import KataContentFusion.MovieDb.SearchResponse;
import KataContentFusion.MovieDb.SearchResult;

@Component
public class MovieEnricher {

    // example: "The Matrix (1999) [en]"
    // "^(.+)(\(\d{4}\))\[(.+)\]$"gm
    // Java syntax requires double escaping of backslashes, and we want to ignore case when matching, "^(.+)\\s+(\\(\\d{4}\\))\\s+\\[(.+)\\]$"
    private static final Pattern titlePattern = Pattern.compile("^(.+)\\s+(\\(\\d{4}\\))\\s+\\[(.+)\\]$", Pattern.CASE_INSENSITIVE);
    
    private final MovieDbClient movieDbClient;
    private final ScanNameRepository scanNameRepo;
    private final ScanNameMovieRepository scanNameMovieRepo;
    private final MovieRepository movieRepo;
    private final GenreRepository genreRepo;
    private final MovieGenreRepository movieGenreRepo;
    private final ProductionCompanyRepository productionCompanyRepo;
    private final MovieProductionCompanyRepository movieProductionCompanyRepo;
    private final ProductionCountryRepository productionCountryRepo;
    private final MovieProductionCountryRepository movieProductionCountryRepo;
    private final RoleRepository roleRepo;
    private final CastRepository castRepo;
    private final MovieCastRepository movieCastRepo;
    private final PersonRepository personRepo;

    public MovieEnricher(
        MovieDbClient movieDbClient,
        ScanNameRepository scanNameRepo,
        ScanNameMovieRepository scanNameMovieRepo,
        MovieRepository movieRepo,
        GenreRepository genreRepo,
        MovieGenreRepository movieGenreRepo,
        ProductionCompanyRepository productionCompanyRepo,
        MovieProductionCompanyRepository movieProductionCompanyRepo,
        ProductionCountryRepository productionCountryRepo,
        MovieProductionCountryRepository movieProductionCountryRepo,
        RoleRepository roleRepo,
        CastRepository castRepo,
        MovieCastRepository movieCastRepo,
        PersonRepository personRepo)
        {
            this.movieDbClient = movieDbClient;
            this.scanNameRepo = scanNameRepo;
            this.scanNameMovieRepo = scanNameMovieRepo;
            this.movieRepo = movieRepo;
            this.genreRepo = genreRepo;
            this.movieGenreRepo = movieGenreRepo;
            this.productionCompanyRepo = productionCompanyRepo;
            this.movieProductionCompanyRepo = movieProductionCompanyRepo;
            this.productionCountryRepo = productionCountryRepo;
            this.movieProductionCountryRepo = movieProductionCountryRepo;
            this.roleRepo = roleRepo;
            this.castRepo = castRepo;
            this.movieCastRepo = movieCastRepo;
            this.personRepo = personRepo;
        }

    public void Enrich(Integer maxCount) {
    
        var scanNamesToEnrich = scanNameRepo.getScanNamesWithoutMovies(PageRequest.of(0, maxCount));        

        for(var scanName : scanNamesToEnrich) {
            System.out.println(scanName.name);
            var matcher = titlePattern.matcher(scanName.name);
            if (matcher.find()) {
                var title = matcher.group(1);
                var year = matcher.group(2);
                                
                var searchResponse = searchMovies(title);
                if (searchResponse.results() == null || searchResponse.results().length == 0) {
                    System.out.println("no matches found");
                    continue;
                }
                
                var searchMatches = filterSearchResults(searchResponse, title, year);

                for(var match : searchMatches) {

                    try {
                        var movieDetails = getMovieDetail(match.id());
                        System.out.format("movie details: %s (%s) [%s]%n", movieDetails.title(), movieDetails.release_date(), movieDetails.id());
                        enrichMovie(movieDetails, scanName.id);

                        TimeUnit.SECONDS.sleep(1); // avoid hitting rate limits
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private SearchResponse searchMovies(String title) {

        System.out.format("searching for: %s%n", title);
        return movieDbClient.searchMovies(title);
    }

    private List<SearchResult> filterSearchResults(SearchResponse searchResponse, String title, String year) {
        
        // exact matches for title and year
        var exactMatches = Arrays.stream(searchResponse.results())
            .filter(r -> r.title().equalsIgnoreCase(title))
            .filter(r -> r.release_date() != null && r.release_date().startsWith(year.substring(1, 5)))
            .toList();

        if (exactMatches.size() > 0) {
            return exactMatches;
        }

        // exact matches for original title and year
        var exactOriginalTitleMatches = Arrays.stream(searchResponse.results())
            .filter(r -> r.original_title().equalsIgnoreCase(title))
            .filter(r -> r.release_date() != null && r.release_date().startsWith(year.substring(1, 5)))
            .toList();

        if (exactOriginalTitleMatches.size() > 0) {
            return exactOriginalTitleMatches;
        }

       // remove punctuation and compare titles
        var normalizedTitle = title.replaceAll("\\p{Punct}", "").toLowerCase();
        var normalizedMatches = Arrays.stream(searchResponse.results())
            .filter(r -> r.title().replaceAll("\\p{Punct}", "").equalsIgnoreCase(normalizedTitle))
            .filter(r -> r.release_date() != null && r.release_date().startsWith(year.substring(1, 5)))
            .toList();

        if (normalizedMatches.size() > 0) {
            return normalizedMatches;
        } else {
            return Arrays.asList(searchResponse.results());
        }         
    }

    private MovieResponse getMovieDetail(Integer movieId) {

        return movieDbClient.getMovieDetail(movieId);
    }

    private void enrichMovie(MovieResponse movieDetails, Integer scanNameId) {

        var movieEntity = getExistingMovieByExternalIdOrNew(movieDetails.id());
        mapMovieResponseToMovieEntity(movieDetails, movieEntity);
        movieRepo.save(movieEntity);

        addGenresToMovie(movieDetails.genres(), movieEntity);
        addProductionCompaniesToMovie(movieDetails.production_companies(), movieEntity);
        addProductionCountriesToMovie(movieDetails.production_countries(), movieEntity);

        var credits = getMovieCredits(movieDetails.id());
        addCastToMovie(credits.cast(), movieEntity);
        addCastToMovie(credits.crew(), movieEntity);

        addMovieToScanName(scanNameId, movieEntity);
    }

    private KataContentFusion.LocalDb.Movie getExistingMovieByExternalIdOrNew(Integer externalId) {

        var existingMovies = movieRepo.findByExternalId(externalId);
        if (existingMovies.size() > 0) {
            return existingMovies.get(0);
        } else {
            return new KataContentFusion.LocalDb.Movie();
        }
    }

    private  void mapMovieResponseToMovieEntity(
        MovieResponse movieResponse, 
        KataContentFusion.LocalDb.Movie movieEntity) {
        
        var releaseDate = LocalDate.parse(movieResponse.release_date());
        movieEntity.externalId = movieResponse.id();
        movieEntity.title = movieResponse.title();
        movieEntity.adult = movieResponse.adult();
        movieEntity.budget = movieResponse.budget();
        movieEntity.imdbId = movieResponse.imdb_id();
        movieEntity.originalLanguage = movieResponse.original_language();
        movieEntity.originalTitle = movieResponse.original_title();
        movieEntity.overview = movieResponse.overview();
        movieEntity.popularity = movieResponse.popularity();
        movieEntity.releaseDate = releaseDate.atStartOfDay();
        movieEntity.revenue = movieResponse.revenue();
        movieEntity.runtime = movieResponse.runtime();
        movieEntity.status = movieResponse.status();
        movieEntity.tagline = movieResponse.tagline();
        movieEntity.voteAverage = movieResponse.vote_average();
        movieEntity.voteCount = movieResponse.vote_count();        
    }

    private void addGenresToMovie(
        KataContentFusion.MovieDb.Genre[] genres, 
        KataContentFusion.LocalDb.Movie movieEntity) {

        for(var genre : genres) {

            var genreDb = genreRepo.findByExternalId(genre.id());
            var genreEntity = new Genre();
            if (genreDb.size() > 0) {
                genreEntity = genreDb.get(0);
            } 

            genreEntity.externalId = genre.id();
            genreEntity.name = genre.name();
            genreEntity = genreRepo.save(genreEntity);

            var movieGenreDb = movieGenreRepo.findByMovieIdAndGenreId(movieEntity.id, genreEntity.id);
            if (movieGenreDb.size() > 0) {
                continue;
            }

            var movieGenreEntity = new MovieGenre();
            movieGenreEntity.movie = movieEntity;
            movieGenreEntity.genre = genreEntity;
            movieGenreRepo.save(movieGenreEntity);
        }
    }

    private void addProductionCompaniesToMovie(
        KataContentFusion.MovieDb.ProductionCompany[] productionCompanies, 
        KataContentFusion.LocalDb.Movie movieEntity) {        

        for(var productionCompany : productionCompanies) {

            var productionCompanyDb = productionCompanyRepo.findByExternalId(productionCompany.id());
            var productionCompanyEntity = new ProductionCompany();
            if (productionCompanyDb.size() > 0) {
                productionCompanyEntity = productionCompanyDb.get(0);
            } 

            productionCompanyEntity.externalId = productionCompany.id();
            productionCompanyEntity.name = productionCompany.name();
            productionCompanyEntity.logoPath = productionCompany.logo_path();
            productionCompanyEntity.originCountry = productionCompany.origin_country();
            productionCompanyEntity = productionCompanyRepo.save(productionCompanyEntity);

            var movieProductionCompanyDb = movieProductionCompanyRepo.findByMovieIdAndProductionCompanyId(movieEntity.id, productionCompanyEntity.id);
            if (movieProductionCompanyDb.size() > 0) {
                continue;
            }

            var movieProductionCompanyEntity = new MovieProductionCompany();
            movieProductionCompanyEntity.movie = movieEntity;
            movieProductionCompanyEntity.productionCompany = productionCompanyEntity;
            movieProductionCompanyRepo.save(movieProductionCompanyEntity);
        }
    }

    private void addProductionCountriesToMovie(
        KataContentFusion.MovieDb.ProductionCountry[] productionCountries, 
        KataContentFusion.LocalDb.Movie movieEntity) {
        
        for(var productionCountry : productionCountries) {

            var productionCountryDb = productionCountryRepo.findByName(productionCountry.name());
            var productionCountryEntity = new ProductionCountry();
            if (productionCountryDb.size() > 0) {
                productionCountryEntity = productionCountryDb.get(0);
            } 

            productionCountryEntity.iso3166_1 = productionCountry.iso_3166_1();
            productionCountryEntity.name = productionCountry.name();
            productionCountryEntity = productionCountryRepo.save(productionCountryEntity);

            var movieProductionCountryDb = movieProductionCountryRepo.findByMovieIdAndProductionCountryId(movieEntity.id, productionCountryEntity.id);
            if (movieProductionCountryDb.size() > 0) {
                continue;
            }

            var movieProductionCompanyEntity = new MovieProductionCompany();
            movieProductionCompanyEntity.movie = movieEntity;
            movieProductionCompanyEntity.productionCompany = null;
            movieProductionCompanyRepo.save(movieProductionCompanyEntity);
        }
    }

    private CreditResponse getMovieCredits(Integer movieId) {

        return movieDbClient.getMovieCredits(movieId);
    }

    private void addCastToMovie(
        KataContentFusion.MovieDb.Cast[] casts, 
        KataContentFusion.LocalDb.Movie movieEntity) {
        
        for(var cast : casts) {

            var roleEntity = getOrAddRole(cast.known_for_department());
            var castDb = castRepo.findByExternalId(cast.id());
            var castEntity = new Cast();
            if (castDb.size() > 0) {
                castEntity = castDb.get(0);
            } 

            castEntity.externalId = cast.id();
            castEntity.name = cast.name();
            castEntity.originalName = cast.original_name();
            castEntity.adult = cast.adult();
            castEntity.gender = cast.gender();
            castEntity.role = roleEntity;
            castEntity.popularity = cast.popularity();
            castEntity.profilePath = cast.profile_path();
            castEntity.castId = cast.cast_id();            
            castEntity.character = cast.character();
            castEntity.creditId = cast.credit_id();
            castEntity.orderNo = cast.order();
            castEntity = castRepo.save(castEntity);

            addPerson(cast.id(), roleEntity);

            var movieCastDb = movieCastRepo.findByMovieIdAndCastId(movieEntity.id, castEntity.id);
            if (movieCastDb.size() > 0) {
                continue;
            } 

            var movieCastEntity = new MovieCast();
            movieCastEntity.movie = movieEntity;
            movieCastEntity.cast = castEntity;
            movieCastRepo.save(movieCastEntity);            
        }
    }

    private KataContentFusion.LocalDb.Role getOrAddRole(String roleName) {

        var roleDb = roleRepo.findByName(roleName);
        if (roleDb.size() > 0) {
            return roleDb.get(0);
        } else {
            var roleEntity = new Role();
            roleEntity.name = roleName;
            return roleRepo.save(roleEntity);
        }
    }

    private void addPerson(Integer externalId, Role role) {

        var personEntity = getPersonOrNew(externalId);
        if (personEntity.id != null) {
            return; // person already exists, no need to update role
        }

        var personResponse = getPersonById(externalId);
        mapPersonResponseToPersonEntity(personResponse, personEntity, role);
        personRepo.save(personEntity);
    }

    private void mapPersonResponseToPersonEntity(PersonResponse personResponse, Person personEntity, Role role) {
        
        var birtday = personResponse.birthday() != null ? LocalDate.parse(personResponse.birthday()) : null;
        var deathday = personResponse.deathday() != null ? LocalDate.parse(personResponse.deathday()) : null;

        personEntity.externalId = personResponse.id();
        personEntity.adult = personResponse.adult();
        personEntity.biography = personResponse.biography();
        personEntity.birthday = birtday != null ? birtday.atStartOfDay() : null;
        personEntity.deathday = deathday != null ? deathday.atStartOfDay() : null;
        personEntity.gender = personResponse.gender();
        personEntity.homepage = personResponse.homepage();
        personEntity.imdbId = personResponse.imdb_id();
        personEntity.role = role;
        personEntity.name = personResponse.name();
        personEntity.placeOfBirth = personResponse.place_of_birth();
        personEntity.popularity = personResponse.popularity();
        personEntity.profilePath = personResponse.profile_path();
    }

    private Person getPersonOrNew(Integer externalId) {

        var personDb = personRepo.findByExternalId(externalId);
        if (personDb.size() > 0) {
            return personDb.get(0);
        } else {
            return new Person();
        }
    }

    private PersonResponse getPersonById(Integer personId) {

        return movieDbClient.getPersonDetail(personId);
    }

    private void addMovieToScanName(Integer scanNameId, Movie movieEntity) {
        
        var scanNameMovieDb = scanNameMovieRepo.findByScanNameIdAndMovieId(scanNameId, movieEntity.id);
        if (scanNameMovieDb.size() > 0) {
            return;
        }

        var scanNameMovieEntity = new ScanNameMovie();
        scanNameMovieEntity.scanName = scanNameRepo.findById(scanNameId).orElseThrow();
        scanNameMovieEntity.movie = movieEntity;
        scanNameMovieRepo.save(scanNameMovieEntity);
    }
}
