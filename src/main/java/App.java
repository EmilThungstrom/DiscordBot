import bot.Bottinator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class App {

    static Bottinator bottinator;

    public static void main(String[] args)
    {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
        bottinator = context.getBean("bottinator", Bottinator.class);
    }
}
