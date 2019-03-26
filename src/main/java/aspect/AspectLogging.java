package aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Aspect
@Component
public class AspectLogging {

    @Pointcut("execution(* random.Random.nextInt(..))")
    public void randomNextInt() { }

    @AfterReturning(pointcut="randomNextInt() && args(diceSize,..)", returning="randInt")
    public void randLogging(JoinPoint joinPoint, int diceSize, int randInt) throws Exception{

        String randLog = randInt + ",";
        Files.write(Paths.get("./d" + diceSize + ".txt"), randLog.getBytes(), StandardOpenOption.APPEND);
    }

}
