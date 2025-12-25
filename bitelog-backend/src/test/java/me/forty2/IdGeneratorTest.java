package me.forty2;

import me.forty2.utils.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class IdGeneratorTest {

    @Autowired
    private IdGenerator idGenerator;

    @Test
    public void generateTest() {
        String service = "test";

        try (ExecutorService es = Executors.newFixedThreadPool(100)) {
            Runnable task = () -> {
                for (int i = 0; i < 100; i++) {
                    System.out.println(idGenerator.nextId(service));
                }
            };

            for (int i = 0; i < 200; i++) {
                es.submit(task);
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
