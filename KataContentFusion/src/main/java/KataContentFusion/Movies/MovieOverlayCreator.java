/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

import KataContentFusion.LocalDb.Repos.MovieRepository;
import KataContentFusion.Tools.ImageMagickUtil;

@Component
public class MovieOverlayCreator {
    
    private static final String WorkingPath = "C:/Temp/cf/";
    private static final String FontPathTitle = "Lato-Bold";
    private static final String FontPathYear = "Lato-Italic";
    private static final Integer Margin = 10;

    private final MovieRepository movieRepository;

    public MovieOverlayCreator(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public void createOverlayForMovie(Integer movieId) {
        
        var movies = movieRepository.findById(movieId);
        if (movies == null) {
            System.out.println("no movie found for id: " + movieId);
            return;
        }

        var movie = movies.get();
        System.out.println("creating overlay for movie " + movie.title);

        var movieClipPath = ensureMovieClipPath(WorkingPath, movieId);
        var titleSize = ImageMagickUtil.measureText(movie.title.replace(" ", ".") , FontPathTitle, 100);
        var yearSize = ImageMagickUtil.measureText("(" + movie.releaseDate.getYear()+ ")", FontPathYear, 80);
        System.out.println("title size: " + titleSize.width() + "x" + titleSize.height());
        System.out.println("year size: " + yearSize.width() + "x" + yearSize.height());
        ImageMagickUtil.createTextImage(titleSize.width() + Margin, titleSize.height() + Margin, movie.title, FontPathTitle, 100, movieClipPath + "title.png");
        ImageMagickUtil.createTextImage(yearSize.width() + Margin, yearSize.height() + Margin, "(" + movie.releaseDate.getYear() + ")", FontPathYear, 80, movieClipPath + "year.png");

        var overlayWidth = Math.max(titleSize.width(), yearSize.width());
        var overlayHeight = titleSize.height() + yearSize.height();
        System.out.println("overlay size: " + overlayWidth + "x" + overlayHeight);
        ImageMagickUtil.combineImages(overlayWidth, overlayHeight, movieClipPath + "title.png", movieClipPath + "year.png", 0, yearSize.height(), movieClipPath + "overlay.png");
    }

    private String ensureMovieClipPath(String workingPath, Integer movieId) {

        var moviePath = workingPath + movieId + "/";
        var path = Path.of(moviePath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                return moviePath;
            }
            catch (IOException ex) {
                System.out.println("Error creating directory: " + path);
            }
        }

        return moviePath;
    }
}
