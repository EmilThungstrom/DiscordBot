package random;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomRandom implements random.Random {

    Random random;

    public RandomRandom(){
        this.random = new Random();
    }

    public int nextInt(int upperLimit){
        return random.nextInt(upperLimit);
    }

}
