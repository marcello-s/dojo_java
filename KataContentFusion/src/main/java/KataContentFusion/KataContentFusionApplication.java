/*
 * The MIT License, Copyright (c) 2011-2026 Marcel Schneider
 * for details see License.txt
 */

package KataContentFusion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class KataContentFusionApplication {

	private static ApplicationContext ctx;
	private static final Logger log = LoggerFactory.getLogger(KataContentFusionApplication.class);

	public static void main(String[] args) {
		ctx = SpringApplication.run(KataContentFusionApplication.class, args);
		var dispatcher = ctx.getBean(Dispatching.class);
		log.info("dispatch");
		dispatcher.Dispatch(args);
	}
}
