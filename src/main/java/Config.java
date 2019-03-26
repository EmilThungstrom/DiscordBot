import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan("bot")
@ComponentScan("random")
@ComponentScan("aspect")
@PropertySource("classpath:../resources/bot.properties")
public class Config {

}
