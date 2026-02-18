/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */


package KataContentFusion.MovieDb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class MovieDbClientConfig {

    @Value("${themoviedb.api.bearer.token}")
    private String token;
    
    @Bean
    public MovieDbClient movieDbClient(RestClient.Builder builder) {

        var restClient = builder.baseUrl("https://api.themoviedb.org")
        .requestInterceptor((request, body, execution) -> {
            return execution.execute(request, body);
        })
        .defaultHeader("Authorization", "Bearer " + token)
        .build();

        var factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        return factory.createClient(MovieDbClient.class);
    }
}