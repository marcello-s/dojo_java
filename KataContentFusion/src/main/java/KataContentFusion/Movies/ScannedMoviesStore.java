/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion.Movies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class ScannedMoviesStore {

    private final String filePath = "C:\\Temp\\scannedMovies.json";

    public void Serialize(MovieVolume movieVolume) {

        var movieVolumes = new ArrayList<MovieVolume>();
        movieVolumes.add(movieVolume);
        var mapper = new ObjectMapper();
        var module = new SimpleModule();
        module.addSerializer(Path.class, new ToStringSerializer());
        mapper.registerModule(module);

        try {
            var json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(movieVolumes);

            try (var writer = Files.newBufferedWriter(Path.of(filePath))) {
                writer.write(json.toString());
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<MovieVolume> Deserialize(String path) {
        
        var mapper = new ObjectMapper();
        var module = new SimpleModule();
        module.addSerializer(Path.class, new ToStringSerializer());
        mapper.registerModule(module);

        try {
            var movieVolumes = mapper.readValue(new File(path), new TypeReference<ArrayList<MovieVolume>>() {});

            return movieVolumes;
        } catch (Exception e) {
            e.printStackTrace();

            return new ArrayList<MovieVolume>();
        }        
    }
}
