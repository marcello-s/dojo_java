/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.util.Collection;

public record MovieVolume(String volume, Collection<Movie> movies) {}
